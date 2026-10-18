package dev.allchineseinstructions.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real Brigadier and server regressions for Chinese selector syntax. */
@PrefixGameTestTemplate(false)
public final class ChineseSelectorGameTests {
    private static final String NAMESPACE = "allchineseinstructions";

    private ChineseSelectorGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        if (Boolean.getBoolean("allchineseinstructions.runTests")) {
            event.register(ChineseSelectorGameTests.class);
        }
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void allFiveChineseSelectors(GameTestHelper helper) throws CommandSyntaxException {
        String[][] aliases = {
                {"选择所有实体", "@e"}, {"选择所有玩家", "@a"},
                {"选择最近玩家", "@p"}, {"选择随机玩家", "@r"}, {"选择自身", "@s"}
        };
        for (String[] alias : aliases) {
            EntitySelector chinese = selector(helper, alias[0]);
            EntitySelector english = selector(helper, alias[1]);
            assertSelectorShape(helper, chinese, english, alias[0]);
            helper.assertTrue(chinese.usesSelector(), "Alias parsed as a player name: " + alias[0]);
        }
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        try {
            assertEntities(helper, selector(helper, "选择自身"), source(helper).withEntity(entity), List.of(entity));
            helper.assertTrue(selector(helper, "选择所有玩家").findPlayers(source(helper)).isEmpty(),
                    "Dedicated test server should not have connected players");
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSelectorFiltersAndSorting(GameTestHelper helper) throws CommandSyntaxException {
        String tag = unique("筛选");
        Entity near = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        Entity far = helper.spawn(EntityType.ARMOR_STAND, 3, 1, 1);
        near.addTag(tag);
        far.addTag(tag);
        near.setCustomName(Component.literal("近处测试实体"));
        far.setCustomName(Component.literal("远处测试实体"));
        CommandSourceStack source = source(helper).withPosition(near.position());
        try {
            assertEntities(helper, selector(helper, "选择所有实体[标签=" + tag
                    + ",类型=minecraft:armor_stand,排序=最近,数量=1]"), source, List.of(near));
            assertEntities(helper, selector(helper, "选择所有实体[标签=" + tag
                    + ",类型=minecraft:armor_stand,排序=最远,数量=1]"), source, List.of(far));
            assertEntities(helper, selector(helper, "@e[标签=" + tag
                    + ",type=minecraft:armor_stand,sort=最近,limit=1]"), source, List.of(near));
            assertEntities(helper, selector(helper, "选择所有实体[tag=" + tag
                    + ",名称=远处测试实体,类型=!minecraft:pig]"), source, List.of(far));
            assertEntities(helper, selector(helper, "选择所有实体[标签=" + tag + ",名称=!远处测试实体]"),
                    source, List.of(near));
            assertEntities(helper, selector(helper, "选择所有实体[标签=" + tag + ",类型=minecraft:pig]"),
                    source, List.of());
            for (String order : List.of("随机", "任意")) {
                List<? extends Entity> selected = selector(helper, "选择所有实体[标签=" + tag
                        + ",排序=" + order + ",数量=1]").findEntities(source);
                helper.assertTrue(selected.size() == 1 && (selected.get(0) == near || selected.get(0) == far),
                        "Sort/limit returned wrong entities: " + order);
            }
        } finally {
            near.discard();
            far.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSpatialAndRotationOptions(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        entity.setXRot(30);
        entity.setYRot(60);
        CommandSourceStack source = source(helper).withEntity(entity).withPosition(entity.position());
        try {
            String[][] matching = {
                    {"距离=..1", "distance=..1"},
                    {"横坐标=" + entity.getX(), "x=" + entity.getX()},
                    {"纵坐标=" + entity.getY(), "y=" + entity.getY()},
                    {"竖坐标=" + entity.getZ(), "z=" + entity.getZ()},
                    {"横向范围=1", "dx=1"}, {"纵向范围=1", "dy=1"}, {"竖向范围=1", "dz=1"},
                    {"俯仰角=20..40", "x_rotation=20..40"}, {"偏航角=50..70", "y_rotation=50..70"}
            };
            for (String[] options : matching) {
                assertEquivalentSelfFilter(helper, source, options[0], options[1], true);
            }
            String[][] excluding = {
                    {"距离=2..", "distance=2.."},
                    {"横坐标=" + (entity.getX() + 10) + ",距离=..1", "x=" + (entity.getX() + 10) + ",distance=..1"},
                    {"纵坐标=" + (entity.getY() + 10) + ",距离=..1", "y=" + (entity.getY() + 10) + ",distance=..1"},
                    {"竖坐标=" + (entity.getZ() + 10) + ",距离=..1", "z=" + (entity.getZ() + 10) + ",distance=..1"},
                    {"横坐标=" + (entity.getX() + 10) + ",横向范围=1", "x=" + (entity.getX() + 10) + ",dx=1"},
                    {"纵坐标=" + (entity.getY() + 10) + ",纵向范围=1", "y=" + (entity.getY() + 10) + ",dy=1"},
                    {"竖坐标=" + (entity.getZ() + 10) + ",竖向范围=1", "z=" + (entity.getZ() + 10) + ",dz=1"},
                    {"俯仰角=70..80", "x_rotation=70..80"}, {"偏航角=10..20", "y_rotation=10..20"}
            };
            for (String[] options : excluding) {
                assertEquivalentSelfFilter(helper, source, options[0], options[1], false);
            }
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chinesePlayerOptionsAndProfileArgument(GameTestHelper helper) throws CommandSyntaxException {
        String[][] options = {
                {"等级=3..9", "level=3..9"},
                {"游戏模式=生存", "gamemode=survival"}, {"游戏模式=创造", "gamemode=creative"},
                {"游戏模式=冒险", "gamemode=adventure"}, {"游戏模式=旁观", "gamemode=spectator"},
                {"游戏模式=!创造", "gamemode=!creative"},
                {"进度={minecraft:story/root=真}", "advancements={minecraft:story/root=true}"},
                {"进度={minecraft:story/root=假}", "advancements={minecraft:story/root=false}"},
                {"进度={minecraft:story/root={crafting_table=假}}", "advancements={minecraft:story/root={crafting_table=false}}"}
        };
        for (String[] option : options) {
            EntitySelector chinese = selector(helper, "选择所有玩家[" + option[0] + "]");
            EntitySelector english = selector(helper, "@a[" + option[1] + "]");
            assertSelectorShape(helper, chinese, english, option[0]);
            helper.assertFalse(chinese.includesEntities(), "Player option accepted nonplayers: " + option[0]);
            helper.assertTrue(chinese.findPlayers(source(helper)).equals(english.findPlayers(source(helper))),
                    "Player selector result differs: " + option[0]);
        }
        for (String alias : List.of("选择所有玩家", "选择最近玩家", "选择随机玩家")) {
            StringReader reader = new StringReader(alias);
            GameProfileArgument.Result result = GameProfileArgument.gameProfile().parse(reader);
            helper.assertTrue(result instanceof GameProfileArgument.SelectorResult,
                    "Game-profile argument treated Chinese selector as a player name: " + alias);
            helper.assertTrue(reader.getCursor() == alias.length(), "Game-profile selector left unread input");
        }
        assertRejected(helper, () -> GameProfileArgument.gameProfile().parse(new StringReader("选择所有实体")),
                "Game-profile selector must reject entity targets");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chinesePlayerOptionMatching(GameTestHelper helper) throws CommandSyntaxException {
        // A detached, per-test fake player supplies real player predicates without logging in.
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SelectorTest"));
        player.experienceLevel = 7;
        CommandSourceStack source = source(helper).withEntity(player);
        try {
            assertEquivalentSelfFilter(helper, source, "等级=3..9", "level=3..9", true);
            assertEquivalentSelfFilter(helper, source, "等级=8..", "level=8..", false);
            String[][] modes = {{"生存", "survival"}, {"创造", "creative"},
                    {"冒险", "adventure"}, {"旁观", "spectator"}};
            for (String[] mode : modes) {
                player.gameMode.changeGameModeForPlayer(GameType.byName(mode[1]));
                assertEquivalentSelfFilter(helper, source, "游戏模式=" + mode[0], "gamemode=" + mode[1], true);
                assertEquivalentSelfFilter(helper, source, "游戏模式=!" + mode[0], "gamemode=!" + mode[1], false);
            }
            ResourceLocation root = new ResourceLocation("minecraft", "story/root");
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(root);
            helper.assertTrue(advancement != null && !advancement.getCriteria().isEmpty(), "Vanilla root advancement missing");
            String criterion = advancement.getCriteria().keySet().iterator().next();
            assertEquivalentSelfFilter(helper, source, "进度={minecraft:story/root=假}",
                    "advancements={minecraft:story/root=false}", true);
            assertEquivalentSelfFilter(helper, source, "进度={minecraft:story/root=真}",
                    "advancements={minecraft:story/root=true}", false);
            assertEquivalentSelfFilter(helper, source, "进度={minecraft:story/root={" + criterion + "=假}}",
                    "advancements={minecraft:story/root={" + criterion + "=false}}", true);
            assertEquivalentSelfFilter(helper, source, "进度={minecraft:story/root={" + criterion + "=真}}",
                    "advancements={minecraft:story/root={" + criterion + "=true}}", false);
        } finally {
            player.getAdvancements().stopListening();
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseTeamScoresAndScoreHolder(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        var scoreboard = helper.getLevel().getServer().getScoreboard();
        String objectiveName = unique("中选");
        String teamName = unique("中队");
        CommandSourceStack source = source(helper).withEntity(entity);
        try {
            dispatcher(helper).execute("计分板 记分项 添加 " + objectiveName + " 虚拟", source);
            dispatcher(helper).execute("队伍 添加 " + teamName, source);
            dispatcher(helper).execute("队伍 加入 " + teamName + " 选择自身", source);
            dispatcher(helper).execute("计分板 玩家 设置 选择自身 " + objectiveName + " 7", source);
            helper.assertTrue(scoreboard.getOrCreatePlayerScore(entity.getScoreboardName(),
                    scoreboard.getObjective(objectiveName)).getScore() == 7,
                    "Score-holder selector did not modify the entity's actual score");
            assertEquivalentSelfFilter(helper, source, "队伍=" + teamName, "team=" + teamName, true);
            assertEquivalentSelfFilter(helper, source, "队伍=!" + teamName, "team=!" + teamName, false);
            assertEquivalentSelfFilter(helper, source, "分数={" + objectiveName + "=7}",
                    "scores={" + objectiveName + "=7}", true);
            assertEquivalentSelfFilter(helper, source, "分数={" + objectiveName + "=8..}",
                    "scores={" + objectiveName + "=8..}", false);
            var parsed = ScoreHolderArgument.scoreHolders().parse(new StringReader("选择自身"));
            helper.assertTrue(parsed instanceof ScoreHolderArgument.SelectorResult,
                    "Score-holder argument did not retain selector semantics");
            helper.assertTrue(parsed.getNames(source, Collections::emptyList).equals(List.of(entity.getScoreboardName())),
                    "Score-holder selector resolved an incorrect scoreboard name");
            assertRejected(helper, () -> ScoreHolderArgument.scoreHolder().parse(new StringReader("选择所有实体")),
                    "Single score-holder argument accepted an unlimited selector");
        } finally {
            if (scoreboard.getObjective(objectiveName) != null) scoreboard.removeObjective(scoreboard.getObjective(objectiveName));
            if (scoreboard.getPlayerTeam(teamName) != null) scoreboard.removePlayerTeam(scoreboard.getPlayerTeam(teamName));
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseNestedExecuteAndFunction(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        Entity second = helper.spawn(EntityType.ARMOR_STAND, 2, 1, 1);
        String uniqueTag = unique("执行");
        entity.addTag(uniqueTag);
        second.addTag(uniqueTag);
        CommandSourceStack source = source(helper).withEntity(entity);
        try {
            int result = dispatcher(helper).execute("执行 作为 选择所有实体[标签=" + uniqueTag
                    + "] 位于 选择自身 运行 执行 如果 实体 选择自身 运行 标签 选择自身 添加 中文嵌套选择", source);
            helper.assertTrue(result == 2 && entity.getTags().contains("中文嵌套选择")
                    && second.getTags().contains("中文嵌套选择"), "Nested execute fork lost its Chinese selectors");
            helper.assertTrue(helper.getLevel().getServer().getFunctions()
                    .get(new ResourceLocation("示例", "中文选择器")).isPresent(),
                    "Function containing Chinese selectors was not loaded");
            helper.assertTrue(dispatcher(helper).execute("函数 示例:中文选择器", source) >= 2,
                    "Function with Chinese selectors did not execute");
            helper.assertTrue(entity.getTags().contains("中文选择器函数") && entity.getTags().contains("中文函数嵌套选择"),
                    "Chinese selectors in function bodies selected the wrong entity");
            helper.assertFalse(second.getTags().contains("中文选择器函数"), "Function self-selector leaked to another entity");
        } finally {
            entity.discard();
            second.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSelectorNbtAndPredicate(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        entity.addTag("选择所有实体");
        entity.addTag("排序=最近");
        entity.setCustomName(Component.literal("选择所有实体"));
        CommandSourceStack source = source(helper).withEntity(entity);
        try {
            assertEquivalentSelfFilter(helper, source,
                    "实体数据={Tags:[\"选择所有实体\",\"排序=最近\"],CustomName:'{\"text\":\"选择所有实体\"}'}",
                    "nbt={Tags:[\"选择所有实体\",\"排序=最近\"],CustomName:'{\"text\":\"选择所有实体\"}'}", true);
            assertEquivalentSelfFilter(helper, source, "实体数据=!{Tags:[\"选择所有实体\"]}",
                    "nbt=!{Tags:[\"选择所有实体\"]}", false);
            assertEquivalentSelfFilter(helper, source, "名称=\"选择所有实体\"", "name=\"选择所有实体\"", true);
            assertEquivalentSelfFilter(helper, source, "谓词=示例:不存在的谓词", "predicate=示例:不存在的谓词", false);
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void selectorWordsRemainOrdinaryNames(GameTestHelper helper) throws CommandSyntaxException {
        for (String name : List.of("选择所有实体甲", "选择自身玩家", "选择随机玩家测试")) {
            EntitySelector selector = selector(helper, name);
            helper.assertFalse(selector.usesSelector(), "Player name was rewritten as a selector: " + name);
            helper.assertTrue(selector.getMaxResults() == 1 && !selector.includesEntities(),
                    "Player name changed its parsed argument type: " + name);
            ScoreHolderArgument.Result holder = ScoreHolderArgument.scoreHolders().parse(new StringReader(name));
            helper.assertFalse(holder instanceof ScoreHolderArgument.SelectorResult,
                    "Score-holder name was rewritten as selector: " + name);
            helper.assertTrue(holder.getNames(source(helper), Collections::emptyList).equals(Collections.singleton(name)),
                    "Literal score-holder name changed: " + name);
            GameProfileArgument.Result profile = GameProfileArgument.gameProfile().parse(new StringReader(name));
            helper.assertFalse(profile instanceof GameProfileArgument.SelectorResult,
                    "Profile name was rewritten as selector: " + name);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSelectorsInMessages(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        entity.setCustomName(Component.literal("消息测试实体"));
        CommandSourceStack source = source(helper).withEntity(entity);
        try {
            String input = "目标 选择自身 完成";
            MessageArgument.Message message = MessageArgument.message().parse(new StringReader(input));
            helper.assertTrue(message.getText().equals(input), "Message's original signed text was modified");
            helper.assertTrue(message.getParts().length == 1, "Chinese message selector was not recognized");
            helper.assertTrue(message.getParts()[0].getStart() == 3 && message.getParts()[0].getEnd() == 7,
                    "Chinese message selector offsets do not refer to original text");
            helper.assertTrue(message.toComponent(source, true).getString().equals("目标 消息测试实体 完成"),
                    "Chinese message selector did not render entity names correctly");
            helper.assertTrue(message.toComponent(source.withPermission(0), false).getString().equals(input),
                    "Message selectors resolved while selector permission was disabled");
            String prose = "我想选择所有实体进行测试，选择自身玩家只是名字";
            MessageArgument.Message natural = MessageArgument.message().parse(new StringReader(prose));
            helper.assertTrue(natural.getParts().length == 0 && natural.toComponent(source, true).getString().equals(prose),
                    "Selector words inside natural-language text were rewritten");
            MessageArgument.Message mixed = MessageArgument.message().parse(new StringReader("@s 选择自身"));
            helper.assertTrue(mixed.getParts().length == 2
                    && mixed.toComponent(source, true).getString().equals("消息测试实体 消息测试实体"),
                    "Mixed English and Chinese message selectors failed");
            String literal = "引用 \"选择所有实体\" {\"text\":\"选择所有玩家\"}";
            MessageArgument.Message quoted = MessageArgument.message().parse(new StringReader(literal));
            helper.assertTrue(quoted.getParts().length == 0 && quoted.getText().equals(literal),
                    "Quoted data in a message was rewritten as Chinese selectors");
            helper.assertTrue(MessageArgument.Message.parseText(new StringReader("选择自身 @s"), false)
                    .getParts().length == 0, "Disabled message selector parsing still recognized a selector");
            entity.setCustomName(Component.literal("@e["));
            MessageArgument.Message quotedOption = MessageArgument.message().parse(
                    new StringReader("目标 选择自身[名称=\"@e[\"] 完成"));
            helper.assertTrue(quotedOption.getParts().length == 1
                    && quotedOption.toComponent(source, true).getString().equals("目标 @e[ 完成"),
                    "Message scanner interpreted quoted @ inside a Chinese selector as another selector");
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSelectorSuggestionsAndRanges(GameTestHelper helper) {
        assertSuggestion(helper, "标签 选择所", "选择所有实体", "标签 选择所有实体");
        assertSuggestion(helper, "标签 选择自", "选择自身", "标签 选择自身");
        assertSuggestion(helper, "标签 选择所有实体[距", "距离=", "标签 选择所有实体[距离=");
        assertSuggestion(helper, "标签 @e[距", "距离=", "标签 @e[距离=");
        assertSuggestion(helper, "标签 选择所有实体[排序=最", "最近", "标签 选择所有实体[排序=最近");
        assertSuggestion(helper, "标签 选择所有实体[排序=最", "最远", "标签 选择所有实体[排序=最远");
        assertSuggestion(helper, "标签 选择所有实体[游戏模式=创", "创造", "标签 选择所有实体[游戏模式=创造");
        assertSuggestion(helper, "标签 选择所有实体[游戏模式=!创", "创造", "标签 选择所有实体[游戏模式=!创造");
        assertSuggestionApplied(helper, "标签 选择所有实体[游戏模式=", "标签 选择所有实体[游戏模式=生存");
        assertSuggestionApplied(helper, "标签 选择所有实体[游戏模式=", "标签 选择所有实体[游戏模式=!生存");
        String repeatedMode = "标签 选择所有实体[游戏模式=!创造,游戏模式=";
        assertSuggestionApplied(helper, repeatedMode, repeatedMode + "!生存");
        Suggestions repeatedModeSuggestions = dispatcher(helper).getCompletionSuggestions(
                dispatcher(helper).parse(repeatedMode, source(helper))).join();
        for (String mode : List.of("生存", "创造", "冒险", "旁观")) {
            helper.assertFalse(repeatedModeSuggestions.getList().stream()
                            .anyMatch(suggestion -> suggestion.apply(repeatedMode).equals(repeatedMode + mode)),
                    "A previous negated game mode must prevent positive game-mode completions: " + mode);
        }
        String usedDistance = "标签 选择所有实体[距离=..5,";
        Suggestions availableOptions = dispatcher(helper).getCompletionSuggestions(
                dispatcher(helper).parse(usedDistance, source(helper))).join();
        helper.assertFalse(availableOptions.getList().stream().anyMatch(suggestion -> {
            String applied = suggestion.apply(usedDistance);
            return applied.equals(usedDistance + "距离=") || applied.equals(usedDistance + "distance=");
        }), "Distance option remained available after it had already been specified");
        for (String invalidSort : List.of("标签 选择自身[排序=", "标签 选择所有实体[排序=最近,排序=")) {
            Suggestions sorts = dispatcher(helper).getCompletionSuggestions(
                    dispatcher(helper).parse(invalidSort, source(helper))).join();
            for (String order : List.of("最近", "最远", "随机", "任意")) {
                helper.assertFalse(sorts.getList().stream()
                                .anyMatch(suggestion -> suggestion.apply(invalidSort).equals(invalidSort + order)),
                        "Inapplicable sort option offered a Chinese sort value: " + invalidSort + order);
            }
        }
        assertSuggestion(helper, "标签 选择所有实体[类型=minecraft:armor_", "minecraft:armor_stand",
                "标签 选择所有实体[类型=minecraft:armor_stand");
        assertSuggestion(helper, "计分板 玩家 重置 选择所", "选择所有实体", "计分板 玩家 重置 选择所有实体");
        assertSuggestion(helper, "op 选择所", "选择所有玩家", "op 选择所有玩家");
        for (String command : List.of("deop", "whitelist add", "whitelist remove", "pardon")) {
            assertSuggestion(helper, command + " 选择所", "选择所有玩家", command + " 选择所有玩家");
        }
        assertSuggestion(helper, "op 选择所有玩家[游戏模式=创", "创造", "op 选择所有玩家[游戏模式=创造");
        assertSuggestion(helper, "op 选择所有实体[类型=minecraft:pla", "minecraft:player",
                "op 选择所有实体[类型=minecraft:player");
        String invalidPlayerType = "op 选择所有玩家[类型=minecraft:pla";
        Suggestions invalidPlayerTypes = dispatcher(helper).getCompletionSuggestions(
                dispatcher(helper).parse(invalidPlayerType, source(helper))).join();
        helper.assertFalse(invalidPlayerTypes.getList().stream().anyMatch(suggestion -> suggestion.apply(invalidPlayerType)
                .equals("op 选择所有玩家[类型=minecraft:player")),
                "Players-only selector offered an inapplicable entity-type option");
        assertSuggestion(helper, "op @", "@a", "op @a");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseSelectorValidationAndPermissions(GameTestHelper helper) throws CommandSyntaxException {
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        try {
            assertRejected(helper, () -> new EntitySelectorParser(new StringReader("选择自身"), false).parse(),
                    "Chinese selector bypassed parser's allowSelectors=false");
            EntitySelector selector = selector(helper, "选择自身");
            assertRejected(helper, () -> selector.findEntities(source(helper).withEntity(entity).withPermission(0)),
                    "Chinese selector bypassed source permissions");
            assertRejected(helper, () -> EntityArgument.entity().parse(new StringReader("选择所有实体")),
                    "Single-entity argument accepted unlimited Chinese selector");
            assertRejected(helper, () -> EntityArgument.players().parse(new StringReader("选择所有实体")),
                    "Players-only argument accepted nonplayer Chinese selector");
            assertRejected(helper, () -> selector(helper, "选择所有实体[数量=0]"), "Invalid Chinese selector limit was accepted");
            assertRejected(helper, () -> selector(helper, "选择所有实体[距离=-1]"), "Negative Chinese distance was accepted");
            assertRejected(helper, () -> selector(helper, "选择所有实体[距离=..5,distance=..6]"),
                    "Mixing option languages bypassed duplicate-option validation");
            assertMappedSelectorError(helper, "选择所有实体[数量=0]", "@e[limit=0]", "0");
            assertMappedSelectorError(helper, "选择所有实体[距离=-1]", "@e[distance=-1]", "-1");
            assertMappedSelectorError(helper, "选择所有实体[排序=未知]", "@e[sort=未知]", "未知");
            assertMappedSelectorError(helper, "选择所有实体[游戏模式=飞行]", "@e[gamemode=飞行]", "飞行");
            assertMappedSelectorError(helper, "选择所有实体[未知参数=2]", "@e[未知参数=2]", "未知参数");
            assertMappedSelectorError(helper, "选择所有实体[距离=..5", "@e[distance=..5", null);
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    private static EntitySelector selector(GameTestHelper helper, String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        EntitySelector result = EntityArgument.entities().parse(reader);
        helper.assertTrue(reader.getCursor() == input.length(), "Selector did not consume all input: " + input);
        return result;
    }

    private static void assertSelectorShape(GameTestHelper helper, EntitySelector chinese, EntitySelector english, String input) {
        helper.assertTrue(chinese.getMaxResults() == english.getMaxResults()
                        && chinese.includesEntities() == english.includesEntities()
                        && chinese.isSelfSelector() == english.isSelfSelector()
                        && chinese.isWorldLimited() == english.isWorldLimited()
                        && chinese.usesSelector() == english.usesSelector(),
                "Chinese selector has different typed behavior: " + input);
    }

    private static void assertEquivalentSelfFilter(GameTestHelper helper, CommandSourceStack source,
                                                   String chinese, String english, boolean matches) throws CommandSyntaxException {
        EntitySelector chineseSelector = selector(helper, "选择自身[" + chinese + "]");
        EntitySelector englishSelector = selector(helper, "@s[" + english + "]");
        assertSelectorShape(helper, chineseSelector, englishSelector, chinese);
        List<? extends Entity> expected = matches ? List.of(source.getEntity()) : List.of();
        assertEntities(helper, englishSelector, source, expected);
        assertEntities(helper, chineseSelector, source, expected);
    }

    private static void assertEntities(GameTestHelper helper, EntitySelector selector, CommandSourceStack source,
                                       List<? extends Entity> expected) throws CommandSyntaxException {
        List<? extends Entity> actual = selector.findEntities(source);
        helper.assertTrue(actual.equals(expected), "Selector selected unexpected entities: expected "
                + expected.stream().map(Entity::getStringUUID).toList() + ", got "
                + actual.stream().map(Entity::getStringUUID).toList());
    }

    private static void assertSuggestion(GameTestHelper helper, String input, String expected, String applied) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(helper);
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source(helper))).join();
        Suggestion suggestion = suggestions.getList().stream().filter(item -> item.getText().equals(expected)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing TAB suggestion '" + expected + "' for '"
                        + input + "': " + suggestions.getList()));
        helper.assertTrue(suggestion.apply(input).equals(applied),
                "TAB replacement range changed preceding selector text: " + suggestion.apply(input));
    }

    private static void assertSuggestionApplied(GameTestHelper helper, String input, String applied) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(helper);
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source(helper))).join();
        helper.assertTrue(suggestions.getList().stream().anyMatch(suggestion -> suggestion.apply(input).equals(applied)),
                "No TAB suggestion produced '" + applied + "' from '" + input + "': " + suggestions.getList());
    }

    private static void assertMappedSelectorError(GameTestHelper helper, String chinese, String english, String marker) {
        String prefix = "执行 作为 ";
        String original = prefix + chinese;
        String canonical = prefix + english;
        StringReader originalReader = new StringReader(original);
        StringReader canonicalReader = new StringReader(canonical);
        originalReader.setCursor(prefix.length());
        canonicalReader.setCursor(prefix.length());
        CommandSyntaxException originalError = selectorError(originalReader);
        CommandSyntaxException canonicalError = selectorError(canonicalReader);
        int originalOffset = marker == null ? original.length() : original.indexOf(marker, prefix.length());
        int canonicalOffset = marker == null ? canonical.length() : canonical.indexOf(marker, prefix.length());
        helper.assertTrue(canonicalError.getCursor() == canonicalOffset,
                "Canonical error does not point to the expected invalid token: " + canonicalError.getMessage());
        helper.assertTrue(originalError.getType() == canonicalError.getType(),
                "Chinese selector changed the canonical error type: " + chinese);
        helper.assertTrue(original.equals(originalError.getInput()) && originalError.getCursor() == originalOffset,
                "Chinese selector error lost original text/cursor: " + originalError.getMessage());
        helper.assertTrue(originalReader.getCursor() - originalOffset == canonicalReader.getCursor() - canonicalOffset,
                "Chinese selector reader cursor does not map to canonical error position: " + chinese);
    }

    private static CommandSyntaxException selectorError(StringReader reader) {
        try {
            new EntitySelectorParser(reader).parse();
        } catch (CommandSyntaxException expected) {
            return expected;
        }
        throw new IllegalStateException("Expected invalid selector: " + reader.getString());
    }

    private static void assertRejected(GameTestHelper helper, ThrowingAction action, String message) {
        boolean rejected = false;
        try {
            action.run();
        } catch (CommandSyntaxException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, message);
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(GameTestHelper helper) {
        return helper.getLevel().getServer().getCommands().getDispatcher();
    }

    private static CommandSourceStack source(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel())
                .withPosition(helper.absoluteVec(new Vec3(1, 1, 1))).withPermission(4).withSuppressedOutput();
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws CommandSyntaxException;
    }
}
