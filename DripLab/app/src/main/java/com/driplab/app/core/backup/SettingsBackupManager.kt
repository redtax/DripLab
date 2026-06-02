package com.driplab.app.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.recipe.BackupData
import com.driplab.app.core.recipe.RecipeBackupManager
import com.driplab.app.core.theme.ThemeManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsBackupManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recipeBackupManager: RecipeBackupManager
) {
    companion object {
        private const val TAG = "DripLab"
        private const val EXPORT_FILE_PREFIX = "driplab_backup_"
        private const val EXPORT_FILE_SUFFIX = ".json"
    }

    private val gson = Gson()

    suspend fun exportData(recipeDao: RecipeDao): File = withContext(Dispatchers.IO) {
        recipeBackupManager.backup(recipeDao)
        val sourceFile = File(appContext.filesDir, "driplab_backup.json")
        val exportFile = File(appContext.cacheDir, "${EXPORT_FILE_PREFIX}${System.currentTimeMillis()}$EXPORT_FILE_SUFFIX")
        sourceFile.copyTo(exportFile, overwrite = true)
        Log.i(TAG, "Export file created: ${exportFile.absolutePath}")
        exportFile
    }

    suspend fun importData(
        uri: Uri,
        recipeDao: RecipeDao,
        themeManager: ThemeManager
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val json = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(Exception("无法读取文件"))

            val data = gson.fromJson(json, BackupData::class.java)

            val backupFile = File(appContext.filesDir, "driplab_backup.json")
            backupFile.writeText(json)

            themeManager.restoreFromBackup(data.settings)

            val existingRecipes = recipeDao.getAllRecipesWithStepsOnce()
            val hasUserRecipes = existingRecipes.any { !it.recipe.isDefault }
            val recipeRestored = if (!hasUserRecipes) {
                recipeBackupManager.restoreTo(recipeDao)
                true
            } else {
                false
            }

            val summary = ImportSummary(
                settingsRestored = true,
                recipeCount = data.recipes.size,
                recipesRestored = recipeRestored
            )
            Log.i(TAG, "Import completed: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}")
            Result.failure(e)
        }
    }
}

data class ImportSummary(
    val settingsRestored: Boolean,
    val recipeCount: Int,
    val recipesRestored: Boolean
)