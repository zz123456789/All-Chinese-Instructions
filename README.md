# All-Chinese Instructions · 中文指令

[GitHub 源码](https://github.com/zz123456789/All-Chinese-Instructions) · [问题反馈](https://github.com/zz123456789/All-Chinese-Instructions/issues)

Minecraft Java **1.20.1 / Forge 47.4.10+（47.x）/ Java 17** 的中文指令模组，也是供其他模组调用的中文指令库。

CurseForge 创建项目所需的填写值、双语介绍、首发日志和图片说明统一保存在 [CurseForge 项目填写](release/curseforge/v1.0.0/CurseForge项目填写.md) 一个文件中，按表单顺序使用。图片与模组 JAR 单独提供，版本固定为 1.0.0。

本项目采用 [MIT 许可证](LICENSE)，允许使用、修改、分发和商业使用，分发时须保留版权声明和许可证全文。Minecraft、Forge 等第三方组件仍遵循各自的许可条款。

## 安装

将 `All-Chinese-Instructions-1.20.1-1.0.0.jar` 放进游戏实例的 `mods` 文件夹。多人游戏时，**服务端和所有客户端都需要安装**，以同步中文参数类型和中文资源路径规则。单人游戏安装一份即可。

中文指令沿用原版权限；单人需要开启作弊，多人需要对应管理员权限。游戏语言设为简体中文后，原版反馈信息也会使用中文。

## 使用

```mcfunction
/游戏模式 创造 选择自身
/游戏模式 生存 选择自身
/游戏规则 保留物品栏 真
/天气 晴天
/时间 设置 白天
/执行 作为 选择所有玩家 位于 选择自身 运行 给予 选择自身 minecraft:diamond 1
/计分板 记分项 添加 测试 虚拟
/计分板 玩家 设置 选择自身 测试 10
/计分板 记分项 设置显示 侧边栏 测试
/函数 示例:开场
```

输入 `/游` 后按 **TAB** 可补全为 `/游戏模式`，在 `/游戏模式 ` 后按 TAB 可选择 `生存`、`创造`、`冒险`、`旁观`等中文参数。聊天栏、命令方块和函数使用同一套服务端指令树。英文形式仍然有效，也可混用，例如 `/gamemode 创造 @s`。

涵盖原版顶层命令、子命令、45 项游戏规则、13 种有限取值参数，以及全部 5 种选择器和 21 项选择器筛选选项。完整的中文名称见 [指令对照表](docs/command-translations.md)。原版按环境或权限隐藏的命令仍遵守原有规则。

玩家名、任意聊天文字、JSON、NBT 内容不会被全文替换。已有物品、方块和实体资源继续使用 `minecraft:diamond` 等原版 ID；不会将现有资源重命名。数字、坐标、`20t` 等时间格式沿用原版。

## 中文选择器

选择器可以直接使用中文，无需输入 `@`。英文选择器和英文筛选选项仍然有效，也可以与中文混用。

| 原版 | 中文写法 |
| --- | --- |
| `@e` | `选择所有实体` |
| `@a` | `选择所有玩家` |
| `@p` | `选择最近玩家` |
| `@r` | `选择随机玩家` |
| `@s` | `选择自身`，也可写 `选择自己`、`选择执行者` |

```mcfunction
/给予 选择最近玩家 minecraft:diamond 1
/执行 作为 选择所有实体[类型=minecraft:armor_stand,标签=测试,数量=1,排序=最近] 运行 标签 选择自身 添加 命中
/游戏模式 生存 选择所有玩家[游戏模式=!旁观]
/计分板 玩家 设置 选择自身[标签=测试] 测试 10
```

在选择器位置输入 `选择` 后按 TAB 可补全中文选择器；在 `选择所有实体[` 后补全中文选项，在 `排序=`、`游戏模式=` 后补全对应中文值。全部 21 项选项的对照、坐标别名和进度真假值写法见 [指令对照表](docs/command-translations.md#中文选择器与筛选选项)。半角 `[]`、`=`、`,`、`!`、`..` 保留原版含义，例如 `距离=..10`、`数量=1`、`排序=最近`。实体类型 ID 与 NBT 字段名仍使用原版写法。

中文选择器适用于实体和玩家参数、计分板目标、玩家档案参数，以及 `/说`、`/私信` 等消息中原本支持选择器的位置。消息正文只识别独立的中文选择器词，不会替换引号中的普通文字或 `选择所有实体玩家` 这样的完整名称。`选择自身` 与 `@s` 一样需要当前执行者；服务器控制台直接执行时不代表某个玩家。

## 中文函数与资源名称

数据包中的文件：

```text
pack.mcmeta
data/
  示例/
    functions/
      开场.mcfunction
      中文选择器.mcfunction
      嵌套/
        测试.mcfunction
    tags/
      functions/
        中文标签.json
```

`开场.mcfunction` 使用 UTF-8 编码，指令行不加 `/`：

```mcfunction
天气 晴天
时间 设置 白天
函数 示例:嵌套/测试
```

放入存档的 `datapacks` 文件夹并执行 `/重载`，然后调用 `/函数 示例:开场`。`/函数 示例:中文选择器` 展示自身、玩家和附近盔甲架的筛选，只输出消息。中文函数名、子文件夹、命名空间和函数标签均支持补全。可直接使用 [示例数据包](examples/中文函数示例)。

中文记分项、队伍名和标签可不加引号。资源名支持常用汉字、扩展 A、兼容汉字和 `〇`；罕用的扩展 B 等代理对汉字暂不支持。原版对空格、标点和路径安全的约束仍保留。

## 作为库使用

通过 Forge 总线监听 `RegisterChineseTranslationsEvent`。先在正常的 `RegisterCommandsEvent` 注册你的原始指令，再为其添加中文名称：

```java
MinecraftForge.EVENT_BUS.addListener((RegisterChineseTranslationsEvent event) -> {
    var translations = event.translations();
    translations.command("mycommand", "我的指令");
    translations.literal("mycommand", "start", "开始");
    // 参数键是已注册的 ArgumentType ID；返回值仍由原参数解析器生成。
    translations.argument("brigadier:bool", "true", "是");
    translations.argument("brigadier:bool", "false", "否");
});
```

公开包：`dev.allchineseinstructions.api`。模组 ID：`allchineseinstructions`。指令别名保留原节点的执行回调、权限、参数名和重定向，不以字符串替换后重新执行的方式实现。

第三方模组若让多个根命令共用同一个子树对象，应使用 `literal("*", 原关键字, 中文名称)` 为共享节点指定统一译名；共享节点只在第一次遍历时装饰。依赖本库的模组还应在自己的 `mods.toml` 声明 `allchineseinstructions` 为必要依赖，并在客户端与服务端同时加载。

也可以创建 `config/allchineseinstructions/translations.json` 添加自定义别名，并使用 `/reload` 或 `/重载` 重建指令树：

```json
{
  "commands": { "gamemode": ["模式"] },
  "literals": { "time": { "day": ["白昼"] } },
  "arguments": { "minecraft:gamemode": { "creative": ["创造模式"] } }
}
```

新增的名称与内置词典合并。已有同名节点优先，发生冲突会在日志中提示。客户端会收到服务端下发的参数别名，不需要手动复制服务端词典。

## 构建与验证

安装 Java 17，使用仓库内的 Gradle Wrapper：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

成品位于 `build/libs/`，其中不带 `-sources` 的 JAR 用于安装。`-sources.jar` 为源码。

构建包含解析、补全、权限、重定向和字符规则单元测试。Forge GameTest 会在独立测试世界中检查实际指令执行、中文函数加载、中文资源路径、参数序列化和客户端指令树重建。具体项目见 [集成测试说明](docs/integration-tests.md)。图形界面中的输入法和 TAB 按键体验仍需在游戏客户端中人工验收。
