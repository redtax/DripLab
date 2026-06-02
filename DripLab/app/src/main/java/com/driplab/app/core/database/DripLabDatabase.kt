package com.driplab.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
    exportSchema = false
)
abstract class DripLabDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun brewNoteDao(): BrewNoteDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipe_steps ADD COLUMN waterRatio REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipe_steps ADD COLUMN durationRatio REAL NOT NULL DEFAULT 0")
            }
        }
    }
}