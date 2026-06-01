package com.driplab.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.recipe.ImportResult
import com.driplab.app.core.recipe.RecipeFormatManager
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import com.driplab.app.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeEditState(
    val recipe: Recipe = Recipe(method = BrewMethod.POUR_OVER),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val exportText: String? = null,
    val importResult: ImportResult? = null
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val formatManager: RecipeFormatManager
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeEditState())
    val state: StateFlow<RecipeEditState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecipeEditEvent>()
    val events = _events.asSharedFlow()

    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val recipe = recipeRepository.getRecipeById(recipeId)
            if (recipe != null) {
                _state.value = _state.value.copy(
                    recipe = recipe,
                    isEditing = true,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun loadImportedRecipe(json: String) {
        try {
            val recipe = com.google.gson.Gson().fromJson(json, Recipe::class.java)
            _state.value = _state.value.copy(
                recipe = recipe.copy(id = 0, isDefault = false),
                isEditing = false
            )
        } catch (_: Exception) {}
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(name = name)
        )
    }

    fun updateMethod(method: BrewMethod) {
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(method = method)
        )
    }

    fun updateCoffeeWeight(weight: Float) {
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(coffeeWeight = weight)
        )
    }

    fun updateWaterRatio(ratio: String) {
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(waterRatio = ratio)
        )
    }

    fun updateTemperature(temp: Int) {
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(temperature = temp)
        )
    }

    fun updateStep(index: Int, step: RecipeStep) {
        val steps = _state.value.recipe.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = step
            _state.value = _state.value.copy(
                recipe = _state.value.recipe.copy(steps = steps)
            )
        }
    }

    fun addStep() {
        val steps = _state.value.recipe.steps.toMutableList()
        steps.add(
            RecipeStep(
                sequence = steps.size + 1,
                phase = BrewPhase.POUR,
                duration = 30,
                targetWater = 50,
                instruction = ""
            )
        )
        _state.value = _state.value.copy(
            recipe = _state.value.recipe.copy(steps = steps)
        )
    }

    fun removeStep(index: Int) {
        val steps = _state.value.recipe.steps.toMutableList()
        if (index in steps.indices) {
            steps.removeAt(index)
            steps.forEachIndexed { i, step ->
                steps[i] = step.copy(sequence = i + 1)
            }
            _state.value = _state.value.copy(
                recipe = _state.value.recipe.copy(steps = steps)
            )
        }
    }

    fun saveRecipe() {
        viewModelScope.launch {
            val current = _state.value.recipe
            val ratioNum = current.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
            val totalWater = current.coffeeWeight * ratioNum
            val stepsWithRatio = current.steps.map { step ->
                val ratio = if (totalWater > 0f) step.targetWater.toFloat() / totalWater * 100f else 0f
                step.copy(waterRatio = ratio)
            }
            val recipe = current.copy(steps = stepsWithRatio)
            recipeRepository.saveRecipe(recipe)
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    fun exportRecipe() {
        viewModelScope.launch {
            val current = _state.value.recipe
            val ratioNum = current.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
            val totalWater = current.coffeeWeight * ratioNum
            val stepsWithRatio = current.steps.map { step ->
                val ratio = if (totalWater > 0f) step.targetWater.toFloat() / totalWater * 100f else 0f
                step.copy(waterRatio = ratio)
            }
            val text = formatManager.exportRecipe(
                current.copy(steps = stepsWithRatio),
                stepsWithRatio
            )
            _state.value = _state.value.copy(exportText = text)
        }
    }

    fun clearExportText() {
        _state.value = _state.value.copy(exportText = null)
    }

    fun importRecipeText(text: String) {
        val result = formatManager.parseRecipe(text)
        if (result.success && result.recipe != null) {
            val steps = result.steps.map { step ->
                RecipeStep(
                    sequence = step.sequence,
                    phase = step.phase,
                    duration = step.duration,
                    targetWater = step.targetWater,
                    waterRatio = step.waterRatio,
                    instruction = step.instruction
                )
            }
            _state.value = _state.value.copy(
                recipe = result.recipe.copy(steps = steps, isDefault = false),
                importResult = result
            )
        } else {
            _state.value = _state.value.copy(importResult = result)
        }
    }

    fun clearImportResult() {
        _state.value = _state.value.copy(importResult = null)
    }
}

sealed class RecipeEditEvent {
    data object NavigateBack : RecipeEditEvent()
}