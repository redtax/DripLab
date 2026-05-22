package com.driplab.app.ui.pourover

import android.content.Context
import android.speech.tts.TextToSpeech
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class PourOverUiState(
    val coffeeWeight: Float = 15f,
    val selectedRatioPreset: Int = 1,
    val ratioPresets: List<RatioPreset> = BrewCalculator.ratioPresets,
    val customRatio: Float = 15f,
    val waterAmount: Float = 225f,
    val ratioLabel: String = "1:15",
    val suggestedTemp: Int = 92,
    val temperature: Int = 92,
    val selectedRecipeId: Long = -1L,
    val presetRecipes: List<Recipe> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val brewState: BrewState = BrewState(),
    val voicePrompt: String = "",
    val showPresetSelector: Boolean = false,
)

@HiltViewModel
class PourOverViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PourOverUiState())
    val uiState: StateFlow<PourOverUiState> = _uiState.asStateFlow()

    val brewTimer = BrewTimer()
    private var tts: TextToSpeech? = null

    init {
        loadPresetRecipes()
        setupBrewTimerCallbacks()
        initTts()
    }

    private fun loadPresetRecipes() {
        viewModelScope.launch {
            val presets = recipeRepository.getDefaultRecipes(BrewMethod.POUR_OVER)
            _uiState.value = _uiState.value.copy(presetRecipes = presets)
        }
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
            }
        }
    }

    private fun setupBrewTimerCallbacks() {
        brewTimer.onPhaseChanged = { phase, targetWater, instruction ->
            val prompt = when (phase) {
                BrewPhase.BLOOM -> "闷蒸开始，注入${targetWater}克水，${instruction}"
                BrewPhase.POUR -> "开始注水，目标${targetWater}克"
                BrewPhase.WAIT -> "注水量达标时请暂停，等待滴滤"
                else -> instruction
            }
            _uiState.value = _uiState.value.copy(voicePrompt = prompt)
            speak(prompt)
        }

        brewTimer.onPhaseCountdownEnd = { phase ->
            val prompt = when (phase) {
                BrewPhase.BLOOM -> "闷蒸完成"
                BrewPhase.POUR -> "注水阶段结束"
                BrewPhase.WAIT -> "滴滤完成"
                else -> ""
            }
            if (prompt.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(voicePrompt = prompt)
                speak(prompt)
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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

    fun adjustCoffeeWeight(delta: Float) {
        val state = _uiState.value
        val newWeight = (state.coffeeWeight + delta).coerceIn(5f, 40f)
        val rounded = (newWeight * 10).toInt() / 10f
        updateCoffeeWeight(rounded)
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
                _uiState.value = _uiState.value.copy(brewState = brewState)
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
        _uiState.value = _uiState.value.copy(brewState = BrewState())
    }

    fun advanceToNextStep() {
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
                RecipeStep(sequence = 1, phase = BrewPhase.BLOOM, duration = 30, targetWater = (totalWater * 0.15f).toInt(), instruction = "闷蒸30秒"),
                RecipeStep(sequence = 2, phase = BrewPhase.POUR, duration = 45, targetWater = (totalWater * 0.40f).toInt(), instruction = "第一段注水"),
                RecipeStep(sequence = 3, phase = BrewPhase.POUR, duration = 45, targetWater = (totalWater * 0.30f).toInt(), instruction = "第二段注水"),
                RecipeStep(sequence = 4, phase = BrewPhase.WAIT, duration = 30, targetWater = (totalWater * 0.15f).toInt(), instruction = "最后注水并等待滴滤")
            )
        )
    }
}