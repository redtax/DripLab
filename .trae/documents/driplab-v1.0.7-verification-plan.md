# DripLab v1.0.7 验证与确认计划

## Summary

用户要求 v1.0.7 的三个核心修复（TTS 模块保持原样、卸载数据保留澄清后暂缓、配方新增编辑界面恢复）、README 升级、APK 打包、用户数据保护、推送 GitHub。

**当前状态：v1.0.7 全部工作已于上一轮会话中实施完毕并推送至远端。**

本计划作为新一次 `/plan` 会话的交付物，**重新核对每项要求的代码现状**，并提供用户真机验证指引。

---

## Phase 1 探索结论（代码现状核对）

| 用户要求 | 文件 | 实施结果 |
|---|---|---|
| ① 恢复滴答倒计时音 | [PourOverViewModel.kt:320-336](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L320-L336) | ✅ `USAGE_MEDIA` 保留（系统媒体渠道，**非闹铃渠道**） |
| ① 滴答音 | [PourOverViewModel.kt:348-361](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L348-L361) | ✅ `playTickSound()` 完整保留 + 新增 Logcat 诊断日志 |
| ① TTS 模块零修改 | [PourOverViewModel.kt:243-318](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L243-L318) | ✅ `initTts` / `tryEngine` / `createOnInitListener` / `speak` / `resolveEngineList` 全部未动 |
| ② 卸载数据保留 | 用户澄清 | ⏸️ 国内无 Google 云，**暂不实施**（本轮无代码变更） |
| ③ 配方"新增"按钮 | [RecipeScreen.kt:119-126](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L119-L126) | ✅ TopAppBar 增加 `Icons.Default.NoteAdd`「新增配方」（`onEditRecipe(0L)`） |
| ③ 导入走编辑界面 | [RecipeScreen.kt:213-222](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeScreen.kt#L213-L222) | ✅ 弹窗确认按钮改为 `onEditImported(text)` → `recipe_edit/0?importedJson=...` |
| ③ 原有编辑界面保留 | [RecipeEditScreen.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/recipe/RecipeEditScreen.kt) | ✅ 顶部的"导入"按钮和导入弹窗完全保留 |
| ④ README 升级 v1.0.7 | [README.md:3,95-108](file:///workspace/DripLab/README.md#L3) | ✅ 顶部版本号 + v1.0.7 完整 changelog |
| ④ APK 打包 README | [assets/README.md:3,95-108](file:///workspace/DripLab/app/src/main/assets/README.md#L3) | ✅ 与根 README 同步，安装时自动覆盖 |
| ④ 用户数据升级保护 | 既有机制 | ✅ v1.0.6 旧 TTS prefs 迁移 + Room v4→v5→v6 迁移 + 4 个 SharedPreferences key 全部保留 |
| ④ 推送 GitHub | 提交 `f31f347` | ✅ `trae/solo-agent-HUfLDt` HEAD 包含 v1.0.7 提交 |

### Git 状态

```
f31f347 feat: 滴落间Lab v1.0.7 - 倒计时音恢复 + 配方新增编辑界面恢复
971e741 feat: 滴落间Lab v1.0.6 - TTS 引擎检测修复 + 数据迁移
```

- 5 个文件变更，54 行新增，11 行删除
- 已推送到 `origin/trae/solo-agent-HUfLDt`

---

## 现状分析

### 已经完整实施的功能

1. **滴答倒计时音诊断**（**核心修复**）
   - `initSoundPool()` 在加载 4 个声音资源后输出 1 行 INFO 日志
   - `playClickSound()` 输出 DEBUG 日志（被 AlertMode 门控时打 `SKIPPED`）
   - `playTickSound()` 输出 DEBUG 日志（剩余秒数、hasSound、playResult ID）
   - `playDingSound()` 输出 DEBUG 日志
   - 用户可在 logcat 用 `adb logcat -s DripLab:D` 单独过滤滴答音链路

2. **USAGE_MEDIA 渠道**（**核心修复**）
   - `initSoundPool()` 明确使用 `AudioAttributes.USAGE_MEDIA` + `CONTENT_TYPE_MUSIC`
   - 用户手机媒体音量键可直接调节滴答音音量
   - **未使用 `USAGE_ALARM` / `STREAM_ALARM`**，与 TTS 走同一媒体通道

3. **TTS 模块零修改**（**强约束**）
   - `initTts()` 第 243-260 行未动
   - `tryEngine()` 第 263-282 行未动
   - `createOnInitListener()` 第 285-298 行未动
   - `speak()` 第 200-235 行未动
   - `resolveEngineList()` 第 240-318 行未动

4. **配方"新增"按钮**（**核心修复**）
   - TopAppBar 第一个 IconButton = `Icons.Default.NoteAdd`（加减号图标）
   - 点击触发 `onEditRecipe(0L)` → 导航到 `recipe_edit/0` 空编辑界面
   - 用户在编辑界面填表 + 保存才落库

5. **导入流程修复**（**核心修复**）
   - TopAppBar 第二个 IconButton = `Icons.Default.ContentPaste`（剪贴板图标）
   - 点击弹窗 → 输入文本 → 点"导入" → 触发 `onEditImported(text)` → 导航到 `recipe_edit/0?importedJson=...`
   - 用户的导入文本在编辑界面呈现，用户确认保存才进配方库

6. **原有编辑界面保留**
   - `RecipeEditScreen` 顶部的"导入"按钮和导入弹窗**未删除、未修改**
   - 该入口仍可让用户在编辑过程中再导入别的配方

7. **用户数据升级保护**（**不破坏既有机制**）
   - `android:allowBackup="true"` 未变
   - Room v4→v5→v6 迁移完整
   - SharedPreferences 4 个 key（主题、提示模式、背景音乐、TTS 引擎）零覆盖
   - v1.0.6 旧 `driplab_tts_prefs/selected_tts_engine` → 新 `driplab_prefs/tts_engine` 一次性迁移

8. **README 双源同步**
   - 根 [README.md](file:///workspace/DripLab/README.md) 用于 GitHub 展示
   - [app/src/main/assets/README.md](file:///workspace/DripLab/app/src/main/assets/README.md) 用于 APK 打包
   - 安装时 `assets/README.md` 会覆盖设备上老版本的同位置文件
   - 主题、配方、笔记、参数设置（v1.0.6 起的 SharedPreferences 迁移）均不覆盖

9. **Git 推送**
   - 提交 `f31f347` 推送到 `origin/trae/solo-agent-HUfLDt`

### 暂不实施的功能

- **卸载数据保留**（用户澄清：国内无 Google 云，方案待后续议定）
  - 本轮未新增任何卸载保留代码
  - 既有 `allowBackup=true` 与 SettingsBackupManager 维持原状

---

## 本计划（本次会话）的处理方式

由于 v1.0.7 的全部代码改动、文档、推送均已在上一轮完成，**本计划文件作为新会话的核对记录**，不重复执行任何代码修改。

**若用户在真机验证后报告任何问题**，将根据本计划的"Verification"章节快速定位：
- 滴答音无音 → 检查 `DripLab: TTS_TICK: ...` 日志中的 `hasSound` / `playResult`
- 新增按钮缺失 → 检查 `RecipeScreen.kt:120-122` 是否在 TopAppBar
- 导入直接落库 → 检查 `RecipeScreen.kt:216` 是否为 `onEditImported`

---

## Assumptions & Decisions

### 强约束（用户明确要求）

1. **TTS 模块零修改**：v1.0.7 严格保持 TTS 链路不动，仅在 SoundPool 部分新增日志
2. **USAGE_MEDIA 渠道**：滴答音使用系统媒体渠道，音量键统一控制
3. **导入需二次确认**：用户粘贴文本后，必须经过编辑界面再保存才进配方库
4. **原有编辑界面保留**：`RecipeEditScreen` 顶部的导入入口不能删
5. **用户数据不覆盖**：升级到 v1.0.7 不会丢失用户原有数据
6. **不修改无关代码**：未涉及 brew timer 核心逻辑、配方解析、数据库结构等

### 决策记录

1. **滴答音音量维持 0.12f / tickAlert 0.3f**：用户未要求改音量，最小化变更
2. **AlertMode 偏好门控保留**：用户 SILENT 模式下滴答音会静音，由日志提示状态
3. **新增按钮图标选 NoteAdd + ContentPaste**：比 `Icons.Default.Add`（加号）更明确区分两操作
4. **导入弹窗文案不变**：仅修改确认按钮行为，弹窗标题/placeholder/文本域保持原样
5. **不修改 `RecipeFormatManager` / `RecipeViewModel.importRecipe()`**：保留向后兼容

### 风险点

- **风险 1**：用户 `AlertMode = SILENT` 时滴答音会静音 → 诊断日志暴露 `hasSound=false`，用户可自行切换
- **风险 2**：真机媒体音量被系统静音 → 应用层无法检测，需用户自查
- **风险 3**：v1.0.7 推送后需用户在真机验证三件事

---

## Verification

### 1. 编译验证（需在用户本地执行，沙箱无 Android SDK）
```bash
cd /workspace/DripLab
gradle :app:assembleDebug
```
- 预期：`BUILD SUCCESSFUL`，无 Kotlin 编译错误
- 重点：v1.0.6 时 403/426/436/439 行的 `testResult?.xxx` 已使用 `?.` 安全调用

### 2. 滴答音真机验证
```bash
adb shell am force-stop com.driplab.app
adb logcat -c
adb logcat -s DripLab:D
```
操作：
1. 启动 app → 「我的」→ 确认 `AlertMode = STANDARD`（非 SILENT）
2. 进入「冲煮」→ 开始冲煮
3. 观察日志中 `TTS_TICK: remaining=... hasSound=true played soundId=...`
4. 听媒体通道（手机音量键调大）是否每秒有"嗒"声
5. 最后 10 秒应听到急促的 `tick_alert` 音

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

---

## 后续步骤

本计划文件**不发起任何新代码改动**。如果用户希望进一步：

1. **真机诊断滴答音**：用户跑 `adb logcat -s DripLab:D` 提供日志，根据 `TTS_TICK: hasSound=... playResult=...` 定位
2. **后续版本（v1.0.8+）规划**：若用户有新的功能/修复需求，将创建新计划文件
3. **卸载数据保留方案**：等待用户与项目方议定方案后，再单独制定计划
