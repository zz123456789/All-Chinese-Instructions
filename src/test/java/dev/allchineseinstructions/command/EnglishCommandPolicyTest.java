package dev.allchineseinstructions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import dev.allchineseinstructions.api.ChineseTranslationRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.*;

class EnglishCommandPolicyTest {
    private CommandDispatcher<Object> dispatcher(AtomicBoolean allowEnglish) {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("mode").requires(source -> (int) source >= 2)
                .then(literal("set").then(argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder) -> builder.suggest("minecraft:diamond")
                                .suggest("Steve").suggest("set").buildFuture())
                        .executes(context -> 7))));
        dispatcher.register(literal("say").then(argument("text", StringArgumentType.greedyString())
                .executes(context -> 11)));
        CommandNode<Object> execute = dispatcher.register(literal("execute"));
        execute.addChild(literal("run").redirect(dispatcher.getRoot()).build());
        var translations = new ChineseTranslationRegistry();
        translations.command("mode", "模式");
        translations.literal("mode", "set", "设置");
        translations.command("say", "说");
        translations.command("execute", "执行");
        translations.literal("execute", "run", "运行");
        ChineseCommandTree.install(dispatcher, translations, node -> {}, fail -> fail(fail));
        EnglishCommandPolicy.install(dispatcher, allowEnglish::get);
        return dispatcher;
    }

    @Test
    void defaultPolicyRejectsEnglishAndPreservesChineseCommands() throws Exception {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        assertEquals(7, dispatcher.execute("模式 设置 Steve", 2));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mode set Steve", 2));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("模式 set Steve", 2));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("mode set Steve", 2)));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("模式 set Steve", 2)));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("模式 设置 Steve", 2)));
    }

    @Test
    void dynamicFlagRestoresBothLanguagesWithoutRebuilding() throws Exception {
        AtomicBoolean allowEnglish = new AtomicBoolean(false);
        var dispatcher = dispatcher(allowEnglish);
        CommandNode<Object> english = dispatcher.getRoot().getChild("mode");
        assertTrue(EnglishCommandPolicy.isBlocked(english));
        allowEnglish.set(true);
        assertFalse(EnglishCommandPolicy.isBlocked(english));
        assertEquals(7, dispatcher.execute("mode set Steve", 2));
        assertEquals(7, dispatcher.execute("模式 设置 Steve", 2));
        assertEquals(7, dispatcher.execute("mode 设置 Steve", 2));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("mode set Steve", 2)));
        allowEnglish.set(false);
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mode set Steve", 2));
    }

    @Test
    void originalPermissionChecksArePreservedInBothModes() {
        AtomicBoolean allowEnglish = new AtomicBoolean(false);
        var dispatcher = dispatcher(allowEnglish);
        assertFalse(dispatcher.getRoot().getChild("模式").canUse(0));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("模式 设置 Steve", 0));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("mode set Steve", 0)));
        allowEnglish.set(true);
        assertFalse(dispatcher.getRoot().getChild("mode").canUse(0));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mode set Steve", 0));
    }

    @Test
    void repeatedInstallationDoesNotWrapRequirementsAgain() {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        var mode = dispatcher.getRoot().getChild("mode");
        var requirement = mode.getRequirement();
        EnglishCommandPolicy.install(dispatcher.getRoot(), () -> true);
        assertSame(requirement, mode.getRequirement());
        assertTrue(mode.canUse(2));
        assertFalse(mode.canUse(0));
    }

    @Test
    void sharedChildrenAndNestedRedirectsKeepTheirIdentity() throws Exception {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        var root = dispatcher.getRoot();
        assertSame(root.getChild("mode").getChild("set"), root.getChild("模式").getChild("set"));
        assertSame(root, root.getChild("执行").getChild("运行").getRedirect());
        assertEquals(7, dispatcher.execute("执行 运行 执行 运行 模式 设置 Steve", 2));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("执行 run 模式 设置 Steve", 2)));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("执行 运行 mode set Steve", 2)));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("执行 运行 执行 运行 模式 set Steve", 2)));
    }

    @Test
    void traversalReachesNodesAccessibleOnlyThroughRedirects() {
        var dispatcher = new CommandDispatcher<Object>();
        var target = literal("目标").then(literal("hidden").executes(context -> 1)).build();
        dispatcher.register(literal("入口").redirect(target));
        EnglishCommandPolicy.install(dispatcher, () -> false);
        assertTrue(EnglishCommandPolicy.isBlocked(target.getChild("hidden")));
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse("入口 hidden", new Object())));
    }

    @Test
    void numbersPunctuationAndHanAliasesRemainAvailable() {
        var dispatcher = new CommandDispatcher<Object>();
        for (String name : List.of("1", "-", "?", "中文", "mod:中文", "〇", "English", "mod:command")) {
            dispatcher.register(literal(name).executes(context -> 1));
        }
        EnglishCommandPolicy.install(dispatcher, () -> false);
        for (String name : List.of("1", "-", "?", "中文", "mod:中文", "〇")) {
            assertFalse(EnglishCommandPolicy.isBlocked(dispatcher.getRoot().getChild(name)), name);
        }
        assertTrue(EnglishCommandPolicy.isBlocked(dispatcher.getRoot().getChild("English")));
        assertTrue(EnglishCommandPolicy.isBlocked(dispatcher.getRoot().getChild("mod:command")));
    }

    @Test
    void freeTextRegistryIdsAndUnknownWordsDoNotTriggerPolicyMessage() throws Exception {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        assertEquals(11, dispatcher.execute("说 execute run mode set", 2));
        assertEquals(7, dispatcher.execute("模式 设置 minecraft:diamond", 2));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("说 execute run mode set", 2)));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("模式 设置 minecraft:diamond", 2)));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("unknown", 2)));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("模式 unknown", 2)));
        assertFalse(EnglishCommandPolicy.blockedInput(dispatcher.parse("", 2)));
    }

    @Test
    void commandReaderCanStartAfterSlash() {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        StringReader reader = new StringReader("/mode set Steve");
        reader.skip();
        assertTrue(EnglishCommandPolicy.blockedInput(dispatcher.parse(reader, 2)));
    }

    private List<String> suggestions(CommandDispatcher<Object> dispatcher, String input) throws Exception {
        ParseResults<Object> parse = dispatcher.parse(input, 2);
        Suggestions raw = dispatcher.getCompletionSuggestions(parse).get();
        return EnglishCommandPolicy.filterSuggestions(parse, raw, input.length()).getList()
                .stream().map(Suggestion::getText).toList();
    }

    @Test
    void filtersEnglishRootAndNestedLiteralSuggestions() throws Exception {
        AtomicBoolean allowEnglish = new AtomicBoolean(false);
        var dispatcher = dispatcher(allowEnglish);
        assertEquals(List.of("执行", "模式", "说"), suggestions(dispatcher, ""));
        assertEquals(List.of(), suggestions(dispatcher, "m"));
        assertEquals(List.of("设置"), suggestions(dispatcher, "模式 "));
        assertEquals(List.of("执行", "模式", "说"), suggestions(dispatcher, "执行 运行 "));
        assertEquals(List.of("设置"), suggestions(dispatcher, "执行 运行 模式 "));
        allowEnglish.set(true);
        assertTrue(suggestions(dispatcher, "").contains("mode"));
        assertTrue(suggestions(dispatcher, "模式 ").contains("set"));
    }

    @Test
    void preservesArgumentSuggestionsEvenWhenTheyMatchAnEnglishCommand() throws Exception {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        assertEquals(List.of("minecraft:diamond", "set", "Steve"), suggestions(dispatcher, "模式 设置 "));
    }

    @Test
    void preservesUnrelatedSuggestionRangesAndTooltipObjects() {
        var dispatcher = dispatcher(new AtomicBoolean(false));
        var parse = dispatcher.parse("模式 s", 2);
        var allowedRange = StringRange.between(0, 4);
        Suggestion unrelated = new Suggestion(allowedRange, "set");
        Suggestion blocked = new Suggestion(StringRange.between(3, 4), "set");
        Suggestions raw = new Suggestions(allowedRange, List.of(unrelated, blocked));
        Suggestions filtered = EnglishCommandPolicy.filterSuggestions(parse, raw, 4);
        assertEquals(allowedRange, filtered.getRange());
        assertEquals(List.of(unrelated), filtered.getList());
        assertSame(unrelated, filtered.getList().get(0));
    }

    @Test
    void newlyAddedChineseAliasDoesNotInheritAnOldGate() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(literal("english").executes(context -> 1));
        EnglishCommandPolicy.install(dispatcher, () -> false);
        var translations = new ChineseTranslationRegistry();
        translations.command("english", "中文");
        ChineseCommandTree.install(dispatcher, translations, node -> {}, fail -> fail(fail));
        EnglishCommandPolicy.install(dispatcher, () -> false);
        assertEquals(1, dispatcher.execute("中文", new Object()));
        assertTrue(EnglishCommandPolicy.isBlocked(dispatcher.getRoot().getChild("english")));
    }
}
