package com.driplab.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE method = :method ORDER BY createdAt DESC")
    suspend fun getRecipesByMethod(method: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE isDefault = 1 AND method = :method")
    suspend fun getDefaultRecipes(method: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY sequence")
    suspend fun getStepsByRecipeId(recipeId: Long): List<RecipeStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RecipeStepEntity>)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsByRecipeId(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: Long)
}