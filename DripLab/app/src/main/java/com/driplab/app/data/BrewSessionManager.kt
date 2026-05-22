package com.driplab.app.data

import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SessionState(
    val activeRecipe: Recipe? = null,
    val activeMethod: BrewMethod = BrewMethod.POUR_OVER
)

@Singleton
class BrewSessionManager @Inject constructor() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun setActiveRecipe(recipe: Recipe) {
        _state.value = SessionState(
            activeRecipe = recipe,
            activeMethod = recipe.method
        )
    }

    fun clearActiveRecipe() {
        _state.value = SessionState(
            activeMethod = _state.value.activeMethod
        )
    }
}