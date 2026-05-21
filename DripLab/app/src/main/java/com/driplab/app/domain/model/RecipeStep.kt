package com.driplab.app.domain.model

data class RecipeStep(
    val id: Long = 0,
    val recipeId: Long = 0,
    val sequence: Int,
    val phase: BrewPhase,
    val duration: Int,
    val targetWater: Int = 0,
    val instruction: String = ""
)