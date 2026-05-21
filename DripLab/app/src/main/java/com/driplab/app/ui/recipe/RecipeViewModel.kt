package com.driplab.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val importResult: String? = null
)

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl
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

    fun getFilteredRecipes(): List<Recipe> {
        val state = _uiState.value
        return if (state.selectedMethod != null) {
            state.recipes.filter { it.method == state.selectedMethod }
        } else {
            state.recipes
        }
    }
}