package dev.allchineseinstructions;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import dev.allchineseinstructions.api.ChineseTranslationRegistry;
import dev.allchineseinstructions.api.RegisterChineseTranslationsEvent;
import dev.allchineseinstructions.argument.LocalizedArgumentInfo;
import dev.allchineseinstructions.argument.LocalizedArgumentType;
import dev.allchineseinstructions.command.ChineseCommandTree;
import dev.allchineseinstructions.command.EnglishCommandPolicy;
import dev.allchineseinstructions.config.ChineseCommandsConfig;
import dev.allchineseinstructions.gametest.ChineseCommandsGameTests;
import dev.allchineseinstructions.gametest.ChineseSelectorGameTests;
import dev.allchineseinstructions.command.ArgumentNodeAdapter;
import dev.allchineseinstructions.command.ProfileSelectorSuggestions;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AllChineseInstructions.MOD_ID)
public final class AllChineseInstructions {
    public static final String MOD_ID = "allchineseinstructions";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MOD_ID);

    static {
        ARGUMENT_TYPES.register("localized", () -> ArgumentTypeInfos.registerByClass(
                LocalizedArgumentType.argumentClass(), new LocalizedArgumentInfo()));
    }

    public AllChineseInstructions() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChineseCommandsConfig.SPEC,
                ChineseCommandsConfig.FILE_NAME);
        ARGUMENT_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ChineseCommandsGameTests::register);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ChineseSelectorGameTests::register);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::checkEnglishCommand);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        ChineseTranslationRegistry translations = new ChineseTranslationRegistry();
        String resource = "/assets/" + MOD_ID + "/command_translations.json";
        try (var stream = AllChineseInstructions.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing built-in translation catalog");
            translations.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load Chinese command translations", exception);
        }
        Path extra = FMLPaths.CONFIGDIR.get().resolve(MOD_ID).resolve("translations.json");
        if (Files.isRegularFile(extra)) {
            try (var reader = Files.newBufferedReader(extra, StandardCharsets.UTF_8)) {
                translations.load(reader);
            } catch (Exception exception) {
                LOGGER.error("Could not load custom Chinese command translations from {}", extra, exception);
            }
        }
        MinecraftForge.EVENT_BUS.post(new RegisterChineseTranslationsEvent(translations));
        boolean allowEnglish = ChineseCommandsConfig.allowEnglishCommands();
        ChineseCommandTree.install(event.getDispatcher(), translations,
                node -> decorateArgument(node, translations, allowEnglish), LOGGER::warn);
        EnglishCommandPolicy.install(event.getDispatcher(), () -> allowEnglish);
        LOGGER.info("中文指令已注册，共 {} 个顶层指令节点", event.getDispatcher().getRoot().getChildren().size());
    }

    private void checkEnglishCommand(CommandEvent event) {
        if (EnglishCommandPolicy.blockedInput(event.getParseResults())) {
            event.setCanceled(true);
            event.getParseResults().getContext().getSource().sendFailure(
                    Component.literal(ChineseCommandsConfig.RESTRICTION_MESSAGE));
        }
    }

    private static <S> void decorateArgument(ArgumentCommandNode<S, ?> node, ChineseTranslationRegistry translations,
                                             boolean allowEnglish) {
        ProfileSelectorSuggestions.install(node);
        if (node.getType() instanceof LocalizedArgumentType<?>) return;
        if (!ArgumentTypeInfos.isClassRecognized(node.getType().getClass())) return;
        var type = ArgumentTypeInfos.unpack(node.getType()).type();
        var id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(type);
        if (id == null) return;
        var aliases = translations.argumentAliases(id.toString());
        boolean chineseWords = node.getType() instanceof StringArgumentType stringType
                && stringType.getType() != StringArgumentType.StringType.GREEDY_PHRASE;
        if (!aliases.isEmpty() || chineseWords) {
            var localized = new LocalizedArgumentType<>(node.getType(), aliases, allowEnglish);
            ArgumentNodeAdapter.setType(node, localized);
            var originalProvider = node.getCustomSuggestions();
            if (originalProvider != null) {
                com.mojang.brigadier.suggestion.SuggestionProvider<S> provider = (context, builder) ->
                        originalProvider.getSuggestions(context, builder).thenCombine(
                                localized.listSuggestions(context, new SuggestionsBuilder(builder.getInput(), builder.getStart())),
                                (original, translated) -> localized.filterSuggestions(
                                        Suggestions.merge(builder.getInput(), java.util.List.of(original, translated)),
                                        builder.getInput()));
                ArgumentNodeAdapter.setSuggestions(node, provider);
            }
        }
    }
}
