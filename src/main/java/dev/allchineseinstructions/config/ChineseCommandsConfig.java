package dev.allchineseinstructions.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Each command tree takes a snapshot so commands and loaded functions share one policy. */
public final class ChineseCommandsConfig {
    public static final String FILE_NAME = "allchineseinstructions-common.toml";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ENGLISH_COMMANDS;
    public static final String RESTRICTION_MESSAGE = "英文指令已被配置文件限制，请将 config/"
            + FILE_NAME + " 中的 allowEnglishCommands 改为 true，然后执行 /重载 或重启游戏/服务器。";

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ALLOW_ENGLISH_COMMANDS = builder
                .comment("是否允许原英文指令。默认 false：仅显示、补全和执行中文命令及已翻译的固定参数。",
                        "true：恢复中英文命令、子命令和固定参数。资源 ID、玩家名、NBT/JSON 和自由文本不受限制。",
                        "修改后执行 /重载 或重启游戏/服务器。多人游戏使用服务端设置。",
                        "Allow original English commands. Apply changes with /重载 or a restart.")
                .define("allowEnglishCommands", false);
        SPEC = builder.build();
    }

    private ChineseCommandsConfig() {}

    public static boolean allowEnglishCommands() {
        return SPEC.isLoaded() && ALLOW_ENGLISH_COMMANDS.get();
    }
}
