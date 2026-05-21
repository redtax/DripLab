package com.driplab.app.core.timer

import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrewState(
    val currentPhase: BrewPhase = BrewPhase.IDLE,
    val currentStepIndex: Int = -1,
    val elapsedSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val stepDuration: Int = 0,
    val stepRemainingSeconds: Int = 0,
    val totalWaterPoured: Int = 0,
    val targetWater: Int = 0,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val steps: List<RecipeStep> = emptyList()
) {
    val stepProgress: Float
        get() = if (stepDuration > 0) {
            1f - stepRemainingSeconds.toFloat() / stepDuration.toFloat()
        } else 0f

    val totalProgress: Float
        get() = if (totalSeconds > 0) {
            elapsedSeconds.toFloat() / totalSeconds.toFloat()
        } else 0f
}

class BrewTimer {

    private val _brewState = MutableStateFlow(BrewState())
    val brewState: StateFlow<BrewState> = _brewState.asStateFlow()

    private var timer: kotlinx.coroutines.Job? = null

    fun loadRecipe(recipe: Recipe) {
        val steps = recipe.steps.filter { it.phase != BrewPhase.IDLE && it.phase != BrewPhase.COMPLETE }
        val totalSeconds = steps.sumOf { it.duration }

        _brewState.value = BrewState(
            currentPhase = BrewPhase.IDLE,
            currentStepIndex = -1,
            totalSeconds = totalSeconds,
            steps = steps
        )
    }

    fun start() {
        if (_brewState.value.isComplete) return

        val state = _brewState.value
        if (state.currentPhase == BrewPhase.IDLE) {
            advanceToNextStep()
        }

        _brewState.value = _brewState.value.copy(isRunning = true)
        startTimer()
    }

    fun pause() {
        timer?.cancel()
        _brewState.value = _brewState.value.copy(isRunning = false)
    }

    fun resume() {
        if (_brewState.value.isComplete) return
        _brewState.value = _brewState.value.copy(isRunning = true)
        startTimer()
    }

    fun stop() {
        timer?.cancel()
        _brewState.value = BrewState()
    }

    fun skipToNextStep() {
        timer?.cancel()
        advanceToNextStep()
        if (!_brewState.value.isComplete && _brewState.value.isRunning) {
            startTimer()
        }
    }

    private fun advanceToNextStep() {
        val state = _brewState.value
        val nextIndex = state.currentStepIndex + 1

        if (nextIndex >= state.steps.size) {
            _brewState.value = state.copy(
                currentPhase = BrewPhase.COMPLETE,
                isRunning = false,
                isComplete = true,
                stepRemainingSeconds = 0
            )
            return
        }

        val step = state.steps[nextIndex]
        _brewState.value = state.copy(
            currentPhase = step.phase,
            currentStepIndex = nextIndex,
            stepDuration = step.duration,
            stepRemainingSeconds = step.duration,
            targetWater = state.totalWaterPoured + step.targetWater
        )
    }

    private fun startTimer() {
        timer?.cancel()
        timer = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).let { scope ->
            kotlinx.coroutines.launch(scope) {
                while (_brewState.value.isRunning && !_brewState.value.isComplete) {
                    kotlinx.coroutines.delay(1000L)
                    val current = _brewState.value
                    val newRemaining = current.stepRemainingSeconds - 1
                    val newElapsed = current.elapsedSeconds + 1

                    if (newRemaining <= 0) {
                        if (current.currentStepIndex + 1 >= current.steps.size) {
                            _brewState.value = current.copy(
                                elapsedSeconds = newElapsed,
                                stepRemainingSeconds = 0,
                                currentPhase = BrewPhase.COMPLETE,
                                isRunning = false,
                                isComplete = true
                            )
                        } else {
                            val nextStep = current.steps[current.currentStepIndex + 1]
                            _brewState.value = current.copy(
                                elapsedSeconds = newElapsed,
                                currentPhase = nextStep.phase,
                                currentStepIndex = current.currentStepIndex + 1,
                                stepDuration = nextStep.duration,
                                stepRemainingSeconds = nextStep.duration,
                                targetWater = current.totalWaterPoured + nextStep.targetWater
                            )
                        }
                    } else {
                        _brewState.value = current.copy(
                            elapsedSeconds = newElapsed,
                            stepRemainingSeconds = newRemaining
                        )
                    }
                }
            }
        }
    }
}