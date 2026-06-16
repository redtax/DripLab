package com.driplab.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.recipe.RecipeBackupManager
import com.driplab.app.core.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DripLabApplication : Application() {

    @Inject
    lateinit var backupManager: RecipeBackupManager

    @Inject
    lateinit var recipeDao: RecipeDao

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate() {
        super.onCreate()
        if (backupManager.hasBackup()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allRecipes = recipeDao.getAllRecipesWithStepsOnce()
                    val hasUserRecipes = allRecipes.any { !it.recipe.isDefault }
                    if (!hasUserRecipes) {
                        backupManager.restoreTo(recipeDao)
                        Log.i("DripLab", "Restored user recipes from backup")
                    } else {
                        Log.i("DripLab", "Skipping restore: user recipes already exist")
                    }
                    // v1.0.9 修复：仅当 SharedPreferences 为空（首装）时从 driplab_backup.json 恢复 settings
                    // 避免旧备份覆盖用户当前设置，导致每次重启提示模式被重置为 SILENT
                    val prefs = applicationContext.getSharedPreferences(
                        "driplab_prefs", Context.MODE_PRIVATE
                    )
                    val isFirstInstall = prefs.all.isEmpty()
                    if (isFirstInstall) {
                        val backupSettings = backupManager.getBackupSettings()
                        if (backupSettings != null) {
                            themeManager.restoreFromBackup(backupSettings)
                            Log.i("DripLab", "Restored settings from backup (first install)")
                        }
                    } else {
                        Log.i("DripLab", "Skipped settings restore: user prefs already exist")
                    }
                } catch (e: Exception) {
                    Log.e("DripLab", "Restore check failed: ${e.message}")
                }
            }
        }
    }
}