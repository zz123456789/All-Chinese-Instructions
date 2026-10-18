package dev.allchineseinstructions.argument;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LocalizedArgumentTypeTest {
    private static final Map<String, List<String>> BOOL_ALIASES = Map.of(
            "true", List.of("真", "是", "开启"),
            "false", List.of("假", "否", "关闭"));
    private static final SimpleCommandExceptionType ERROR = new SimpleCommandExceptionType(new LiteralMessage("test"));

    private static LocalizedArgumentType<Boolean> bool() {
        return new LocalizedArgumentType<>(BoolArgumentType.bool(), BOOL_ALIASES);
    }

    @Test
    void parsesEnglishAndChineseBooleansWithOriginalCursor() throws Exception {
        for (String input : List.of("true", "真", "是", "开启")) {
            StringReader reader = new StringReader(input);
            assertTrue(bool().parse(reader), input);
            assertEquals(input.length(), reader.getCursor());
        }
        for (String input : List.of("false", "假", "否", "关闭")) {
            StringReader reader = new StringReader(input);
            assertFalse(bool().parse(reader), input);
            assertEquals(input.length(), reader.getCursor());
        }
    }

    @Test
    void changesOnlyCurrentWholeTokenAndPreservesSuffix() throws Exception {
        String input = "cmd 开启 假 {text:\"真\"}";
        StringReader reader = new StringReader(input);
        reader.setCursor(4);
        assertTrue(bool().parse(reader));
        assertEquals(6, reader.getCursor());
        assertEquals(" 假 {text:\"真\"}", reader.getRemaining());
        assertEquals(input, reader.getString());
    }

    @Test
    void neitherPrefixesNorNbtNorQuotedAliasesAreTranslated() {
        for (String input : List.of("真假", "真{}", "{text:\"真\"}", "\"真\"")) {
            StringReader reader = new StringReader(input);
            assertThrows(CommandSyntaxException.class, () -> bool().parse(reader), input);
        }
    }

    @Test
    void delegateSeesUntouchedLaterTextEvenWhenItReadsPastFirstToken() throws Exception {
        var argument = new LocalizedArgumentType<>(StringArgumentType.greedyString(), BOOL_ALIASES);
        StringReader reader = new StringReader("真 假 {text:\"真\"}");
        assertEquals("true 假 {text:\"真\"}", argument.parse(reader));
        assertEquals(reader.getTotalLength(), reader.getCursor());
    }

    @Test
    void mapsErrorAtTokenEndBackToOriginalInput() {
        ArgumentType<String> failing = reader -> {
            reader.readUnquotedString();
            throw ERROR.createWithContext(reader);
        };
        StringReader reader = new StringReader("cmd 开启 next");
        reader.setCursor(4);
        var argument = new LocalizedArgumentType<>(failing, BOOL_ALIASES);
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> argument.parse(reader));
        assertEquals("cmd 开启 next", exception.getInput());
        assertEquals(6, exception.getCursor());
        assertEquals(6, reader.getCursor());
        assertSame(ERROR, exception.getType());
    }

    @Test
    void preservesResetReaderAndIndependentErrorCursor() {
        ArgumentType<String> failing = reader -> {
            int start = reader.getCursor();
            reader.readUnquotedString();
            CommandSyntaxException exception = ERROR.createWithContext(reader);
            reader.setCursor(start);
            throw exception;
        };
        StringReader reader = new StringReader("cmd 真 next");
        reader.setCursor(4);
        var argument = new LocalizedArgumentType<>(failing, BOOL_ALIASES);
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> argument.parse(reader));
        assertEquals(5, exception.getCursor());
        assertEquals(4, reader.getCursor());
    }

    @Test
    void partialConsumptionDoesNotAdvanceToEndOfShortAlias() throws Exception {
        ArgumentType<Character> partial = StringReader::read;
        StringReader reader = new StringReader("cmd 真 next");
        reader.setCursor(4);
        var argument = new LocalizedArgumentType<>(partial, BOOL_ALIASES);
        assertEquals(Character.valueOf('t'), argument.parse(reader));
        assertEquals(4, reader.getCursor());
        assertEquals("真 next", reader.getRemaining());
    }

    @Test
    void mapsInteriorErrorsAndErrorsAfterTranslatedToken() {
        ArgumentType<String> partial = reader -> {
            reader.setCursor(reader.getCursor() + 2);
            throw ERROR.createWithContext(reader);
        };
        StringReader interior = new StringReader("cmd 开启 next");
        interior.setCursor(4);
        var partialArgument = new LocalizedArgumentType<>(partial, BOOL_ALIASES);
        CommandSyntaxException middle = assertThrows(CommandSyntaxException.class, () -> partialArgument.parse(interior));
        assertEquals(5, middle.getCursor());
        assertEquals(5, interior.getCursor());

        ArgumentType<String> beyond = reader -> {
            reader.readUnquotedString();
            reader.skipWhitespace();
            reader.skip();
            throw ERROR.createWithContext(reader);
        };
        StringReader suffix = new StringReader("cmd 真 next");
        suffix.setCursor(4);
        var beyondArgument = new LocalizedArgumentType<>(beyond, BOOL_ALIASES);
        CommandSyntaxException after = assertThrows(CommandSyntaxException.class, () -> beyondArgument.parse(suffix));
        assertEquals(7, after.getCursor());
        assertEquals(7, suffix.getCursor());
    }

    @Test
    void combinesEnglishAndChineseSuggestions() {
        Suggestions suggestions = bool().listSuggestions(null, new SuggestionsBuilder("cmd ", 4)).join();
        assertEquals(Set.of("true", "false", "真", "是", "开启", "假", "否", "关闭"),
                suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toSet()));
        assertEquals(StringRange.at(4), suggestions.getRange());
    }

    @Test
    void chinesePrefixCompletionHasOriginalReplacementRange() {
        Suggestions suggestions = bool().listSuggestions(null, new SuggestionsBuilder("cmd 开", 4)).join();
        assertEquals(1, suggestions.getList().size());
        Suggestion suggestion = suggestions.getList().get(0);
        assertEquals("开启", suggestion.getText());
        assertEquals(StringRange.between(4, 5), suggestion.getRange());
        assertEquals("cmd 开启", suggestion.apply("cmd 开"));
        assertEquals("true", suggestion.getTooltip().getString());
    }

    @Test
    void preservesDelegateSuggestionOffsets() {
        ArgumentType<String> offsetType = new ArgumentType<>() {
            @Override
            public String parse(StringReader reader) {
                return reader.readUnquotedString();
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return builder.createOffset(builder.getStart() + 1).suggest("x").buildFuture();
            }
        };
        var argument = new LocalizedArgumentType<>(offsetType, BOOL_ALIASES);
        Suggestions suggestions = argument.listSuggestions(null, new SuggestionsBuilder("cmd ab", 4)).join();
        assertEquals(StringRange.between(5, 6), suggestions.getRange());
        assertEquals("cmd ax", suggestions.getList().get(0).apply("cmd ab"));
    }

    @Test
    void dispatcherParsesFollowingArgumentAndCompletesAtCursor() throws Exception {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.<Object, Boolean>argument("flag", bool())
                        .then(RequiredArgumentBuilder.<Object, Integer>argument("count", IntegerArgumentType.integer())
                                .executes(context -> BoolArgumentType.getBool(context, "flag")
                                        ? IntegerArgumentType.getInteger(context, "count") : 0))));
        assertEquals(23, dispatcher.execute("cmd 开启 23", new Object()));
        assertEquals(0, dispatcher.execute("cmd 假 23", new Object()));
        assertEquals(23, dispatcher.execute("cmd true 23", new Object()));
        var parsed = dispatcher.parse("cmd 开 23", new Object());
        Suggestions suggestions = dispatcher.getCompletionSuggestions(parsed, 5).join();
        assertEquals(StringRange.between(4, 5), suggestions.getRange());
        assertEquals("cmd 开启 23", suggestions.getList().get(0).apply("cmd 开 23"));
    }

    @Test
    void defensivelyCopiesAliasTableAndRejectsAmbiguity() {
        List<String> truthy = new ArrayList<>(List.of("真"));
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("true", truthy);
        var argument = new LocalizedArgumentType<>(BoolArgumentType.bool(), aliases);
        truthy.clear();
        aliases.clear();
        assertEquals(Map.of("true", List.of("真")), argument.getAliases());
        assertThrows(UnsupportedOperationException.class, () -> argument.getAliases().clear());
        assertThrows(IllegalArgumentException.class, () -> new LocalizedArgumentType<>(BoolArgumentType.bool(),
                Map.of("true", List.of("是"), "false", List.of("是"))));
        assertThrows(IllegalArgumentException.class, () -> new LocalizedArgumentType<>(BoolArgumentType.bool(),
                Map.of("true", List.of("false"), "false", List.of("否"))));
    }

    @Test
    void emptyAliasTableAcceptsChineseWordsAndPreservesFollowingArguments() throws Exception {
        for (StringArgumentType type : List.of(StringArgumentType.word(), StringArgumentType.string())) {
            var argument = new LocalizedArgumentType<>(type, Map.of());
            StringReader reader = new StringReader("cmd 生存积分A_1 23");
            reader.setCursor(4);
            assertEquals("生存积分A_1", argument.parse(reader));
            assertEquals(11, reader.getCursor());
            assertEquals(" 23", reader.getRemaining());
        }
    }

    @Test
    void chineseObjectiveWordWorksInDispatcherWithFollowingArgument() throws Exception {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("score")
                .then(RequiredArgumentBuilder.<Object, String>argument("objective",
                        new LocalizedArgumentType<>(StringArgumentType.word(), Map.of()))
                        .then(RequiredArgumentBuilder.<Object, Integer>argument("value", IntegerArgumentType.integer())
                                .executes(context -> {
                                    assertEquals("生存积分", StringArgumentType.getString(context, "objective"));
                                    return IntegerArgumentType.getInteger(context, "value");
                                }))));
        assertEquals(37, dispatcher.execute("score 生存积分 37", new Object()));
    }

    @Test
    void quotedStringsRetainVanillaEscapesAndErrors() throws Exception {
        var argument = new LocalizedArgumentType<>(StringArgumentType.string(), Map.of());
        StringReader reader = new StringReader("cmd \"中文 \\\"标题\\\"\" next");
        reader.setCursor(4);
        assertEquals("中文 \"标题\"", argument.parse(reader));
        assertEquals(" next", reader.getRemaining());

        StringReader malformed = new StringReader("\"中文\\q\"");
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> argument.parse(malformed));
        assertEquals(4, exception.getCursor());
        assertEquals(malformed.getString(), exception.getInput());

        var word = new LocalizedArgumentType<>(StringArgumentType.word(), Map.of());
        StringReader quotedWord = new StringReader("\"中文\"");
        assertEquals("", word.parse(quotedWord));
        assertEquals(0, quotedWord.getCursor());
    }

    @Test
    void unquotedChineseWordsStopBeforeOriginalForbiddenPunctuation() throws Exception {
        var argument = new LocalizedArgumentType<>(StringArgumentType.word(), Map.of());
        for (String separator : List.of(":", "/", "，", "。", "{", "[", "@", "!", "é", "😀")) {
            StringReader reader = new StringReader("中文" + separator + "后续");
            assertEquals("中文", argument.parse(reader), separator);
            assertEquals(2, reader.getCursor(), separator);
            assertEquals(separator + "后续", reader.getRemaining(), separator);
        }
    }

    @Test
    void englishAndGreedyStringBehaviorStayUnchanged() throws Exception {
        for (StringArgumentType type : List.of(StringArgumentType.word(), StringArgumentType.string())) {
            var argument = new LocalizedArgumentType<>(type, Map.of());
            for (String input : List.of("abc_123-.+ next", "abc:def", "", "@p")) {
                StringReader vanillaReader = new StringReader(input);
                StringReader localizedReader = new StringReader(input);
                assertEquals(type.parse(vanillaReader), argument.parse(localizedReader), input);
                assertEquals(vanillaReader.getCursor(), localizedReader.getCursor(), input);
            }
        }
        var greedy = new LocalizedArgumentType<>(StringArgumentType.greedyString(), Map.of());
        StringReader reader = new StringReader("中文消息 保留标点，\"引号\" @p");
        assertEquals(reader.getString(), greedy.parse(reader));
        assertEquals(reader.getTotalLength(), reader.getCursor());
    }
}
