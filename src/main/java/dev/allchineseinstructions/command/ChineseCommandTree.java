package dev.allchineseinstructions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.allchineseinstructions.api.ChineseTranslationRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

/** Adds real Brigadier literals while sharing the original execution graph. */
public final class ChineseCommandTree {
    private ChineseCommandTree() {}

    public static <S> void install(CommandDispatcher<S> dispatcher, ChineseTranslationRegistry translations,
                                   Consumer<ArgumentCommandNode<S, ?>> argumentDecorator,
                                   Consumer<String> warning) {
        Set<CommandNode<S>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (CommandNode<S> command : new ArrayList<>(dispatcher.getRoot().getChildren())) {
            decorate(command, command.getName(), translations, argumentDecorator, warning, visited);
            if (command instanceof LiteralCommandNode<S> literal) {
                for (String alias : translations.commandAliases(command.getName())) {
                    addAlias(dispatcher.getRoot(), literal, alias, warning);
                }
            }
        }
    }

    private static <S> void decorate(CommandNode<S> parent, String command,
                                     ChineseTranslationRegistry translations,
                                     Consumer<ArgumentCommandNode<S, ?>> argumentDecorator,
                                     Consumer<String> warning, Set<CommandNode<S>> visited) {
        if (!visited.add(parent)) return;
        if (parent instanceof ArgumentCommandNode<S, ?> argument) argumentDecorator.accept(argument);
        for (CommandNode<S> child : new ArrayList<>(parent.getChildren())) {
            decorate(child, command, translations, argumentDecorator, warning, visited);
            if (child instanceof LiteralCommandNode<S> literal) {
                for (String alias : translations.literalAliases(command, child.getName())) {
                    addAlias(parent, literal, alias, warning);
                }
            }
        }
    }

    private static <S> void addAlias(CommandNode<S> parent, LiteralCommandNode<S> original,
                                     String alias, Consumer<String> warning) {
        if (parent.getChild(alias) != null) {
            warning.accept("Alias collision under " + parent.getName() + ": " + alias);
            return;
        }
        LiteralCommandNode<S> translated = new LiteralCommandNode<>(alias, original.getCommand(),
                original.getRequirement(), original.getRedirect(), original.getRedirectModifier(), original.isFork());
        // Children are shared, retaining parsed argument names, callbacks, forks and permissions.
        for (CommandNode<S> child : original.getChildren()) translated.addChild(child);
        parent.addChild(translated);
    }
}
