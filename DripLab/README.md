# 滴落间Lab — DripLab

> 滴落间Lab v1.0.7 · 咖啡冲煮助手 · Redtax 制作

---

## 应用简介

滴落间Lab是一款专为咖啡爱好者打造的多方法冲煮辅助工具，支持手冲、浸泡、意式、爱乐压等多种冲煮方式。通过可视化计时、语音引导、配方管理和冲煮笔记，帮助用户稳定复刻每一次风味。

---

## 功能特性

### 冲煮计时与语音引导

- **可视化圆形计时器**：实时显示阶段倒计时、当前阶段、阶段进度、总进度
- **TTS 语音播报**：每个阶段开始时自动朗读操作指令（闷蒸/注水/滴滤），阶段结束时播报提示音
- **多音效体系**：支持滴答倒计时声（最后 10 秒自动切换为急促提示音）、阶段完成"叮"提示音
- **倒计时音随音量键统一调节**：所有音效走媒体音频通道，可通过手机音量键全局控制

### 冲煮阶段

| 阶段 | 说明 |
|---|---|
| 闷蒸 (Bloom) | 预湿咖啡粉，唤醒风味 |
| 注水 (Pour) | 分段注入热水，萃取风味物质 |
| 滴滤 (Wait) | 停止注水，等待滤液完成 |

### 配方管理

- **5 个预设配方**：经典手冲、极浅烘慢冲、浸泡法、爱乐压（AeroPress）、金杯手冲
- **配方变量**：支持 `${water}`（总水量）、`${coffee}`（咖啡量）等模板变量
- **导出为 JSON**：可设置导出为百分比参数，便于分享
- **导入 JSON 配方**：自动识别百分比参数并还原
- **配方卡片优化**：简洁展示标题与属性摘要，隐藏步骤详情，提升浏览效率
- **一键选定配方**：单击配方卡片即可将配方写入冲煮页，Toast 提示，无需二次确认

### 冲煮笔记

- 每次冲煮后可记录研磨刻度、水温、滤杯、风味感受等笔记
- 支持搜索历史笔记
- 数据持久化存储

### 主题与提示模式

- **5 款主题配色**：经典棕、暗烘黑、薄荷冷萃、燕麦拿铁、摩卡渐变
- **深色模式**：独立开关
- **提示模式**：有声音+有震动 / 有声音+无震动 / 无声音+有震动 / 无声音+无震动
- **背景音乐**：内置咖啡厅环境音、雨声白噪音、轻柔爵士，支持自定义上传音频

### TTS 语音引擎管理

- 自动检测手机中所有可用的 TTS 引擎
- 在「我的」页面可查看引擎列表，点击测试按钮预览朗读效果
- 测试通过后勾选，App 冲煮时自动优先使用用户指定的引擎
- 支持引擎热切换，无需重启 App

### 备份与恢复

- **统一备份导出**：一键导出配方、主题、提示模式、声音等所有自定义设置
- **导入恢复**：选择备份文件，自动恢复所有设置和配方
- **重装保护**：导入的备份文件持久化到内部存储，App 重装后自动恢复
- 备份文件格式为 JSON，可通过系统分享面板发送到微信、邮件等

### 时长比例变量

- 配方导出文本新增 `时%` 变量，与 `水%` 变量配套
- 导入时自动识别百分比参数并还原为实际时长
- 根据咖啡粉量调整，自动按比例重算目标水量和阶段时长

---

## 技术架构

| 技术 | 说明 |
|---|---|
| **Jetpack Compose** | Material3 UI 框架 |
| **Hilt** | 依赖注入 |
| **Room** | 本地数据库（配方 + 笔记） |
| **Coroutines + StateFlow** | 响应式状态管理 |
| **TextToSpeech API** | 语音合成 |
| **SoundPool** | 音效播放（媒体音频流） |
| **DataStore** | 主题/提示模式偏好存储 |

### 数据库迁移

- **v4→v5**：为 `recipe_steps` 表添加 `waterRatio` 字段（当前注水占总水量的比例），用于更精确的阶段注水控制
- **v5→v6**：为 `recipe_steps` 表添加 `durationRatio` 字段（当前阶段时长占总时长的比例），用于时长比例变量导出

---

## 版本历史

### v1.0.7

- 恢复滴答倒计时音：保留 `USAGE_MEDIA` 系统媒体渠道（不再使用闹铃渠道），在 `initSoundPool` / `playClickSound` / `playTickSound` / `playDingSound` 全部增加 Logcat 诊断日志（`DripLab: TTS_TICK: ...`），便于真机精确诊断
- 保留 `AlertMode` 偏好门控（用户 SILENT 模式下滴答音会静音），但通过日志告知 `hasSound=false` 状态，方便用户自查
- 恢复配方页「新增」按钮：手冲/浸泡/意式/爱乐压/法压/摩卡/冷萃全部 7 种方法的 TopAppBar 均增加 `Icons.Default.NoteAdd`「新增配方」按钮（点击 → 直接跳转到空编辑界面 `recipe_edit/0`）
- 导入按钮修复：原 TopAppBar 的 `Add` 图标改为 `Icons.Default.ContentPaste`「导入配方」语义更清晰
- 导入流程修复：原导入弹窗确认按钮直接调用 `viewModel.importRecipe(text)` 写入数据库（绕过编辑界面），现改为调用 `onEditImported(text)` 跳转到 `recipe_edit/0?importedJson=...`，由用户二次确认编辑后才存入配方库
- 保留 `RecipeEditScreen` 顶部的"导入"按钮和导入弹窗（用户要求"原有的编辑界面应予以保留"）
- 保留 v1.0.6 旧 TTS SharedPreferences（`driplab_tts_prefs/selected_tts_engine` → `driplab_prefs/tts_engine`）的一次性迁移逻辑
- 保持 TTS 模块零修改（不修改 `initTts` / `tryEngine` / `createOnInitListener` / `speak` / `resolveEngineList`）
- 保持 Room 数据库 v4→v5→v6 迁移兼容
- 保持 SharedPreferences（主题设置/提示模式/背景音乐/TTS 引擎选择）零覆盖
- 关于页 README 按钮自动读取新版本日志
- 修复本地 Gradle 构建失败：`settings.gradle.kts` 的 `RepositoriesMode.FAIL_ON_PROJECT_REPOS` 与用户本地 `~/.gradle/init.d/init.gradle` 全局 init script 添加的 maven 仓库冲突，改为 `PREFER_SETTINGS` 后兼容自定义 init script（settings 仓库仍优先使用 google/mavenCentral）

### v1.0.6

- 修复 TTS 引擎检测：真机上讯飞语记的包名为 `com.iflytek.vflynote`（小米/讯飞联合定制版主包），原候选列表全部不匹配导致显示「未安装」
- AndroidManifest 新增 `<queries>` 声明（Android 11+ 包可见性），确保 `queryIntentServices` / `getPackageInfo` 能正常访问 TTS 引擎和推荐包
- 引擎检测三层兜底：候选包名匹配 → `_ttsState.value.engines` 已发现引擎 → 标签关键字匹配（`vflynote` / `voicenote` / `iflytek` / `iFly` / `讯飞`）
- Android 16 适配：`initTts()` 优先使用三段式 `TextToSpeech(context, listener, enginePackage)` 显式指定引擎
- TTS 初始化全链路日志：起手记录 API/设备型号、用户选择 vs 已发现、onInit 状态码解释、setLanguage 详细结果名（`LANG_AVAILABLE` / `LANG_MISSING_DATA` 等）、voices / zhVoices 数量、speak 入队结果
- 用户选择引擎 onInit 失败时，构造 `priorityList = [用户选择, 系统默认, ...其他发现引擎]` 自动回退，不再沉默失败
- TTS 测试代码增强：包名预检、`TTS_SERVICE` 注册检测、`tryFallbackDefaultEngine()` 默认引擎兜底
- 推荐引擎区：列出 Google 文字转语音 / 小米大脑语音引擎 / 讯飞语记 三款，提供安装提示与「打开系统 TTS 设置」一键跳转
- 旧 TTS SharedPreferences（`driplab_tts_prefs/selected_tts_engine`）到新位置（`driplab_prefs/tts_engine`）的迁移逻辑，升级时保留用户原有引擎选择
- 关于页面 README 按钮自动读取新版本日志

### v1.0.5

- 恢复「我的 → TTS 引擎」测试与选择 UI：上次备份功能开发时被移除的引擎测试区重新加入
- 修复冲煮运行状态下 TTS 语音失效的 bug：`tryEngine` 失败时 `onInitListener` 没有触发下一个引擎的重试，重试链断裂
- 修复设置页 `testResult` 空指针编译错误：使用 `?.` 安全调用替代直接属性访问
- 推荐安装 TTS 引擎区初版：Google 文字转语音、小米大脑语音引擎、讯飞语记三款引擎的安装指引

### v1.0.4

- 新增统一备份导出/导入功能，支持配方、主题、声音等全部自定义设置的一键备份与恢复
- 新增配方卡片单击选定功能，点击卡片即可将配方设为冲煮页当前配方，Toast 提示
- 配方列表优化：隐藏步骤详情，仅展示标题标签与属性摘要，提升浏览效率
- 新增 `durationRatio` 时长比例字段，配方导出文本支持 `水%` + `时%` 双变量
- 数据库 v5→v6 迁移，新增 `durationRatio` 列
- 修复首页底部导航栏点击首页跳转到冲煮页的问题
- 修复 Room Migration 参数名不匹配编译警告
- 修复 ArrowBack 图标废弃警告
- 修复 SettingsScreen 多余非空断言警告
- 关于页面新增 README 按钮，可查看完整更新日志

### v1.0.3

- 新增 TTS 引擎发现与选择系统，可在设置页测试并指定冲煮用语音引擎
- 音效系统全面迁移至媒体音频流（USAGE_MEDIA），音量响应更饱满
- 音效基础音量统一调至 90%，可通过音量键全局调节
- 优化设置页面布局，新增 TTS 引擎测试区
- 关于页面添加 Redtax 版权声明

### v1.0.2

- 新增配方导出百分比参数开关，便于分享
- 修复预设配方更新逻辑，App 升级时统一规范预设配方
- 数据库迁移保留用户笔记数据

### v1.0.1

- 配方导入/导出 JSON 功能
- 配方变量模板支持（`${water}`、`${coffee}` 等）
- 新增金杯手冲、极浅烘慢冲两个预设配方

### v1.0.0

- 初始版本
- 支持 5 种冲煮配方（经典手冲、浸泡法、爱乐压等）
- 圆形计时器 + TTS 语音播报
- 冲煮笔记记录
- 5 款主题配色 + 深色模式
- 多种提示模式（声音/震动/背景音）

---

## 致谢

感谢每一位咖啡爱好者的支持。滴落间Lab 让每一次冲煮都成为值得记录的仪式。