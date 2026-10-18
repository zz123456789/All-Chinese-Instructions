package dev.allchineseinstructions.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates the syntax of one entity selector without changing user data.
 *
 * <p>All positions are UTF-16 offsets in the complete input, as in Brigadier.
 * Names, resource locations, quoted strings, NBT and score objective names are
 * deliberately copied verbatim. This class has no Minecraft dependencies so
 * consumers can use the same grammar outside a running game.</p>
 */
public final class ChineseSelectorSyntax {
    private static final Map<String, String> ALIASES = immutableMap(
            "选择所有实体", "@e", "选择所有玩家", "@a", "选择最近玩家", "@p",
            "选择随机玩家", "@r", "选择自身", "@s", "选择自己", "@s", "选择执行者", "@s");

    private static final Map<String, String> OPTIONS = immutableMap(
            "名称", "name", "距离", "distance", "等级", "level",
            "横坐标", "x", "纵坐标", "y", "竖坐标", "z",
            "横向范围", "dx", "纵向范围", "dy", "竖向范围", "dz",
            "俯仰角", "x_rotation", "偏航角", "y_rotation",
            "数量", "limit", "排序", "sort", "游戏模式", "gamemode",
            "队伍", "team", "类型", "type", "标签", "tag", "实体数据", "nbt",
            "分数", "scores", "进度", "advancements", "谓词", "predicate",
            "x坐标", "x", "y坐标", "y", "z坐标", "z",
            "x范围", "dx", "y范围", "dy", "z范围", "dz");

    private static final Map<String, String> GAME_MODES = immutableMap(
            "生存", "survival", "创造", "creative", "冒险", "adventure",
            "旁观", "spectator", "旁观者", "spectator");

    private static final Map<String, String> SORT_ORDERS = immutableMap(
            "最近", "nearest", "最远", "furthest", "随机", "random", "任意", "arbitrary");

    private static final Map<String, String> BOOLEANS = immutableMap("真", "true", "假", "false");

    private ChineseSelectorSyntax() {
    }

    /** Chinese selector aliases, in preferred suggestion order. */
    public static Map<String, String> aliases() {
        return ALIASES;
    }

    /** Chinese option names mapped to the 21 vanilla option keys. */
    public static Map<String, String> options() {
        return OPTIONS;
    }

    /** Chinese enum values for a vanilla option key; arbitrary data is excluded. */
    public static Map<String, String> values(String vanillaOption) {
        return switch (vanillaOption) {
            case "gamemode" -> GAME_MODES;
            case "sort" -> SORT_ORDERS;
            case "advancements" -> BOOLEANS;
            default -> Map.of();
        };
    }

    /** Whether a complete Chinese selector alias starts at this offset. */
    public static boolean isChineseSelectorAt(String input, int start) {
        Objects.requireNonNull(input, "input");
        return start >= 0 && start < input.length() && chineseAliasAt(input, start) != null;
    }

    /** The vanilla selector for a complete Chinese alias at this position, or null. */
    public static String selectorAt(String input, int start) {
        if (!isChineseSelectorAt(input, start)) {
            return null;
        }
        return ALIASES.get(chineseAliasAt(input, start));
    }

    /**
     * The completion slot at the end of an input truncated to the current cursor.
     * Nested NBT, score objectives, resource names and quoted data never become
     * option-name or translated-enum completion slots.
     */
    public static Completion completionContext(String input, int start) {
        Objects.requireNonNull(input, "input");
        if (start < 0 || start > input.length()) {
            return none(input.length());
        }
        String alias = chineseAliasAt(input, start);
        int cursor;
        if (alias != null) {
            cursor = start + alias.length();
        } else if (start + 1 < input.length() && input.charAt(start) == '@'
                && "eaprs".indexOf(input.charAt(start + 1)) >= 0) {
            cursor = start + 2;
        } else {
            String prefix = input.substring(start);
            boolean matches = ALIASES.keySet().stream().anyMatch(key -> key.startsWith(prefix))
                    || ALIASES.values().stream().anyMatch(value -> value.startsWith(prefix));
            return matches ? new Completion("selector", start, "", prefix) : none(input.length());
        }
        if (cursor == input.length()) {
            return new Completion("selector", start, "", input.substring(start));
        }
        if (input.charAt(cursor++) != '[') {
            return none(input.length());
        }
        while (cursor <= input.length()) {
            cursor = skipWhitespace(input, cursor, input.length());
            int keyStart = cursor;
            if (cursor == input.length()) {
                return new Completion("option", keyStart, "", "");
            }
            if (isQuote(input.charAt(cursor)) || input.charAt(cursor) == ']') {
                return none(input.length());
            }
            while (cursor < input.length() && !Character.isWhitespace(input.charAt(cursor))
                    && "=,[]{}".indexOf(input.charAt(cursor)) < 0) {
                cursor++;
            }
            String key = input.substring(keyStart, cursor);
            String vanillaKey = OPTIONS.getOrDefault(key, key);
            if (cursor == input.length()) {
                return new Completion("option", keyStart, vanillaKey, key);
            }
            cursor = skipWhitespace(input, cursor, input.length());
            if (cursor == input.length() || input.charAt(cursor) != '=') {
                return none(input.length());
            }
            int valueStart = ++cursor;
            cursor = valueEnd(input, valueStart);
            if (cursor == input.length()) {
                if (vanillaKey.equals("advancements")) {
                    return advancementCompletion(input, valueStart);
                }
                if (vanillaKey.equals("gamemode") || vanillaKey.equals("sort")) {
                    valueStart = skipWhitespace(input, valueStart, input.length());
                    if (vanillaKey.equals("gamemode") && valueStart < input.length()
                            && input.charAt(valueStart) == '!') {
                        valueStart = skipWhitespace(input, valueStart + 1, input.length());
                    }
                    return scalarCompletion(input, valueStart, vanillaKey);
                }
                return none(input.length());
            }
            if (input.charAt(cursor) != ',') {
                return none(input.length());
            }
            cursor++;
        }
        return none(input.length());
    }

    private static Completion advancementCompletion(String input, int start) {
        int cursor = start;
        int lastEquals = -1;
        while (cursor < input.length()) {
            if (isQuote(input.charAt(cursor))) {
                cursor = quotedEnd(input, cursor, input.length());
                continue;
            }
            if (input.charAt(cursor) == '=') {
                lastEquals = cursor;
            }
            cursor++;
        }
        return lastEquals >= 0
                ? scalarCompletion(input, skipWhitespace(input, lastEquals + 1, input.length()), "advancements")
                : none(input.length());
    }

    private static Completion scalarCompletion(String input, int start, String option) {
        String prefix = input.substring(start);
        for (int cursor = 0; cursor < prefix.length(); cursor++) {
            char character = prefix.charAt(cursor);
            if (Character.isWhitespace(character) || isQuote(character) || "{}[]=,!".indexOf(character) >= 0) {
                return none(input.length());
            }
        }
        return new Completion("value", start, option, prefix);
    }

    private static Completion none(int cursor) {
        return new Completion("none", cursor, "", "");
    }

    /** kind is selector, option, value or none; start is a global replacement offset. */
    public record Completion(String kind, int start, String option, String prefix) {
    }

    /**
     * Rewrite just the selector beginning at {@code start}, keeping every other
     * character in the command. Incomplete options remain usable for completion.
     */
    public static Rewrite transform(String input, int start) {
        Objects.requireNonNull(input, "input");
        if (start < 0 || start > input.length()) {
            throw new IndexOutOfBoundsException("Selector start outside input: " + start);
        }

        List<Edit> edits = new ArrayList<>();
        String alias = chineseAliasAt(input, start);
        int selectorEnd;
        if (alias != null) {
            selectorEnd = start + alias.length();
            edits.add(new Edit(start, selectorEnd, ALIASES.get(alias)));
        } else if (start + 1 < input.length() && input.charAt(start) == '@'
                && "eaprs".indexOf(input.charAt(start + 1)) >= 0) {
            selectorEnd = start + 2;
        } else {
            return rewrite(input, start, start, edits);
        }

        if (selectorEnd >= input.length() || input.charAt(selectorEnd) != '[') {
            return rewrite(input, start, selectorEnd, edits);
        }

        int cursor = selectorEnd + 1;
        while (cursor < input.length()) {
            cursor = skipWhitespace(input, cursor, input.length());
            if (cursor == input.length()) {
                break;
            }
            if (input.charAt(cursor) == ']') {
                cursor++;
                break;
            }
            int keyStart = cursor;
            if (isQuote(input.charAt(cursor))) {
                cursor = quotedEnd(input, cursor, input.length());
            } else {
                while (cursor < input.length() && !Character.isWhitespace(input.charAt(cursor))
                        && "=,[]{}".indexOf(input.charAt(cursor)) < 0) {
                    cursor++;
                }
            }
            int keyEnd = cursor;
            String key = input.substring(keyStart, keyEnd);
            String vanillaKey = OPTIONS.getOrDefault(key, key);
            if (!key.equals(vanillaKey)) {
                edits.add(new Edit(keyStart, keyEnd, vanillaKey));
            }
            cursor = skipWhitespace(input, cursor, input.length());
            if (cursor >= input.length() || input.charAt(cursor) != '=') {
                // An unfinished/invalid key belongs to the parser, not this translator.
                break;
            }
            int valueStart = ++cursor;
            int valueEnd = valueEnd(input, valueStart);
            if (vanillaKey.equals("gamemode") || vanillaKey.equals("sort")) {
                translateEnum(input, valueStart, valueEnd, vanillaKey, edits);
            } else if (vanillaKey.equals("advancements")) {
                translateAdvancementBooleans(input, valueStart, valueEnd, edits);
            }
            cursor = valueEnd;
            if (cursor >= input.length()) {
                break;
            }
            if (input.charAt(cursor) == ']') {
                cursor++;
                break;
            }
            if (input.charAt(cursor) == ',') {
                cursor++;
            } else {
                break;
            }
        }
        return rewrite(input, start, cursor, edits);
    }

    private static String chineseAliasAt(String input, int start) {
        for (String alias : ALIASES.keySet()) {
            if (input.startsWith(alias, start)) {
                int end = start + alias.length();
                if (end == input.length() || input.charAt(end) == '['
                        || Character.isWhitespace(input.charAt(end))) {
                    return alias;
                }
            }
        }
        return null;
    }

    /** Find an outer comma/closing bracket, respecting nested data and escapes. */
    private static int valueEnd(String input, int start) {
        int braces = 0;
        int brackets = 0;
        int cursor = start;
        while (cursor < input.length()) {
            char character = input.charAt(cursor);
            if (isQuote(character)) {
                cursor = quotedEnd(input, cursor, input.length());
                continue;
            }
            if (character == '{') {
                braces++;
            } else if (character == '}') {
                braces = Math.max(0, braces - 1);
            } else if (character == '[') {
                brackets++;
            } else if (character == ']') {
                if (brackets == 0 && braces == 0) {
                    break;
                }
                brackets = Math.max(0, brackets - 1);
            } else if (character == ',' && braces == 0 && brackets == 0) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private static void translateEnum(String input, int start, int end, String key, List<Edit> edits) {
        start = skipWhitespace(input, start, end);
        while (end > start && Character.isWhitespace(input.charAt(end - 1))) {
            end--;
        }
        if (key.equals("gamemode") && start < end && input.charAt(start) == '!') {
            start = skipWhitespace(input, start + 1, end);
        }
        String replacement = values(key).get(input.substring(start, end));
        if (replacement != null) {
            edits.add(new Edit(start, end, replacement));
        }
    }

    private static void translateAdvancementBooleans(String input, int start, int end, List<Edit> edits) {
        int cursor = start;
        while (cursor < end) {
            char character = input.charAt(cursor);
            if (isQuote(character)) {
                cursor = quotedEnd(input, cursor, end);
                continue;
            }
            if (character == '=') {
                int tokenStart = skipWhitespace(input, cursor + 1, end);
                int tokenEnd = tokenStart;
                while (tokenEnd < end && !Character.isWhitespace(input.charAt(tokenEnd))
                        && ",{}[]=".indexOf(input.charAt(tokenEnd)) < 0) {
                    tokenEnd++;
                }
                String replacement = BOOLEANS.get(input.substring(tokenStart, tokenEnd));
                if (replacement != null) {
                    edits.add(new Edit(tokenStart, tokenEnd, replacement));
                    cursor = tokenEnd;
                    continue;
                }
            }
            cursor++;
        }
    }

    private static int quotedEnd(String input, int start, int end) {
        char quote = input.charAt(start);
        int cursor = start + 1;
        while (cursor < end) {
            char character = input.charAt(cursor++);
            if (character == '\\' && cursor < end) {
                cursor++;
            } else if (character == quote) {
                break;
            }
        }
        return cursor;
    }

    private static boolean isQuote(char character) {
        return character == '"' || character == '\'';
    }

    private static int skipWhitespace(String input, int start, int end) {
        while (start < end && Character.isWhitespace(input.charAt(start))) {
            start++;
        }
        return start;
    }

    private static Map<String, String> immutableMap(String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put(pairs[index], pairs[index + 1]);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Rewrite rewrite(String original, int start, int originalEnd, List<Edit> edits) {
        int translatedLength = original.length();
        for (Edit edit : edits) {
            translatedLength += edit.replacement.length() - (edit.end - edit.start);
        }
        StringBuilder translated = new StringBuilder(translatedLength);
        int[] originalToTranslated = new int[original.length() + 1];
        int[] translatedToOriginal = new int[translatedLength + 1];
        int originalPosition = 0;
        for (Edit edit : edits) {
            appendUnchanged(original, originalPosition, edit.start, translated,
                    originalToTranslated, translatedToOriginal);
            int translatedStart = translated.length();
            int oldLength = edit.end - edit.start;
            int newLength = edit.replacement.length();
            translated.append(edit.replacement);
            for (int offset = 0; offset <= oldLength; offset++) {
                originalToTranslated[edit.start + offset] = translatedStart + offset * newLength / oldLength;
            }
            for (int offset = 0; offset <= newLength; offset++) {
                translatedToOriginal[translatedStart + offset] = edit.start + offset * oldLength / newLength;
            }
            originalPosition = edit.end;
        }
        appendUnchanged(original, originalPosition, original.length(), translated,
                originalToTranslated, translatedToOriginal);
        return new Rewrite(original, translated.toString(), start, originalEnd,
                originalToTranslated[originalEnd], !edits.isEmpty(),
                originalToTranslated, translatedToOriginal);
    }

    private static void appendUnchanged(String original, int from, int to, StringBuilder translated,
                                        int[] originalToTranslated, int[] translatedToOriginal) {
        int translatedStart = translated.length();
        translated.append(original, from, to);
        for (int offset = 0; offset <= to - from; offset++) {
            originalToTranslated[from + offset] = translatedStart + offset;
            translatedToOriginal[translatedStart + offset] = from + offset;
        }
    }

    private record Edit(int start, int end, String replacement) {
    }

    /** The rewritten command and mappings for parser cursors, errors and suggestions. */
    public static final class Rewrite {
        private final String original;
        private final String translated;
        private final int start;
        private final int originalEnd;
        private final int translatedEnd;
        private final boolean changed;
        private final int[] originalToTranslated;
        private final int[] translatedToOriginal;

        private Rewrite(String original, String translated, int start, int originalEnd, int translatedEnd,
                        boolean changed, int[] originalToTranslated, int[] translatedToOriginal) {
            this.original = original;
            this.translated = translated;
            this.start = start;
            this.originalEnd = originalEnd;
            this.translatedEnd = translatedEnd;
            this.changed = changed;
            this.originalToTranslated = originalToTranslated;
            this.translatedToOriginal = translatedToOriginal;
        }

        public String original() { return original; }
        public String translated() { return translated; }
        public int start() { return start; }
        public int originalEnd() { return originalEnd; }
        public int translatedEnd() { return translatedEnd; }
        public boolean changed() { return changed; }

        /** Translate a cursor from the rewritten command back to the user's input. */
        public int originalCursor(int translatedCursor) {
            return translatedToOriginal[Math.max(0, Math.min(translatedCursor, translated.length()))];
        }

        /** Translate a cursor from the user's input to the rewritten command. */
        public int translatedCursor(int originalCursor) {
            return originalToTranslated[Math.max(0, Math.min(originalCursor, original.length()))];
        }
    }
}
