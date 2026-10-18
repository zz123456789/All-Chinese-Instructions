package dev.allchineseinstructions.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.allchineseinstructions.util.UnicodeCommandReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * Adds aliases for a finite set of tokens without changing the delegate's result type.
 * Only the complete token at the reader's cursor is translated. Free text, quoted
 * strings, NBT and following arguments are never searched or rewritten.
 * Unquoted vanilla string arguments additionally accept Han characters, even
 * with an empty alias table; quoted and greedy strings retain vanilla parsing.
 *
 * <p>Register {@link LocalizedArgumentInfo} before adding this type to a networked
 * command tree. The delegate must itself have a registered ArgumentTypeInfo.</p>
 */
public final class LocalizedArgumentType<T> implements ArgumentType<T> {
    static final int MAX_TOKENS = 4096;
    static final int MAX_ALIASES_PER_TOKEN = 256;
    static final int MAX_TOKEN_LENGTH = 256;
    private static final SimpleCommandExceptionType ENGLISH_DISABLED = new SimpleCommandExceptionType(
            new LiteralMessage("英文指令已被配置文件限制，请将 config/allchineseinstructions-common.toml 中的 allowEnglishCommands 改为 true，然后执行 /重载 或重启游戏/服务器。"));

    private final ArgumentType<T> delegate;
    private final Map<String, List<String>> aliases;
    private final Map<String, String> canonicalByAlias;
    private final Set<String> translatedEnglishTokens;
    private final BooleanSupplier allowEnglish;

    public LocalizedArgumentType(ArgumentType<T> delegate, Map<String, List<String>> aliases) {
        this(delegate, aliases, true);
    }

    public LocalizedArgumentType(ArgumentType<T> delegate, Map<String, List<String>> aliases,
                                 boolean allowEnglish) {
        this(delegate, aliases, () -> allowEnglish);
    }

    public LocalizedArgumentType(ArgumentType<T> delegate, Map<String, List<String>> aliases,
                                 BooleanSupplier allowEnglish) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.aliases = copyAliases(aliases);
        this.allowEnglish = Objects.requireNonNull(allowEnglish, "allowEnglish");
        Map<String, String> lookup = new LinkedHashMap<>();
        this.aliases.forEach((canonical, values) -> values.forEach(alias -> lookup.put(alias, canonical)));
        this.canonicalByAlias = Collections.unmodifiableMap(lookup);
        LinkedHashSet<String> translatedEnglish = new LinkedHashSet<>();
        this.aliases.forEach((canonical, values) -> {
            if (!values.isEmpty() && canonical.chars().allMatch(character -> character < 128)
                    && canonical.chars().anyMatch(character -> character >= 'a' && character <= 'z'
                            || character >= 'A' && character <= 'Z')) {
                translatedEnglish.add(canonical.toLowerCase(Locale.ROOT));
            }
        });
        this.translatedEnglishTokens = Collections.unmodifiableSet(translatedEnglish);
    }

    public ArgumentType<T> getDelegate() {
        return delegate;
    }

    /** Immutable mapping from original tokens to their accepted aliases. */
    public Map<String, List<String>> getAliases() {
        return aliases;
    }

    public boolean isEnglishAllowed() {
        return allowEnglish.getAsBoolean();
    }

    /** The class token required by Forge's ArgumentTypeInfos.registerByClass API. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Class<LocalizedArgumentType<?>> argumentClass() {
        return (Class) LocalizedArgumentType.class;
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String input = reader.getString();
        int end = start;
        while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
            end++;
        }
        String token = input.substring(start, end);
        if (!isEnglishAllowed() && isTranslatedEnglishToken(token)) {
            throw ENGLISH_DISABLED.createWithContext(reader);
        }
        String canonical = canonicalByAlias.get(token);
        if (canonical == null) {
            return parseUntranslated(reader);
        }

        String translatedInput = input.substring(0, start) + canonical + input.substring(end);
        StringReader translated = new StringReader(translatedInput);
        translated.setCursor(start);
        try {
            return delegate.parse(translated);
        } catch (CommandSyntaxException exception) {
            if (exception.getInput() == null || exception.getCursor() < 0) {
                throw exception;
            }
            throw new CommandSyntaxException(exception.getType(), exception.getRawMessage(), input,
                    originalCursor(exception.getCursor(), start, end, canonical.length(), input));
        } finally {
            reader.setCursor(originalCursor(translated.getCursor(), start, end, canonical.length(), input));
        }
    }

    @SuppressWarnings("unchecked")
    private T parseUntranslated(StringReader reader) throws CommandSyntaxException {
        if (delegate instanceof StringArgumentType stringArgument
                && stringArgument.getType() != StringArgumentType.StringType.GREEDY_PHRASE
                && (!reader.canRead() || !StringReader.isQuotedStringStart(reader.peek()))) {
            // The instanceof check guarantees that this delegate's result type is String.
            return (T) UnicodeCommandReader.readUnquotedString(reader);
        }
        return delegate.parse(reader);
    }

    /**
     * Interior offsets use proportional character boundaries. In particular, a
     * delegate that consumes only part of a canonical token must not appear to
     * have consumed the entire alias (even when the alias is one character).
     */
    private static int originalCursor(int cursor, int start, int end, int canonicalLength, String input) {
        if (cursor <= start) {
            return cursor;
        }
        int translatedEnd = start + canonicalLength;
        if (cursor >= translatedEnd) {
            return cursor + end - translatedEnd;
        }
        int mapped = start + (int) ((long) (cursor - start) * (end - start) / canonicalLength);
        if (mapped > start && Character.isLowSurrogate(input.charAt(mapped))
                && Character.isHighSurrogate(input.charAt(mapped - 1))) {
            mapped--;
        }
        return mapped;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                              SuggestionsBuilder builder) {
        SuggestionsBuilder localized = new SuggestionsBuilder(builder.getInput(), builder.getStart());
        String remaining = builder.getRemainingLowerCase();
        aliases.forEach((canonical, values) -> {
            for (String alias : values) {
                if (alias.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    localized.suggest(alias, new LiteralMessage(canonical));
                }
            }
        });
        Suggestions localSuggestions = localized.build();
        return delegate.listSuggestions(context, builder).thenApply(original ->
                Suggestions.merge(builder.getInput(), List.of(
                        filterSuggestions(original, builder.getInput()), localSuggestions)));
    }

    /** Filters delegate and custom-provider suggestions without rewriting their replacement ranges. */
    public Suggestions filterSuggestions(Suggestions suggestions, String input) {
        if (isEnglishAllowed()) {
            return suggestions;
        }
        return Suggestions.create(input, suggestions.getList().stream()
                .filter(suggestion -> !isTranslatedEnglishToken(suggestion.getText())).toList());
    }

    private boolean isTranslatedEnglishToken(String token) {
        return translatedEnglishTokens.contains(token.toLowerCase(Locale.ROOT));
    }

    @Override
    public Collection<String> getExamples() {
        LinkedHashSet<String> examples = new LinkedHashSet<>(delegate.getExamples());
        if (!isEnglishAllowed()) {
            examples.removeIf(this::isTranslatedEnglishToken);
        }
        aliases.values().forEach(examples::addAll);
        return List.copyOf(examples);
    }

    static Map<String, List<String>> copyAliases(Map<String, List<String>> aliases) {
        Objects.requireNonNull(aliases, "aliases");
        if (aliases.size() > MAX_TOKENS) {
            throw new IllegalArgumentException("Too many canonical tokens: " + aliases.size());
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        Map<String, String> seen = new LinkedHashMap<>();
        for (String canonical : aliases.keySet()) {
            validateToken(canonical);
            seen.put(canonical, canonical);
        }
        aliases.forEach((canonical, values) -> {
            Objects.requireNonNull(values, "aliases for " + canonical);
            if (values.size() > MAX_ALIASES_PER_TOKEN) {
                throw new IllegalArgumentException("Too many aliases for " + canonical);
            }
            List<String> unique = new ArrayList<>();
            for (String alias : values) {
                validateToken(alias);
                String existing = seen.putIfAbsent(alias, canonical);
                if (existing != null && !existing.equals(canonical)) {
                    throw new IllegalArgumentException("Ambiguous alias '" + alias + "': " + existing + "/" + canonical);
                }
                if (!alias.equals(canonical) && !unique.contains(alias)) {
                    unique.add(alias);
                }
            }
            copy.put(canonical, List.copyOf(unique));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static void validateToken(String token) {
        Objects.requireNonNull(token, "token");
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH
                || token.codePoints().anyMatch(Character::isWhitespace)
                || token.indexOf('"') >= 0 || token.indexOf('\'') >= 0) {
            throw new IllegalArgumentException("Expected a single unquoted token: " + token);
        }
    }
}
