package com.driplab.app.domain.model

data class BrewNote(
    val id: Long = 0,
    val recipeId: Long? = null,
    val method: BrewMethod,
    val brewDate: Long = System.currentTimeMillis(),
    val totalTime: Int = 0,
    val acidity: Int? = null,
    val sweetness: Int? = null,
    val bitterness: Int? = null,
    val body: Int? = null,
    val notes: String = "",
    val photoPath: String = ""
)