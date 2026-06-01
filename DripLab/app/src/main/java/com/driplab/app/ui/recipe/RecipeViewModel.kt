package com.driplab.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val exportText: String? = null,
    val navigationTarget: RecipeNavigation? = null
)

sealed class RecipeNavigation {
    data class EditRecipe(val recipeId: Long) : RecipeNavigation()
    data class EditImported(val recipe: Recipe, val steps: List<com.driplab.app.core.recipe.ParsedRecipeStep>) : RecipeNavigation()
}

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeListState())
    val state: StateFlow<RecipeListState> = _state.asStateFlow()

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val recipes = recipeRepository.getAllRecipes()
            _state.value = _state.value.copy(recipes = recipes, isLoading = false)
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipe.id)
            loadRecipes()
        }
    }

    fun exportRecipe(recipe: Recipe) {
        viewModelScope.launch {
            val text = recipeRepository.exportRecipe(recipe.id)
            _state.value = _state.value.copy(exportText = text)
        }
    }

    fun clearExportText() {
        _state.value = _state.value.copy(exportText = null)
    }

    fun editRecipe(recipe: Recipe) {
        _state.value = _state.value.copy(
            navigationTarget = RecipeNavigation.EditRecipe(recipe.id)
        )
    }

    fun importRecipe(text: String) {
        viewModelScope.launch {
            val result = recipeRepository.importRecipe(text)
            if (result.success && result.recipe != null) {
                val imported = result.recipe.copy(steps = result.steps.map { step ->
                    com.driplab.app.domain.model.RecipeStep(
                        sequence = step.sequence,
                        phase = step.phase,
                        duration = step.duration,
                        targetWater = step.targetWater,
                        waterRatio = step.waterRatio,
                        durationRatio = step.durationRatio,
                        instruction = step.instruction
                    )
                })
                _state.value = _state.value.copy(
                    navigationTarget = RecipeNavigation.EditImported(imported, result.steps)
                )
            }
        }
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(navigationTarget = null)
    }
}