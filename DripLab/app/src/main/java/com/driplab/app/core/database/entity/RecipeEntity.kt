package com.driplab.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val method: String,
    val coffeeWeight: Float,
    val waterRatio: String,
    val temperature: Int,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)