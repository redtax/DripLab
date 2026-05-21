package com.driplab.app.domain.model

data class Recipe(
    val id: Long = 0,
    val name: String,
    val method: BrewMethod,
    val coffeeWeight: Float,
    val waterRatio: String,
    val temperature: Int,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val steps: List<RecipeStep> = emptyList()
)