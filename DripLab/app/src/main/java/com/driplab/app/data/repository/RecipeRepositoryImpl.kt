package com.driplab.app.data.repository

import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import com.driplab.app.domain.repository.RecipeRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val gson: Gson
) : RecipeRepository {

    override suspend fun getRecipesByMethod(method: BrewMethod): List<Recipe> {
        val recipeEntities = recipeDao.getRecipesByMethod(method.name)
        return recipeEntities.map { entity ->
            val steps = recipeDao.getStepsByRecipeId(entity.id)
            entity.toDomain(steps)
        }
    }

    override suspend fun getRecipeById(id: Long): Recipe? {
        val entity = recipeDao.getRecipeById(id) ?: return null
        val steps = recipeDao.getStepsByRecipeId(id)
        return entity.toDomain(steps)
    }

    override suspend fun saveRecipe(recipe: Recipe): Long {
        val entity = recipe.toEntity()
        val recipeId = if (recipe.id == 0L) {
            recipeDao.insertRecipe(entity)
        } else {
            recipeDao.updateRecipe(entity)
            recipe.id
        }
        recipeDao.deleteStepsByRecipeId(recipeId)
        recipeDao.insertSteps(recipe.steps.mapIndexed { index, step ->
            step.toEntity(recipeId, index + 1)
        })
        return recipeId
    }

    override suspend fun deleteRecipe(id: Long) {
        recipeDao.deleteRecipeById(id)
    }

    override suspend fun importRecipe(json: String): Boolean {
        return try {
            val token = object : TypeToken<ImportRecipeDto>() {}.type
            val dto: ImportRecipeDto = gson.fromJson(json, token)
            dto.recipes.forEach { recipeDto ->
                val steps = recipeDto.steps.mapIndexed { index, stepDto ->
                    RecipeStep(
                        sequence = index + 1,
                        phase = try { BrewPhase.valueOf(stepDto.phase) } catch (_: Exception) { BrewPhase.POUR },
                        duration = stepDto.duration,
                        targetWater = stepDto.targetWater,
                        instruction = stepDto.instruction ?: ""
                    )
                }
                val recipe = Recipe(
                    name = recipeDto.name,
                    method = try { BrewMethod.valueOf(recipeDto.method) } catch (_: Exception) { BrewMethod.POUR_OVER },
                    coffeeWeight = recipeDto.coffeeWeight,
                    waterRatio = recipeDto.waterRatio,
                    temperature = recipeDto.temperature,
                    steps = steps
                )
                saveRecipe(recipe)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun exportRecipe(id: Long): String {
        val recipe = getRecipeById(id) ?: return "{}"
        val dto = ExportRecipeDto(
            version = "1.0",
            exportDate = System.currentTimeMillis(),
            recipes = listOf(
                RecipeDto(
                    name = recipe.name,
                    method = recipe.method.name,
                    coffeeWeight = recipe.coffeeWeight,
                    waterRatio = recipe.waterRatio,
                    temperature = recipe.temperature,
                    steps = recipe.steps.map { step ->
                        StepDto(
                            phase = step.phase.name,
                            duration = step.duration,
                            targetWater = step.targetWater,
                            instruction = step.instruction
                        )
                    }
                )
            )
        )
        return gson.toJson(dto)
    }

    override suspend fun getDefaultRecipes(method: BrewMethod): List<Recipe> {
        val recipeEntities = recipeDao.getDefaultRecipes(method.name)
        return recipeEntities.map { entity ->
            val steps = recipeDao.getStepsByRecipeId(entity.id)
            entity.toDomain(steps)
        }
    }

    override suspend fun getAllRecipes(): List<Recipe> {
        return emptyList()
    }

    fun getAllRecipesFlow(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipes().map { entityList ->
            entityList.map { entity ->
                Recipe(
                    id = entity.id,
                    name = entity.name,
                    method = try { BrewMethod.valueOf(entity.method) } catch (_: Exception) { BrewMethod.POUR_OVER },
                    coffeeWeight = entity.coffeeWeight,
                    waterRatio = entity.waterRatio,
                    temperature = entity.temperature,
                    isDefault = entity.isDefault,
                    createdAt = entity.createdAt,
                    steps = emptyList()
                )
            }
        }
    }

    private data class ImportRecipeDto(
        val version: String = "1.0",
        val exportDate: Long = 0,
        val recipes: List<RecipeDto> = emptyList()
    )

    private data class ExportRecipeDto(
        val version: String,
        val exportDate: Long,
        val recipes: List<RecipeDto>
    )

    data class RecipeDto(
        val name: String,
        val method: String,
        val coffeeWeight: Float,
        val waterRatio: String,
        val temperature: Int,
        val steps: List<StepDto>
    )

    data class StepDto(
        val phase: String,
        val duration: Int,
        val targetWater: Int,
        val instruction: String? = null
    )

    private fun RecipeEntity.toDomain(steps: List<RecipeStepEntity>): Recipe {
        return Recipe(
            id = id,
            name = name,
            method = try { BrewMethod.valueOf(method) } catch (_: Exception) { BrewMethod.POUR_OVER },
            coffeeWeight = coffeeWeight,
            waterRatio = waterRatio,
            temperature = temperature,
            isDefault = isDefault,
            createdAt = createdAt,
            steps = steps.map { step ->
                RecipeStep(
                    id = step.id,
                    recipeId = step.recipeId,
                    sequence = step.sequence,
                    phase = try { BrewPhase.valueOf(step.phase) } catch (_: Exception) { BrewPhase.POUR },
                    duration = step.duration,
                    targetWater = step.targetWater,
                    instruction = step.instruction
                )
            }
        )
    }

    private fun Recipe.toEntity() = RecipeEntity(
        id = id,
        name = name,
        method = method.name,
        coffeeWeight = coffeeWeight,
        waterRatio = waterRatio,
        temperature = temperature,
        isDefault = isDefault,
        createdAt = createdAt
    )

    private fun RecipeStep.toEntity(recipeId: Long, sequence: Int) = RecipeStepEntity(
        id = id,
        recipeId = recipeId,
        sequence = sequence,
        phase = phase.name,
        duration = duration,
        targetWater = targetWater,
        instruction = instruction
    )
}