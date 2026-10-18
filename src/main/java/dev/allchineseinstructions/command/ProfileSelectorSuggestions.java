package dev.allchineseinstructions.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.allchineseinstructions.argument.LocalizedArgumentType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraftforge.common.ForgeHooks;

/** Restores selector completion when a profile node supplies its own player-name provider. */
public final class ProfileSelectorSuggestions {
    private ProfileSelectorSuggestions() {}

    public static <S> void install(ArgumentCommandNode<S, ?> node) {
        ArgumentType<?> argument = node.getType();
        if (argument instanceof LocalizedArgumentType<?> localized) {
            argument = localized.getDelegate();
        }
        var original = node.getCustomSuggestions();
        if (argument instanceof GameProfileArgument && original != null
                && !(original instanceof ProfileSuggestions<?>)) {
            ArgumentNodeAdapter.setSuggestions(node, new ProfileSuggestions<>(original));
        }
    }

    private record ProfileSuggestions<S>(SuggestionProvider<S> original) implements SuggestionProvider<S> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
                throws CommandSyntaxException {
            CompletableFuture<Suggestions> originalSuggestions = original.getSuggestions(context, builder);
            if (!(context.getSource() instanceof SharedSuggestionProvider source)
                    || !ForgeHooks.canUseEntitySelectors(source)) {
                return originalSuggestions;
            }

            StringReader reader = new StringReader(builder.getInput());
            reader.setCursor(builder.getStart());
            EntitySelectorParser parser = new EntitySelectorParser(reader, true);
            try {
                parser.parse();
            } catch (CommandSyntaxException ignored) {
                // An incomplete selector still carries the parser's completion state.
            }
            SuggestionsBuilder selectorBuilder = new SuggestionsBuilder(builder.getInput(), builder.getStart());
            CompletableFuture<Suggestions> selectors = parser.fillSuggestions(selectorBuilder, names -> {
                // Keep player-name filtering entirely with the original provider:
                // op, deop, whitelist and pardon each use a different name set.
            });
            return originalSuggestions.thenCombine(selectors,
                    (originalResult, selectorResult) -> Suggestions.merge(builder.getInput(),
                            List.of(originalResult, selectorResult)));
        }
    }
}
