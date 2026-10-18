package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import dev.allchineseinstructions.util.UnicodeCommandReader;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.TeamArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps vanilla lookup and validation while allowing Han in objective and team references. */
@Mixin({ObjectiveArgument.class, TeamArgument.class})
public abstract class NamedArgumentMixin {
    @Redirect(method = "parse(Lcom/mojang/brigadier/StringReader;)Ljava/lang/String;",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;",
                    remap = false))
    private String allChineseInstructions$readName(StringReader reader) {
        return UnicodeCommandReader.readUnquotedString(reader);
    }
}
