package com.driplab.app.core.recipe

import android.content.Context
import android.util.Log
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.database.dao.RecipeWithSteps
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int = 1,
    val appVersion: String = "1.0.3",
    val recipes: List<BackupRecipe> = emptyList(),
    val settings: BackupSettings = BackupSettings()
)

data class BackupRecipe(
    val name: String,
    val method: String,
    val coffeeWeight: Float,
    val waterRatio: String,
    val temperature: Int,
    val steps: List<BackupRecipeStep>
)

data class BackupRecipeStep(
    val sequence: Int,
    val phase: String,
    val duration: Int,
    val targetWater: Int,
    val waterRatio: Float,
    val durationRatio: Float = 0f,
    val instruction: String
)

data class BackupSettings(
    val themeIndex: Int = 0,
    val isDarkTheme: Boolean = false,
    val alertModeIndex: Int = 0,
    val bgMusicUri: String? = null,
    val selectedTtsEngine: String? = null
)

@Singleton
class RecipeBackupManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "DripLab"
        private const val BACKUP_FILE = "driplab_backup.json"
    }

    private val gson = Gson()
    private val backupFile: File
        get() = File(appContext.filesDir, BACKUP_FILE)

    suspend fun backup(recipeDao: RecipeDao) = withContext(Dispatchers.IO) {
        try {
            val allRecipes = getAllNonDefaultRecipes(recipeDao)
            val settings = loadCurrentSettings()
            val data = BackupData(
                recipes = allRecipes.map { (recipe, steps) ->
                    BackupRecipe(
                        name = recipe.name,
                        method = recipe.method,
                        coffeeWeight = recipe.coffeeWeight,
                        waterRatio = recipe.waterRatio,
                        temperature = recipe.temperature,
                        steps = steps.map { step ->
                            BackupRecipeStep(
                                sequence = step.sequence,
                                phase = step.phase,
                                duration = step.duration,
                                targetWater = step.targetWater,
                                waterRatio = step.waterRatio,
                                durationRatio = step.durationRatio,
                                instruction = step.instruction
                            )
                        }
                    )
                },
                settings = settings
            )
            val json = gson.toJson(data)
            backupFile.writeText(json)
            Log.i(TAG, "Backup saved: ${backupFile.absolutePath} (${json.length} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}")
        }
        Unit
    }

    suspend fun restoreTo(recipeDao: RecipeDao) = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                Log.i(TAG, "No backup file found")
                return@withContext
            }
            val json = backupFile.readText()
            val data = gson.fromJson(json, BackupData::class.java)
            if (data.recipes.isEmpty()) {
                Log.i(TAG, "Backup has no recipes to restore")
                return@withContext
            }

            data.recipes.forEach { backupRecipe ->
                val recipeId = daoInsertRecipe(recipeDao, backupRecipe)
                if (recipeId > 0) {
                    val steps = backupRecipe.steps.map { step ->
                        RecipeStepEntity(
                            recipeId = recipeId,
                            sequence = step.sequence,
                            phase = step.phase,
                            duration = step.duration,
                            targetWater = step.targetWater,
                            waterRatio = step.waterRatio,
                            durationRatio = step.durationRatio,
                            instruction = step.instruction
                        )
                    }
                    recipeDao.insertSteps(steps)
                }
            }
            Log.i(TAG, "Restored ${data.recipes.size} recipes from backup")
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}")
        }
        Unit
    }

    fun getBackupSettings(): BackupSettings? {
        return try {
            if (!backupFile.exists()) return null
            val json = backupFile.readText()
            val data = gson.fromJson(json, BackupData::class.java)
            data.settings
        } catch (e: Exception) {
            null
        }
    }

    fun hasBackup(): Boolean = backupFile.exists()

    private suspend fun getAllNonDefaultRecipes(dao: RecipeDao): List<Pair<RecipeEntity, List<RecipeStepEntity>>> {
        val all = dao.getAllRecipesWithStepsOnce()
        return all.filter { !it.recipe.isDefault }.map { it.recipe to it.steps }
    }

    private suspend fun daoInsertRecipe(dao: RecipeDao, backup: BackupRecipe): Long {
        return dao.insertRecipe(
            RecipeEntity(
                name = backup.name,
                method = backup.method,
                coffeeWeight = backup.coffeeWeight,
                waterRatio = backup.waterRatio,
                temperature = backup.temperature,
                isDefault = false
            )
        )
    }

    private fun loadCurrentSettings(): BackupSettings {
        val themePrefs = appContext.getSharedPreferences("driplab_prefs", Context.MODE_PRIVATE)
        return BackupSettings(
            themeIndex = themePrefs.getInt("theme_index", 0),
            isDarkTheme = themePrefs.getBoolean("dark_theme", false),
            // v1.0.9 修复：默认值从 0 (SILENT) 改为 2 (STANDARD)，与 ThemeManager.loadState() 保持一致
            // 之前缺省值是 0 会导致用户在全新首次导出时把 SILENT 写进 driplab_backup.json，
            // 下次首装恢复时即便 SharedPreferences 是空的也会被回写为 SILENT
            alertModeIndex = themePrefs.getInt("alert_mode_index", 2),
            bgMusicUri = themePrefs.getString("bg_music_uri", null),
            selectedTtsEngine = themePrefs.getString("tts_engine", null)
        )
    }

    /**
     * v1.0.9 新增：暴露 loadCurrentSettings 供单元测试使用（internal 公开给同模块的测试可见）
     */
    internal fun loadCurrentSettingsForTest(): BackupSettings = loadCurrentSettings()
}