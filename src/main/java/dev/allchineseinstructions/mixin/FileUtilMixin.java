package dev.allchineseinstructions.mixin;

import dev.allchineseinstructions.util.UnicodeResourceNames;
import net.minecraft.FileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows directory data packs to resolve the same Han paths as ZIP data packs. */
@Mixin(FileUtil.class)
public abstract class FileUtilMixin {
    @Inject(method = "isValidStrictPathSegment", at = @At("RETURN"), cancellable = true)
    private static void allChineseInstructions$allowHanSegments(String segment,
                                                               CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ() && UnicodeResourceNames.isValidStrictPathSegment(segment)) {
            callback.setReturnValue(true);
        }
    }
}
