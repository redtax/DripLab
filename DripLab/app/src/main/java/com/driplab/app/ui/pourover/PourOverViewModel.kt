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
import com.driplab.app.data.BrewSessionManager
import com.driplab.app.data.repository.BrewNoteRepositoryImpl
import com.driplab.app.data.repository.RecipeRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewNote
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
    val currentMethod: BrewMethod = BrewMethod.POUR_OVER,
    val brewStartTime: Long = 0,
    val brewEndTime: Long = 0,
    val noteAutoSaved: Boolean = false
)

@HiltViewModel
class PourOverViewModel @Inject constructor(
    private val recipeRepository: RecipeRepositoryImpl,
    private val brewNoteRepository: BrewNoteRepositoryImpl,
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
    private var tickSoundId: Int = 0
    private var tickAlertSoundId: Int = 0
    private var dingSoundId: Int = 0

    init {
        loadRecipesAndAutoSelect()
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

    private fun loadRecipesAndAutoSelect() {
        viewModelScope.launch {
            val presets = recipeRepository.getDefaultRecipes(BrewMethod.POUR_OVER)
            val all = recipeRepository.getRecipesByMethod(BrewMethod.POUR_OVER)
            _uiState.value = _uiState.value.copy(
                presetRecipes = presets,
                allPourOverRecipes = all
            )
            if (_uiState.value.selectedRecipe == null) {
                autoSelectFallback(presets, all)
            }
        }
    }

    private fun autoSelectFallback(presets: List<Recipe>, allRecipes: List<Recipe>) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastId = prefs.getLong(KEY_LAST_RECIPE_ID, -1L)
        val fromPrefs = if (lastId > 0) {
            allRecipes.find { it.id == lastId } ?: presets.find { it.id == lastId }
        } else null
        if (fromPrefs != null) {
            applyRecipe(fromPrefs)
        } else {
            val default = presets.find { it.name == "三段式手冲" } ?: presets.firstOrNull()
            default?.let { applyRecipe(it) }
        }
    }

    private fun saveLastRecipeId(id: Long) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_RECIPE_ID, id).apply()
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
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        val res = appContext.resources
        val pkg = appContext.packageName
        clickSoundId = soundPool?.load(appContext, res.getIdentifier("gear_click", "raw", pkg), 1) ?: 0
        tickSoundId = soundPool?.load(appContext, res.getIdentifier("tick", "raw", pkg), 1) ?: 0
        tickAlertSoundId = soundPool?.load(appContext, res.getIdentifier("tick_alert", "raw", pkg), 1) ?: 0
        dingSoundId = soundPool?.load(appContext, res.getIdentifier("ding", "raw", pkg), 1) ?: 0
    }

    fun playClickSound() {
        val state = themeManager.state.value
        if (state.alertMode.hasSound) {
            soundPool?.play(clickSoundId, 0.3f, 0.3f, 1, 0, 1f)
        }
    }

    private fun playTickSound(remainingSeconds: Int) {
        val alertState = themeManager.state.value
        if (!alertState.alertMode.hasSound) return
        if (remainingSeconds <= 10) {
            soundPool?.play(tickAlertSoundId, 0.5f, 0.5f, 1, 0, 1f)
        } else {
            soundPool?.play(tickSoundId, 0.25f, 0.25f, 1, 0, 1f)
        }
    }

    private fun playDingSound() {
        val alertState = themeManager.state.value
        if (alertState.alertMode.hasSound) {
            soundPool?.play(dingSoundId, 0.6f, 0.6f, 1, 0, 1f)
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

        brewTimer.onTick = { remaining ->
            playTickSound(remaining)
        }

        brewTimer.onPhaseComplete = {
            playDingSound()
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

    fun adjustCoffeeUp() {
        val newWeight = (_uiState.value.coffeeWeight + 0.1f).coerceIn(5f, 150f)
        val rounded = (newWeight * 10).toInt() / 10f
        updateCoffeeWeight(rounded)
        playClickSound()
    }

    fun adjustCoffeeDown() {
        val newWeight = (_uiState.value.coffeeWeight - 0.1f).coerceIn(5f, 150f)
        val rounded = (newWeight * 10).toInt() / 10f
        updateCoffeeWeight(rounded)
        playClickSound()
    }

    fun adjustCoffeeUpFast() {
        val newWeight = (_uiState.value.coffeeWeight + 0.5f).coerceIn(5f, 150f)
        val rounded = (newWeight * 10).toInt() / 10f
        updateCoffeeWeight(rounded)
        playClickSound()
    }

    fun adjustCoffeeDownFast() {
        val newWeight = (_uiState.value.coffeeWeight - 0.5f).coerceIn(5f, 150f)
        val rounded = (newWeight * 10).toInt() / 10f
        updateCoffeeWeight(rounded)
        playClickSound()
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

    fun adjustTempUp() {
        val newTemp = (_uiState.value.temperature + 1).coerceIn(60, 100)
        _uiState.value = _uiState.value.copy(temperature = newTemp)
    }

    fun adjustTempDown() {
        val newTemp = (_uiState.value.temperature - 1).coerceIn(60, 100)
        _uiState.value = _uiState.value.copy(temperature = newTemp)
    }

    fun applyRecipe(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(
            selectedRecipe = recipe,
            selectedRecipeId = recipe.id,
            temperature = recipe.temperature,
            coffeeWeight = recipe.coffeeWeight,
            currentMethod = recipe.method,
            showRecipeDropdown = false,
            showRecipeConfirmDialog = false
        )
        updateCoffeeWeight(recipe.coffeeWeight)
        selectRatioByLabel(recipe.waterRatio)
        saveLastRecipeId(recipe.id)
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
        val selected = state.selectedRecipe
        val recipe = if (selected != null) {
            buildProportionalRecipe(selected, state.waterAmount)
        } else {
            createDefaultRecipe(state)
        }
        val startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(brewStartTime = startTime, noteAutoSaved = false)
        brewTimer.loadRecipe(recipe)
        brewTimer.start()

        viewModelScope.launch {
            brewTimer.brewState.collect { brewState ->
                _uiState.value = _uiState.value.copy(brewState = brewState)
                if (brewState.isComplete && !_uiState.value.noteAutoSaved) {
                    saveBrewNote()
                }
            }
        }
    }

    private fun buildProportionalRecipe(original: Recipe, newTotalWater: Float): Recipe {
        val originalTotal = original.steps.sumOf { it.targetWater }.toFloat()
        if (originalTotal <= 0f) return original
        return original.copy(
            steps = original.steps.map { step ->
                val proportion = step.targetWater / originalTotal
                val newTarget = (proportion * newTotalWater).roundToInt()
                step.copy(targetWater = newTarget)
            }
        )
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

    private fun saveBrewNote() {
        val state = _uiState.value
        val endTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(brewEndTime = endTime, noteAutoSaved = true)

        val recipe = state.selectedRecipe
        val note = BrewNote(
            recipeId = recipe?.id,
            recipeName = recipe?.name ?: "自定义手冲",
            method = state.currentMethod,
            coffeeWeight = state.coffeeWeight,
            waterRatio = state.ratioLabel,
            temperature = state.temperature,
            brewDate = System.currentTimeMillis(),
            startTimeMillis = state.brewStartTime,
            endTimeMillis = endTime,
            totalTime = state.brewState.elapsedSeconds,
            rating = 0,
            review = ""
        )

        viewModelScope.launch {
            brewNoteRepository.saveNote(note)
        }
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

    companion object {
        private const val PREFS_NAME = "driplab_prefs"
        private const val KEY_LAST_RECIPE_ID = "last_recipe_id"
    }
}