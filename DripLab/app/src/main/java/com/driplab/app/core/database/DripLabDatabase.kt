package com.driplab.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.driplab.app.core.database.dao.BrewNoteDao
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.database.entity.BrewNoteEntity
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity

@Database(
    entities = [
        RecipeEntity::class,
        RecipeStepEntity::class,
        BrewNoteEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class DripLabDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun brewNoteDao(): BrewNoteDao
}