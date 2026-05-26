package com.driplab.app.ui.recipe

import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import com.driplab.app.core.calculator.BrewCalculator
import com.driplab.app.core.calculator.BrewCalculator.RatioPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class RecipeEditUiState(
    val recipeId: Long = 0,
    val name: String = "",
    val coffeeWeight: Float = 15f,
    val waterRatio: Int = 15,
    val ratioLabel: String = "1:15",
    val temperature: Int = 92,
    val steps: List<EditableStep> = listOf(
        EditableStep(phase = BrewPhase.BLOOM, duration = 30, targetWater = 45, instruction = ""),
        EditableStep(phase = BrewPhase.POUR, duration = 35, targetWater = 70, instruction = ""),
        EditableStep(phase = BrewPhase.WAIT, duration = 45, targetWater = 0, instruction = "")
    ),
    val ratioPresets: List<RatioPreset> = BrewCalculator.ratioPresets,
    val selectedRatioPreset: Int = 1,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isLoading: Boolean = false,
    val isNew: Boolean = true
)

data class EditableStep(
    val phase: BrewPhase = BrewPhase.POUR,
    val duration: Int = 30,
    val targetWater: Int = 0,
    val instruction: String = ""
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeEditUiState())
    val uiState: StateFlow<RecipeEditUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null

    fun loadRecipe(recipeId: Long) {
        if (recipeId <= 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val recipe = recipeRepository.getRecipeById(recipeId)
            if (recipe != null && recipe.method == BrewMethod.POUR_OVER) {
                val ratioFloat = recipe.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
                val idx = _uiState.value.ratioPresets.indexOfFirst { it.ratio == ratioFloat }
                _uiState.value = _uiState.value.copy(
                    recipeId = recipe.id,
                    name = recipe.name,
                    coffeeWeight = recipe.coffeeWeight,
                    waterRatio = ratioFloat.toInt(),
                    ratioLabel = recipe.waterRatio,
                    temperature = recipe.temperature,
                    selectedRatioPreset = if (idx >= 0) idx else _uiState.value.ratioPresets.size - 1,
                    steps = recipe.steps.map { step ->
                        EditableStep(
                            phase = step.phase,
                            duration = step.duration,
                            targetWater = step.targetWater,
                            instruction = step.instruction
                        )
                    },
                    isLoading = false,
                    isNew = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun initTts(tts: TextToSpeech) {
        this.tts = tts
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateCoffeeWeight(weight: Float) {
        val state = _uiState.value
        val ratio = state.waterRatio.toFloat()
        val calculation = BrewCalculator.calculate(weight, ratio)
        val originalWeight = state.coffeeWeight
        val scale = if (originalWeight > 0f) weight / originalWeight else 1f

        val newSteps = state.steps.map { step ->
            step.copy(targetWater = (step.targetWater * scale).toInt())
        }

        _uiState.value = state.copy(
            coffeeWeight = weight,
            ratioLabel = calculation.ratioLabel,
            steps = newSteps
        )
    }

    fun adjustCoffeeUp() {
        val newWeight = (_uiState.value.coffeeWeight + 0.5f).coerceIn(5f, 150f)
        updateCoffeeWeight((newWeight * 10).toInt() / 10f)
    }

    fun adjustCoffeeDown() {
        val newWeight = (_uiState.value.coffeeWeight - 0.5f).coerceIn(5f, 150f)
        updateCoffeeWeight((newWeight * 10).toInt() / 10f)
    }

    fun selectRatioPreset(index: Int) {
        val state = _uiState.value
        val ratio = state.ratioPresets[index].ratio
        val ratioInt = if (index == state.ratioPresets.size - 1) state.waterRatio else ratio.toInt()
        _uiState.value = state.copy(
            selectedRatioPreset = index,
            waterRatio = ratioInt,
            ratioLabel = "1:${ratioInt}"
        )
    }

    fun updateCustomRatio(ratio: Int) {
        _uiState.value = _uiState.value.copy(
            waterRatio = ratio,
            ratioLabel = "1:${ratio}"
        )
    }

    fun updateTemperature(temp: Int) {
        _uiState.value = _uiState.value.copy(temperature = temp)
    }

    fun adjustTempUp() {
        val newTemp = (_uiState.value.temperature + 1).coerceIn(60, 100)
        _uiState.value = _uiState.value.copy(temperature = newTemp)
    }

    fun adjustTempDown() {
        val newTemp = (_uiState.value.temperature - 1).coerceIn(60, 100)
        _uiState.value = _uiState.value.copy(temperature = newTemp)
    }

    fun updateStep(index: Int, step: EditableStep) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = step
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun updateStepPhase(index: Int, phase: BrewPhase) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = steps[index].copy(phase = phase)
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun updateStepDuration(index: Int, duration: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = steps[index].copy(duration = duration)
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun updateStepWater(index: Int, water: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = steps[index].copy(targetWater = water)
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun updateStepInstruction(index: Int, instruction: String) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) {
            steps[index] = steps[index].copy(instruction = instruction)
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun addStep() {
        val steps = _uiState.value.steps.toMutableList()
        steps.add(EditableStep(phase = BrewPhase.POUR, duration = 30, targetWater = 50, instruction = ""))
        _uiState.value = _uiState.value.copy(steps = steps)
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (steps.size > 1 && index in steps.indices) {
            steps.removeAt(index)
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun moveStepUp(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (index > 0) {
            val temp = steps[index]
            steps[index] = steps[index - 1]
            steps[index - 1] = temp
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun moveStepDown(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (index < steps.size - 1) {
            val temp = steps[index]
            steps[index] = steps[index + 1]
            steps[index + 1] = temp
            _uiState.value = _uiState.value.copy(steps = steps)
        }
    }

    fun previewInstruction(index: Int) {
        val steps = _uiState.value.steps
        if (index in steps.indices && steps[index].instruction.isNotBlank()) {
            tts?.speak(steps[index].instruction, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun saveRecipe() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val recipe = Recipe(
                id = state.recipeId,
                name = state.name,
                method = BrewMethod.POUR_OVER,
                coffeeWeight = state.coffeeWeight,
                waterRatio = state.ratioLabel,
                temperature = state.temperature,
                steps = state.steps.mapIndexed { index, step ->
                    RecipeStep(
                        sequence = index + 1,
                        phase = step.phase,
                        duration = step.duration,
                        targetWater = step.targetWater,
                        instruction = step.instruction
                    )
                }
            )
            recipeRepository.saveRecipe(recipe)
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }

    fun dismissSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}