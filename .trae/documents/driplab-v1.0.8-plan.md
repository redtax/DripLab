# DripLab v1.0.8 实施计划

## Summary

修复用户报告的两个 BUG（最小变更，保留所有已稳定功能）：

1. **滴答音无音**：在冲煮页顶部增加「当前提示模式」提示行，让用户在 SILENT 模式下能直观看到「静音/标准/轻柔/独享」标签，立即知道为什么滴答音被静音。
2. **关于页版本号错乱**：`SettingsScreen.kt` 第 749/773 行的「滴落间Lab v1.0.4」是硬编码字符串（升级时被遗漏），改为 `BuildConfig.VERSION_NAME`，并在 `build.gradle.kts` 启用 `buildConfig=true`，未来版本号自动同步。

> **不修改 TTS 模块**（用户强调保留）
> **不修改 `playTickSound` 的 AlertMode 门控**（用户偏好必须保留）
> **不修改 Room 数据库 / SharedPreferences 结构**（无数据结构调整）

---

## Phase 1 探索结论

| BUG | 文件 | 行 | 现象 | 根因 |
|---|---|---|---|---|
| ① 滴答音无音 | [PourOverViewModel.kt:348-361](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt#L348-L361) | 348-361 | `TTS_TICK: playTickSound SKIPPED (alertMode=SILENT hasSound=false)` | 用户在「我的 → 提示模式」中设为「静音」（`AlertMode.SILENT` 的 `hasSound=false`），与 TTS 共享门控，符合 v1.0.7 设计 |
| ① 用户无可视化提示 | [PourOverScreen.kt:96-134](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverScreen.kt#L96-L134) | 105 起 | 冲煮页 Column 中无 AlertMode 显示 | UI 缺少当前模式提示，用户进 SILENT 后无感知 |
| ② 版本号硬编码 | [SettingsScreen.kt:749](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/settings/SettingsScreen.kt#L749) | 749 | `Text("滴落间Lab v1.0.4", ...)` | 硬编码字符串，v1.0.4 升级到 v1.0.7 时未跟随更新 |
| ② README 弹窗标题硬编码 | [SettingsScreen.kt:773](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/settings/SettingsScreen.kt#L773) | 773 | `Text("滴落间Lab v1.0.4", ...)` | 同上 |
| ② BuildConfig 状态 | [app/build.gradle.kts:44-46](file:///workspace/DripLab/app/build.gradle.kts#L44-L46) | 44-46 | `buildFeatures { compose = true }` | AGP 8.x 默认不生成 BuildConfig，需显式 `buildConfig = true` |
| ② README 正确 | [SettingsScreen.kt:733](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/settings/SettingsScreen.kt#L733) | 733 | `context.assets.open("README.md")` 读取 assets | 正确（assets/README.md 已同步 v1.0.7） |

### 用户选择的方案

- **BUG ①**：在冲煮页顶部加一行「当前提示模式: 静音」提示（5 行代码变更）
- **BUG ②**：启用 BuildConfig + 替换两处硬编码（3 文件，5 行变更）

---

## Proposed Changes

### 改动 1：冲煮页增加 AlertMode 提示行（BUG ①）

**文件 A**：[PourOverViewModel.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverViewModel.kt)

**步骤 1.1**：在 `PourOverUiState` data class（第 38-61 行）中增加 `alertMode` 字段：
```kotlin
data class PourOverUiState(
    ...,
    val noteAutoSaved: Boolean = false,
    val alertMode: AlertMode = AlertMode.STANDARD  // 新增
)
```

**步骤 1.2**：在 `init { ... }` 块中（已有 `initTts()`/`initSoundPool()`）之后，添加对 `themeManager.state` 的观察，将 `alertMode` 同步到 `_uiState`：
```kotlin
init {
    ...,
    initTts()
    initSoundPool()
    // 同步 AlertMode 到 UI 状态
    viewModelScope.launch {
        themeManager.state.collect { theme ->
            _uiState.value = _uiState.value.copy(alertMode = theme.alertMode)
        }
    }
}
```

**文件 B**：[PourOverScreen.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/pourover/PourOverScreen.kt)

**步骤 1.3**：在 `PourOverScreen` 的 Column 顶部（`BrewControlSection` 之前）增加 `AlertModeBanner(state.alertMode)`：
```kotlin
Column(...) {
    // 新增：AlertMode 提示行（仅在非 STANDARD 时显示，避免信息噪音）
    if (state.alertMode != AlertMode.STANDARD) {
        AlertModeBanner(state.alertMode)
        Spacer(modifier = Modifier.height(8.dp))
    }
    BrewControlSection(state, viewModel)
    ...
}
```

**步骤 1.4**：在文件末尾添加 `AlertModeBanner` 私有 Composable：
```kotlin
@Composable
private fun AlertModeBanner(alertMode: AlertMode) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* 可选：点击跳设置页 */ }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "当前提示模式: ${alertMode.displayName}（${if (alertMode.hasSound) "有" else "无"}声音）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
```

**为什么是「非 STANDARD 才显示」**：
- 用户在 STANDARD 模式下不需要任何提示（默认正常）
- 用户在 SILENT/GENTLE/SOLO 模式下，需要立即知道当前模式，因为可能影响滴答音/TTS 播报
- 避免在标准模式下出现「信息噪音」

**不修改的内容（强约束）**：
- ❌ 不修改 `AlertMode` 枚举定义
- ❌ 不修改 `playTickSound`/`playClickSound`/`playDingSound` 的 AlertMode 门控逻辑
- ❌ 不修改 TTS 模块（`initTts`/`tryEngine`/`speak` 等）
- ❌ 不修改 `BrewTimer` 的 `onTick` 回调
- ❌ 不修改 `ThemeManager.loadState()` 逻辑

---

### 改动 2：BuildConfig 启用 + 替换硬编码版本号（BUG ②）

**文件 A**：[app/build.gradle.kts](file:///workspace/DripLab/app/build.gradle.kts)

**步骤 2.1**：在 `buildFeatures { ... }` 块（第 44-46 行）增加 `buildConfig = true`：
```kotlin
buildFeatures {
    compose = true
    buildConfig = true  // 新增：AGP 8.x 默认不生成 BuildConfig
}
```

**文件 B**：[SettingsScreen.kt](file:///workspace/DripLab/app/src/main/java/com/driplab/app/ui/settings/SettingsScreen.kt)

**步骤 2.2**：在 import 区域（`^import com.driplab.app.domain.model.AlertMode` 之后）增加 `BuildConfig` import：
```kotlin
import com.driplab.app.BuildConfig
```

**步骤 2.3**：将第 749 行的硬编码替换：
```diff
- Text("滴落间Lab v1.0.4", style = MaterialTheme.typography.bodyLarge)
+ Text("滴落间Lab v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
```

**步骤 2.4**：将第 773 行的硬编码替换（README 弹窗标题）：
```diff
- Text("滴落间Lab v1.0.4", fontWeight = FontWeight.Bold)
+ Text("滴落间Lab v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
```

**为什么需要 `buildConfig = true`**：
- AGP 8.0+ 默认不生成 `BuildConfig` 类（性能优化）
- 项目当前未启用，需要显式开启才能引用 `BuildConfig.VERSION_NAME` / `VERSION_CODE`

**为什么不用 `packageManager.getPackageInfo(...).versionName`**：
- 该方案需要 Context + PackageManager，代码更复杂
- BuildConfig 在编译期固化，零运行时开销
- 与 Compose 直接组合更优雅

**不修改的内容（强约束）**：
- ❌ 不修改 `app/build.gradle.kts` 的 versionCode/versionName（保持 7/1.0.7）
- ❌ 不修改 `AndroidManifest.xml`
- ❌ 不修改 ThemeManager / SettingsViewModel
- ❌ 不修改任何其他显示版本号的位置（如日志）

---

### 改动 3：版本号升级到 v1.0.8 + README 同步

**文件 A**：[app/build.gradle.kts](file:///workspace/DripLab/app/build.gradle.kts)
```diff
-        versionCode = 7
-        versionName = "1.0.7"
+        versionCode = 8
+        versionName = "1.0.8"
```

**文件 B**：[README.md](file:///workspace/DripLab/README.md)
- 顶部版本号改为 v1.0.8
- 在 v1.0.7 之前新增 `### v1.0.8` 章节

**文件 C**：[app/src/main/assets/README.md](file:///workspace/DripLab/app/src/main/assets/README.md)
- 与根 README 同步（APK 安装时自动覆盖）

**v1.0.8 changelog**（将写入 README）：
```markdown
### v1.0.8

- 修复关于页版本号错乱：`SettingsScreen.kt` 两处硬编码「滴落间Lab v1.0.4」改为 `BuildConfig.VERSION_NAME`，并启用 `buildFeatures.buildConfig=true`，未来版本号自动同步
- 冲煮页顶部增加「当前提示模式: 静音/标准/轻柔/独享」提示横幅（仅在非 STANDARD 模式时显示），方便用户立即知道 TTS/滴答音门控状态
- 保持 TTS 模块零修改
- 保持 `playTickSound` 的 AlertMode 偏好门控（用户 SILENT 模式下滴答音会静音，仍可通过「我的 → 提示模式」切换）
- 保持 Room 数据库 v4→v5→v6 迁移兼容
- 保持 SharedPreferences（主题设置/提示模式/背景音乐/TTS 引擎选择）零覆盖
- 保留 v1.0.6 旧 TTS SharedPreferences 一次性迁移逻辑
```

---

## Assumptions & Decisions

### 决策记录

1. **BUG ① 不绕过 AlertMode 门控**：用户偏好门控是 v1.0.3 起的成熟设计，强制绕过会破坏「静音模式」语义。改用「提示横幅」让用户知道原因。
2. **横幅仅在非 STANDARD 时显示**：避免在正常模式下出现信息噪音；用户主动切到非默认模式后才有必要提示。
3. **横幅可点击（可选）**：未在首版实现点击跳设置，避免引入新导航逻辑；后续可迭代。
4. **BuildConfig 而非 stringResource**：BuildConfig 在编译期固化、零运行时开销；stringResource 需要 build.gradle 生成 string 任务，复杂度更高。
5. **BuildConfig.VERSION_NAME 而非 VERSION_CODE**：用户看到的是「v1.0.8」字符串而非「8」数字，符合既有 UI 风格。
6. **不修改 pour-over AlertMode 行为**：仅在 UI 暴露状态，不修改 playTickSound/playClickSound/playDingSound 的判断逻辑。
7. **不删除 v1.0.7 章节**：v1.0.8 是新增 changelog，历史版本完整保留。
8. **README 双源同步**：根 README（GitHub 展示）+ assets/README（APK 打包用），安装时自动覆盖老版本。

### 风险点

- **风险 1**：用户已习惯标准模式，新增横幅在非 STANDARD 时才显示 → 标准模式用户体验完全不变
- **风险 2**：BuildConfig.VERSION_NAME 在 Kotlin 字符串模板中需要 `${...}` 包裹 → 已用 `${BuildConfig.VERSION_NAME}` 正确转义
- **风险 3**：v1.0.8 推送后需用户在真机验证两件事（关于页版本号/冲煮页模式提示）

---

## Verification

### 1. 编译验证（需在用户本地执行）
```bash
cd /workspace/DripLab
gradle :app:assembleDebug
```
- 预期：`BUILD SUCCESSFUL`，无 Kotlin 编译错误
- 重点：`SettingsScreen.kt` 第 749/773 行的 `BuildConfig.VERSION_NAME` 解析正常

### 2. 关于页版本号真机验证
1. 启动 app → 「我的」→ 「关于」
2. 卡片标题应显示「滴落间Lab v1.0.8」
3. 点击「查看 README 更新日志」→ 弹窗标题应显示「滴落间Lab v1.0.8」
4. 弹窗内的 README 内容（从 assets 加载）应包含 v1.0.8 changelog

### 3. 冲煮页模式提示真机验证
```bash
adb logcat -c
adb logcat -s DripLab:D
```
操作：
1. 启动 app → 「我的」→ 「提示模式」选「静音」
2. 返回「冲煮」页
3. **应看到顶部横幅**：「🔔 当前提示模式: 静音（无声音）」
4. 切到「标准」模式
5. **应看不到**该横幅（仅在非 STANDARD 时显示）
6. 切到「轻柔」模式
7. **应看到**横幅：「🔔 当前提示模式: 轻柔（有声音）」

### 4. 滴答音验证（用户切到 STANDARD 后）
1. 「我的」→ 提示模式设为「标准」
2. 进入「冲煮」→ 开始冲煮
3. 听媒体通道（手机音量键调大）是否每秒有"嗒"声
4. logcat 不应有 `TTS_TICK: playTickSound SKIPPED (alertMode=SILENT hasSound=false)`

### 5. 数据升级验证（v1.0.7 → v1.0.8）
- 安装 v1.0.8 覆盖 v1.0.7
- 验证：原有配方列表、笔记、主题、提示模式、背景音乐、TTS 引擎选择均保留
- 验证：原 SILENT 模式设置保留（v1.0.8 不应重置用户的 alertMode）

### 6. Git 推送验证
```bash
cd /workspace/DripLab
git log --oneline -3
git push origin trae/solo-agent-HUfLDt
```
- 预期：最新提交 `feat: 滴落间Lab v1.0.8 - 修复关于页版本号 + 冲煮页模式提示`

---

## 强约束（用户明确要求）

- **a. 修复范围控制**：仅修改 4 个文件（PourOverViewModel.kt/PourOverScreen.kt/SettingsScreen.kt/build.gradle.kts），TTS/滴答音/数据库/UI 业务逻辑零影响
- **b. 重大变更确认机制**：不涉及功能删除/页面重构/核心业务改写/数据结构调整，符合最小变更原则
- **c. 数据兼容与迁移保障**：未触动 Room v4→v5→v6、未触动 SharedPreferences 4 个 key、未触动旧 TTS prefs 迁移，v1.0.7 升级到 v1.0.8 用户数据完全保留
