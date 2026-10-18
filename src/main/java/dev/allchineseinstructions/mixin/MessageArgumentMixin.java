package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import dev.allchineseinstructions.selector.ChineseSelectorSyntax;
import net.minecraft.commands.arguments.MessageArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets vanilla message parsing consume whole Chinese selectors and preserve signed text. */
@Mixin(MessageArgument.Message.class)
public abstract class MessageArgumentMixin {
    @Redirect(method = "parseText",
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;peek()C", remap = false))
    private static char allChineseInstructions$recognizeSelector(StringReader reader) {
        String input = reader.getString();
        int cursor = reader.getCursor();
        if (ChineseSelectorSyntax.isChineseSelectorAt(input, cursor)
                && (cursor == 0 || Character.isWhitespace(input.charAt(cursor - 1)))
                && allChineseInstructions$isUnquotedText(input, cursor)) {
            // Only the branch check sees '@'. EntitySelectorParser receives the
            // untouched reader and consumes the original Chinese token and options.
            return '@';
        }
        return reader.peek();
    }

    @Unique
    private static boolean allChineseInstructions$isUnquotedText(String input, int cursor) {
        char quote = 0;
        boolean escaped = false;
        int brackets = 0;
        int braces = 0;
        for (int index = 0; index < cursor; index++) {
            char current = input.charAt(index);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
            } else if (current == '"' || current == '\'') {
                quote = current;
            } else if (current == '[') {
                brackets++;
            } else if (current == ']') {
                brackets = Math.max(0, brackets - 1);
            } else if (current == '{') {
                braces++;
            } else if (current == '}') {
                braces = Math.max(0, braces - 1);
            }
        }
        return quote == 0 && brackets == 0 && braces == 0;
    }
}
