package com.driplab.app.data.repository

import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity
import com.driplab.app.core.recipe.ImportResult
import com.driplab.app.core.recipe.RecipeBackupManager
import com.driplab.app.core.recipe.RecipeFormatManager
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import com.driplab.app.domain.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val formatManager: RecipeFormatManager,
    private val backupManager: RecipeBackupManager
) : RecipeRepository {

    override suspend fun getRecipesByMethod(method: BrewMethod): List<Recipe> = withContext(Dispatchers.IO) {
        recipeDao.getRecipesWithStepsByMethod(method.name).map { it.toDomain() }
    }

    override suspend fun getRecipeById(id: Long): Recipe? = withContext(Dispatchers.IO) {
        recipeDao.getRecipeWithSteps(id)?.toDomain()
    }

    override suspend fun saveRecipe(recipe: Recipe): Long = withContext(Dispatchers.IO) {
        val entity = recipe.toEntity()
        val recipeId = if (recipe.id > 0) {
            recipeDao.updateRecipe(entity)
            recipeDao.deleteStepsByRecipeId(recipe.id)
            recipe.id
        } else {
            recipeDao.insertRecipe(entity)
        }
        val ratioNum = recipe.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
        val totalWater = recipe.coffeeWeight * ratioNum
        val totalDuration = recipe.steps.sumOf { it.duration }
        val steps = recipe.steps.map { step ->
            val waterR = if (step.waterRatio > 0f) step.waterRatio else {
                if (totalWater > 0f) step.targetWater.toFloat() / totalWater * 100f else 0f
            }
            val durR = if (step.durationRatio > 0f) step.durationRatio else {
                if (totalDuration > 0) step.duration.toFloat() / totalDuration * 100f else 0f
            }
            RecipeStepEntity(
                recipeId = recipeId,
                sequence = step.sequence,
                phase = step.phase.name,
                duration = step.duration,
                targetWater = step.targetWater,
                waterRatio = waterR,
                durationRatio = durR,
                instruction = step.instruction
            )
        }
        recipeDao.insertSteps(steps)
        backupManager.backup(recipeDao)
        recipeId
    }

    override suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) {
        recipeDao.deleteRecipeById(id)
        backupManager.backup(recipeDao)
    }

    override suspend fun importRecipe(text: String): ImportResult = withContext(Dispatchers.IO) {
        formatManager.parseRecipe(text)
    }

    override suspend fun exportRecipe(id: Long): String = withContext(Dispatchers.IO) {
        val recipe = recipeDao.getRecipeWithSteps(id) ?: return@withContext ""
        val domain = recipe.toDomain()
        formatManager.exportRecipe(domain, domain.steps)
    }

    override suspend fun getDefaultRecipes(method: BrewMethod): List<Recipe> = withContext(Dispatchers.IO) {
        recipeDao.getDefaultRecipesWithSteps(method.name).map { it.toDomain() }
    }

    override suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        recipeDao.getAllRecipesWithStepsOnce().map { it.toDomain() }
    }

    override suspend fun backup() = backupManager.backup(recipeDao)

    override suspend fun restore() = backupManager.restoreTo(recipeDao)

    override fun hasBackup(): Boolean = backupManager.hasBackup()

    private fun com.driplab.app.core.database.dao.RecipeWithSteps.toDomain(): Recipe {
        return Recipe(
            id = recipe.id,
            name = recipe.name,
            method = BrewMethod.valueOf(recipe.method),
            coffeeWeight = recipe.coffeeWeight,
            waterRatio = recipe.waterRatio,
            temperature = recipe.temperature,
            isDefault = recipe.isDefault,
            steps = steps.sortedBy { it.sequence }.map { it.toDomain() }
        )
    }

    private fun RecipeStepEntity.toDomain(): RecipeStep {
        return RecipeStep(
            id = id,
            recipeId = recipeId,
            sequence = sequence,
            phase = BrewPhase.valueOf(phase),
            duration = duration,
            targetWater = targetWater,
            waterRatio = waterRatio,
            durationRatio = durationRatio,
            instruction = instruction
        )
    }

    private fun Recipe.toEntity(): RecipeEntity {
        return RecipeEntity(
            id = id,
            name = name,
            method = method.name,
            coffeeWeight = coffeeWeight,
            waterRatio = waterRatio,
            temperature = temperature,
            isDefault = isDefault
        )
    }
}