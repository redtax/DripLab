package com.driplab.app.ui.pourover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.calculator.BrewCalculator
import com.driplab.app.core.calculator.BrewCalculator.RatioPreset
import com.driplab.app.core.timer.BrewState
import com.driplab.app.core.timer.BrewTimer
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PourOverUiState(
    val coffeeWeight: Float = 15f,
    val selectedRatioPreset: Int = 1,
    val ratioPresets: List<RatioPreset> = BrewCalculator.ratioPresets,
    val customRatio: Float = 16f,
    val waterAmount: Float = 240f,
    val ratioLabel: String = "1:16",
    val suggestedTemp: Int = 90,
    val temperature: Int = 90,
    val selectedRecipeId: Long = -1L,
    val presetRecipes: List<Recipe> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val brewState: BrewState = BrewState(),
    val isBrewing: Boolean = false,
    val showPresetSelector: Boolean = false,
)

@HiltViewModel
class PourOverViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(PourOverUiState())
    val uiState: StateFlow<PourOverUiState> = _uiState.asStateFlow()

    val brewTimer = BrewTimer()

    init {
        loadPresetRecipes()
    }

    private fun loadPresetRecipes() {
        viewModelScope.launch {
            val presets = recipeRepository.getDefaultRecipes(BrewMethod.POUR_OVER)
            _uiState.value = _uiState.value.copy(presetRecipes = presets)
        }
    }

    fun updateCoffeeWeight(weight: Float) {
        val state = _uiState.value
        val ratio = getCurrentRatio(state)
        val calculation = BrewCalculator.calculate(weight, ratio)
        _uiState.value = state.copy(
            coffeeWeight = weight,
            waterAmount = calculation.waterAmount,
            ratioLabel = calculation.ratioLabel,
            suggestedTemp = calculation.suggestedTemp
        )
    }

    fun selectRatioPreset(index: Int) {
        val state = _uiState.value
        if (index == state.ratioPresets.size - 1) {
            _uiState.value = state.copy(selectedRatioPreset = index)
            return
        }
        val ratio = state.ratioPresets[index].ratio
        val calculation = BrewCalculator.calculate(state.coffeeWeight, ratio)
        _uiState.value = state.copy(
            selectedRatioPreset = index,
            waterAmount = calculation.waterAmount,
            ratioLabel = calculation.ratioLabel,
            suggestedTemp = calculation.suggestedTemp
        )
    }

    fun updateCustomRatio(ratio: Float) {
        val state = _uiState.value
        val calculation = BrewCalculator.calculate(state.coffeeWeight, ratio)
        _uiState.value = state.copy(
            customRatio = ratio,
            waterAmount = calculation.waterAmount,
            ratioLabel = "1:${ratio.toInt()}",
            suggestedTemp = calculation.suggestedTemp
        )
    }

    fun updateTemperature(temp: Int) {
        _uiState.value = _uiState.value.copy(temperature = temp)
    }

    fun selectPresetRecipe(index: Int) {
        val state = _uiState.value
        if (index < 0 || index >= state.presetRecipes.size) return
        val recipe = state.presetRecipes[index]
        _uiState.value = state.copy(
            selectedPresetIndex = index,
            selectedRecipeId = recipe.id,
            temperature = recipe.temperature,
            coffeeWeight = recipe.coffeeWeight,
            showPresetSelector = false
        )
        updateCoffeeWeight(recipe.coffeeWeight)
    }

    fun togglePresetSelector() {
        _uiState.value = _uiState.value.copy(
            showPresetSelector = !_uiState.value.showPresetSelector
        )
    }

    fun startBrewing() {
        val state = _uiState.value
        val recipe = when {
            state.selectedRecipeId > 0 -> state.presetRecipes.firstOrNull { it.id == state.selectedRecipeId }
            else -> null
        }

        val brewRecipe = recipe ?: createDefaultRecipe(state)

        brewTimer.loadRecipe(brewRecipe)
        brewTimer.start()

        viewModelScope.launch {
            brewTimer.brewState.collect { brewState ->
                _uiState.value = _uiState.value.copy(
                    brewState = brewState,
                    isBrewing = brewState.isRunning && !brewState.isComplete
                )
            }
        }
    }

    fun pauseBrewing() {
        brewTimer.pause()
    }

    fun resumeBrewing() {
        brewTimer.resume()
    }

    fun stopBrewing() {
        brewTimer.stop()
        _uiState.value = _uiState.value.copy(isBrewing = false)
    }

    fun skipStep() {
        brewTimer.skipToNextStep()
    }

    private fun getCurrentRatio(state: PourOverUiState): Float {
        return if (state.selectedRatioPreset == state.ratioPresets.size - 1) {
            state.customRatio
        } else {
            state.ratioPresets[state.selectedRatioPreset].ratio
        }
    }

    private fun createDefaultRecipe(state: PourOverUiState): Recipe {
        val totalWater = state.waterAmount
        return Recipe(
            name = "自定义手冲",
            method = BrewMethod.POUR_OVER,
            coffeeWeight = state.coffeeWeight,
            waterRatio = state.ratioLabel,
            temperature = state.temperature,
            steps = listOf(
                RecipeStep(sequence = 1, phase = BrewPhase.BLOOM, duration = 30, targetWater = (totalWater * 0.15f).toInt()),
                RecipeStep(sequence = 2, phase = BrewPhase.POUR, duration = 45, targetWater = (totalWater * 0.35f).toInt()),
                RecipeStep(sequence = 3, phase = BrewPhase.POUR, duration = 45, targetWater = (totalWater * 0.35f).toInt()),
                RecipeStep(sequence = 4, phase = BrewPhase.WAIT, duration = 30, targetWater = (totalWater * 0.15f).toInt())
            )
        )
    }
}