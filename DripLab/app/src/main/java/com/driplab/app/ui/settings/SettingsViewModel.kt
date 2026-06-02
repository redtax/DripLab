package com.driplab.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.backup.ImportSummary
import com.driplab.app.core.backup.SettingsBackupManager
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.theme.AppThemeState
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.core.theme.ThemeManager
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val exportFile: File? = null,
    val importSummary: ImportSummary? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val recipeDao: RecipeDao,
    private val backupManager: SettingsBackupManager
) : ViewModel() {

    val uiState: StateFlow<AppThemeState> = themeManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeState())

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun selectTheme(theme: DripTheme) {
        themeManager.selectTheme(theme)
    }

    fun toggleDarkTheme() {
        themeManager.toggleDarkTheme()
    }

    fun selectAlertMode(mode: AlertMode) {
        themeManager.selectAlertMode(mode)
    }

    fun setBgMusicUri(uri: String) {
        themeManager.setBgMusicUri(uri)
    }

    fun exportBackup() {
        viewModelScope.launch {
            _backupState.value = BackupUiState(isLoading = true)
            try {
                val file = backupManager.exportData(recipeDao)
                _backupState.value = BackupUiState(exportFile = file)
            } catch (e: Exception) {
                _backupState.value = BackupUiState(errorMessage = "导出失败: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState(isLoading = true)
            try {
                val result = backupManager.importData(uri, recipeDao, themeManager)
                result.fold(
                    onSuccess = { summary ->
                        _backupState.value = BackupUiState(importSummary = summary)
                    },
                    onFailure = { e ->
                        _backupState.value = BackupUiState(errorMessage = "导入失败: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _backupState.value = BackupUiState(errorMessage = "导入失败: ${e.message}")
            }
        }
    }

    fun clearBackupResult() {
        _backupState.value = BackupUiState()
    }
}