# All-Chinese Instructions 的两平台项目填写与发布记录

首发版本固定为 **1.0.0**。CurseForge、Modrinth 的填写字段、共用介绍、发布日志、图片说明和上传记录均保留在本文件中。图片在 `assets` 文件夹，模组 JAR 与本文件放在一起。已创建项目的字段可用于后续编辑，不要重复创建项目或上传同一版本。

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
Chinese commands, selectors, TAB completion, and function paths for Minecraft 1.20.1 Forge. Extensible library with English compatibility.
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
| License | MIT |

**All-Chinese Instructions（中文指令）** brings Chinese command syntax to Minecraft and provides an extensible library for other Forge mods. Use Chinese names in chat, command blocks, and UTF-8 `.mcfunction` files while keeping English commands and mixed-language syntax available.

Place `All-Chinese-Instructions-1.20.1-1.0.0.jar` in the instance's `mods` folder. Single-player requires one installation; multiplayer requires the same mod on **the server and every client**.

## Using Chinese commands

| Action | Command |
| --- | --- |
| Switch yourself to creative mode | `/游戏模式 创造 选择自身` |
| Keep inventory after death | `/游戏规则 保留物品栏 真` |
| Give each player one diamond | `/给予 选择所有玩家 minecraft:diamond 1` |
| Set clear weather | `/天气 晴天` |
| Set daytime | `/时间 设置 白天` |

Type `/游` and press **TAB** to complete `/游戏模式`. After `/游戏模式 `, TAB offers Chinese values such as `生存`, `创造`, `冒险`, and `旁观`. English and Chinese forms can be mixed, for example `/gamemode 创造 @s`.

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

At a selector argument, type `选择` and press TAB to see Chinese selectors. After `选择所有实体[`, TAB suggests Chinese filter options; after `排序=` or `游戏模式=`, it suggests the corresponding Chinese values. English selectors and options remain valid and can be mixed with Chinese syntax.

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

Create `config/allchineseinstructions/translations.json` to add aliases, then run `/reload` or `/重载` to rebuild the command tree:

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

The built-in dictionary covers vanilla commands. Other mods can add aliases through the API or configuration. Existing English commands remain available. Compatibility with individual command-changing mods and full modpacks still needs testing in the intended setup.

**What has been tested?**

Version **1.0.0** has passed **44 unit tests and 21 Forge GameTests**, including parsing, completion, permissions, actual command execution, Chinese functions and resource paths, parameter serialization, and client command-tree reconstruction. Graphical input-method interaction, physical TAB-key interaction, and real multiplayer connections have not been manually verified. This first release is marked **Beta**.

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
| 许可证 | MIT |

**All-Chinese Instructions（中文指令）**让 Minecraft 支持中文指令语法，同时提供供其他 Forge 模组调用的中文指令库。聊天栏、命令方块和 UTF-8 `.mcfunction` 文件均可使用中文，英文指令与中英混用仍然有效。

将 `All-Chinese-Instructions-1.20.1-1.0.0.jar` 放入游戏实例的 `mods` 文件夹。单人安装一份即可；多人游戏需要**服务端和所有客户端同时安装**。

## 中文指令与补全

| 功能 | 示例 |
| --- | --- |
| 将自己切换为创造模式 | `/游戏模式 创造 选择自身` |
| 开启死亡不掉落 | `/游戏规则 保留物品栏 真` |
| 给予每名玩家一颗钻石 | `/给予 选择所有玩家 minecraft:diamond 1` |
| 设置晴天 | `/天气 晴天` |
| 设置白天 | `/时间 设置 白天` |

输入 `/游` 后按 **TAB** 可补全 `/游戏模式`；在 `/游戏模式 ` 后按 TAB 可选择 `生存`、`创造`、`冒险`、`旁观`等中文参数。也可以中英混用，例如 `/gamemode 创造 @s`。

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

在选择器位置输入 `选择` 后按 TAB 可补全中文选择器；输入 `选择所有实体[` 可补全筛选选项，在 `排序=`、`游戏模式=` 后可补全相应中文值。英文选择器、英文选项与中英混用均保留。

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

创建 `config/allchineseinstructions/translations.json` 可以添加自定义别名，修改后执行 `/reload` 或 `/重载` 重建指令树：

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

内置词典覆盖原版指令，其他模组可通过扩展接口或配置添加别名，已有英文指令仍然有效。具体指令修改模组与整合包的兼容性仍需要在对应环境中测试。

**这个版本测试到了什么程度？**

**1.0.0** 已通过 **44 项单元测试和 21 项 Forge GameTest**，涵盖解析、补全、权限、实际指令执行、中文函数与资源路径、参数序列化和客户端指令树重建等。图形界面的输入法、实际 TAB 按键和真实多人连接尚未人工验收，因此首次发布标记为 **Beta**。

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
1.0.0 — Initial release

Chinese commands and subcommands, all 45 game rules, supported values from 13 argument types, all 5 selectors, and all 21 selector filter options.
TAB completion, Chinese function paths and namespaces, an extensible library API, and custom command aliases.
English and mixed-language syntax remain supported, with vanilla permissions and target restrictions.
Requires Minecraft 1.20.1, Forge 47.4.10 or a later 47.x version, and Java 17. Install on both client and server for multiplayer.
44 unit tests and 21 Forge GameTests passed. Manual GUI and real multiplayer checks are pending.

1.0.0 — 首次发布

支持中文命令与子命令、全部 45 项游戏规则、13 种参数类型的受支持取值、全部 5 种选择器和 21 项选择器筛选选项。
支持 TAB 补全、中文函数路径与命名空间，并提供可扩展指令库和自定义别名。
保留英文和中英混用，沿用原版权限及目标限制。
需要 Minecraft 1.20.1、Forge 47.4.10 或更高的 47.x 版本、Java 17；多人游戏双端安装。
44 项单元测试和 21 项 Forge GameTest 全部通过，图形界面和真实多人连接尚未人工验收。
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
Chinese commands, selectors, TAB completion and Chinese function paths for Minecraft 1.20.1 Forge. AI-generated promotional artwork; not a gameplay screenshot.

适用于 Minecraft 1.20.1 Forge 的中文指令、中文选择器、TAB 补全和中文函数路径。AI 生成宣传图，不是游戏实测截图。
```

### 指令示例图

文件：`assets/commands-guide.png`

Title：

```text
Chinese Command Examples
```

Caption：

```text
Chinese command, game rule and function examples. Typeset reference guide, not an in-game screenshot. English syntax remains supported; existing resource IDs keep their original names.

中文命令、游戏规则和函数示例。排版说明图，不是游戏内截图；英文写法仍可使用，已有资源 ID 保持原名。
```

### 中文选择器图

文件：`assets/selectors-guide.png`

Title：

```text
Chinese Selectors - Version 1.0.0
```

Caption：

```text
All five vanilla selectors have Chinese aliases without an @ prefix, with Chinese filter options and TAB completion. Typeset reference guide, not an in-game screenshot. Resource IDs and NBT keys retain their original spelling.

全部五种原版选择器支持不带 @ 的中文写法，以及中文筛选选项和 TAB 补全。排版说明图，不是游戏内截图；资源 ID 和 NBT 字段名保持原样。
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

## 7 本次上传记录（2026-10-18）

| 平台 | 项目与文件 | 已确认结果 |
| --- | --- | --- |
| Modrinth | [项目页面](https://modrinth.com/mod/all-chinese-instructions) · 项目 ID `kcuW0c7V` · [1.0.0 文件](https://modrinth.com/mod/all-chinese-instructions/version/l51uB4tz) | 已上传 1.0.0 Beta，保存完整双语介绍、`/中文` 图标和三张展示图，MIT，Minecraft 1.20.1 / Forge，双端必装；已提交公开审核，回读状态为 `processing`。 |
| CurseForge | [作者后台文件列表](https://authors.curseforge.com/#/projects/1751560/files) · 项目 ID `1751560` · 文件 ID `9235853` | 上传接口已接受 1.0.0 Beta JAR 及双语日志，版本标签为 Minecraft 1.20.1、Forge、Java 17、Client、Server，审核通过后自动发布。尚未确认公开审核结果。 |
| GitHub | [公开源码仓库](https://github.com/zz123456789/All-Chinese-Instructions) | MIT 源码及本单一发布文案文件。 |

**CurseForge 尚待完成：** 本次新整理的项目介绍尚未保存到 CurseForge 页面，图库状态也未能核验。Windows 浏览器操作因工具无法可靠识别当前网址而被停止；发布接口仍完成了 JAR 与版本日志上传。后续在现有项目中用第 2 节正文更新介绍，并核对第 5 节图库即可，不需要再次上传 JAR。

**文件校验：** Modrinth 返回的主文件 SHA-512 与本地待上传 JAR 一致。CurseForge 上传回执记录使用同一个 JAR；尚未从平台下载回验。

```text
All-Chinese-Instructions-1.20.1-1.0.0.jar
SHA-512: f75f33411335f4f69dcfe7e0d8311eacc5e12812108ba154f0d0c9a823fdd882fdbacd8c6a0f835940669533e213c71e92bdc1513446d2135a4918f0f734483a
```

发布凭据没有写入本文件、源码、JAR 或发布素材包。
