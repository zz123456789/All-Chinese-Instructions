package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.allchineseinstructions.util.UnicodeCommandReader;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supports Chinese names in selector team/tag/scores/name values, preserving vanilla validation. */
@Mixin(EntitySelectorOptions.class)
public abstract class EntitySelectorOptionsMixin {
    // Option readers are compiler-generated lambda methods. Select by the exact
    // Brigadier invocation rather than depending on unstable lambda method names.
    @Redirect(method = "*",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;",
                    remap = false))
    private static String allChineseInstructions$readUnquotedOption(StringReader reader) {
        return UnicodeCommandReader.readUnquotedString(reader);
    }

    @Redirect(method = "*",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;",
                    remap = false))
    private static String allChineseInstructions$readOption(StringReader reader) throws CommandSyntaxException {
        return UnicodeCommandReader.readString(reader);
    }
}
