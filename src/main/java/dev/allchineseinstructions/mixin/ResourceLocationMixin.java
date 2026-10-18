package dev.allchineseinstructions.mixin;

import dev.allchineseinstructions.util.UnicodeResourceNames;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps constructors, codecs, packet decoding, and command token reading aligned. */
@Mixin(ResourceLocation.class)
public abstract class ResourceLocationMixin {
    @Inject(method = {"validPathChar", "validNamespaceChar", "isAllowedInResourceLocation"},
            at = @At("RETURN"), cancellable = true)
    private static void allChineseInstructions$allowHan(char character,
                                                        CallbackInfoReturnable<Boolean> callback) {
        if (UnicodeResourceNames.isHan(character)) {
            callback.setReturnValue(true);
        }
    }
}
