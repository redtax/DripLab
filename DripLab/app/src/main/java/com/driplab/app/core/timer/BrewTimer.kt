package com.driplab.app.core.timer

import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrewState(
    val currentPhase: BrewPhase = BrewPhase.IDLE,
    val currentStepIndex: Int = -1,
    val elapsedSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val stepDuration: Int = 0,
    val stepRemainingSeconds: Int = 0,
    val totalWaterPoured: Int = 0,
    val targetWater: Int = 0,
    val currentStepTargetWater: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
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

    private val scope = CoroutineScope(Dispatchers.Default)
    private val _brewState = MutableStateFlow(BrewState())
    val brewState: StateFlow<BrewState> = _brewState.asStateFlow()

    private var phaseTimer: Job? = null
    private var elapsedTimer: Job? = null
    var onPhaseChanged: ((BrewPhase, Int, String) -> Unit)? = null
    var onPhaseCountdownEnd: ((BrewPhase) -> Unit)? = null

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
        if (state.currentPhase == BrewPhase.IDLE || state.isPaused) {
            advanceToNextStep()
        }

        _brewState.value = _brewState.value.copy(isRunning = true, isPaused = false)
        startElapsedTimer()
        startPhaseTimer()
    }

    fun pause() {
        phaseTimer?.cancel()
        _brewState.value = _brewState.value.copy(isPaused = true)
    }

    fun resume() {
        if (_brewState.value.isComplete) return
        _brewState.value = _brewState.value.copy(isPaused = false)
        startPhaseTimer()
    }

    fun stop() {
        phaseTimer?.cancel()
        elapsedTimer?.cancel()
        _brewState.value = BrewState()
    }

    fun skipToNextStep() {
        phaseTimer?.cancel()
        advanceToNextStep()
        if (!_brewState.value.isComplete) {
            startPhaseTimer()
        }
    }

    private fun advanceToNextStep() {
        val state = _brewState.value
        val nextIndex = state.currentStepIndex + 1

        if (nextIndex >= state.steps.size) {
            _brewState.value = state.copy(
                currentPhase = BrewPhase.COMPLETE,
                isRunning = false,
                isPaused = false,
                isComplete = true,
                stepRemainingSeconds = 0
            )
            elapsedTimer?.cancel()
            phaseTimer?.cancel()
            return
        }

        val step = state.steps[nextIndex]
        val previousWater = state.steps.take(nextIndex).sumOf { it.targetWater }
        val cumulativeTarget = previousWater + step.targetWater
        _brewState.value = state.copy(
            currentPhase = step.phase,
            currentStepIndex = nextIndex,
            stepDuration = step.duration,
            stepRemainingSeconds = step.duration,
            totalWaterPoured = previousWater,
            targetWater = cumulativeTarget,
            currentStepTargetWater = step.targetWater
        )

        onPhaseChanged?.invoke(step.phase, step.targetWater, step.instruction)
    }

    private fun startElapsedTimer() {
        elapsedTimer?.cancel()
        elapsedTimer = scope.launch {
            while (_brewState.value.isRunning && !_brewState.value.isComplete) {
                delay(1000L)
                if (_brewState.value.isRunning && !_brewState.value.isComplete) {
                    _brewState.value = _brewState.value.copy(
                        elapsedSeconds = _brewState.value.elapsedSeconds + 1
                    )
                }
            }
        }
    }

    private fun startPhaseTimer() {
        phaseTimer?.cancel()
        if (_brewState.value.isPaused || _brewState.value.isComplete) return

        phaseTimer = scope.launch {
            while (_brewState.value.isRunning && !_brewState.value.isPaused && !_brewState.value.isComplete) {
                delay(1000L)
                val current = _brewState.value
                if (current.isPaused || !current.isRunning || current.isComplete) continue

                val newRemaining = current.stepRemainingSeconds - 1

                if (newRemaining <= 0) {
                    onPhaseCountdownEnd?.invoke(current.currentPhase)
                    advanceToNextStep()
                    if (_brewState.value.isComplete) {
                        _brewState.value = _brewState.value.copy(isRunning = false)
                        elapsedTimer?.cancel()
                    }
                } else {
                    _brewState.value = current.copy(stepRemainingSeconds = newRemaining)
                }
            }
        }
    }
}