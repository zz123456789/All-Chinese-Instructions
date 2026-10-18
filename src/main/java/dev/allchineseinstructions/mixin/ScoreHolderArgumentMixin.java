package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import dev.allchineseinstructions.selector.ChineseSelectorSyntax;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sends Chinese selector tokens through vanilla score-holder selector validation. */
@Mixin(ScoreHolderArgument.class)
public abstract class ScoreHolderArgumentMixin {
    @Redirect(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ScoreHolderArgument$Result;",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;peek()C",
                    ordinal = 0, remap = false))
    private char allChineseInstructions$recognizeSelector(StringReader reader) {
        return ChineseSelectorSyntax.isChineseSelectorAt(reader.getString(), reader.getCursor())
                ? '@' : reader.peek();
    }
}
