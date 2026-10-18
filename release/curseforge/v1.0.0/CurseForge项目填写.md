# All-Chinese Instructions 的两平台项目填写与发布记录

首发版本固定为 **1.0.0**。本次加入英文指令配置后，以新版 JAR 替换此前的 1.0.0 文件。CurseForge、Modrinth 的填写字段、共用介绍、发布日志、图片说明和上传记录均保留在本文件中。图片在 `assets` 文件夹，模组 JAR 与本文件放在一起。使用已创建的项目更新，不要重复创建项目；替换文件后按平台实际结果更新第 7 节记录。

## 1 创建项目 General

游戏选择 **Minecraft**。当前页面各项按下面填写：

| 页面字段 | 填写内容或选择 |
| --- | --- |
| Name | `All-Chinese Instructions` |
| Logo | 选择 `assets/icon-400.png`，400×400，图案为 `/中文` |
| Summary | 复制下面的英文简介 |
| Class | `Mods` |
| Main category | `API and Library` |
| Additional categories | `Utility & QoL`、`Server Utility` |
| Allow Comments | 勾选 |
| Unlisted project | 不勾选，便于项目发布后被搜索到 |

**Summary（只复制这一行英文）：**

```text
Chinese commands, selectors, TAB completion and function paths for Minecraft 1.20.1 Forge. English commands are optional via configuration.
```

先选 `Class = Mods`，再选主分类和附加分类。必填项完成后点击 **Next**。`Unlisted project` 控制项目是否公开列出，不是版本成熟度选项。

## 2 Description 项目介绍

下面两个 `PROJECT_DESCRIPTION` 标记之间为两平台共用的项目介绍，英文在前、中文在后。支持 Markdown 的编辑模式可直接粘贴正文；富文本模式可复制本文件预览中渲染后的正文。标记本身不属于介绍。

<!-- PROJECT_DESCRIPTION_START -->
## Overview

| Item | Details |
| --- | --- |
| Requirements | Minecraft Java Edition 1.20.1 · Forge 47.4.10 or a compatible later 47.x release · Java 17 |
| Installation | Required on the server and every client; no additional mod dependencies |
| Commands | Chinese aliases for vanilla commands and subcommands, all 45 game rules, and supported values from 13 argument types |
| Selectors | All 5 selectors and all 21 filter options, with Chinese TAB completion |
| Functions | Chinese commands, function paths, namespaces, folders, and function tags |
| English commands | Disabled by default; enable `allowEnglishCommands` and reload to use both languages |
| License | MIT |

**All-Chinese Instructions（中文指令）** brings Chinese command syntax to Minecraft and provides an extensible library for other Forge mods. Use Chinese names in chat, command blocks, and UTF-8 `.mcfunction` files. English command keywords and translated fixed argument values are hidden from suggestions and disabled by default; a configuration option restores English and mixed-language syntax.

Place `All-Chinese-Instructions-1.20.1-1.0.0.jar` in the instance's `mods` folder. Single-player requires one installation; multiplayer requires the same mod on **the server and every client**.

This revision keeps version **1.0.0** but updates the argument synchronization format. Replace the JAR on the server and every client together; do not mix the earlier and revised 1.0.0 files.

## Using Chinese commands

| Action | Command |
| --- | --- |
| Switch yourself to creative mode | `/游戏模式 创造 选择自身` |
| Keep inventory after death | `/游戏规则 保留物品栏 真` |
| Give each player one diamond | `/给予 选择所有玩家 minecraft:diamond 1` |
| Set clear weather | `/天气 晴天` |
| Set daytime | `/时间 设置 白天` |

Type `/游` and press **TAB** to complete `/游戏模式`. After `/游戏模式 `, TAB offers Chinese values such as `生存`, `创造`, `冒险`, and `旁观`. By default, English command names, subcommands, and translated fixed argument values cannot be completed or executed. Entering a restricted English command displays a Chinese configuration notice in chat. Enable the option below to use English or mixed forms such as `/gamemode 创造 @s`.

Commands retain their vanilla permission level, target count, and player-only restrictions. Enable cheats in single-player or use an account with the required server permissions. Commands hidden by the current environment or permissions remain subject to those rules. Set the game language to Simplified Chinese to also see vanilla feedback in Chinese.

## Chinese selectors

Enter Chinese selector names directly, **without an `@` prefix**:

| Vanilla | Chinese | Target |
| --- | --- | --- |
| `@e` | `选择所有实体` | All entities |
| `@a` | `选择所有玩家` | All players |
| `@p` | `选择最近玩家` | Nearest player |
| `@r` | `选择随机玩家` | Random player |
| `@s` | `选择自身` | Current command executor; also accepts `选择自己` and `选择执行者` |

```mcfunction
/执行 作为 选择所有实体[类型=minecraft:armor_stand,距离=..10,数量=1,排序=最近] 运行 标签 选择自身 添加 命中
/游戏模式 生存 选择所有玩家[游戏模式=!旁观]
```

The first example tags the nearest armor stand within 10 blocks. The second changes all non-spectator players to survival mode. All 21 selector option keys have Chinese names; sort orders, game modes, and advancement booleans also accept Chinese values.

At a selector argument, type `选择` and press TAB to see Chinese selectors. After `选择所有实体[`, TAB suggests Chinese filter options; after `排序=` or `游戏模式=`, it suggests the corresponding Chinese values. Vanilla selectors such as `@e`, English selector options, and their vanilla values remain valid regardless of the English command setting.

Chinese selectors work in entity/player arguments, scoreboard targets, player-profile arguments, and selector-enabled positions in messages such as `/说` and `/私信`. Message text recognizes separate selector words, leaving quoted ordinary text and complete names such as `选择所有实体玩家` unchanged. Like `@s`, `选择自身` needs a current executor and does not identify a player when used directly from the server console.

## Chinese functions and resource names

Create a data pack with a `pack.mcmeta` and this function file:

```text
data/示例/functions/开场.mcfunction
```

Save the function as **UTF-8**, with no leading `/` on its command lines:

```mcfunction
天气 晴天
时间 设置 白天
```

Place the data pack in the world's `datapacks` folder, run `/重载`, then run `/函数 示例:开场`. The mod does not create example functions automatically. Chinese function names, subfolders, namespaces, and function tags support TAB completion. Scoreboard objectives, team names, and tags can also use unquoted Chinese names.

Resource names support common CJK characters, Extension A, compatibility ideographs, and `〇`. Rare characters requiring UTF-16 surrogate pairs, including Extension B, are not currently supported. Vanilla restrictions on spaces, punctuation, and path safety remain in place.

## Configuration and library API

The first launch creates `config/allchineseinstructions-common.toml` with this default:

```toml
allowEnglishCommands = false
```

Set it to `true`, save the file, then run **`/重载`** or restart the game/server to restore English commands, subcommands, translated fixed argument values, and their TAB completion alongside Chinese syntax. Changing it back to `false` also requires a reload or restart. The setting applies to a command-tree snapshot; reloading rebuilds functions and synchronizes the new command tree with connected clients. Multiplayer uses the server's setting, so players do not need to copy the configuration file.

The default restriction also affects English commands in existing `.mcfunction` files and English-only commands from other mods. Enable English commands before loading such data packs, or add Chinese aliases for other mods. Resource IDs, player/objective/team/tag names, NBT, JSON, free text, and vanilla selector syntax remain available.

Create `config/allchineseinstructions/translations.json` to add aliases, then run `/重载` to rebuild the command tree. English `/reload` is available only when English commands are already enabled:

```json
{
  "commands": { "gamemode": ["模式"] },
  "literals": { "time": { "day": ["白昼"] } },
  "arguments": { "minecraft:gamemode": { "creative": ["创造模式"] } }
}
```

Custom aliases merge with the built-in dictionary. Existing nodes with the same name take precedence, and conflicts are reported in the log. Clients receive argument aliases from the server, so players do not need to copy the server's dictionary manually.

Mod developers can listen for `RegisterChineseTranslationsEvent` on the Forge event bus after registering their original commands through `RegisterCommandsEvent`:

```java
MinecraftForge.EVENT_BUS.addListener((RegisterChineseTranslationsEvent event) -> {
    var translations = event.translations();
    translations.command("mycommand", "我的指令");
    translations.literal("mycommand", "start", "开始");
});
```

The public API is in `dev.allchineseinstructions.api`; the mod ID is `allchineseinstructions`. Aliases preserve original execution callbacks, permissions, argument names, and redirects. If several root commands share the same child-node object, use `literal("*", originalKeyword, chineseName)` for a shared translation; shared nodes are decorated only on their first traversal. Dependent mods should declare this library as required in `mods.toml` and load it on both sides.

## Frequently asked questions

**Does every part of a command become Chinese?**

Command aliases, supported finite argument values, and selectors have Chinese forms. Existing resource IDs such as `minecraft:diamond`, NBT fields, JSON keys, numbers, coordinates, and time formats such as `20t` retain their vanilla syntax. ASCII `[]`, `=`, `,`, `!`, and `..` keep their normal meanings. Player names, arbitrary chat text, JSON, and NBT are not subjected to whole-text replacement.

**Can I use this with other mods?**

The built-in dictionary covers vanilla commands. Other mods can add aliases through the API or configuration. English-only command names and subcommands are hidden and restricted by default; enable `allowEnglishCommands` and reload if they do not have Chinese aliases. Compatibility with individual command-changing mods and full modpacks still needs testing in the intended setup.

**What has been tested?**

Automated checks cover parsing, completion, permissions, actual command execution, Chinese functions and resource paths, parameter serialization, and client command-tree reconstruction. The repository's integration-test document records verification results for each revision. Graphical input-method interaction, physical TAB-key interaction, and real multiplayer connections have not been manually verified. This first release is marked **Beta**.

**Can I include it in a modpack or modify it?**

Yes. The project is **MIT licensed**. Use, modification, distribution, and commercial use are permitted when the copyright and license notices are retained. Third-party components, including Minecraft and Forge, remain subject to their own terms.

[Source code](https://github.com/zz123456789/All-Chinese-Instructions) · [Command reference](https://github.com/zz123456789/All-Chinese-Instructions/blob/main/docs/command-translations.md) · [Report an issue](https://github.com/zz123456789/All-Chinese-Instructions/issues)

## 中文介绍 · 概述

| 项目 | 说明 |
| --- | --- |
| 运行要求 | Minecraft Java 1.20.1 · Forge 47.4.10 或兼容的更高 47.x 版本 · Java 17 |
| 安装位置 | 服务端与所有客户端都需要安装，无其他模组前置 |
| 中文指令 | 原版命令与子命令、全部 45 项游戏规则、13 种参数类型的受支持取值 |
| 中文选择器 | 全部 5 种选择器与 21 项筛选选项，支持中文 TAB 补全 |
| 中文函数 | 函数内容、资源路径、命名空间、文件夹和函数标签 |
| 英文指令 | 默认禁用；开启 `allowEnglishCommands` 并重载后，中英文同时可用 |
| 许可证 | MIT |

**All-Chinese Instructions（中文指令）**让 Minecraft 支持中文指令语法，同时提供供其他 Forge 模组调用的中文指令库。聊天栏、命令方块和 UTF-8 `.mcfunction` 文件均可使用中文。默认隐藏并禁止英文命令关键字及已有中文译名的英文固定参数，开启配置后可恢复英文和中英混用。

将 `All-Chinese-Instructions-1.20.1-1.0.0.jar` 放入游戏实例的 `mods` 文件夹。单人安装一份即可；多人游戏需要**服务端和所有客户端同时安装**。

本次修订仍为 **1.0.0**，但参数同步格式已更新。升级时须为服务端和所有客户端一起替换新版 JAR，不能混用修订前后的 1.0.0 文件。

## 中文指令与补全

| 功能 | 示例 |
| --- | --- |
| 将自己切换为创造模式 | `/游戏模式 创造 选择自身` |
| 开启死亡不掉落 | `/游戏规则 保留物品栏 真` |
| 给予每名玩家一颗钻石 | `/给予 选择所有玩家 minecraft:diamond 1` |
| 设置晴天 | `/天气 晴天` |
| 设置白天 | `/时间 设置 白天` |

输入 `/游` 后按 **TAB** 可补全 `/游戏模式`；在 `/游戏模式 ` 后按 TAB 可选择 `生存`、`创造`、`冒险`、`旁观`等中文参数。默认不显示、不补全且不能执行英文命令、英文子命令和已翻译的英文固定参数；输入受限英文指令时，聊天框会显示配置限制提示。开启下方配置后，可使用 `/gamemode 创造 @s` 等中英混合写法。

指令沿用原版权限、目标数量和仅玩家等限制。单人游戏需要开启作弊，多人游戏需要对应的管理员权限；原版按运行环境或权限隐藏的指令仍遵守原有规则。将游戏语言设为简体中文后，原版反馈信息也会显示为中文。

## 中文选择器

直接输入中文即可，**不需要加 `@`**：

| 原版 | 中文写法 | 选择目标 |
| --- | --- | --- |
| `@e` | `选择所有实体` | 所有实体 |
| `@a` | `选择所有玩家` | 所有玩家 |
| `@p` | `选择最近玩家` | 最近的玩家 |
| `@r` | `选择随机玩家` | 随机一名玩家 |
| `@s` | `选择自身` | 当前执行者，也可写 `选择自己`、`选择执行者` |

```mcfunction
/执行 作为 选择所有实体[类型=minecraft:armor_stand,距离=..10,数量=1,排序=最近] 运行 标签 选择自身 添加 命中
/游戏模式 生存 选择所有玩家[游戏模式=!旁观]
```

第一条指令为 10 格内最近的盔甲架添加“命中”标签；第二条将所有非旁观模式的玩家切换为生存模式。全部 21 项筛选选项都有中文名称，排序、游戏模式及进度真假值也支持中文。

在选择器位置输入 `选择` 后按 TAB 可补全中文选择器；输入 `选择所有实体[` 可补全筛选选项，在 `排序=`、`游戏模式=` 后可补全相应中文值。`@e` 等原版选择器、英文筛选选项及其原版取值始终保留，不受英文指令开关限制。

中文选择器适用于实体和玩家参数、计分板目标、玩家档案参数，以及 `/说`、`/私信` 等消息中原本支持选择器的位置。消息正文只识别独立的中文选择器词，不会替换引号中的普通文字或 `选择所有实体玩家` 这样的完整名称。`选择自身` 与 `@s` 一样需要当前执行者，服务器控制台直接执行时不代表某个玩家。

## 中文函数与资源路径

创建包含 `pack.mcmeta` 的数据包，并添加以下函数文件：

```text
data/示例/functions/开场.mcfunction
```

函数使用 **UTF-8** 编码，文件内每行指令不加 `/`：

```mcfunction
天气 晴天
时间 设置 白天
```

放入存档的 `datapacks` 文件夹后执行 `/重载`，再运行 `/函数 示例:开场`。模组不会自动创建示例函数。中文函数名、子文件夹、命名空间和函数标签均支持补全；中文记分项、队伍名和标签可不加引号。

资源名支持常用汉字、扩展 A、兼容汉字和 `〇`，暂不支持扩展 B 等需要 UTF-16 代理对的罕用字。原版对空格、标点和路径安全的约束仍然保留。

## 配置文件与扩展接口

首次启动会生成 `config/allchineseinstructions-common.toml`，默认内容为：

```toml
allowEnglishCommands = false
```

将值改为 `true` 并保存，再执行 **`/重载`** 或重启游戏／服务器，即可同时显示、补全和使用中英文命令、子命令及受支持的固定参数。改回 `false` 后同样需要重载或重启。配置随指令树生效，重载会重新解析函数并向在线客户端同步新的指令树。多人游戏以服务端配置为准，玩家无需手动复制配置文件。

默认限制也适用于已有 `.mcfunction` 文件内的英文指令，以及没有中文别名的第三方英文指令。使用英文数据包前，应开启英文指令并重载；第三方指令也可通过扩展接口或词典添加中文别名。资源 ID、玩家／记分项／队伍／标签名称、NBT、JSON、自由文本和原版选择器语法不受限制。

创建 `config/allchineseinstructions/translations.json` 可以添加自定义别名，修改后执行 `/重载` 重建指令树。英文 `/reload` 仅在已启用英文指令时可用：

```json
{
  "commands": { "gamemode": ["模式"] },
  "literals": { "time": { "day": ["白昼"] } },
  "arguments": { "minecraft:gamemode": { "creative": ["创造模式"] } }
}
```

新增名称与内置词典合并，已有同名节点优先，冲突会在日志中提示。客户端会收到服务端下发的参数别名，不需要手动复制服务端词典。

模组开发者可在正常的 `RegisterCommandsEvent` 中注册原始指令，再通过 Forge 事件总线监听 `RegisterChineseTranslationsEvent` 添加中文命令、字面量和参数别名。公开包为 `dev.allchineseinstructions.api`，模组 ID 为 `allchineseinstructions`。别名保留原节点的执行回调、权限、参数名和重定向。

多个根命令共用同一子树对象时，应使用 `literal("*", 原关键字, 中文名称)` 指定统一译名；共享节点只在第一次遍历时装饰。依赖本库的模组还应在自己的 `mods.toml` 中声明必要依赖，并在客户端与服务端同时加载。完整接入示例见源码仓库 README。

## 常见问题

**是不是指令里的每一部分都会变成中文？**

命令别名、受支持的有限参数取值和选择器可使用中文。已有资源 ID（例如 `minecraft:diamond`）、NBT 字段、JSON 键、数字、坐标和 `20t` 等时间格式保持原版写法。半角 `[]`、`=`、`,`、`!`、`..` 保留原意；玩家名、任意聊天文字、JSON 和 NBT 内容不会被全文替换。

**其他模组的指令也能使用中文吗？**

内置词典覆盖原版指令，其他模组可通过扩展接口或配置添加别名。默认模式会隐藏并限制没有中文别名的英文命令和子命令；需要使用时，开启 `allowEnglishCommands` 并重载。具体指令修改模组与整合包的兼容性仍需要在对应环境中测试。

**这个版本测试到了什么程度？**

自动化检查涵盖解析、补全、权限、实际指令执行、中文函数与资源路径、参数序列化和客户端指令树重建等；各次修订的验证结果记录在仓库的集成测试说明中。图形界面的输入法、实际 TAB 按键和真实多人连接尚未人工验收，因此首次发布标记为 **Beta**。

**可以放进整合包、修改或再分发吗？**

可以。本项目采用 **MIT 许可证**，允许使用、修改、分发和商业使用，分发时须保留版权声明和许可证全文。Minecraft、Forge 等第三方组件仍遵循各自的许可条款。

[GitHub 源码](https://github.com/zz123456789/All-Chinese-Instructions) · [完整指令对照表](https://github.com/zz123456789/All-Chinese-Instructions/blob/main/docs/command-translations.md) · [问题反馈](https://github.com/zz123456789/All-Chinese-Instructions/issues)
<!-- PROJECT_DESCRIPTION_END -->

## 3 授权与链接设置

创建流程或后续项目设置中出现以下字段时，使用这些值：

| 字段 | 填写内容 |
| --- | --- |
| License | `MIT License`（MIT） |
| Owner / Team | 你当前登录的账号或自己的团队 |
| Slug / URL（如允许编辑） | `all-chinese-instructions`，以网站实际可用为准 |
| Source / Source Code URL | `https://github.com/zz123456789/All-Chinese-Instructions` |
| Issues / Issue Tracker URL | `https://github.com/zz123456789/All-Chinese-Instructions/issues` |
| Website / Wiki / Discord / Donation | 没有自己的公开链接时留空 |

## 4 首个版本文件

创建项目后，进入文件上传页面填写：

| 字段 | 填写内容 |
| --- | --- |
| File | `All-Chinese-Instructions-1.20.1-1.0.0.jar` |
| Display Name | `All-Chinese Instructions 1.0.0 - Forge 1.20.1` |
| Version（如出现） | `1.0.0` |
| Release Type | `Beta`，适用于当前尚未完成人工客户端验收的首发版本 |
| Minecraft Versions | 只选 `1.20.1` |
| Mod Loader | `Forge` |
| Java Versions（如出现） | `17` |
| Environment | `Client & Server`；分别选择时，客户端和服务端都选 `Required` |
| Required Dependencies / Related Projects | 留空，没有其他模组前置 |
| Embedded Libraries | 留空 |
| Optional / Incompatible Projects | 留空 |
| Changelog | 复制下方双语首发日志 |

**Changelog（下面整段为可复制内容）：**

<!-- CHANGELOG_START -->
```text
1.0.0 — Initial release, revised configuration defaults

Chinese commands and subcommands, all 45 game rules, supported values from 13 argument types, all 5 selectors, and all 21 selector filter options.
TAB completion, Chinese function paths and namespaces, an extensible library API, and custom command aliases.
English command names, subcommands, and translated fixed argument values are hidden, not suggested, and blocked by default. Restricted English input displays a Chinese configuration notice.
Set allowEnglishCommands = true in config/allchineseinstructions-common.toml, then run /重载 or restart to restore English and mixed-language syntax. Reloading also rebuilds functions and synchronizes clients.
Existing English data-pack functions and English-only commands from other mods require this option or Chinese aliases. Resource IDs, names, NBT/JSON/free text, and vanilla selector syntax remain supported. Vanilla permissions and target restrictions are retained.
Requires Minecraft 1.20.1, Forge 47.4.10 or a later 47.x version, and Java 17. Install on both client and server for multiplayer.
Version remains 1.0.0. Replace the previous 1.0.0 JAR on the server and every client together because the argument synchronization format changed.
See the repository's integration-test document for verification results. Manual GUI and real multiplayer checks are pending.

1.0.0 — 首次发布，修订默认配置

支持中文命令与子命令、全部 45 项游戏规则、13 种参数类型的受支持取值、全部 5 种选择器和 21 项选择器筛选选项。
支持 TAB 补全、中文函数路径与命名空间，并提供可扩展指令库和自定义别名。
默认隐藏、不补全并禁止英文命令、英文子命令和已翻译的英文固定参数；输入受限英文指令时显示中文配置提示。
在 config/allchineseinstructions-common.toml 中设置 allowEnglishCommands = true，保存后执行 /重载 或重启，即可恢复英文与中英混用。重载同时重新解析函数并同步客户端。
已有英文数据包函数及第三方仅英文指令需要开启该配置或使用中文别名。资源 ID、名称、NBT/JSON/自由文本与原版选择器语法保持可用，沿用原版权限及目标限制。
需要 Minecraft 1.20.1、Forge 47.4.10 或更高的 47.x 版本、Java 17；多人游戏双端安装。
版本保持 1.0.0。参数同步格式已更新，须为服务端与所有客户端同时替换旧的 1.0.0 JAR。
验证结果见仓库内的集成测试说明；图形界面和真实多人连接尚未人工验收。
```
<!-- CHANGELOG_END -->

## 5 Gallery 图片填写内容

项目 Logo 已在 General 中使用 `assets/icon-400.png`。下面三张图用于可选的 Gallery；按每张图给出的 Title 和 Caption 填写。

### 展示封面

文件：`assets/gallery-cover.png`

Title：

```text
All-Chinese Instructions - Overview
```

Caption：

```text
Chinese commands, selectors, TAB completion and function paths for Minecraft 1.20.1 Forge. English commands can be enabled in configuration. AI-generated promotional artwork; not a gameplay screenshot.

适用于 Minecraft 1.20.1 Forge 的中文指令、中文选择器、TAB 补全和中文函数路径；英文指令可通过配置开启。AI 生成宣传图，不是游戏实测截图。
```

### 指令示例图

文件：`assets/commands-guide.png`

Title：

```text
Chinese Command Examples
```

Caption：

```text
Chinese command, game rule and function examples. Typeset reference guide, not an in-game screenshot. English commands require allowEnglishCommands = true and a reload; existing resource IDs keep their original names.

中文命令、游戏规则和函数示例。排版说明图，不是游戏内截图；英文指令需设置 allowEnglishCommands = true 并重载，已有资源 ID 保持原名。
```

### 中文选择器图

文件：`assets/selectors-guide.png`

Title：

```text
Chinese Selectors - Version 1.0.0
```

Caption：

```text
All five vanilla selectors have Chinese aliases without an @ prefix, with Chinese filter options and TAB completion. Typeset reference guide, not an in-game screenshot. Vanilla selectors and filter options remain valid in both command modes; resource IDs and NBT keys retain their original spelling.

全部五种原版选择器支持不带 @ 的中文写法，以及中文筛选选项和 TAB 补全。排版说明图，不是游戏内截图；两种配置模式均保留原版选择器和筛选选项，资源 ID 和 NBT 字段名保持原样。
```

## 6 Modrinth 项目与版本字段

Modrinth 与 CurseForge 共用上面的项目介绍、首发日志和图片；无需另建文案文件。

| 字段 | 填写内容 |
| --- | --- |
| Title | `All-Chinese Instructions` |
| Slug | `all-chinese-instructions`，以网站实际可用为准 |
| Project type | `mod` |
| Summary / Description | 第 1 节的英文 Summary |
| Body | `PROJECT_DESCRIPTION` 标记之间的完整双语正文 |
| Categories | `library`、`utility` |
| Client side | `required` |
| Server side | `required` |
| Version environment | `client_and_server`；以平台回读的双端必装状态为准 |
| License | `MIT` |
| Icon | `assets/icon-400.png` |
| Source URL | `https://github.com/zz123456789/All-Chinese-Instructions` |
| Issues URL | `https://github.com/zz123456789/All-Chinese-Instructions/issues` |
| Version name | `All-Chinese Instructions 1.0.0 - Forge 1.20.1` |
| Version number | `1.0.0` |
| Version type | `beta` |
| Game versions | `1.20.1` |
| Loaders | `forge` |
| Primary file | `All-Chinese-Instructions-1.20.1-1.0.0.jar` |
| Dependencies | 空，无其他模组前置 |
| Changelog | 第 4 节双语首发日志，去掉代码围栏和提取标记 |
| Gallery | 第 5 节三张图片及对应标题和说明；保留 AI 生成宣传图说明 |

## 7 修订替换与上传记录（2026-10-18）

本次修订仍为 **1.0.0**，加入默认关闭的 `allowEnglishCommands` 配置。以下记录区分接口接受上传、完成旧文件替换和平台审核，避免把上传成功误写成已公开发布。

| 平台 | 项目与文件 | 已确认结果 |
| --- | --- | --- |
| Modrinth | [项目页面](https://modrinth.com/mod/all-chinese-instructions) · 项目 ID `kcuW0c7V` · [新版 1.0.0](https://modrinth.com/mod/all-chinese-instructions/version/x3LoXkDD) | 新版本 ID `x3LoXkDD`，Beta；旧版本 `l51uB4tz` 已删除。已回读核对新 JAR 的 SHA-512、双端必装状态、双语介绍和三张图库说明。项目状态为 `processing`，仍待审核。 |
| CurseForge | [作者后台文件列表](https://authors.curseforge.com/#/projects/1751560/files) · 项目 ID `1751560` · 新文件 ID `9236776` | 新版 1.0.0 Beta 与双语日志已获上传接口接受；旧文件 `9235853` 尚未删除或归档，替换未全部完成。新文件审核状态未能确认。 |
| GitHub | [公开源码仓库](https://github.com/zz123456789/All-Chinese-Instructions) | 本次源码、MIT 许可证、配置说明、测试结果、新 JAR 和六文件素材包一并同步至 `main`，版本保持 1.0.0。 |

**CurseForge 待完成：** 删除或归档旧文件 `9235853`，保存第 1 节 Summary 和第 2 节新正文，核对第 5 节图库说明。浏览器连接返回 `Debugger unattached`；Windows 界面工具因无法可靠识别当前浏览器网址而停止操作，因此未将这些后台操作标记为完成。无需再次上传已经接收的新文件 `9236776`。

**本次修订 JAR 校验：** Modrinth 返回的 SHA-512 与本地构建一致；CurseForge 上传回执记录使用同一份新 JAR，尚未从平台下载回验。源码验证通过 64 项单元测试，默认关闭和开启英文两种配置各通过 24 项 Forge GameTest；图形界面和真实多人连接仍待人工验收。

```text
All-Chinese-Instructions-1.20.1-1.0.0.jar
SHA-512: 982988dc094fcd548bed040da729bb715e5b727ef997201773ac9151e9c06dfdfdc8b9bf70ad0b1e895c8382fc14b85c273f4e5582aeba3e70362ce3fa9470fe
```

发布凭据没有写入本文件、源码、JAR 或发布素材包。
