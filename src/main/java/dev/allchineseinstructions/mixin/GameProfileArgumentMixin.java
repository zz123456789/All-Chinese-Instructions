package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import dev.allchineseinstructions.selector.ChineseSelectorSyntax;
import net.minecraft.commands.arguments.GameProfileArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserves player-only validation and profile lookups for Chinese selector tokens. */
@Mixin(GameProfileArgument.class)
public abstract class GameProfileArgumentMixin {
    @Redirect(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/GameProfileArgument$Result;",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;peek()C",
                    ordinal = 0, remap = false))
    private char allChineseInstructions$recognizeSelector(StringReader reader) {
        return ChineseSelectorSyntax.isChineseSelectorAt(reader.getString(), reader.getCursor())
                ? '@' : reader.peek();
    }
}
