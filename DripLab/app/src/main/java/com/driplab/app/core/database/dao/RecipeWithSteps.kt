package com.driplab.app.core.database.dao

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity

data class RecipeWithSteps(
    @Embedded
    val recipe: RecipeEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val steps: List<RecipeStepEntity>
)
