# SmartPhone

SmartPhone 是一个面向 **Minecraft 1.21.1 / NeoForge** 的手机模组。它按手机绑定的主人保存资料；主人可以是真实玩家，也可以是已注册的虚拟角色。模组提供桌面与应用、相机和图册、聊天与图片消息、好友私聊、双人语音通话、官方消息、记事本和小游戏等功能。

当前项目版本：`1.0.3`。

## 功能概览

- **手机桌面与应用管理**：解锁手机、打开应用、长按拖动调整图标顺序；应用商店可安装或卸载可下载应用。
- **手持手机展示**：切换到主手手机时立即播放举起动画，右手从右下方握住手机；原版第一人称物品画面会隐藏，先显示待机锁屏，向上滑动后进入桌面。
- **手机归属**：物品上的 `smart_phone:phone_owner` 组件指向真实玩家或已注册虚拟角色。未绑定手机首次使用时绑定持有者；拿到已绑定的手机会打开该主人对应的资料；启用密码后必须先输入正确的六位密码。
- **相机与图册**：使用第一人称相机拍照，在图册浏览、涂鸦编辑、删除照片；也可从操作系统文件选择器导入外部 **PNG / JPEG** 图片。
- **社交与聊天**：内置公共聊天室、好友申请、好友私聊和图片消息。聊天图片会被处理为缩略图后传输，服务端保留大小限制以保护网络与存档。
- **语音通话**：通过 Simple Voice Chat 建立一对一、隔离的临时语音通话。
- **内容与娱乐**：官方消息、记事本，以及贪吃蛇、2048、像素鸟、扫雷、别踩白块儿等可下载小游戏。
- **手机数据编辑器**：左侧交互预览、右侧参数 View；按玩家名、UUID 或虚拟角色名编辑指定手机的壁纸、时间、应用、预设会话、消息和笔记，保存后直接生效。
- **服务器管理**：可禁用指定应用、调整手机 UI 偏移，并通过管理员命令打开手机、管理设置或发送官方消息。

## 运行环境与依赖

| 项目 | 要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | 项目开发版本为 `21.1.248`；模组元数据要求 `21` 或更高 |
| Java（开发/构建） | `21` |
| Simple Voice Chat API | `2.6.0` 或更高；模组元数据中的必需依赖 |
| Simple Voice Chat | 语音通话必须由已安装且已连接的语音聊天服务支持 |

服务器和加入该服务器的客户端应安装相同版本的 SmartPhone 及发行包注明的前置模组。语音聊天未正确安装或未建立连接时，手机的其他功能仍可使用，但无法建立语音通话。

## 安装与首次使用

1. 安装与本项目匹配的 Minecraft `1.21.1` 和 NeoForge。
2. 将 SmartPhone 及发行包要求的前置模组放入服务器与客户端的 `mods` 目录；如需通话，同时正确部署 Simple Voice Chat。
3. 进入游戏后，在创意模式的“智能手机”分类中取得 `smart_phone:phone`，或由服务器、整合包、数据包自行发放。
4. 默认配置下，把手机切换到主手便会开始举起动画并显示待机锁屏。动画结束后用鼠标向上滑动解锁，再点击桌面应用。按 Esc 可收起；切换到其他物品后再次拿起会重新展示锁屏。

当前源码没有提供手机的原版合成配方；生存服务器可按自身规则决定发放方式。

## 使用要点

### 应用与桌面

手机、应用控件、弹窗和数据编辑器统一使用 OreUI 风格。默认手机包含应用商店、设置、相机、图册、聊天室、电话、消息和记事本。按住桌面图标约 400 毫秒后拖到另一个图标上可以交换位置；右键图标可卸载可下载应用，系统应用不可卸载。

手机主人支持真实玩家和已注册的虚拟角色；资料保存在服务器主世界，物品组件引用主人。持有他人或角色手机时，待机名称、壁纸、应用、记事本、官方消息和预设聊天读写到该主人名下；聊天发送者名称也跟随手机主人。在线好友关系、私聊路由与语音连接仍由实际在线玩家承担，本地相册使用当前客户端的照片目录。

### 虚拟角色手机

地图作者不需要提前知道游玩者的玩家 ID。先注册角色，再编辑角色的手机：

```mcfunction
/smart_phone character register "张三"
/smart_phone editor "张三"
/smart_phone config "张三" locked true
/smart_phone bind "张三"
```

`bind` 绑定主手手机。也可以直接通过物品组件创建手机：

```mcfunction
/give @s smart_phone:phone[smart_phone:phone_owner="张三"]
```

角色需先注册；未注册的名称不会被自动当成真实玩家。中文或带空格的名称请使用双引号。名称支持 1–64 个字符，不能是 UUID 或含首尾空格、控制字符；重复注册不会覆盖已有资料。名称解析优先匹配已注册角色；与真实玩家同名时，可使用玩家 UUID 明确指定真实玩家。

待机页显示“张三的手机”，编辑器中新建的主人消息、预设聊天回复以及公共聊天发送者使用“张三”。角色手机副本共享同一份资料和密码；没有主人组件的手机仍会在首次打开时绑定实际持有者。旧的玩家 UUID 存档、手机组件继续兼容，无需手动迁移。

### 六位密码锁屏

在手机的“设置”中点击“启用六位密码”，连续输入两遍相同的六位数字。设置后，收起再拿出手机，上滑待机页会进入密码页；圆形按键只显示数字，六个圆点表示输入进度，输入第六位自动验证。“取消”返回待机页。也可用主键盘或小键盘输入数字，Backspace 删除上一位。

密码跟随手机绑定的主人（真实玩家或虚拟角色），同一主人的手机副本共用密码；其他玩家拿到手机也必须输入密码。修改或关闭密码需要旧密码。连续五次输错后，等待 30 秒才可继续尝试。未启用密码的手机保持上滑直接解锁。

管理员可在手机数据编辑器的“基础”页直接输入六位数字，点击“保存并生效”设置或更换密码，无需旧密码。输入框只接受数字，最多六位；不足六位不能保存。已有密码只存摘要，编辑器不会回填原数字；保留空白可继续沿用，点击“清除密码”并保存则关闭密码。

密码摘要单独保存在服务端 SavedData，客户端不会收到密码或摘要；锁定期间只同步待机页所需信息，并由服务端拒绝受保护手机资料的读写。管理员数据编辑器仍可编辑手机资料，普通编辑保存不会覆盖密码。`/smart_phone reload` 重置自己的手机资料时也会移除密码。

### 管理员手机配置

编辑器“基础”页新增“拦截（禁止解锁）”“隐藏日期和时间”“隐藏信号和电量图标”“隐藏手机主人名称”“隐藏锁图标”。五个开关都跟随手机绑定的主人保存，默认关闭。

也可以通过 `/smart_phone config <玩家名、UUID或角色名> <参数> <值>` 修改，要求 2 级权限，支持服务器控制台与命令方块。例如：

```mcfunction
/smart_phone config Dev wallpaper smart_phone:textures/ui/default_wallpaper.png
/smart_phone config Dev locked true
/smart_phone config Dev hide_date true
/smart_phone config Dev hide_status_icons true
/smart_phone config Dev hide_owner_name true
/smart_phone config Dev hide_lock_icon true
```

`locked true` 会拦截解锁：显示闭合锁图标，隐藏上滑提示，正在打开的应用或相机会返回待机页；即使密码正确也不能解锁。改为 `locked false` 后恢复原有上滑／密码解锁流程。`hide_date` 控制待机大时间和顶部时间，`hide_status_icons` 同时控制信号与电量图标。`hide_owner_name` 隐藏待机页的手机主人名称，`hide_lock_icon` 隐藏底部锁图标；隐藏图标不会解除拦截或密码。布尔参数改为 `false` 可恢复显示。

修改会立即同步到正在使用该手机的玩家；离线玩家配置会保存，之后打开手机时生效。壁纸使用资源位置，所指图片需存在于客户端的模组或资源包中。普通手机保存不能覆盖管理员开关。管理员编辑器可直接进入被拦截手机的聊天、消息预览，编辑内容不会自动解除拦截；返回待机预览后仍显示实际锁定状态。

### 相机与图册

相机以第一人称横握手机取景：手机按原机身比例放大到眼前，双手托住下方两端，上下横条显示操作提示。取景采用 HUD 渲染，玩家仍可正常移动和转头。右键 / C 拍照，滚轮在 1× / 1.5× / 2× / 3× 之间进行数字变焦，仅放大手机屏幕里的景物，手机外的视野保持不变；G / 中键打开图册；Esc 关闭相机应用并回到同一部手机的已解锁桌面。照片保存当前变焦倍率下的取景区域，不包含手机外壳、双手或提示条。图册支持查看、涂鸦编辑和删除本地照片，也支持通过系统文件选择器导入 PNG 与 JPEG 文件。在照片详情页点击“编辑”，可选择七种颜色与三档笔刷，拖动绘画，按整笔撤回；“保存副本”保留原图并按原始像素尺寸生成新照片，取消或 Esc 放弃草稿。导入时会校验文件与像素尺寸，并转换为手机可用的图片数据；照片保存在当前客户端的玩家照片目录中。

### 聊天、图片与通话

聊天室包含公共频道、好友关系和私聊。图片消息会以 `160 × 90` PNG 缩略图发送，服务端拒绝超过 `100 KiB` 的图片载荷；普通聊天内容最多 `160` 个字符。

在手机编辑器的“聊天”页选中一条预设消息后，可填写“头像玩家 ID”，例如 `Notch`，使用该正版 Java 玩家账号的皮肤头像，无需对方在线。发送者名称仍可填写“张三”，手机归属与消息左右方向不会改变。头像按消息单独保存，留空恢复发送者原头像。首次加载需要客户端联网访问皮肤服务；加载中或无法取得皮肤时保留默认头像，界面仍可正常操作。

语音通话使用 Simple Voice Chat 的临时隔离组，适用于双方在线且都拥有可用语音聊天连接的场景。呼叫无人响应会在 60 秒后超时。

## 管理配置与命令

SmartPhone 使用 NeoForge 通用配置 `smart_phone_config.toml`。配置文件路径由 NeoForge 的客户端/服务器通用配置规则决定。

```toml
[config]
phoneMarginLeft = 0.0
phoneMarginTop = 0.0
heldPhoneMode = true

[apps]
disabledApps = "smart_phone:camera,smart_phone:phone_call"
```

`phoneMarginLeft` 和 `phoneMarginTop` 用于调整手机界面的位置，范围均为 `-100` 到 `100`。`disabledApps` 接受以逗号分隔的应用 ID；被禁用的应用不会在桌面显示，也不会自动安装。

`heldPhoneMode` 开启后，玩家切换到主手手机时会立即开始举起动画，右手与手机一起移到右侧；`phoneMarginLeft` 和 `phoneMarginTop` 同时微调手机界面与右手的停留位置。关闭后恢复原来的居中界面，使用手机物品或管理员命令打开。

所有 `/smart_phone` 子命令均要求 2 级权限：

| 命令 | 作用 |
| --- | --- |
| `/smart_phone open` | 为执行命令的玩家打开手机 |
| `/smart_phone bind <玩家名、UUID或角色名>` | 将主手手机绑定到服务器已知的玩家或已注册的虚拟角色；玩家名还需能由服务器玩家缓存解析 |
| `/smart_phone editor [玩家名、UUID或角色名]` | 打开手机数据编辑器；省略目标时编辑主手手机绑定主人，没有绑定则编辑自己 |
| `/smart_phone reload` | 清除执行者保存的手机资料，并在下次打开时重建默认内容 |
| `/smart_phone message send <targets> <title> <body>` | 向一个或多个玩家发送官方消息 |

手机数据编辑器直接读取指定主人的 SavedData，不需要打开、导入或导出工程文件。左侧可滑动解锁、打开应用；右侧分为基础、应用、聊天、消息、笔记五个 View，分隔线可拖动。顶部“界面尺寸”可选择自动或数字档位，数字越大，编辑器文字和控件越大；偏好独立保存在客户端 `config/smart_phone_client.toml` 的 `[editor].guiScale`，不会改变游戏 GUI 缩放或编辑器外的手机尺寸。修改先保存在草稿，点击“保存并生效”写回服务器；未保存关闭时会询问是否放弃修改。目标支持服务器已有的玩家、离线玩家和已注册虚拟角色。

“消息”页同样按选中项编辑：点击左侧手机中的消息，右侧只显示该条消息的发送者、标题、日期时间、已读状态和正文。右下角 `+` 新建并选中消息，`−` 删除当前选中项；选择预览消息不会自动标记为已读。

聊天记录和预设消息的日期使用日历选择器编辑：点击日期按钮，切换年份、月份并选择一天，再输入时、分、秒（24 小时制），也可点击“此刻”。日期按当前客户端的本机时区显示，弹窗会标明时区；“确认”更新编辑草稿，“取消”或 Esc 保留原值，最后点击“保存并生效”才写入服务器。现有存档仍沿用毫秒时间戳，无需转换。

“聊天”页编写的是这部手机的独立预设会话。右下角“会话 +”新建频道，“+ 好友”新建预设好友；好友玩家 ID 可以虚构，无需对应服务器中的真实玩家。左侧手机在“频道／好友”页分别展示它们，点击会话后编辑名称和好友 ID，点击消息后仅编辑这一条消息的发送者、是否由手机主人发送、内容、日期与图片。“消息 + / −”新增或删除当前消息，“会话 −”删除当前会话。

选中消息后，点击“从相册选择图片”，在左侧手机里选择已有照片（包括导入的 PNG/JPEG）；可以保留文字形成图文消息，也可以移除或替换图片。选图使用当前客户端相册，保存的是图片缩略图数据，保存后不依赖原照片文件。点击“保存并生效”后，玩家仍可向这些预设频道和好友发送文字、图片；记录写入手机绑定主人的 SavedData，对方不会自动回复，公共频道和真实好友关系不受影响。聊天消息显示发送者头像：收到的消息位于左侧，手机主人发送的消息位于右侧；在线玩家使用当前皮肤，离线或虚构 ID 使用稳定的默认玩家头像。手机机身保持约 1∶1.88 的宽高比，随窗口等比缩放；编辑器预览沿用相同比例，界面尺寸设置保持独立。预览中的电话、相机和独立图册应用仍需在真实手机中使用。

> **注意：**`/smart_phone reload` 不会热重载配置。它会删除执行者的手机资料，可能清除壁纸、时间设置、已安装应用、桌面排序、记事本和手机内官方消息；使用前请备份需要保留的数据。

## 详细文档

分模块文档位于项目的 [`doc/`](doc/) 目录：

- [模组介绍与默认应用](doc/00-模组介绍.md)
- [手机与应用](doc/10-使用.手机与应用.md)
- [小游戏](doc/15-使用.小游戏.md)
- [相机与图册](doc/20-功能.相机与图册.md)
- [社交与聊天](doc/30-功能.社交与聊天.md)
- [语音通话](doc/40-功能.语音通话.md)
- [手机数据编辑器](doc/45-管理.手机数据编辑器.md)
- [配置与命令](doc/50-管理.配置与命令.md)

这些页面采用项目 Wiki 所需的 HTML 文档格式；在对应 Wiki 渲染器中打开时可获得目录、提示框和跨页跳转。

## 开发与验证

本项目使用 Java 21。编译主源码：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileJava --no-daemon
```

项目包含 LDLib2 客户端 UI 回归测试，其中 `group:smart_phone` 会启动真实客户端和集成服务器，覆盖手持手机的举起与鼠标交互、手机归属，以及聊天室大图片消息的 UI、C2S/S2C 往返与字节一致性验证；`phone_editor` 场景覆盖编辑预览、指定玩家保存、草稿隔离、SavedData 序列化和已打开手机的即时更新；`preset_chat` 覆盖预览选择编辑、频道与消息增删、摘要、绑定手机发送和持久化，`preset_chat_media` 覆盖虚构好友、相册选图与换图、双方头像、SavedData 图片持久化和玩家回复，`phone_passcode` 覆盖六位密码设置与确认、锁定数据隔离、未授权写入、旧会话失效、错误重试限制、键盘输入、修改与关闭密码；`phone_theme` 遍历各应用并截图检查主题。`blocked_phone_editor` 覆盖拦截手机编辑、连续切页和中断上滑；`phone_character` 覆盖虚拟角色注册、名称物品组件、资料持久化与聊天发送身份；`phone_config` 覆盖管理员开关、即时同步与权限：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew runClient \
  -PldTest=group:smart_phone \
  -PldTestWindow=1280x720 \
  -PldTestInputMode=SYNTHETIC \
  --no-daemon \
  --no-configuration-cache
```

UI 测试会在 `build/ldlib2-uitest/` 生成报告与截图。测试桥接依赖客户端退出后写入报告，因此运行该命令时必须保留 `--no-configuration-cache`。

`preset_chat_media` 同时验证预设头像字段的独立编辑、保存与客户端同步。需要实际联网验证正版账号皮肤下载、切换和清空时，将上述选择器改为 `-PldTest=preset_avatar_online`；该场景使用 `Notch` 和 `jeb_`，独立放在 `smart_phone_online` 分组，避免普通离线回归依赖外部皮肤服务。

## 一键发布到 CurseForge / Modrinth

发布配置参考 ViScriptLib 的 `gradle/publish-conventions.gradle`，使用 CurseForgeGradle `1.1.28` 与 Minotaur `2.10.0`。发布会自动构建并上传 `jar` 任务产物，包括已有的 ViScriptLib Jar-in-Jar 依赖。

1. `gradle.properties` 已填写 SmartPhone 的 CurseForge 项目 ID `1464710`。填写 `publish_modrinth_project_id`（项目 ID 或 slug）后，`publishMods` 会同时上传两个平台；某个平台的 ID 留空且未通过环境变量或 `publish.env` 提供时会跳过该平台。
2. 将 `publish.env.example` 复制为根目录的 `publish.env`，填写 `CURSEFORGE_TOKEN` / `MODRINTH_TOKEN`。也可以直接设置同名环境变量，环境变量优先。`publish.env` 已被 Git 忽略；token 不要写入 `gradle.properties`。Modrinth token 需要创建版本（Create versions）的权限。
3. 修改 `mod_version`，并通过 `CHANGELOG.md` 或 `publish_changelog` 属性提供本次更新说明。

一键构建并上传所有已配置的平台：

```bash
./gradlew publishMods --no-configuration-cache
```

先试运行，构建 JAR 并打印两个平台的配置，不访问上传 API，也不需要 token：

```bash
./gradlew publishMods -Ppublish_dry_run --no-configuration-cache
```

单独上传一个平台：

```bash
./gradlew publishCurseforge --no-configuration-cache
./gradlew modrinth --no-configuration-cache
```

单平台任务也支持 `-Ppublish_dry_run`。使用 `./gradlew previewModPublishing --no-configuration-cache` 可随时构建并预览，无需 token。

两个平台共用 `mod_version`、Minecraft `1.21.1`、NeoForge 和更新说明。版本号含 `alpha` / `beta` 时自动选择对应发布类型，也可用 `-Ppublish_release_type=beta` 覆盖。更新说明优先级为：`PUBLISH_CHANGELOG`（环境变量优先于 `publish.env`）> `publish_changelog` > `CHANGELOG.md` > 默认版本说明。

项目 ID 优先级为 `gradle.properties` / `-P` 属性 > 对应的 `CURSEFORGE_PROJECT_ID` / `MODRINTH_PROJECT_ID` 环境变量 > `publish.env`。前置关系在 `publish_curseforge_requires` / `publish_modrinth_requires` 和对应的 `_optional` 属性中设置，使用逗号分隔的平台 slug；当前默认必需前置为 LDLib 与 Simple Voice Chat，可选联动为 KubeJS。

`publishMods` 会在任何上传开始前统一检查所有已配置平台的凭据；单平台任务只检查自己的凭据。发布插件的执行过程使用 Gradle Project API，因此发布命令需加 `--no-configuration-cache`。两个平台的上传分别执行，网络或平台 API 错误仍可能导致仅其中一个平台成功；补传时使用对应的单平台任务。

## 许可证

本项目使用 [GNU LGPL 3.0](https://www.gnu.org/licenses/lgpl-3.0.html) 许可证。
