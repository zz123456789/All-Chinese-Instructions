package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.allchineseinstructions.command.EnglishCommandPolicy;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The server also filters requests from clients that ask for hidden literal completions. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CommandSuggestionsMixin {
    @Redirect(method = "handleCustomCommandSuggestions", at = @At(value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;)Ljava/util/concurrent/CompletableFuture;",
            remap = false))
    private CompletableFuture<Suggestions> allchineseinstructions$filterEnglish(
            CommandDispatcher<CommandSourceStack> dispatcher, ParseResults<CommandSourceStack> parse) {
        return dispatcher.getCompletionSuggestions(parse).thenApply(suggestions ->
                EnglishCommandPolicy.filterSuggestions(parse, suggestions, parse.getReader().getTotalLength()));
    }
}
