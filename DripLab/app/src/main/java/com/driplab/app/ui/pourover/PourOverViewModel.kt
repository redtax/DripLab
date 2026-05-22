package com.driplab.app.ui.pourover

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.calculator.BrewCalculator
import com.driplab.app.core.calculator.BrewCalculator.RatioPreset
import com.driplab.app.core.theme.ThemeManager
import com.driplab.app.core.timer.BrewState
import com.driplab.app.core.timer.BrewTimer
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.data.BrewSessionManager
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
    val selectedRecipe: Recipe? = null,
    val allPourOverRecipes: List<Recipe> = emptyList(),
    val presetRecipes: List<Recipe> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val brewState: BrewState = BrewState(),
    val voicePrompt: String = "",
    val showRecipeDropdown: Boolean = false,
    val showRecipeConfirmDialog: Boolean = false,
    val pendingRecipe: Recipe? = null,
    val gearValue: Float = 15f,
    val currentMethod: BrewMethod = BrewMethod.POUR_OVER,
)

@HiltViewModel
class PourOverViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl,
    private val themeManager: ThemeManager,
    private val brewSessionManager: BrewSessionManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PourOverUiState())
    val uiState: StateFlow<PourOverUiState> = _uiState.asStateFlow()

    val brewTimer = BrewTimer()
    private var tts: TextToSpeech? = null
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0

    init {
        loadRecipes()
        observeSessionRecipes()
        setupBrewTimerCallbacks()
        initTts()
        initSoundPool()
    }

    private fun observeSessionRecipes() {
        viewModelScope.launch {
            brewSessionManager.state.collect { session ->
                session.activeRecipe?.let { recipe ->
                    applyRecipe(recipe)
                    brewSessionManager.clearActiveRecipe()
                }
            }
        }
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            val presets = recipeRepository.getDefaultRecipes(BrewMethod.POUR_OVER)
            val all = recipeRepository.getRecipesByMethod(BrewMethod.POUR_OVER)
            _uiState.value = _uiState.value.copy(
                presetRecipes = presets,
                allPourOverRecipes = all
            )
        }
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
            }
        }
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()
        clickSoundId = soundPool?.load(appContext, appContext.resources.getIdentifier("gear_click", "raw", appContext.packageName), 1) ?: 0
    }

    fun playClickSound() {
        val state = themeManager.state.value
        if (state.alertMode.hasSound) {
            soundPool?.play(clickSoundId, 0.3f, 0.3f, 1, 0, 1f)
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
            gearValue = weight,
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

    fun setCoffeeWeightFromGear(weight: Float) {
        val rounded = (weight * 10).toInt() / 10f
        val clamped = rounded.coerceIn(5f, 40f)
        if (clamped != _uiState.value.coffeeWeight) {
            updateCoffeeWeight(clamped)
            playClickSound()
        }
    }

    fun updateGearValue(value: Float) {
        _uiState.value = _uiState.value.copy(gearValue = value)
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

    fun applyRecipe(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(
            selectedRecipe = recipe,
            selectedRecipeId = recipe.id,
            temperature = recipe.temperature,
            coffeeWeight = recipe.coffeeWeight,
            gearValue = recipe.coffeeWeight,
            currentMethod = recipe.method,
            showRecipeDropdown = false,
            showRecipeConfirmDialog = false
        )
        updateCoffeeWeight(recipe.coffeeWeight)
        selectRatioByLabel(recipe.waterRatio)
    }

    fun showRecipeConfirmDialog(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(
            showRecipeConfirmDialog = true,
            pendingRecipe = recipe
        )
    }

    fun dismissRecipeConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showRecipeConfirmDialog = false,
            pendingRecipe = null
        )
    }

    fun confirmPendingRecipe() {
        val recipe = _uiState.value.pendingRecipe ?: return
        applyRecipe(recipe)
    }

    fun toggleRecipeDropdown() {
        _uiState.value = _uiState.value.copy(
            showRecipeDropdown = !_uiState.value.showRecipeDropdown
        )
    }

    private fun selectRatioByLabel(label: String) {
        val state = _uiState.value
        val idx = state.ratioPresets.indexOfFirst { it.label == label }
        if (idx >= 0) {
            selectRatioPreset(idx)
        }
    }

    fun startBrewing() {
        val state = _uiState.value
        val recipe = state.selectedRecipe ?: createDefaultRecipe(state)
        brewTimer.loadRecipe(recipe)
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

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        soundPool?.release()
    }
}