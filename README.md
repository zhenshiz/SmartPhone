# SmartPhone

SmartPhone 是一个面向 **Minecraft 1.21.1 / NeoForge** 的手机模组。它为每位玩家保存一部可持久化的手机，并提供桌面与应用、相机和图册、聊天与图片消息、好友私聊、双人语音通话、官方消息、记事本和小游戏等功能。

当前项目版本：`1.0.1`。

## 功能概览

- **手机桌面与应用管理**：解锁手机、打开应用、长按拖动调整图标顺序；应用商店可安装或卸载可下载应用。
- **相机与图册**：使用第一人称相机拍照，在图册浏览、删除照片；也可从操作系统文件选择器导入外部 **PNG / JPEG** 图片。
- **社交与聊天**：内置公共聊天室、好友申请、好友私聊和图片消息。聊天图片会被处理为缩略图后传输，服务端保留大小限制以保护网络与存档。
- **语音通话**：通过 Simple Voice Chat 建立一对一、隔离的临时语音通话。
- **内容与娱乐**：官方消息、记事本，以及贪吃蛇、2048、像素鸟、扫雷、别踩白块儿等可下载小游戏。
- **服务器管理**：可禁用指定应用、调整手机 UI 偏移，并通过管理员命令打开手机、管理设置或发送官方消息。

## 运行环境与依赖

| 项目 | 要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | 项目开发版本为 `21.1.216`；模组元数据要求 `21` 或更高 |
| Java（开发/构建） | `21` |
| Simple Voice Chat API | `2.6.0` 或更高；模组元数据中的必需依赖 |
| Simple Voice Chat | 语音通话必须由已安装且已连接的语音聊天服务支持 |

服务器和加入该服务器的客户端应安装相同版本的 SmartPhone 及发行包注明的前置模组。语音聊天未正确安装或未建立连接时，手机的其他功能仍可使用，但无法建立语音通话。

## 安装与首次使用

1. 安装与本项目匹配的 Minecraft `1.21.1` 和 NeoForge。
2. 将 SmartPhone 及发行包要求的前置模组放入服务器与客户端的 `mods` 目录；如需通话，同时正确部署 Simple Voice Chat。
3. 进入游戏后，在创意模式的“智能手机”分类中取得 `smart_phone:phone`，或由服务器、整合包、数据包自行发放。
4. 手持手机并使用物品，按锁屏提示向上滑动解锁，即可进入桌面。

当前源码没有提供手机的原版合成配方；生存服务器可按自身规则决定发放方式。

## 使用要点

### 应用与桌面

默认手机包含应用商店、设置、相机、图册、聊天室、电话、消息和记事本。按住桌面图标约 400 毫秒后拖到另一个图标上可以交换位置；右键图标可卸载可下载应用，系统应用不可卸载。

### 相机与图册

相机以第一人称模式取景并保存照片。图册支持查看、删除本地照片，也支持通过系统文件选择器导入 PNG 与 JPEG 文件。导入时会校验文件与像素尺寸，并转换为手机可用的图片数据；照片保存在当前客户端的玩家照片目录中。

### 聊天、图片与通话

聊天室包含公共频道、好友关系和私聊。图片消息会以 `160 × 90` PNG 缩略图发送，服务端拒绝超过 `100 KiB` 的图片载荷；普通聊天内容最多 `160` 个字符。

语音通话使用 Simple Voice Chat 的临时隔离组，适用于双方在线且都拥有可用语音聊天连接的场景。呼叫无人响应会在 60 秒后超时。

## 管理配置与命令

SmartPhone 使用 NeoForge 通用配置 `smart_phone_config.toml`。配置文件路径由 NeoForge 的客户端/服务器通用配置规则决定。

```toml
[config]
phoneMarginLeft = 0.0
phoneMarginTop = 0.0

[apps]
disabledApps = "smart_phone:camera,smart_phone:phone_call"
```

`phoneMarginLeft` 和 `phoneMarginTop` 用于调整手机界面的位置，范围均为 `-100` 到 `100`。`disabledApps` 接受以逗号分隔的应用 ID；被禁用的应用不会在桌面显示，也不会自动安装。

所有 `/smart_phone` 子命令均要求 2 级权限：

| 命令 | 作用 |
| --- | --- |
| `/smart_phone open` | 为执行命令的玩家打开手机 |
| `/smart_phone setting` | 打开执行者的完整手机资料配置界面 |
| `/smart_phone reload` | 清除执行者保存的手机资料，并在下次打开时重建默认内容 |
| `/smart_phone message send <targets> <title> <body>` | 向一个或多个玩家发送官方消息 |

> **注意：**`/smart_phone reload` 不会热重载配置。它会删除执行者的手机资料，可能清除壁纸、时间设置、已安装应用、桌面排序、记事本和手机内官方消息；使用前请备份需要保留的数据。

## 详细文档

分模块文档位于项目的 [`doc/`](doc/) 目录：

- [模组介绍与默认应用](doc/00-模组介绍.md)
- [手机与应用](doc/10-使用.手机与应用.md)
- [相机与图册](doc/20-功能.相机与图册.md)
- [社交与聊天](doc/30-功能.社交与聊天.md)
- [语音通话](doc/40-功能.语音通话.md)
- [配置与命令](doc/50-管理.配置与命令.md)

这些页面采用项目 Wiki 所需的 HTML 文档格式；在对应 Wiki 渲染器中打开时可获得目录、提示框和跨页跳转。

## 开发与验证

本项目使用 Java 21。编译主源码：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileJava --no-daemon
```

项目包含 LDLib2 客户端 UI 回归测试，其中 `group:smart_phone` 会启动真实客户端和集成服务器，覆盖聊天室大图片消息的 UI、C2S/S2C 往返与字节一致性验证：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew runClient \
  -PldTest=group:smart_phone \
  -PldTestWindow=1280x720 \
  -PldTestInputMode=SYNTHETIC \
  --no-daemon \
  --no-configuration-cache
```

UI 测试会在 `build/ldlib2-uitest/` 生成报告与截图。测试桥接依赖客户端退出后写入报告，因此运行该命令时必须保留 `--no-configuration-cache`。

## 许可证

本项目使用 [GNU LGPL 3.0](https://www.gnu.org/licenses/lgpl-3.0.html) 许可证。

