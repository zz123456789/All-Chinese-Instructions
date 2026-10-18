package dev.allchineseinstructions.selector;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;

/** Adds Chinese completions without changing vanilla permissions or replacement ranges. */
public final class ChineseSelectorSuggestions {
    private ChineseSelectorSuggestions() {}

    public static Suggestions merge(EntitySelectorParser parser, SuggestionsBuilder builder,
                                    boolean allowSelectors, Suggestions vanilla) {
        if (!allowSelectors) return vanilla;
        var context = ChineseSelectorSyntax.completionContext(builder.getInput(), builder.getStart());
        if (context.kind().equals("none")) return vanilla;
        SuggestionsBuilder chinese = new SuggestionsBuilder(builder.getInput(), context.start());
        if (context.kind().equals("selector")) {
            ChineseSelectorSyntax.aliases().forEach((alias, canonical) -> {
                if (alias.startsWith(context.prefix())) chinese.suggest(alias, new LiteralMessage(canonical));
            });
        } else if (context.kind().equals("option")) {
            Set<String> keys = availableOptions(parser);
            ChineseSelectorSyntax.options().forEach((alias, canonical) -> {
                if (keys.contains(canonical) && alias.startsWith(context.prefix())) {
                    chinese.suggest(alias + "=", new LiteralMessage(canonical));
                }
            });
        } else if (context.kind().equals("value")) {
            // The parser may have rejected a duplicate option before reaching its value.
            if (availableOptions(parser).contains(context.option())) {
                boolean gameMode = context.option().equals("gamemode");
                int preceding = context.start() - 1;
                while (preceding >= 0 && Character.isWhitespace(builder.getInput().charAt(preceding))) preceding--;
                boolean inverted = gameMode && preceding >= 0 && builder.getInput().charAt(preceding) == '!';
                ChineseSelectorSyntax.values(context.option()).forEach((alias, canonical) -> {
                    if (!alias.startsWith(context.prefix())) return;
                    if (!gameMode || inverted || !parser.hasGamemodeNotEquals()) {
                        chinese.suggest(alias, new LiteralMessage(canonical));
                    }
                    // With an empty value, vanilla offers both positive and negative modes.
                    // After an exclusion, only additional exclusions remain applicable.
                    if (gameMode && !inverted && context.prefix().isEmpty()) {
                        chinese.suggest("!" + alias, new LiteralMessage("!" + canonical));
                    }
                });
            }
        }
        return Suggestions.merge(builder.getInput(), List.of(vanilla, chinese.build()));
    }

    private static Set<String> availableOptions(EntitySelectorParser parser) {
        SuggestionsBuilder available = new SuggestionsBuilder("", 0);
        EntitySelectorOptions.suggestNames(parser, available);
        return available.build().getList().stream()
                .map(value -> value.getText().replace("=", "")).collect(Collectors.toSet());
    }
}
