package dev.allchineseinstructions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Restricts English command literals without removing nodes or changing redirects.
 * Install after Chinese aliases are created, while the command tree is being built.
 * The supplier is read for every check, so a thread-safe configuration supplier can
 * change the policy without rebuilding the graph. Installation itself belongs on
 * the command tree's owning thread; suggestion filtering only reads the graph.
 */
public final class EnglishCommandPolicy {
    // Brigadier is a bootstrap library, outside Mixin's transformer. See ArgumentNodeAdapter.
    private static final Field REQUIREMENT = requirementField();

    private EnglishCommandPolicy() {}

    public static <S> void install(CommandDispatcher<S> dispatcher, BooleanSupplier allowEnglish) {
        install(dispatcher.getRoot(), allowEnglish);
    }

    public static <S> void install(CommandNode<S> root, BooleanSupplier allowEnglish) {
        Objects.requireNonNull(allowEnglish, "allowEnglish");
        Set<CommandNode<S>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<CommandNode<S>> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            CommandNode<S> node = pending.removeFirst();
            if (!visited.add(node)) continue;
            if (node instanceof LiteralCommandNode<S> literal) {
                Predicate<S> original = node.getRequirement();
                if (isEnglishLiteral(literal.getLiteral())) {
                    if (original instanceof GatedRequirement<S> gated) {
                        gated.allowEnglish = allowEnglish;
                    } else {
                        setRequirement(node, new GatedRequirement<>(original, allowEnglish));
                    }
                } else if (original instanceof GatedRequirement<S> gated) {
                    // Also handles an alias added after a previous installation.
                    setRequirement(node, gated.original);
                }
            }
            pending.addAll(node.getChildren());
            if (node.getRedirect() != null) pending.addLast(node.getRedirect());
        }
    }

    public static boolean isBlocked(CommandNode<?> node) {
        return node != null && node.getRequirement() instanceof GatedRequirement<?> gated
                && !gated.allowEnglish.getAsBoolean();
    }

    /** Detect only a known restricted literal where parsing actually stopped. */
    public static <S> boolean blockedInput(ParseResults<S> parse) {
        if (!parse.getReader().canRead()) return false;
        String input = parse.getReader().getString();
        int start = parse.getReader().getCursor();
        while (start < input.length() && input.charAt(start) == ' ') start++;
        int end = input.indexOf(' ', start);
        if (end < 0) end = input.length();
        if (start == end) return false;

        CommandContextBuilder<S> context = parse.getContext().getLastChild();
        CommandNode<S> parent = context.getNodes().isEmpty() ? context.getRootNode()
                : context.getNodes().get(context.getNodes().size() - 1).getNode();
        CommandNode<S> candidate = parent.getChild(input.substring(start, end));
        return isBlocked(candidate);
    }

    /**
     * Brigadier 1.1.8 does not check requirements when gathering suggestions.
     * Remove only exact literal completions at the active command-tree position;
     * resource identifiers, names, free text and their replacement ranges survive.
     */
    public static <S> Suggestions filterSuggestions(ParseResults<S> parse, Suggestions suggestions,
                                                    int cursor) {
        SuggestionContext<S> context = parse.getContext().findSuggestionContext(cursor);
        int start = Math.min(context.startPos, cursor);
        ArrayList<Suggestion> retained = new ArrayList<>(suggestions.getList().size());
        for (Suggestion suggestion : suggestions.getList()) {
            boolean exactLiteralRange = suggestion.getRange().getStart() == start
                    && suggestion.getRange().getEnd() == cursor;
            if (!exactLiteralRange || !isBlocked(context.parent.getChild(suggestion.getText()))) {
                retained.add(suggestion);
            }
        }
        return retained.size() == suggestions.getList().size() ? suggestions
                : new Suggestions(suggestions.getRange(), retained);
    }

    private static boolean isEnglishLiteral(String literal) {
        boolean asciiLetter = false;
        for (int offset = 0; offset < literal.length();) {
            int character = literal.codePointAt(offset);
            if (Character.UnicodeScript.of(character) == Character.UnicodeScript.HAN) return false;
            if (character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z') {
                asciiLetter = true;
            }
            offset += Character.charCount(character);
        }
        return asciiLetter;
    }

    private static Field requirementField() {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Brigadier command node layout", exception);
        }
    }

    private static <S> void setRequirement(CommandNode<S> node, Predicate<S> requirement) {
        try {
            REQUIREMENT.set(node, requirement);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot configure Brigadier command requirement", exception);
        }
    }

    private static final class GatedRequirement<S> implements Predicate<S> {
        private final Predicate<S> original;
        private volatile BooleanSupplier allowEnglish;

        private GatedRequirement(Predicate<S> original, BooleanSupplier allowEnglish) {
            this.original = original;
            this.allowEnglish = allowEnglish;
        }

        @Override
        public boolean test(S source) {
            return allowEnglish.getAsBoolean() && original.test(source);
        }
    }
}
