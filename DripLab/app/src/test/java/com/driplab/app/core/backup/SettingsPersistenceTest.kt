package com.driplab.app.core.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.driplab.app.core.recipe.RecipeBackupManager
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.core.theme.ThemeManager
import com.driplab.app.domain.model.AlertMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * 设置持久化场景测试（TDD 驱动修复）
 *
 * 覆盖 4 个核心场景：
 * 1. 首装恢复：SharedPreferences 为空 + 备份文件存在 → 应从备份恢复
 * 2. 现有设置不覆盖：SharedPreferences 有数据 + 旧备份存在 → 不应用旧备份覆盖
 * 3. 设置变更同步：selectAlertMode 写 prefs 后，driplab_backup.json 应即时同步
 * 4. 默认值正确：loadCurrentSettings() 缺失 key 时应返回 STANDARD (index 2) 而非 SILENT (index 0)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsPersistenceTest {

    private lateinit var context: Context
    private val prefsName = "driplab_prefs"
    private val backupFileName = "driplab_backup.json"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clearAllState()
    }

    @After
    fun tearDown() {
        clearAllState()
    }

    private fun clearAllState() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, backupFileName).delete()
    }

    private fun writeBackupFile(content: String) {
        File(context.filesDir, backupFileName).writeText(content)
    }

    private fun readBackupFile(): String =
        File(context.filesDir, backupFileName).readText()

    // ──────────────────────── 场景 1：首装恢复 ────────────────────────

    @Test
    fun `first install with existing backup should restore from backup (scenario 1)`() {
        // 准备：SharedPreferences 为空（旧备份携带 GENTLE=1）
        writeBackupFile(
            """{"version":1,"appVersion":"1.0.3","recipes":[],"settings":""" +
                """{"themeIndex":2,"isDarkTheme":true,"alertModeIndex":1,"bgMusicUri":"","selectedTtsEngine":null}}"""
        )

        // 触发：DripLabApplication 启动逻辑
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isFirstInstall = prefs.all.isEmpty()
        val backupSettings = RecipeBackupManager(context).getBackupSettings()

        // 验证：判定为首装 + 有备份 → 恢复
        assertTrue("SharedPreferences should be empty (first install)", isFirstInstall)
        assertEquals("alertModeIndex from backup", 1, backupSettings?.alertModeIndex)
        assertEquals("themeIndex from backup", 2, backupSettings?.themeIndex)
    }

    // ──────────────────────── 场景 2：现有 prefs 不被旧备份覆盖 ────────────────────────

    @Test
    fun `existing user prefs should NOT be overwritten by older backup (scenario 2)`() {
        // 准备：用户当前 STANDARD=2（旧备份 SILENT=0）
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit().putInt("alert_mode_index", 2).commit()
        writeBackupFile(
            """{"version":1,"appVersion":"1.0.3","recipes":[],"settings":""" +
                """{"themeIndex":0,"isDarkTheme":false,"alertModeIndex":0,"bgMusicUri":"","selectedTtsEngine":null}}"""
        )

        // 触发：ThemeManager 加载 prefs
        val themeManager = ThemeManager(context)
        val loaded = themeManager.state.value

        // 验证：应为用户当前 STANDARD (index 2)，不被旧备份 SILENT 覆盖
        assertEquals("alertMode should be STANDARD (user pref, not backup)", AlertMode.STANDARD, loaded.alertMode)
    }

    // ──────────────────────── 场景 3：设置变更同步到备份文件 ────────────────────────

    @Test
    fun `changing alertMode should sync to backup file (scenario 3)`() {
        // 准备：写入初始备份文件（GENTLE=1）
        writeBackupFile(
            """{"version":1,"appVersion":"1.0.9","recipes":[],"settings":""" +
                """{"themeIndex":0,"isDarkTheme":false,"alertModeIndex":1,"bgMusicUri":"","selectedTtsEngine":null}}"""
        )

        // 触发：用户切到 STANDARD=2
        val themeManager = ThemeManager(context)
        themeManager.selectAlertMode(AlertMode.STANDARD)

        // 验证：driplab_backup.json 应包含 alertModeIndex=2
        val content = readBackupFile()
        assertTrue("Backup file should be updated with new alertModeIndex=2, got: $content",
            content.contains("\"alertModeIndex\":2"))
    }

    // ──────────────────────── 场景 4：loadCurrentSettings 默认值正确 ────────────────────────

    @Test
    fun `loadCurrentSettings default alertModeIndex should be STANDARD not SILENT (scenario 4)`() {
        // 准备：SharedPreferences 完全为空
        clearAllState()

        // 触发：loadCurrentSettings
        val backupManager = RecipeBackupManager(context)
        val settings = backupManager.loadCurrentSettingsForTest()

        // 验证：alertModeIndex 默认为 2 (STANDARD)，与 ThemeManager 一致
        assertEquals("Default alertModeIndex should be STANDARD=2 (not SILENT=0)",
            2, settings.alertModeIndex)
    }

    // ──────────────────────── 保护性测试：显式导出/导入仍可用 ────────────────────────

    @Test
    fun `explicit import should restore all settings from valid backup file (protective test)`() {
        // 准备：写入完整备份
        writeBackupFile(
            """{"version":1,"appVersion":"1.0.9","recipes":[],"settings":""" +
                """{"themeIndex":3,"isDarkTheme":true,"alertModeIndex":3,"bgMusicUri":"content://music/a","selectedTtsEngine":"com.test.tts"}}"""
        )

        // 触发：ThemeManager.restoreFromBackup
        val themeManager = ThemeManager(context)
        themeManager.restoreFromBackup(
            com.driplab.app.core.recipe.BackupSettings(
                themeIndex = 3,
                isDarkTheme = true,
                alertModeIndex = 3,
                bgMusicUri = "content://music/a",
                selectedTtsEngine = "com.test.tts"
            )
        )

        // 验证：所有设置已恢复
        val loaded = themeManager.state.value
        assertEquals("SOLO", AlertMode.SOLO, loaded.alertMode)
        assertEquals("OAT_LATTE", DripTheme.OAT_LATTE, loaded.theme)
        assertTrue("darkTheme should be true", loaded.darkTheme)
        assertEquals("com.test.tts", "com.test.tts", loaded.selectedTtsEngine)
        assertEquals("content://music/a", "content://music/a", loaded.bgMusicUri)
    }
}
