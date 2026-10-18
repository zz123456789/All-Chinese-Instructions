package dev.allchineseinstructions.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import java.lang.reflect.Field;

/**
 * Brigadier is loaded in Forge's bootstrap layer, outside Mixin's transformer.
 * Its 1.1.8 field names are stable and unobfuscated. Change only the parser and
 * optional suggestion provider; preserve node identity and the redirect graph.
 */
public final class ArgumentNodeAdapter {
    private static final Field TYPE = field("type");
    private static final Field SUGGESTIONS = field("customSuggestions");

    private ArgumentNodeAdapter() {}

    private static Field field(String name) {
        try {
            Field field = ArgumentCommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Brigadier argument node layout", exception);
        }
    }

    public static void setType(ArgumentCommandNode<?, ?> node, ArgumentType<?> type) {
        set(TYPE, node, type);
    }

    public static <S> void setSuggestions(ArgumentCommandNode<S, ?> node, SuggestionProvider<S> provider) {
        set(SUGGESTIONS, node, provider);
    }

    private static void set(Field field, ArgumentCommandNode<?, ?> node, Object value) {
        try {
            field.set(node, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot extend Brigadier argument node", exception);
        }
    }
}
