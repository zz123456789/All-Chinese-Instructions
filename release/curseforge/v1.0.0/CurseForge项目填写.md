# All-Chinese Instructions 的 CurseForge 项目填写内容

首发版本固定为 **1.0.0**。从当前创建页面的 General 开始，按下面顺序填写；介绍、发布日志和图片说明都在本文件中。图片在 `assets` 文件夹，模组 JAR 与本文件放在一起。

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

下面从英文标题到中文许可证的一整段可作为 Description，英文在前、中文在后。支持 Markdown 的编辑模式可直接粘贴原文；富文本模式可复制本文件预览中渲染后的正文。

### All-Chinese Instructions

**All-Chinese Instructions**, also known as **中文指令**, brings Chinese command syntax to Minecraft Java 1.20.1 and provides an extensible library for other Forge mods.

**Features**

- Chinese aliases for vanilla commands and subcommands, all 45 game rules, and supported values from 13 argument types.
- Chinese names for all 5 selectors and all 21 selector filter options. Sort orders, game modes, and advancement booleans also support Chinese values.
- TAB completion for Chinese commands, supported arguments, selectors, filter options, and Chinese function resource names.
- Chinese commands in chat, command blocks, and UTF-8 `.mcfunction` files. Function names, folders, namespaces, and function tags can use Chinese characters.
- An extensible API through `RegisterChineseTranslationsEvent`, plus custom aliases in `config/allchineseinstructions/translations.json`.
- English commands remain available, including mixed Chinese and English syntax. Vanilla permissions and target restrictions still apply.

**Requirements and installation**

Minecraft Java **1.20.1**, **Forge 47.4.10 or a later 47.x version**, and **Java 17** are required. Place the JAR in the instance's `mods` folder. For multiplayer, install the mod on **the server and every client**. No additional mod dependencies are required.

**Examples**

```text
/游戏模式 创造 选择自身
/游戏规则 保留物品栏 真
/给予 选择所有玩家 minecraft:diamond 1
/执行 作为 选择所有实体[类型=minecraft:armor_stand,距离=..10,数量=1,排序=最近] 运行 标签 选择自身 添加 命中
/函数 示例:开场
```

The function example requires a data pack containing `data/示例/functions/开场.mcfunction`. Save the file as UTF-8, omit the leading `/` from commands inside it, and run `/重载` after installing the data pack. The mod does not create example functions automatically.

**Chinese selectors**

| Vanilla | Chinese |
| --- | --- |
| `@e` | `选择所有实体` — all entities |
| `@a` | `选择所有玩家` — all players |
| `@p` | `选择最近玩家` — nearest player |
| `@r` | `选择随机玩家` — random player |
| `@s` | `选择自身` — command executor; also `选择自己` or `选择执行者` |

Type `/游` and press TAB to complete `/游戏模式`. Type `选择` at a selector argument to see Chinese selectors, or `选择所有实体[` to see filter options.

Existing resource IDs such as `minecraft:diamond`, NBT fields, JSON keys, and ASCII punctuation retain their vanilla spelling. Chinese resource names support common CJK characters, Extension A, compatibility ideographs, and `〇`; rare characters requiring UTF-16 surrogate pairs, including Extension B, are not currently supported.

Version **1.0.0** has passed **44 unit tests and 21 Forge GameTests**. Graphical input-method interaction, physical TAB-key interaction, and real multiplayer connections have not been manually verified.

License: **MIT**. Use, modification, distribution, and commercial use are permitted when the copyright and license notices are retained. Third-party components, including Minecraft and Forge, remain subject to their own terms.

### 中文指令

**All-Chinese Instructions（中文指令）**让 Minecraft Java 1.20.1 支持中文指令语法，同时提供供其他 Forge 模组调用的中文指令库。

**主要功能**

- 原版命令、子命令、全部 45 项游戏规则，以及 13 种参数类型的受支持取值均有中文写法。
- 全部 5 种选择器和 21 项筛选选项支持中文，排序、游戏模式及进度真假值也有中文写法。
- 中文命令、受支持的参数、选择器、筛选选项和中文函数资源名支持 TAB 补全。
- 聊天栏、命令方块及 UTF-8 编码的 `.mcfunction` 文件均可使用中文指令；函数名、文件夹、命名空间及函数标签可使用中文。
- 提供 `RegisterChineseTranslationsEvent` 扩展事件，也可通过 `config/allchineseinstructions/translations.json` 添加自定义别名，再执行 `/重载`。
- 保留英文指令和中英混用，沿用原版权限、目标数量及仅玩家等限制。

**安装要求**

需要 Minecraft Java **1.20.1**、**Forge 47.4.10 或更高的 47.x 版本**、**Java 17**。将 JAR 放入实例的 `mods` 文件夹；多人游戏需要**服务端与所有客户端同时安装**，没有其他模组前置。

**使用示例**

```text
/游戏模式 创造 选择自身
/游戏规则 保留物品栏 真
/给予 选择所有玩家 minecraft:diamond 1
/执行 作为 选择所有实体[类型=minecraft:armor_stand,距离=..10,数量=1,排序=最近] 运行 标签 选择自身 添加 命中
/函数 示例:开场
```

`选择所有实体`、`选择所有玩家`、`选择最近玩家`、`选择随机玩家`、`选择自身` 分别对应 `@e`、`@a`、`@p`、`@r`、`@s`，直接输入中文即可，不加 `@`。自身选择器也可以写成 `选择自己` 或 `选择执行者`。

输入 `/游` 后按 TAB 可补全 `/游戏模式`；在选择器位置输入 `选择` 可补全中文选择器，输入 `选择所有实体[` 可补全筛选选项。例如 `距离=..10`、`数量=1`、`排序=最近`、`游戏模式=!旁观`。

使用 `/函数 示例:开场` 前，需要自行创建包含 `data/示例/functions/开场.mcfunction` 的数据包。函数使用 UTF-8 编码，文件内每行指令不加 `/`，装入数据包后执行 `/重载`。模组不会自动创建示例函数。

已有资源 ID（例如 `minecraft:diamond`）、NBT 字段、JSON 键和半角语法符号保持原版写法。中文资源名支持常用汉字、扩展 A、兼容汉字和 `〇`，暂不支持扩展 B 等需要 UTF-16 代理对的罕用字。

**1.0.0** 已通过 **44 项单元测试和 21 项 Forge GameTest**；图形界面的输入法、实际 TAB 按键和真实多人连接尚未人工验收。

许可证：**MIT**。允许使用、修改、分发和商业使用，分发时须保留版权声明和许可证全文。Minecraft、Forge 等第三方组件仍遵循各自的许可条款。

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
