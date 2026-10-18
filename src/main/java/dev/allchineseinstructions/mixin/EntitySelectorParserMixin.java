package dev.allchineseinstructions.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.allchineseinstructions.selector.ChineseSelectorSyntax;
import dev.allchineseinstructions.selector.ChineseSelectorSuggestions;
import dev.allchineseinstructions.util.UnicodeCommandReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Parses Chinese selector syntax through vanilla while keeping original input offsets. */
@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserMixin {
    @Shadow @Final @Mutable private StringReader reader;
    @Shadow @Final private boolean allowSelectors;
    @Unique private boolean allchineseinstructions$parsing;
    @Unique private boolean allchineseinstructions$suggesting;
    @Unique private ChineseSelectorSyntax.Rewrite allchineseinstructions$rewrite;
    @Unique private StringReader allchineseinstructions$translatedReader;

    @Inject(method = "parse", at = @At("HEAD"), cancellable = true)
    private void allchineseinstructions$parseChinese(CallbackInfoReturnable<EntitySelector> callback)
            throws CommandSyntaxException {
        if (allchineseinstructions$parsing) return;
        var rewrite = ChineseSelectorSyntax.transform(reader.getString(), reader.getCursor());
        if (!rewrite.changed()) return;
        allchineseinstructions$rewrite = rewrite;
        StringReader original = reader;
        StringReader translated = new StringReader(rewrite.translated());
        translated.setCursor(rewrite.translatedCursor(original.getCursor()));
        allchineseinstructions$translatedReader = translated;
        allchineseinstructions$parsing = true;
        reader = translated;
        try {
            callback.setReturnValue(((EntitySelectorParser) (Object) this).parse());
        } catch (CommandSyntaxException exception) {
            if (exception.getInput() == null || exception.getCursor() < 0) throw exception;
            throw new CommandSyntaxException(exception.getType(), exception.getRawMessage(),
                    original.getString(), rewrite.originalCursor(exception.getCursor()));
        } finally {
            original.setCursor(rewrite.originalCursor(translated.getCursor()));
            reader = original;
            allchineseinstructions$parsing = false;
        }
    }

    @Inject(method = "fillSuggestions", at = @At("HEAD"), cancellable = true)
    private void allchineseinstructions$suggestChinese(SuggestionsBuilder builder,
            Consumer<SuggestionsBuilder> names,
            CallbackInfoReturnable<CompletableFuture<Suggestions>> callback) {
        if (allchineseinstructions$suggesting) return;
        EntitySelectorParser parser = (EntitySelectorParser) (Object) this;
        StringReader original = reader;
        var rewrite = allchineseinstructions$rewrite;
        allchineseinstructions$suggesting = true;
        CompletableFuture<Suggestions> future;
        try {
            SuggestionsBuilder canonicalBuilder = builder;
            if (rewrite != null) {
                reader = allchineseinstructions$translatedReader;
                canonicalBuilder = new SuggestionsBuilder(rewrite.translated(), rewrite.translatedCursor(builder.getStart()));
            }
            future = parser.fillSuggestions(canonicalBuilder, names);
        } finally {
            reader = original;
            allchineseinstructions$suggesting = false;
        }
        callback.setReturnValue(future.thenApply(suggestions -> {
            Suggestions mapped = suggestions;
            if (rewrite != null) {
                var entries = suggestions.getList().stream().map(suggestion -> new Suggestion(
                        StringRange.between(rewrite.originalCursor(suggestion.getRange().getStart()),
                                rewrite.originalCursor(suggestion.getRange().getEnd())),
                        suggestion.getText(), suggestion.getTooltip())).toList();
                mapped = Suggestions.create(builder.getInput(), entries);
            }
            return ChineseSelectorSuggestions.merge(parser, builder, allowSelectors, mapped);
        }));
    }

    @Redirect(method = {"parseNameOrUUID", "parseOptions"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;",
                    remap = false))
    private String allChineseInstructions$readSelectorString(StringReader reader) throws CommandSyntaxException {
        return UnicodeCommandReader.readString(reader);
    }
}
