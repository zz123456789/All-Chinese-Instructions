package dev.allchineseinstructions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import dev.allchineseinstructions.api.ChineseTranslationRegistry;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.*;

class ChineseCommandTreeTest {
    private ChineseTranslationRegistry translations() {
        var registry = new ChineseTranslationRegistry();
        registry.command("mode", "模式");
        registry.literal("mode", "set", "设置");
        registry.command("exec", "执行");
        registry.literal("exec", "run", "运行");
        return registry;
    }

    @Test
    void keepsCallbacksArgumentNamesAndEnglishSyntax() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(literal("mode").requires(source -> (int) source >= 2)
                .then(literal("set").then(argument("value", StringArgumentType.word())
                        .executes(context -> StringArgumentType.getString(context, "value").length()))));
        List<String> decorated = new ArrayList<>();
        ChineseCommandTree.install(dispatcher, translations(), node -> decorated.add(node.getName()), fail -> fail(fail));
        assertEquals(4, dispatcher.execute("模式 设置 test", 2));
        assertEquals(4, dispatcher.execute("mode set test", 2));
        assertEquals(4, dispatcher.execute("mode 设置 test", 2));
        assertEquals(List.of("value"), decorated);
        assertThrows(Exception.class, () -> dispatcher.execute("模式 设置 test", 0));
        assertFalse(dispatcher.getRoot().getChild("模式").canUse(0));
    }

    @Test
    void preservesExecuteRedirectAndNestedSuggestions() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(literal("mode").then(literal("set").executes(context -> 7)));
        CommandNode<Object> execute = dispatcher.register(literal("exec"));
        execute.addChild(literal("run").redirect(dispatcher.getRoot()).build());
        ChineseCommandTree.install(dispatcher, translations(), node -> {}, fail -> fail(fail));
        assertEquals(7, dispatcher.execute("执行 运行 模式 设置", new Object()));
        var result = dispatcher.getCompletionSuggestions(dispatcher.parse("模式 设", new Object())).get();
        assertTrue(result.getList().stream().anyMatch(suggestion -> suggestion.getText().equals("设置")));
    }

    @Test
    void collisionsDoNotReplaceExistingCommands() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(literal("mode").executes(context -> 1));
        dispatcher.register(literal("模式").executes(context -> 2));
        List<String> warnings = new ArrayList<>();
        ChineseCommandTree.install(dispatcher, translations(), node -> {}, warnings::add);
        assertEquals(2, dispatcher.execute("模式", new Object()));
        assertEquals(1, warnings.size());
    }

    @Test
    void loadsBuiltinTranslationsAndRejectsInvalidAliases() throws Exception {
        var registry = new ChineseTranslationRegistry();
        try (var stream = getClass().getResourceAsStream("/assets/allchineseinstructions/command_translations.json")) {
            assertNotNull(stream);
            registry.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        assertTrue(registry.commandAliases("gamemode").contains("游戏模式"));
        assertTrue(registry.commandAliases("function").contains("函数"));
        assertTrue(registry.argumentAliases("minecraft:gamemode").get("survival").contains("生存"));
        assertThrows(IllegalArgumentException.class, () -> registry.command("say", "包含 空格"));
        assertThrows(IllegalArgumentException.class, () -> registry.command("say", "/错误"));
    }
}
