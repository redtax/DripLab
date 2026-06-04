# DripLab v1.0.7 实施计划

## Summary

完成用户提出的三个修复点（TTS 模块保持原样），并完成版本文档、APK 打包、用户数据保护与 GitHub 推送：
1. **恢复计时滴答倒计时音**（系统声音渠道 USAGE_MEDIA，不动 TTS）
2. **保留卸载数据能力**（已澄清：国内无 Google 云，**本次暂不改，待后续议定**）
3. **恢复配方「新增」编辑界面**（TopAppBar 新增"新增"按钮，"导入"按钮改为跳转到编辑界面供二次确认）
4. **README 更新到 v1.0.7** + APK 打包 + 旧 prefs 迁移 + 推 GitHub

---

## Phase 1 探索结论

| 模块 | 文件 | 状态 |
|---|---|---|
| 倒计时音 | [PourOverViewModel.kt:321-330](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L321-L330) | 代码完整，`onTick` 已绑定；当前 `USAGE_MEDIA` 正确 |
| 倒计时音 | [PourOverViewModel.kt:413-422](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L413-L422) | `playTickSound` 受 `alertMode.hasSound` 控制；音量 0.12 偏低 |
| 倒计时音 | [BrewTimer.kt:73-78](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/timer/BrewTimer.kt#L73-L78) | `onTick` 每秒调用，逻辑正确 |
| 配方 TopAppBar | [RecipeScreen.kt:117-121](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L117-L121) | 只有一个 `Add` 图标，contentDescription 写"导入配方"，无"新增"按钮 |
| 导入弹窗 | [RecipeScreen.kt:208-218](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L208-L218) | 确认按钮直接 `viewModel.importRecipe(text)` 写入数据库，**绕过编辑界面** |
| 导航回调 | [MainActivity.kt:78-79](file:///workspace/DripLab/app/src/main/java/com/driplab/app/MainActivity.kt#L78-L79) | `onEditRecipe(0L)` 路由到 `recipe_edit/0`（已存在，可直接复用） |
| 导航回调 | [MainActivity.kt:80-82](file:///workspace/DripLab/app/src/main/java/com/driplab/app/MainActivity.kt#L80-L82) | `onEditImported(json)` 路由到 `recipe_edit/0?importedJson=...`（已存在） |
| 编辑界面 | [RecipeEditScreen.kt:81-83](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeEditScreen.kt#L81-L83) | `LaunchedEffect(importedJson)` 已会调用 `viewModel.loadImportedRecipe(json)` 填充表单 |
| 旧 prefs 迁移 | [ThemeManager.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/theme/ThemeManager.kt) | v1.0.6 已加 `OLD_TTS_PREFS_NAME`/`OLD_KEY_TTS_ENGINE` 一次性迁移，v1.0.7 沿用 |

---

## Proposed Changes

### 改动 1：恢复并强化滴答倒计时音（TTS 模块零修改）

**文件**：[PourOverViewModel.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt)

**修改点**：
1. **保留** `initSoundPool()` 中的 `USAGE_MEDIA`（系统媒体渠道，禁止改回 USAGE_ALARM/STREAM_ALARM）
2. **保留** `onTick` 回调绑定的 `playTickSound`
3. **保留** `playTickSound()` 中 `alertMode.hasSound` 的门控（**这是用户已有的偏好设置**，不能强行绕过）
4. **新增** Logcat 日志便于真机诊断（`DripLab: TTS_TICK: ...` 单独 tag，避免和 TTS 混在一起）：
   - `initSoundPool()` 末尾：`tickSoundId=$id tickAlertSoundId=$id` 确认声音资源加载成功
   - `playTickSound()` 入口：`remaining=$s alertMode=$m hasSound=$b` 提示用户当前是否被门控静音
   - `playTickSound()` 出口：`played soundId=$id vol=$v` 提示实际播放
5. **新增** `playClickSound()` 也加同样的诊断日志（保持代码风格一致）

**不做的事**（强约束）：
- ❌ 不修改 [PourOverViewModel.kt:243-345](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L243-L345) 的 TTS 模块任何一行
- ❌ 不修改 `initTts` / `tryEngine` / `createOnInitListener` / `speak` / `resolveEngineList`
- ❌ 不修改 `BrewTimer.kt` 的 `onTick` 调用逻辑
- ❌ 不删除 `playTickSound` 函数
- ❌ 不降低音量到 0
- ❌ 不强行移除 `alertMode.hasSound` 门控（用户偏好必须保留）

**为什么用户听不到音的可能原因**（在诊断日志输出后即可定位）：
- 用户的 `AlertMode` 被设成了 `SILENT`（`hasSound = false`）→ 在日志中看到 `hasSound=false` 即确认
- SoundPool 加载失败（`tickSoundId == 0`）→ 资源 ID 异常
- 媒体音量被系统静音 → 这是用户设备问题，应用层无法检测

---

### 改动 2：恢复配方"新增"按钮 + 导入走编辑界面

**文件 A**：[RecipeScreen.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt) — `RecipeScreen` 函数的 `TopAppBar.actions` 区域

**修改**：
- 在 [RecipeScreen.kt:117-121](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L117-L121) 的 `actions = { ... }` 中改为两个 `IconButton`：
  1. 第一个 `IconButton`（最左，"新增"）：`Icons.Default.NoteAdd`（已有 `Add` 图标复用，但改 contentDescription 为"新增配方"），点击 → `onEditRecipe(0L)`
  2. 第二个 `IconButton`（最右，"导入"）：`Icons.Default.ContentPaste`（更贴合"导入文本"语义），点击 → `showImportDialog = true`

**修改**：
- 在 [RecipeScreen.kt:208-218](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L208-L218) 弹窗 `confirmButton` 中：
  - **删除** `viewModel.importRecipe(importText.trim())` 直接导入
  - **改为** `onEditImported(importText.trim())` 触发导航到 `recipe_edit/0?importedJson=...`
  - 关闭弹窗 + 清空输入框保持原状

**文件 B**：[MainActivity.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/MainActivity.kt) — `RecipeScreen(...)` 调用处

**修改**：
- 在 [MainActivity.kt:78-82](file:///workspace/DripLab/app/src/main/java/com/driplab/app/MainActivity.kt#L78-L82) 确认 `onEditRecipe = { recipeId -> ... }` 已支持 `recipeId=0` 跳到 `recipe_edit/0`（**已存在，无需改**）
- 确认 `onEditImported = { json -> ... }` 路由到 `recipe_edit/0?importedJson=${Uri.encode(json)}`（**已存在，无需改**）

**编辑界面验证**：
- [RecipeEditScreen.kt:81-83](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeEditScreen.kt#L81-L83) 已有 `LaunchedEffect(importedJson)` → `viewModel.loadImportedRecipe(importedJson)` 填充表单
- [RecipeEditViewModel.kt:64-72](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeEditViewModel.kt#L64-L72) `loadImportedRecipe` 正确解析 JSON 并 `id=0, isDefault=false`
- 用户在编辑界面点"保存"才走 `saveRecipe()` 落库，**符合二次确认要求**

**不删除的内容**：
- ❌ 不删除现有 `RecipeEditScreen` 顶部"导入"按钮（用户说"原有的编辑界面应予以保留"）
- ❌ 不删除现有 `RecipeEditScreen` 导入弹窗代码（仅调整弹窗导航目标即可）
- ❌ 不修改 `RecipeEditViewModel.loadImportedRecipe` 逻辑

---

### 改动 3：用户数据升级保护（沿用 v1.0.6 已有机制）

**说明**：用户已澄清"国内无 Google 云备份，该方案暂不改动和增加"，**本次不实施任何新的卸载保护代码**。

**已有机制**（v1.0.6 已完成，本版本**只确认不破坏**）：
- `android:allowBackup="true"` 在 [AndroidManifest.xml:18](file:///workspace/DripLab/app/src/main/AndroidManifest.xml) 已设 → Android Auto Backup 默认开启
- ThemeManager 旧 TTS prefs 一次性迁移（v1.0.6 已有）→ v1.0.7 安装后用户原有 TTS 引擎选择保留
- Room 数据库 v4→v5→v6 迁移完整 → 配方/笔记保留
- SharedPreferences 现有 4 个 key 全部保留 → 主题/提示模式/背景音乐保留

**不做的事**：
- ❌ 不新增 `dataExtractionRules`（v1.0.6 已加基础 `<queries>`，与备份无关）
- ❌ 不新增 MediaStore 公共下载导出
- ❌ 不新增 WorkManager 同步任务
- ❌ 不修改 `SettingsBackupManager` 内部逻辑

---

### 改动 4：版本号 & README & GitHub 推送

**文件 A**：[app/build.gradle.kts:11-12](file:///workspace/DripLab/app/build.gradle.kts#L11-L12)
```diff
-        versionCode = 6
-        versionName = "1.0.6"
+        versionCode = 7
+        versionName = "1.0.7"
```

**文件 B**：[README.md](file:///workspace/DripLab/README.md) 根目录
- 顶部版本号改为 v1.0.7
- 新增 v1.0.7 版本日志（在 v1.0.6 之前）
- v1.0.6 标题保留作为历史

**文件 C**：[app/src/main/assets/README.md](file:///workspace/DripLab/app/src/main/assets/README.md) APK 打包用
- 内容与根 README 同步（`assets/README.md` 会被 APK 自动打包，安装时覆盖老版本）
- 不变结构，只更新版本号 + v1.0.7 条目

**v1.0.7 完整 changelog**（将写入 README）：
```markdown
### v1.0.7

- 恢复滴答倒计时音：保留 USAGE_MEDIA 系统媒体渠道，新增 Logcat 诊断日志（DripLab: TTS_TICK tag）便于真机定位
- 恢复配方页"新增"按钮：TopAppBar 增加"新增配方"和"导入配方"两个独立按钮
- 导入按钮修复：导入文本不再直接写入配方库，而是跳转至编辑界面供用户二次确认
- 卸载数据保留（说明）：Android Auto Backup 已开启（allowBackup=true），重装到同 Google 账号可自动恢复；国内设备无 Google 服务时，需手动从「我的 → 备份与恢复」导出 driplab_backup.json
- 保持对 TTS 模块零修改
- 保持对 RecipeEditScreen 既有导入功能零修改
- 保持对 Room 数据库 v4→v5→v6 迁移兼容
- 保持对 SharedPreferences（主题/提示模式/背景音乐/旧 TTS 引擎）一次性迁移
```

**Git 推送流程**：
1. `git add` 修改的 4 个文件（build.gradle, root README, assets/README, PourOverViewModel.kt 或 RecipeScreen.kt）
2. `git commit -m "feat: 滴落间Lab v1.0.7 - 倒计时音恢复 + 配方新增编辑界面恢复"`
3. `git push origin trae/solo-agent-HUfLDt`（当前分支，避免直接 push main）

---

## Assumptions & Decisions

### 决策记录

1. **滴答音音量不动**：当前 `tickSoundId` 音量 0.12f，`tickAlertSoundId` 音量 0.3f（最后 10 秒）。用户没要求改音量，保持现状避免引入"调整音量被用户视为修改"的争议。

2. **AlertMode 门控保留**：`SILENT` 模式下滴答音会静音。这是用户既有偏好（v1.0.3 起就有的设计），不能强制覆盖。诊断日志会清楚显示 `hasSound=false`，用户可自行切换。

3. **卸载保留方案不实施**：用户已明确表示国内无 Google 服务，本次**只做现有机制的不破坏**，不新增功能。

4. **新增按钮图标**：`Icons.Default.NoteAdd`（加减号图标）+ `Icons.Default.ContentPaste`（剪贴板图标），比 `Icons.Default.Add`（加号）更明确区分两个操作。

5. **导入弹窗文案不变**：仅修改确认按钮行为，弹窗的标题、placeholder、文本域保持原样以最小化变更。

6. **不修改 `RecipeFormatManager`**：JSON 解析和导出逻辑保持原样。

7. **不修改 `RecipeViewModel.importRecipe()` 方法**：保留原方法，因为可能其他地方还在调用（虽然新流程已不再从 RecipeScreen 调用，但保留向后兼容）。

### 风险点

- **风险 1**：用户可能因为 `AlertMode = SILENT` 听不到滴答音 → 诊断日志暴露问题，用户可自行在「我的」切到 `STANDARD`
- **风险 2**：新增"新增配方"按钮后，导入按钮的图标变更 → 已在 plan 中选定具体图标 (`ContentPaste`)
- **风险 3**：v1.0.7 推送后需要用户在真机验证三件事（滴答音 / 新增按钮 / 导入跳转编辑）

---

## Verification

### 1. 编译验证（沙箱无 Android SDK，需在用户本地执行）
```bash
cd /workspace/DripLab
gradle :app:assembleDebug
```
- 预期：BUILD SUCCESSFUL，无 Kotlin 编译错误
- 重点：v1.0.6 时 403/426/436/439 行的 `testResult?.xxx` 已正确使用 `?.`，本次不应重新引入空指针

### 2. 滴答音真机验证
```bash
adb shell am force-stop com.driplab.app
adb logcat -c
adb logcat -s DripLab:D
```
操作：
1. 启动 app → 「我的」→ 确认 `AlertMode = STANDARD`（不是 SILENT）
2. 进入「冲煮」→ 开始冲煮
3. 观察日志中 `TTS_TICK: remaining=... hasSound=true played soundId=...`
4. 听媒体通道（手机音量键调大）是否每秒有"嗒"声

### 3. 新增按钮验证
1. 进入「手冲配方」列表
2. TopAppBar 右上角应有两个图标：左 `NoteAdd`（新增）、右 `ContentPaste`（导入）
3. 点击 `NoteAdd` → 跳转 `recipe_edit/0`，空表单
4. 在编辑界面填名称 + 加阶段 → 保存 → 返回列表，新配方出现

### 4. 导入跳转编辑验证
1. 进入「手冲配方」列表
2. 点击 `ContentPaste` → 弹窗
3. 粘贴一份配方文本 → 点"导入"
4. **应跳转到** `recipe_edit/0?importedJson=...`，**而不是**直接进配方库
5. 在编辑界面检查表单是否填充了文本内容
6. 手动点"保存"才进配方库

### 5. 数据升级验证（v1.0.6 → v1.0.7）
- 安装 v1.0.7 覆盖 v1.0.6
- 验证：原有配方列表、笔记、主题、提示模式、背景音乐、TTS 引擎选择均保留

### 6. Git 推送验证
```bash
cd /workspace/DripLab
git log --oneline -3
git push origin trae/solo-agent-HUfLDt
```
- 预期：最新提交 `feat: 滴落间Lab v1.0.7 - 倒计时音恢复 + 配方新增编辑界面恢复`
- 远端 `trae/solo-agent-HUfLDt` HEAD 包含 v1.0.7 提交
