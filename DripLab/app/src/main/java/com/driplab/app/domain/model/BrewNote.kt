package com.driplab.app.domain.model

data class BrewNote(
    val id: Long = 0,
    val recipeId: Long? = null,
    val recipeName: String = "",
    val method: BrewMethod,
    val coffeeWeight: Float = 0f,
    val waterRatio: String = "",
    val temperature: Int = 0,
    val brewDate: Long = System.currentTimeMillis(),
    val startTimeMillis: Long = 0,
    val endTimeMillis: Long = 0,
    val totalTime: Int = 0,
    val rating: Int = 0,
    val review: String = "",
    val acidity: Int? = null,
    val sweetness: Int? = null,
    val bitterness: Int? = null,
    val body: Int? = null,
    val notes: String = "",
    val photoPath: String = ""
)