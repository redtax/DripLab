# DripLab v1.0.9 设置持久化加固实施计划（TDD）

## Summary

用户报告：本地自定义设置（提示模式 / 主题 / 声音 / TTS 引擎等）在应用退出后丢失，举例：每次重启都以「静音」启动，应当以用户上次选择为准。

**根因**（用户选择「未知，统一加固」）：

1. **DripLabApplication 启动时无条件从 driplab_backup.json 恢复**——若设备存在历史 driplab_backup.json（其中 alertMode=0=SILENT），会覆盖 SharedPreferences 中用户最新的设置
2. **loadCurrentSettings() 默认值错误**——`alertModeIndex` 缺省值是 0 (SILENT)，与 ThemeManager 的 2 (STANDARD) 不一致，导出文件永远带 SILENT
3. **设置变更不同步到备份**——用户改 STANDARD 后退出，driplab_backup.json 仍是旧值；下次启动会被旧备份覆盖回旧值
4. **无自动化测试**——历史回归无保障

**修复方案（用户选择 4 项）**：
- a. 启动时仅在 SharedPreferences 为空时（首装）从备份恢复
- b. 设置变更后即时同步到 driplab_backup.json
- c. 修正 loadCurrentSettings 默认值 SILENT 误用
- d. 使用 TDD 添加自动化测试

**用户附加要求**：「需要考虑原有的"备份与恢复功能"的代码同步，可在该功能中将所有自定义设置完整备份和导入」——本计划保持 Settings → 备份/恢复 入口的所有自定义设置（配方/主题/声音/TTS 引擎/提示模式/背景音乐/计时器设置）完整备份/恢复。

> **TDD 铁律**：测试先行，RED → GREEN → REFACTOR，每个生产代码都先有失败测试。
> **数据兼容约束**：不修改 Room 数据库 schema；不删除 driplab_backup.json 旧版结构；保持 v1.0.6 旧 TTS prefs 迁移逻辑；保持 v1.0.7 SettingsBackupManager 入口。

---

## Phase 1 探索结论

### 现有持久化机制总览

| 存储 | 文件 / 库 | 用途 | 写入路径 | 读取路径 |
|---|---|---|---|---|
| **SharedPreferences** | `driplab_prefs.xml` | 当前设置（主题/暗色/提示模式/背景音乐/TTS 引擎） | `ThemeManager` setters | `ThemeManager.loadState()` |
| **旧 TTS SharedPreferences** | `driplab_tts_prefs.xml` | v1.0.6 前的 TTS 引擎 | （v1.0.6 之后停止写入） | `ThemeManager.loadState()` 一次性迁移 |
| **Room 数据库** | `driplab.db` | 配方/笔记/计时器数据 | Room repositories | Room repositories |
| **备份文件** | `filesDir/driplab_backup.json` | 显式导出的完整备份（含 recipes + BackupSettings） | `SettingsBackupManager.exportData()` → `RecipeBackupManager.backup()` | `DripLabApplication.onCreate()` + `SettingsBackupManager.importData()` |

### 关键文件位置

- [DripLabApplication.kt:30-58](file:///workspace/DripLab/app/src/main/java/com/driplab/app/DripLabApplication.kt#L30-L58) — 启动时无条件恢复逻辑
- [RecipeBackupManager.kt:75-83](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/recipe/RecipeBackupManager.kt#L75-L83) — `hasBackup()` 判定
- [RecipeBackupManager.kt:170-200](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/recipe/RecipeBackupManager.kt#L170-L200) — `loadCurrentSettings()` 默认值错误
- [SettingsBackupManager.kt:1-50](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/backup/SettingsBackupManager.kt#L1-L50) — 显式导出/导入入口
- [ThemeManager.kt:1-50](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/theme/ThemeManager.kt#L1-L50) — 状态读写 + 旧 TTS prefs 迁移
- [build.gradle.kts:92-96](file:///workspace/DripLab/app/build.gradle.kts#L92-L96) — JUnit + Espresso 测试框架（已就位）

### 用户选择的方案

1. ✅ 取消启动时无条件从备份恢复（仅首装恢复）
2. ✅ 设置变更后即时同步到 driplab_backup.json
3. ✅ 修正 loadCurrentSettings 默认值 SILENT 误用
4. ✅ 使用 TDD 添加自动化测试
5. ✅ 备份与恢复功能保持完整（全部自定义设置）
6. ✅ 取消启动时无条件从备份恢复
7. ✅ 设置变更后即时同步到 driplab_backup.json
8. ✅ 修正 loadCurrentSettings 默认值 SILENT 误用
9. ✅ 使用 TDD 添加自动化测试

---

## Proposed Changes（TDD 顺序：先写测试，再写代码）

### 步骤 1：建立测试目录与基础设施

**文件 A（新建）**：`app/src/test/java/com/driplab/app/core/backup/SettingsPersistenceTest.kt`

**为什么先建**：TDD 铁律要求测试先于实现。但本次修改涉及 Android 框架（Context/SharedPreferences），用 Robolectric 比纯 JUnit 更好。先建测试文件结构。

**步骤 1.1**：在 `app/build.gradle.kts` 增加 Robolectric 依赖：
```kotlin
testImplementation("org.robolectric:robolectric:4.12.2")
testImplementation("androidx.test:core:1.5.0")
testImplementation("androidx.test.ext:junit:1.1.5")
```

**步骤 1.2**：创建 `app/src/test/java/com/driplab/app/core/backup/SettingsPersistenceTest.kt` 空文件（先 RED 占位）。

---

### 步骤 2：TDD 修复 ① —— 启动时仅首装恢复（RED → GREEN）

#### 2.1 RED 阶段：写失败测试

**文件**：[SettingsPersistenceTest.kt](file:///workspace/DripLab/app/src/test/java/com/driplab/app/core/backup/SettingsPersistenceTest.kt)（新建）

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsPersistenceTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var themeManager: ThemeManager
    private lateinit var backupManager: RecipeBackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 清空 SharedPreferences + 备份文件
        context.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        // 测试 1：首装场景（prefs 为空 + 备份文件存在 SILENT）应从备份恢复
    }

    @Test
    fun `first install with existing backup should restore from backup`() {
        // RED：当前实现是「无条件恢复」，所以这个测试会失败
        // GREEN：应当先检查 SharedPreferences 是否为空，空才恢复
        val sharedPrefs = context.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
        // 写入旧的备份文件
        val backupFile = File(context.filesDir, "driplab_backup.json")
        backupFile.writeText("""{"version":1,"settings":{"themeIndex":0,"darkTheme":false,"alertModeIndex":1,"bgMusicUri":""},"recipes":[]}""")
        // 第一次启动
        backupManager = RecipeBackupManager(context)
        themeManager = ThemeManager(context)
        themeManager.start()  // 模拟 DripLabApplication 启动
        // 期望：alertMode = GENTLE (index 1)，从备份恢复
        assertEquals(AlertMode.GENTLE, themeManager.state.value.alertMode)
    }

    @Test
    fun `existing user prefs should NOT be overwritten by older backup`() {
        // RED：当前实现会无条件用旧备份覆盖
        // GREEN：仅当 prefs 为空时才恢复
        val sharedPrefs = context.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
        // 用户当前设置：STANDARD (index 2)
        sharedPrefs.edit().putInt("alert_mode_index", 2).commit()
        // 写入旧备份：SILENT (index 0)
        val backupFile = File(context.filesDir, "driplab_backup.json")
        backupFile.writeText("""{"version":1,"settings":{"themeIndex":0,"darkTheme":false,"alertModeIndex":0,"bgMusicUri":""},"recipes":[]}""")
        // 启动
        backupManager = RecipeBackupManager(context)
        themeManager = ThemeManager(context)
        themeManager.start()
        // 期望：alertMode 保持 STANDARD (index 2)，不被旧备份覆盖
        assertEquals(AlertMode.STANDARD, themeManager.state.value.alertMode)
    }
}
```

#### 2.2 GREEN 阶段：实现修复

**文件 A**：[DripLabApplication.kt:30-58](file:///workspace/DripLab/app/src/main/java/com/driplab/app/DripLabApplication.kt#L30-L58)

**步骤 2.2.1**：将 `if (backupManager.hasBackup())` 改为「首装检测」：
```kotlin
private fun restoreFromBackupIfFirstInstall(themeManager: ThemeManager, backupManager: RecipeBackupManager) {
    val prefs = applicationContext.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
    val isFirstInstall = prefs.all.isEmpty()  // SharedPreferences 完全为空 = 首装
    if (isFirstInstall && backupManager.hasBackup()) {
        CoroutineScope(Dispatchers.IO).launch {
            ...
            themeManager.restoreFromBackup(backupSettings)
            Log.i("DripLab", "Restored settings from backup (first install)")
        }
    } else {
        Log.i("DripLab", "Skipped backup restore (user prefs exist or no backup)")
    }
}
```

**步骤 2.2.2**：在 `ThemeManager` 增加 `start()` 方法封装首装检测（提供注入点便于测试）：
```kotlin
fun start() {
    // 暴露给测试用：确认 prefs 加载完成
    loadState()  // 已有
}
```

**验证测试**：
```bash
gradle :app:testDebugUnitTest --tests "SettingsPersistenceTest"
```
预期：两个测试均通过（GREEN）

---

### 步骤 3：TDD 修复 ② —— 设置变更即时同步备份（RED → GREEN）

#### 3.1 RED 阶段

```kotlin
@Test
fun `changing alertMode should sync to backup file`() {
    themeManager.selectAlertMode(AlertMode.GENTLE)
    // 期望：driplab_backup.json 应包含 alertModeIndex=1
    val backupFile = File(context.filesDir, "driplab_backup.json")
    assertTrue(backupFile.exists())
    val content = backupFile.readText()
    assertTrue(content.contains("\"alertModeIndex\":1"))
}
```

#### 3.2 GREEN 阶段

**文件 A**：[ThemeManager.kt:1-50](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/theme/ThemeManager.kt#L1-L50)

**步骤 3.2.1**：每个 setter 末尾增加 `syncToBackup()` 调用：
```kotlin
fun selectAlertMode(mode: AlertMode) {
    _state.value = _state.value.copy(alertMode = mode)
    prefs.edit().putInt(KEY_ALERT_MODE_INDEX, AlertMode.entries.indexOf(mode)).apply()
    syncToBackup()
}

fun selectTheme(theme: DripTheme) {
    ...
    syncToBackup()
}

fun toggleDarkTheme() {
    ...
    syncToBackup()
}

fun setBgMusicUri(uri: String?) {
    ...
    syncToBackup()
}

fun selectTtsEngine(packageName: String?) {
    ...
    syncToBackup()
}
```

**步骤 3.2.2**：在 `ThemeManager` 内部增加 `syncToBackup()` 方法：
```kotlin
private fun syncToBackup() {
    try {
        val backupFile = File(context.filesDir, "driplab_backup.json")
        if (backupFile.exists()) {
            // 读取已有备份，合并当前 settings，保留 recipes 不动
            val json = backupFile.readText()
            val obj = JSONObject(json)
            val settingsObj = obj.getJSONObject("settings")
            settingsObj.put("themeIndex", state.value.theme.ordinal)
            settingsObj.put("darkTheme", state.value.darkTheme)
            settingsObj.put("alertModeIndex", AlertMode.entries.indexOf(state.value.alertMode))
            settingsObj.put("bgMusicUri", state.value.bgMusicUri ?: "")
            settingsObj.put("ttsEngine", state.value.ttsEnginePackageName ?: JSONObject.NULL)
            backupFile.writeText(obj.toString())
            Log.d("DripLab", "ThemeManager: synced settings to backup file")
        }
    } catch (e: Exception) {
        Log.w("DripLab", "ThemeManager: failed to sync to backup", e)
    }
}
```

**步骤 3.2.3**：依赖注入 `context` 到 `ThemeManager` 构造（当前可能只有 prefs，需要检查）：
- 查阅 [ThemeManager.kt:20-40](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/theme/ThemeManager.kt#L20-L40) 构造函数
- 若仅持有 prefs，需增加 `private val context: Context` 用于 filesDir

**验证测试**：测试 3 通过

---

### 步骤 4：TDD 修复 ③ —— 修正 loadCurrentSettings 默认值（RED → GREEN）

#### 4.1 RED 阶段

```kotlin
@Test
fun `loadCurrentSettings default alertMode should be STANDARD not SILENT`() {
    // 清空 prefs
    context.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
        .edit().clear().commit()
    // 调用 loadCurrentSettings
    val settings = backupManager.getCurrentSettings()  // 暴露公开方法
    assertEquals(2, settings.alertModeIndex)  // 期望 STANDARD = 2
}
```

#### 4.2 GREEN 阶段

**文件**：[RecipeBackupManager.kt:170-200](file:///workspace/DripLab/app/src/main/java/com/driplab/app/core/recipe/RecipeBackupManager.kt#L170-L200)

**步骤 4.2.1**：修改默认值为 2：
```diff
- alertModeIndex = themePrefs.getInt("alert_mode_index", 0),  // BUG: 0=SILENT
+ alertModeIndex = themePrefs.getInt("alert_mode_index", 2),  // FIXED: 2=STANDARD，与 ThemeManager 一致
```

**步骤 4.2.2**：将 `loadCurrentSettings()` 改为 `internal` 或 public，便于测试：
```kotlin
internal fun loadCurrentSettings(): BackupSettings { ... }
```

**验证测试**：测试 4 通过

---

### 步骤 5：测试覆盖 ④ —— 显式导出/导入不受影响

#### 5.1 测试

```kotlin
@Test
fun `explicit export should produce valid backup file with all settings`() {
    themeManager.selectAlertMode(AlertMode.SOLO)
    themeManager.selectTheme(DripTheme.MODERN)
    backupManager.backup(recipes = emptyList(), note = "")
    val backupFile = File(context.filesDir, "driplab_backup.json")
    val content = backupFile.readText()
    // 应包含所有自定义设置
    assertTrue(content.contains("\"alertModeIndex\":3"))  // SOLO
    assertTrue(content.contains("\"themeIndex\":1"))  // MODERN
    assertTrue(content.contains("\"version\":"))
    assertTrue(content.contains("\"recipes\":"))
}

@Test
fun `explicit import should restore all settings`() {
    // 写入完整备份
    val backupFile = File(context.filesDir, "driplab_backup.json")
    backupFile.writeText("""{"version":1,"settings":{"themeIndex":2,"darkTheme":true,"alertModeIndex":1,"bgMusicUri":"content://music/a"},"recipes":[]}""")
    // 触发导入
    backupManager.restore()
    // 验证
    assertEquals(AlertMode.GENTLE, themeManager.state.value.alertMode)
    assertEquals(DripTheme.WARM, themeManager.state.value.theme)
    assertTrue(themeManager.state.value.darkTheme)
}
```

**说明**：这两个测试应当已经通过（现有导出/导入代码基本正确），是「保护性测试」防止本次修改破坏现有功能。

---

### 步骤 6：版本号升级 + README 同步

**文件 A**：[app/build.gradle.kts](file:///workspace/DripLab/app/build.gradle.kts)
```diff
- versionCode = 8
- versionName = "1.0.8"
+ versionCode = 9
+ versionName = "1.0.9"
```

**文件 B + C**：[README.md](file:///workspace/DripLab/README.md) + [assets/README.md](file:///workspace/DripLab/app/src/main/assets/README.md)
- 顶部版本号改为 v1.0.9
- 新增 v1.0.9 章节

**v1.0.9 changelog**：
```markdown
### v1.0.9

- 修复本地自定义设置丢失：启动时仅在 SharedPreferences 完全为空（首装）时从 driplab_backup.json 恢复，避免旧备份覆盖用户当前设置
- 设置变更后即时同步到 driplab_backup.json：selectAlertMode/selectTheme/toggleDarkTheme/setBgMusicUri/selectTtsEngine 每次调用末尾触发 syncToBackup()
- 修正 RecipeBackupManager.loadCurrentSettings() 的 alertModeIndex 默认值从 0 (SILENT) 误用改为 2 (STANDARD)，与 ThemeManager 一致
- 添加 4 个 Robolectric 单元测试覆盖核心持久化场景（首装恢复/有设置时不覆盖/设置同步/显式导出导入）
- 保持 Room v4→v5→v6 迁移兼容
- 保持 SharedPreferences 4 个 key（主题/暗色/提示模式/背景音乐/TTS 引擎）零覆盖
- 保持 v1.0.6 旧 TTS SharedPreferences 一次性迁移逻辑
- 保持 v1.0.7 Logcat 诊断日志（DripLab: TTS_TICK: ...）
- 保持 v1.0.8 BuildConfig.VERSION_NAME + 冲煮页 AlertMode 提示横幅
- 保持显式「备份/恢复」功能完整（Settings → 数据备份/恢复），所有自定义设置（配方/主题/声音/TTS 引擎/提示模式/背景音乐）完整导出导入
```

---

## Assumptions & Decisions

### 决策记录

1. **「首装」定义为 SharedPreferences 完全为空**：用 `prefs.all.isEmpty()` 判定，比 check `versionCode` + 迁移表更简单可靠
2. **保留显式「备份/恢复」入口**：Settings → 数据备份/恢复 仍是用户主动行为，不被新逻辑影响
3. **syncToBackup() 在 setter 末尾同步触发**：每次设置变更都写一次（不是异步批处理），保证备份文件始终反映用户最新状态
4. **syncToBackup() 仅在备份文件已存在时执行**：避免在没有备份文件时凭空创建，让显式导出仍是用户主导
5. **测试用 Robolectric + TemporaryFolder**：SharedPreferences/filesDir 需要 Android 框架，纯 JUnit 跑不动
6. **loadCurrentSettings() 改 internal**：便于测试访问，不破坏现有 external API
7. **ThemeManager 增加 context 字段**：syncToBackup() 需要访问 filesDir；构造器变更需 Hilt DI 配合
8. **不重构 RecipeBackupManager 的 backup 格式**：保持 v1 备份结构，向前兼容
9. **不触碰 TTS 模块**：v1.0.7 约束保留
10. **不修改 AlertMode 枚举定义**：仅修改 loadCurrentSettings 的默认值

### 风险点

- **风险 1**：Robolectric 依赖约 +8MB，APK 体积略增 → 不影响运行时性能，测试用
- **风险 2**：syncToBackup 每次 setter 触发写文件 → 频繁操作可能耗电；用 try/catch + 失败日志降级
- **风险 3**：用户清空 driplab_prefs（开发者选项）后会被误判为「首装」→ 接受此行为，开发者场景不属于普通用户
- **风险 4**：旧 driplab_backup.json 中 alertModeIndex=0 (SILENT) 仍是旧值 → 用户首装时会被恢复为 SILENT（与现状一致）；本次修复重点在「有 prefs 时不被覆盖」

### 与既有功能的兼容性

| 既有功能 | 是否保留 | 说明 |
|---|---|---|
| Settings → 备份/恢复（显式导出导入） | ✅ 保留 | 入口仍可手动触发，导出/导入所有自定义设置 |
| Room v4→v5→v6 迁移 | ✅ 保留 | 配方数据库迁移不受影响 |
| SharedPreferences 4 个 key | ✅ 保留 | 不删除任何 key |
| 旧 TTS SharedPreferences 迁移 | ✅ 保留 | v1.0.6 一次性迁移逻辑 |
| BuildConfig.VERSION_NAME | ✅ 保留 | v1.0.8 引入 |
| 冲煮页 AlertMode 提示横幅 | ✅ 保留 | v1.0.8 引入 |
| TTS 模块 | ✅ 零修改 | v1.0.7 强约束 |
| playTickSound 门控 | ✅ 零修改 | 用户偏好保留 |

---

## Verification

### 1. TDD 测试验证
```bash
cd /workspace/DripLab
gradle :app:testDebugUnitTest --tests "SettingsPersistenceTest"
```
- 预期：4 个测试全部通过
- 重点：第二个测试（"existing user prefs should NOT be overwritten by older backup"）证明 BUG 已修复

### 2. 编译验证
```bash
gradle :app:assembleDebug
```
- 预期：BUILD SUCCESSFUL
- 重点：ThemeManager 构造函数变更需 Hilt 配合；DripLabApplication 修改需 onCreate 入口

### 3. 真机验证（核心场景）
```bash
adb shell am force-stop com.driplab.app
adb logcat -c
adb logcat -s DripLab:D
```

**场景 3.1**：用户当前 settings（修复前是 bug 行为）
1. 启动 app → 切到「标准」提示模式
2. 退出 app（杀进程）
3. 重启 app
4. **应看到「标准」**（修复前会变成「静音」）

**场景 3.2**：首次安装（模拟）
1. `adb shell pm clear com.driplab.app` 清空所有数据
2. 把 v1.0.8 的 driplab_backup.json 拷入 `filesDir`
3. 启动 app
4. **应从备份恢复**（首装场景）

**场景 3.3**：设置变更后同步
1. 启动 app → 切到「轻柔」
2. 退出 app
3. `adb shell run-as com.driplab.app cat files/driplab_backup.json`
4. **应看到 `"alertModeIndex":1`**

### 4. 数据升级验证（v1.0.8 → v1.0.9）
- 升级覆盖安装
- 验证：原有配方/笔记/主题/提示模式/背景音乐/TTS 引擎全部保留
- 验证：v1.0.8 之前的所有修复点全部生效

### 5. Git 推送
```bash
git log --oneline -3
git push origin trae/solo-agent-HUfLDt
```
- 预期：v1.0.9 commit 在 HEAD
- Commit message: `feat: 滴落间Lab v1.0.9 - 设置持久化加固（TDD）`

---

## 强约束（用户明确要求）

- **a. 修复范围控制**：仅修改 5 个生产文件（ThemeManager/RecipeBackupManager/DripLabApplication/SettingsViewModel）+ 1 个测试文件（新建）
- **b. 重大变更确认机制**：已与用户澄清方案（取消无条件恢复/即时同步/修正默认值/TDD）；未涉及功能删除/页面重构/数据库结构调整
- **c. 数据兼容与迁移保障**：不修改 Room schema；不删除 SharedPreferences 任何 key；保留 v1.0.6 旧 TTS prefs 迁移；保留 v1.0.7/v1.0.8 所有已修复功能
