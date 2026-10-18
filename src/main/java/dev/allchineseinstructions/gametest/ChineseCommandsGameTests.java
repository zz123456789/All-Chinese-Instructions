package dev.allchineseinstructions.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.server.command.CommandHelper;

/** Optional real-server regression tests; registered only by an explicit JVM property. */
@PrefixGameTestTemplate(false)
public final class ChineseCommandsGameTests {
    private static final String NAMESPACE = "allchineseinstructions";

    private ChineseCommandsGameTests() {
    }

    // Do not add @GameTestHolder: Forge scans that annotation unconditionally.
    public static void register(RegisterGameTestsEvent event) {
        if (Boolean.getBoolean("allchineseinstructions.runTests")) {
            event.register(ChineseCommandsGameTests.class);
        }
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseResourceLocations(GameTestHelper helper) throws CommandSyntaxException {
        ResourceLocation id = new ResourceLocation("示例", "嵌套/测试");
        helper.assertTrue(id.equals(ResourceLocation.read(new StringReader("示例:嵌套/测试"))),
                "ResourceLocation constructors and command token reading disagree");
        helper.assertTrue(ResourceLocation.isValidResourceLocation("示例:嵌套/测试"),
                "Chinese resource ID validation failed");
        helper.assertTrue(FileUtil.decomposePath("functions/嵌套/测试.mcfunction").result().isPresent(),
                "Directory data-pack paths rejected Chinese file names");
        helper.assertTrue(FileUtil.decomposePath("functions/../测试.mcfunction").error().isPresent(),
                "Dot-segment path safety must remain active");
        helper.assertTrue(ResourceLocation.tryParse("示例:错误 路径") == null,
                "Whitespace must remain invalid in resource IDs");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseGameModeArguments(GameTestHelper helper) throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(helper);
        assertGameMode(helper, dispatcher, source(helper), "游戏模式 创造", GameType.CREATIVE);
        assertGameMode(helper, dispatcher, source(helper), "游戏模式 生存", GameType.SURVIVAL);
        assertGameMode(helper, dispatcher, source(helper), "gamemode 创造", GameType.CREATIVE);
        assertGameMode(helper, dispatcher, source(helper), "游戏模式 creative", GameType.CREATIVE);
        assertGameMode(helper, dispatcher, source(helper), "gamemode creative", GameType.CREATIVE);
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void nestedChineseExecute(GameTestHelper helper) throws CommandSyntaxException {
        var entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        CommandSourceStack source = source(helper).withEntity(entity);
        try {
            int english = dispatcher(helper).execute("execute as @s at @s run gamerule doDaylightCycle", source);
            int chinese = dispatcher(helper).execute("执行 作为 @s 位于 @s 运行 游戏规则 昼夜更替", source);
            helper.assertTrue(chinese == english, "Chinese execute redirects changed the command result");
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseBooleanGameRule(GameTestHelper helper) throws CommandSyntaxException {
        var server = helper.getLevel().getServer();
        var rule = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean previous = rule.get();
        try {
            dispatcher(helper).execute("游戏规则 昼夜更替 假", source(helper));
            helper.assertFalse(rule.get(), "Chinese false value did not change the game rule");
            dispatcher(helper).execute("游戏规则 昼夜更替 真", source(helper));
            helper.assertTrue(rule.get(), "Chinese true value did not change the game rule");
        } finally {
            rule.set(previous, server);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void unquotedChineseNames(GameTestHelper helper) throws CommandSyntaxException {
        var scoreboard = helper.getLevel().getServer().getScoreboard();
        CommandSourceStack source = source(helper);
        var dispatcher = dispatcher(helper);
        helper.assertTrue(scoreboard.getObjective("中文测试") == null, "Test objective already exists");
        helper.assertTrue(scoreboard.getPlayerTeam("中文队伍") == null, "Test team already exists");
        var entity = helper.spawn(EntityType.ARMOR_STAND, 1, 1, 1);
        try {
            dispatcher.execute("计分板 记分项 添加 中文测试 虚拟", source);
            dispatcher.execute("计分板 玩家 设置 #测试 中文测试 7", source);
            helper.assertTrue(dispatcher.execute("计分板 玩家 获取 #测试 中文测试", source) == 7,
                    "Unquoted Chinese objective or score-holder name failed");
            dispatcher.execute("队伍 添加 中文队伍", source);
            helper.assertTrue(scoreboard.getPlayerTeam("中文队伍") != null,
                    "Unquoted Chinese team name failed");
            dispatcher.execute("标签 @s 添加 中文标签", source.withEntity(entity));
            helper.assertTrue(entity.getTags().contains("中文标签"), "Unquoted Chinese entity tag failed");
            dispatcher.execute("队伍 加入 中文队伍 @s", source.withEntity(entity));
            dispatcher.execute("计分板 玩家 设置 @s 中文测试 7", source.withEntity(entity));
            dispatcher.execute("标签 @s[team=中文队伍,tag=中文标签,scores={中文测试=7}] 添加 中文筛选成功",
                    source.withEntity(entity));
            helper.assertTrue(entity.getTags().contains("中文筛选成功"),
                    "Chinese names were not accepted when referenced in selector options");
        } finally {
            var objective = scoreboard.getObjective("中文测试");
            if (objective != null) {
                scoreboard.removeObjective(objective);
            }
            var team = scoreboard.getPlayerTeam("中文队伍");
            if (team != null) {
                scoreboard.removePlayerTeam(team);
            }
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseFunctionsAndTags(GameTestHelper helper) throws CommandSyntaxException {
        ServerFunctionManager manager = helper.getLevel().getServer().getFunctions();
        ResourceLocation id = new ResourceLocation("示例", "开场");
        CommandFunction function = manager.get(id).orElseThrow(() ->
                new IllegalStateException("Chinese function file was not loaded: " + id));
        helper.assertTrue(manager.get(new ResourceLocation("示例", "嵌套/测试")).isPresent(),
                "Nested Chinese function resource was not loaded");
        FunctionTrace trace = new FunctionTrace();
        int count = manager.execute(function, source(helper), trace);
        helper.assertTrue(count >= 3, "Nested Chinese function did not execute its commands");
        helper.assertTrue(trace.errors.isEmpty(), "Function execution errors: " + trace.errors);
        helper.assertTrue(trace.commands.stream().anyMatch(command -> command.contains("中文嵌套函数已执行")),
                "Nested Chinese function never reached its command body");
        helper.assertTrue(manager.getTag(new ResourceLocation("示例", "中文标签")).size() == 1,
                "Chinese function tag was not loaded");
        helper.assertTrue(dispatcher(helper).execute("函数 示例:开场", source(helper)) >= 3,
                "Chinese /function command failed");
        helper.assertTrue(dispatcher(helper).execute("函数 #示例:中文标签", source(helper)) >= 3,
                "Chinese function tag command failed");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void chineseTabSuggestions(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(helper);
        CommandSourceStack source = source(helper);
        assertSuggestion(helper, dispatcher, source, "游戏", "游戏模式");
        assertSuggestion(helper, dispatcher, source, "执行 ", "运行");
        assertSuggestion(helper, dispatcher, source, "游戏模式 创", "创造");
        assertSuggestion(helper, dispatcher, source, "游戏规则 昼夜更替 ", "假");
        assertSuggestion(helper, dispatcher, source, "函数 示例:开", "示例:开场");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void permissionsRemainEnforced(GameTestHelper helper) {
        CommandSourceStack restricted = source(helper).withPermission(0);
        boolean rejected = false;
        try {
            dispatcher(helper).execute("游戏模式 创造", restricted);
        } catch (CommandSyntaxException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Chinese alias bypassed the original command permission");
        RootCommandNode<SharedSuggestionProvider> root = clientTree(dispatcher(helper), restricted);
        helper.assertTrue(root.getChild("游戏模式") == null, "Restricted Chinese command leaked into the client tree");
        helper.assertTrue(root.getChild("gamemode") == null, "Restricted vanilla command leaked into the client tree");
        CommandDispatcher<SharedSuggestionProvider> client = new CommandDispatcher<>(root);
        Suggestions suggestions = client.getCompletionSuggestions(client.parse("游戏", restricted)).join();
        helper.assertFalse(suggestions.getList().stream().anyMatch(value -> value.getText().equals("游戏模式")),
                "Restricted Chinese command leaked into TAB suggestions");
        helper.succeed();
    }

    @GameTest(templateNamespace = NAMESPACE, template = "empty")
    public static void commandPacketRoundTrip(GameTestHelper helper) throws CommandSyntaxException {
        CommandSourceStack source = source(helper);
        RootCommandNode<SharedSuggestionProvider> root = clientTree(dispatcher(helper), source);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ClientboundCommandsPacket(root).write(buffer);
            ClientboundCommandsPacket decoded = new ClientboundCommandsPacket(buffer);
            CommandBuildContext context = CommandBuildContext.simple(helper.getLevel().registryAccess(),
                    helper.getLevel().enabledFeatures());
            CommandDispatcher<SharedSuggestionProvider> client = new CommandDispatcher<>(decoded.getRoot(context));
            assertGameMode(helper, client, source, "游戏模式 创造", GameType.CREATIVE);
            assertSuggestion(helper, client, source, "游戏模式 创", "创造");
            assertSuggestion(helper, client, source, "执行 ", "运行");
            String selectorInput = "标签 选择自身[类型=minecraft:armor_stand] 添加 中文同步";
            ParseResults<SharedSuggestionProvider> selectorParsed = client.parse(selectorInput, source);
            helper.assertFalse(selectorParsed.getReader().canRead(),
                    "Chinese selector did not parse after command packet decoding");
            EntitySelector selector = selectorParsed.getContext().build(selectorInput)
                    .getArgument("targets", EntitySelector.class);
            helper.assertTrue(selector.usesSelector() && selector.isSelfSelector() && selector.getMaxResults() == 1,
                    "Decoded command tree lost the Chinese selector's typed behavior");
            String[][] selectorSuggestions = {
                    {"标签 选择所", "标签 选择所有实体"},
                    {"标签 选择所有实体[距", "标签 选择所有实体[距离="},
                    {"标签 选择所有实体[游戏模式=!创", "标签 选择所有实体[游戏模式=!创造"},
                    {"标签 @e[排序=最", "标签 @e[排序=最近"}
            };
            for (String[] expected : selectorSuggestions) {
                Suggestions completions = client.getCompletionSuggestions(client.parse(expected[0], source)).join();
                helper.assertTrue(completions.getList().stream()
                                .anyMatch(suggestion -> suggestion.apply(expected[0]).equals(expected[1])),
                        "Decoded command tree lost Chinese selector completion/ranges for: " + expected[0]
                                + "; suggestions: " + completions.getList());
            }
            helper.assertTrue(client.getRoot().getChild("gamemode") != null,
                    "Vanilla commands disappeared after packet decoding");
            helper.assertTrue(buffer.readableBytes() == 0, "Command packet serializer left unread bytes");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(GameTestHelper helper) {
        return helper.getLevel().getServer().getCommands().getDispatcher();
    }

    private static CommandSourceStack source(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel())
                .withPosition(helper.absoluteVec(new Vec3(1, 1, 1))).withPermission(4).withSuppressedOutput();
    }

    private static <S> void assertGameMode(GameTestHelper helper, CommandDispatcher<S> dispatcher,
                                          S source, String input, GameType expected) throws CommandSyntaxException {
        ParseResults<S> parsed = dispatcher.parse(input, source);
        if (parsed.getReader().canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
                    .createWithContext(parsed.getReader());
        }
        helper.assertTrue(parsed.getContext().build(input).getArgument("gamemode", GameType.class) == expected,
                "Wrong typed game-mode value for: " + input);
    }

    private static <S> void assertSuggestion(GameTestHelper helper, CommandDispatcher<S> dispatcher,
                                            S source, String input, String expected) {
        Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source)).join();
        helper.assertTrue(suggestions.getList().stream().anyMatch(value -> value.getText().equals(expected)),
                "Missing TAB suggestion '" + expected + "' for '" + input + "': " + suggestions.getList());
    }

    @SuppressWarnings("unchecked")
    private static RootCommandNode<SharedSuggestionProvider> clientTree(
            CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source) {
        RootCommandNode<SharedSuggestionProvider> root = new RootCommandNode<>();
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> copies = new HashMap<>();
        copies.put(dispatcher.getRoot(), root);
        CommandHelper.mergeCommandNode(dispatcher.getRoot(), root, copies, source, context -> 0,
                provider -> SuggestionProviders.safelySwap(
                        (SuggestionProvider<SharedSuggestionProvider>) (SuggestionProvider<?>) provider));
        return root;
    }

    private static final class FunctionTrace implements ServerFunctionManager.TraceCallbacks {
        private final List<String> commands = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public void onCommand(int depth, String command) {
            commands.add(command);
        }

        @Override
        public void onReturn(int depth, String command, int result) {
        }

        @Override
        public void onError(int depth, String message) {
            errors.add(message);
        }

        @Override
        public void onCall(int depth, ResourceLocation function, int size) {
        }
    }
}
