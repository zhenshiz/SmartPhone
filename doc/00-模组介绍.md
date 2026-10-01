<h1 id="smartphone-模组文档" data-toc-id="smartphone-模组文档">SmartPhone 模组文档</h1>
<p>SmartPhone 为 Minecraft 中的每位玩家提供一部可持久化保存的手机。手机包含桌面、内置应用、相机与图册、聊天室、好友私聊、语音通话、消息和小游戏等功能。</p>
<p>本目录按功能拆分页面。先阅读本页完成安装和首次使用，再按需要进入相机、社交、通话或管理页面。</p>

<div type="important" data-type="admonition" data-admo-type="important">
  <div data-type="admonition-title">版本与依赖</div>
  <div data-type="admonition-content">
    <p>当前项目版本为 SmartPhone 1.0.3，目标为 Minecraft 1.21.1 与 NeoForge 21.1.248；模组元数据要求 NeoForge 21 或更高版本。</p>
    <p>Simple Voice Chat API 被声明为必需依赖，版本范围为 2.6.0 或更高。只有语音通话需要有效的语音聊天连接，详细条件见<a href="?file=&quot;40-功能.语音通话.md&quot;">语音通话</a>。</p>
  </div>
</div>

<h2 id="目录" data-toc-id="目录">目录</h2>
<ul>
  <li><p><a href="?file=&quot;10-使用.手机与应用.md&quot;">手机与应用</a>：打开、解锁、桌面、应用商店和手机设置。</p></li>
  <li><p><a href="?file=&quot;15-使用.小游戏.md&quot;">小游戏</a>：操作方式、别踩白块儿的判定与扫雷规则。</p></li>
  <li><p><a href="?file=&quot;20-功能.相机与图册.md&quot;">相机与图册</a>：拍照、管理照片和从外部文件导入 PNG/JPEG。</p></li>
  <li><p><a href="?file=&quot;30-功能.社交与聊天.md&quot;">社交与聊天</a>：公共频道、好友申请、私聊和图片消息。</p></li>
  <li><p><a href="?file=&quot;40-功能.语音通话.md&quot;">语音通话</a>：通过 Simple Voice Chat 发起、接听与结束双人通话。</p></li>
  <li><p><a href="?file=&quot;45-管理.手机数据编辑器.md&quot;">手机数据编辑器</a>：预览和预设指定玩家或虚拟角色的手机资料。</p></li>
  <li><p><a href="?file=&quot;50-管理.配置与命令.md&quot;">配置与命令</a>：服务器管理员配置、禁用应用和管理命令。</p></li>
</ul>

<h2 id="获取与打开手机" data-toc-id="获取与打开手机">获取与打开手机</h2>
<p>模组注册了物品 <code>smart_phone:phone</code>，并将其放入创意模式物品栏的“智能手机”分类。当前源码中没有提供合成配方数据；生存服务器应由整合包、数据包或管理员自行决定发放方式。</p>
<div currentstep="0" data-type="steps">
  <div data-type="step-item">
    <div data-type="admonition-title">取得手机</div>
    <div data-type="admonition-content">
      <p>在创意模式的“智能手机”分类中取得手机，或由服务器按自身规则发放 <code>smart_phone:phone</code>。</p>
    </div>
  </div>
  <div data-type="step-item">
    <div data-type="admonition-title">在手中使用</div>
    <div data-type="admonition-content">
      <p>默认配置下，切换到主手手机便会立即显示右侧举起动画，右手从右下方握住手机，并展示待机锁屏；首次打开会创建默认手机数据和默认内置应用。</p>
    </div>
  </div>
  <div data-type="step-item">
    <div data-type="admonition-title">解锁并进入桌面</div>
    <div data-type="admonition-content">
      <p>动画结束后，用鼠标在锁屏上向上滑动以进入桌面，再点击图标使用应用。关闭自动展示配置后，使用手机物品仍会打开锁屏，按提示上滑解锁。</p>
    </div>
  </div>
</div>

<h2 id="默认应用" data-toc-id="默认应用">默认应用</h2>
<table data-type="custom-table" data-with-header-row="true" style="min-width: 100px;">
  <tbody>
    <tr>
      <th>应用</th>
      <th>默认安装</th>
      <th>用途</th>
    </tr>
    <tr>
      <td>应用商店</td>
      <td>是</td>
      <td>安装或卸载可下载应用</td>
    </tr>
    <tr>
      <td>设置</td>
      <td>是</td>
      <td>修改手机壁纸</td>
    </tr>
    <tr>
      <td>相机与图册</td>
      <td>是</td>
      <td>拍摄、导入、浏览和删除照片</td>
    </tr>
    <tr>
      <td>聊天室</td>
      <td>是</td>
      <td>公共频道、好友和私聊</td>
    </tr>
    <tr>
      <td>电话</td>
      <td>是</td>
      <td>发起和处理语音通话</td>
    </tr>
    <tr>
      <td>消息与记事本</td>
      <td>是</td>
      <td>查看官方消息和保存个人笔记</td>
    </tr>
  </tbody>
</table>

<h2 id="可下载应用" data-toc-id="可下载应用">可下载应用</h2>
<p>应用商店会列出可安装且未被服务器配置禁用的应用。当前注册的小游戏包括贪吃蛇、2048、像素鸟、扫雷和别踩白块儿。</p>
<p>各游戏的开始方法、按键和失败条件见<a href="?file=&quot;15-使用.小游戏.md&quot;">小游戏</a>。</p>
<p>系统应用不能通过应用商店卸载；可下载应用可以在应用商店中安装或卸载。管理员也可以通过配置隐藏特定应用，见<a href="?file=&quot;50-管理.配置与命令.md&quot;">配置与命令</a>。</p>

<h2 id="数据保存" data-toc-id="数据保存">数据保存</h2>
<p>手机资料按绑定的主人保存在服务器主世界的 SavedData 中；主人可以是真实玩家或已注册的虚拟角色。资料包含已安装应用、应用排列、壁纸、时间来源、记事本和官方消息等信息。聊天室与好友关系同样保存在服务器主世界数据中。</p>
<div type="warning" data-type="admonition" data-admo-type="warning">
  <div data-type="admonition-title">重置前先备份</div>
  <div data-type="admonition-content">
    <p>管理员命令 <code>/smart_phone reload</code> 会删除执行者的已保存手机信息，并在下次使用时重新建立默认内容。它可能清除个人壁纸、应用排列、记事本和手机内保存的官方消息。</p>
  </div>
</div>
