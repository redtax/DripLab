package com.driplab.app.ui.recipe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.data.BrewSessionManager
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiState(
    val recipes: List<Recipe> = emptyList(),
    val selectedMethod: BrewMethod? = null,
    val isLoading: Boolean = true,
    val importText: String = "",
    val showImportDialog: Boolean = false,
    val importResult: String? = null,
    val showConfirmDialog: Boolean = false,
    val pendingRecipe: Recipe? = null
)

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl,
    private val brewSessionManager: BrewSessionManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    init {
        loadRecipes()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            recipeRepository.getAllRecipesFlow().collect { recipes ->
                _uiState.value = _uiState.value.copy(
                    recipes = recipes,
                    isLoading = false
                )
            }
        }
    }

    fun filterByMethod(method: BrewMethod?) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun selectRecipeForBrewing(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = true,
            pendingRecipe = recipe
        )
    }

    fun confirmRecipeSelection() {
        val recipe = _uiState.value.pendingRecipe ?: return
        brewSessionManager.setActiveRecipe(recipe)
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false,
            pendingRecipe = null
        )
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false,
            pendingRecipe = null
        )
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipe.id)
        }
    }

    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true, importText = "", importResult = null)
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false, importResult = null)
    }

    fun updateImportText(text: String) {
        _uiState.value = _uiState.value.copy(importText = text)
    }

    fun importRecipe() {
        viewModelScope.launch {
            val success = recipeRepository.importRecipe(_uiState.value.importText)
            _uiState.value = _uiState.value.copy(
                importResult = if (success) "导入成功" else "导入失败，请检查JSON格式"
            )
            if (success) {
                _uiState.value = _uiState.value.copy(showImportDialog = false)
            }
        }
    }

    fun exportRecipe(recipe: Recipe): String {
        var result = ""
        viewModelScope.launch {
            result = recipeRepository.exportRecipe(recipe.id)
        }
        return result
    }

    fun exportToClipboard(recipe: Recipe) {
        viewModelScope.launch {
            val json = recipeRepository.exportRecipe(recipe.id)
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("recipe", json))
            Toast.makeText(appContext, "配方JSON已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFilteredRecipes(): List<Recipe> {
        val state = _uiState.value
        return if (state.selectedMethod != null) {
            state.recipes.filter { it.method == state.selectedMethod }
        } else {
            state.recipes
        }
    }
}