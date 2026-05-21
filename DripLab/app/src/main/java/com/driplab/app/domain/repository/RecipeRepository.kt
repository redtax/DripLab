package com.driplab.app.domain.repository

import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.Recipe

interface RecipeRepository {
    suspend fun getRecipesByMethod(method: BrewMethod): List<Recipe>
    suspend fun getRecipeById(id: Long): Recipe?
    suspend fun saveRecipe(recipe: Recipe): Long
    suspend fun deleteRecipe(id: Long)
    suspend fun importRecipe(json: String): Boolean
    suspend fun exportRecipe(id: Long): String
    suspend fun getDefaultRecipes(method: BrewMethod): List<Recipe>
    suspend fun getAllRecipes(): List<Recipe>
}