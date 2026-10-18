# 集成测试

测试针对 Minecraft 1.20.1 / Forge 47，在真实服务端、原版指令分发器和应用后的 Mixin 上运行。

## 运行

在项目根目录执行：

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
```

`test` 检查纯 Java 词典、Unicode 验证规则及选择器词法转换和游标映射；`runGameTestServer` 检查真实游戏环境。英文限制配置需要覆盖默认 `false` 和显式 `true` 两种指令树；旧有英文兼容性用例须在允许英文的指令树上执行。首次运行需要下载 Forge 和 Minecraft 依赖，并使用 Java 17。

测试数据位于 `src/gametest/resources`，仅由 GameTest 运行配置加载：中文函数在 `data/示例`，结构模板在 `data/allchineseinstructions/structures/empty.nbt`。它们不打包进正式模组，也不会给普通游戏加入示例命名空间。

GameTest 运行配置须设置：

```groovy
property 'forge.enabledGameTestNamespaces', 'allchineseinstructions'
property 'allchineseinstructions.runTests', 'true'
```

测试通过 `RegisterGameTestsEvent` 注册，只有 JVM 属性 `allchineseinstructions.runTests=true` 时才加入测试注册表。测试类没有 `@GameTestHolder`，因为 Forge 会自动扫描该注解，绕过属性开关；每个方法通过 `@GameTest(templateNamespace="allchineseinstructions", template="empty")` 指定模板，通过 `@PrefixGameTestTemplate(false)` 关闭类名前缀。模组入口在 MOD 事件总线上注册 `ChineseCommandsGameTests::register` 和 `ChineseSelectorGameTests::register`。

## 覆盖内容

| 测试 | 验证内容 |
| --- | --- |
| `chineseResourceLocations` | 中文命名空间、资源路径、命令读取，以及目录数据包路径校验；拒绝路径中的 `..` 和空白字符 |
| `chineseGameModeArguments` | 在允许英文的指令树中，中文、英文和混合写法均得到原版 `GameType` 参数值 |
| `nestedChineseExecute` | 在允许英文的指令树中，`执行 作为 @s 位于 @s 运行 …` 的重定向和执行结果与英文一致 |
| `chineseBooleanGameRule` | 中文游戏规则与真假参数；测试完成后恢复原值 |
| `unquotedChineseNames` | 不加引号的中文记分项、队伍和实体标签名称；计分操作执行并读取结果 |
| `chineseFunctionsAndTags` | 从测试数据包资源加载中文函数和标签，执行中文嵌套函数并检查运行错误 |
| `chineseTabSuggestions` | 根指令、子命令、游戏模式、布尔参数、中文函数资源名补全 |
| `permissionsRemainEnforced` | 无权限指令执行失败；同步给客户端的指令树及补全不泄露受限指令 |
| `commandPacketRoundTrip` | 完整指令树序列化/反序列化，保留中文参数类型、中文补全及对应配置下的英文可用状态 |
| `englishPolicyFeedback` | 英文根命令、子命令、嵌套命令及固定参数的配置提示；英文自由名称和资源 ID 保持可用 |
| `englishPolicySuggestions` | 服务端补全过滤与客户端指令树均遵循英文开关，覆盖根命令、子命令、嵌套命令和固定参数 |
| `englishFunctionPolicy` | 中文函数可解析，英文函数是否能解析与配置一致 |
| `allFiveChineseSelectors` | 五种中文选择器与原版的类型、数量和自身语义一致 |
| `chineseSelectorFiltersAndSorting` | 真实实体的类型、标签、名称、否定、排序及数量筛选 |
| `chineseSpatialAndRotationOptions` | 距离、三轴坐标、三轴范围、俯仰角、偏航角的正反匹配 |
| `chinesePlayerOptionsAndProfileArgument` | 等级、模式、进度语法和玩家档案选择器分支 |
| `chinesePlayerOptionMatching` | FakePlayer 的实际等级、四种模式及否定条件、进度和嵌套条件真假值 |
| `chineseTeamScoresAndScoreHolder` | 队伍、分数筛选和计分板目标选择器的真实执行 |
| `chineseNestedExecuteAndFunction` | 中文选择器嵌套执行、分叉和中文函数文件加载执行 |
| `chineseSelectorNbtAndPredicate` | NBT、谓词和任意数据内容不被替换 |
| `selectorWordsRemainOrdinaryNames` | 带选择器词前缀的完整名称保持原义 |
| `chineseSelectorsInMessages` | 消息替换、原文偏移、混合语法、引号内容和权限 |
| `chineseSelectorSuggestionsAndRanges` | 选择器、选项、有限值和资源 ID 的补全及应用范围，过滤重复和不可用选项；含 op/deop、白名单增删和 pardon 的自定义补全入口 |
| `chineseSelectorValidationAndPermissions` | 原版权限、单实体/仅玩家限制、无效参数的原始输入和错误游标 |

`commandPacketRoundTrip` 使用与 `Commands.sendCommands` 相同的 Forge 指令树过滤流程，然后写入并重新读取 `ClientboundCommandsPacket`。解码后还验证中文选择器、中文筛选选项、否定模式补全和英文选择器混用。它可以发现自定义参数序列化遗漏和重定向图损坏；它不能替代人工检查聊天框输入法和真实网络连接。

英文限制改动还需验证：默认配置生成；英文根命令、子命令和已翻译固定参数的拒绝与补全过滤；中文提示；中文嵌套执行及函数；客户端指令树中的配置快照；`true` 模式恢复中英文与混合语法。资源 ID、英文名称、NBT、JSON、自由文本和原版选择器应保持可用。重载测试需确认新设置同时用于指令树、数据包函数及在线客户端，不混用新旧状态。

未加引号的中文名称通过 `UnicodeCommandReader` 读取：普通字符串参数由已注册的参数包装器调用；记分项、队伍引用和选择器中的名称由 Minecraft 类上的调用点 Mixin 调用。Brigadier 本身由 Forge 的启动类加载器加载，不直接对其 `StringReader` 注入 Mixin。1.0.0 将选择器关键字转换为原版语法后，交给同一原版解析器校验，并映射错误和补全的字符位置。因此 `选择自身[标签=中文标签,队伍=中文队伍,分数={中文测试=7}]` 与英文写法采用同一筛选语义。NBT 字段名和 SNBT 语法没有扩展。

## 人工验收

客户端和服务端安装同一份新版 JAR。本次修订仍为 1.0.0，但参数同步格式已更新，不能混用新旧文件。将 `examples/中文函数示例` 整个文件夹复制到世界的 `datapacks` 文件夹，执行 `/重载`。保持 `config/allchineseinstructions-common.toml` 中 `allowEnglishCommands = false`，在有权限的世界中输入以下命令，并在中途按 Tab：

```mcfunction
/游戏模式 创造
/执行 作为 选择自身 位于 选择自身 运行 游戏规则 昼夜更替
/游戏规则 昼夜更替 假
/函数 示例:开场
/函数 #示例:中文标签
/函数 示例:中文选择器
/执行 作为 选择所有实体[类型=minecraft:armor_stand,距离=..10,数量=1,排序=最近] 运行 说 选择自身
```

确认 `/gamemode survival`、`/时间 set 白天` 和 `/游戏模式 creative` 均被拒绝并显示中文配置限制提示，且根命令、子命令和对应固定参数处没有英文补全。`/给予 @s minecraft:diamond 1`、`/说 English text` 和中文指令中的原版选择器筛选语法仍应可用。

将 `allowEnglishCommands` 改为 `true` 并保存，执行 `/重载`。确认 `/gamemode survival`、`/gamemode 创造 @s`、英文子命令和固定参数恢复执行与补全，中文指令仍然可用。再改回 `false` 并重载，确认限制重新生效。用一个包含英文命令的测试 `.mcfunction` 文件检查两种模式下的数据包加载结果；已存在英文数据包的正式存档应先启用英文模式再重载。

中文函数示例是独立的 Minecraft 1.20.1 数据包（`pack_format: 15`），不会自动运行。`/函数 示例:开场` 应依次显示“中文函数入口已执行”和“中文嵌套函数已执行”。中文资源名支持 BMP 中的汉字扩展 A、基本汉字、兼容汉字及 `〇`；不接受代理对表示的扩展 B 汉字、空格或额外标点。

单元测试或编译成功不代表游戏内测试已通过；发布时应记录实际执行的命令及结果。

## 本次配置修订验证记录

2026-10-18，Java 17.0.19 / Minecraft 1.20.1 / Forge 47.4.10：

- `test build` 成功，64 项 JUnit 测试全部通过。
- 自动生成的 `allowEnglishCommands = false` 配置下，24 项 Forge GameTest 全部通过。
- 配置改为 `true` 后重新启动 GameTest 服务端，同一批 24 项测试全部通过；随后恢复 `false` 并再次通过。
- 覆盖聊天限制提示、英文根命令/子命令/嵌套命令/有限参数的限制与补全、中文函数、原版权限、自由名称与资源 ID，以及客户端命令树往返序列化。
- 日志：`build/english-policy-enabled-verification.log`、`build/english-policy-default-gametest.log`；测试报告：`build/reports/tests/test/index.html`。

自动化验证覆盖重启后的两种配置。图形输入法、实际 TAB 按键、真实多人连接和在线 `/重载` 切换仍需人工验收。

## 修订前验证记录

以下是加入英文限制配置之前的 1.0.0 验证记录，不代表修订后 JAR 的测试结果。修订版发布前须补充本次实际执行结果。原验证环境为 2026-10-18、Java 17.0.19、Minecraft 1.20.1、Forge 47.4.10：

- Gradle `build` 成功，已产出经过 `reobfJar` 处理的正式 JAR 和源码 JAR。
- 44 项 JUnit 测试通过，失败数为 0，包括 18 项选择器语法与游标映射测试。
- 21 项 Forge GameTest 全部通过，日志明确记录 `All 21 required tests passed`。
- `unquotedChineseNames` 还验证了 `@s[team=中文队伍,tag=中文标签,scores={中文测试=7}]` 能选中正确实体。
- 正式 JAR 包含 Mixin 配置和混淆映射，不包含测试函数和结构数据。

本机构建复用了已有依赖缓存，实际命令为 `gradlew.bat --offline -Dnet.minecraftforge.gradle.check.certs=false test build runGameTestServer`。该参数仅跳过 ForgeGradle 启动时的联网证书探测；安装和运行模组不需要这个参数。Gradle 核心测试报告位于 `build/reports/tests/test/index.html`，修订前集成测试日志位于 `build/first-release-verification.log`。

未进行图形客户端输入法、实际 TAB 按键和多人连接的人工验收；对应的指令解析、补全结果和网络包编解码已有自动化验证。
