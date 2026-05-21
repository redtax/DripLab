package com.driplab.app.data

import android.content.Context
import androidx.room.Room
import com.driplab.app.core.database.DripLabDatabase
import com.driplab.app.core.database.entity.RecipeEntity
import com.driplab.app.core.database.entity.RecipeStepEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRecipeInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun initialize(database: DripLabDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.recipeDao()
            val existingPresets = dao.getDefaultRecipesWithSteps("POUR_OVER")
            if (existingPresets.isNotEmpty()) return@launch

            val presets = listOf(
                createOnePour(),
                createThreeStage(),
                createFourStage(),
                createChampion()
            )

            presets.forEach { (recipe, steps) ->
                val recipeId = dao.insertRecipe(recipe)
                dao.insertSteps(steps.map { it.copy(recipeId = recipeId) })
            }
        }
    }

    private fun createOnePour(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "一刀流",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:16",
            temperature = 90,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 40, instruction = "注入40g水进行闷蒸"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 120, targetWater = 200, instruction = "一次性注水至目标水量240g")
        )
    }

    private fun createThreeStage(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "三段式",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:16",
            temperature = 90,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 45, instruction = "闷蒸30秒"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 35, targetWater = 70, instruction = "第一段: 注水至约115ml (30%)"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 35, targetWater = 70, instruction = "第二段: 注水至约185ml (60%)"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "WAIT", duration = 45, targetWater = 55, instruction = "第三段: 注水至240ml (100%)，等待滴滤完成")
        )
    }

    private fun createFourStage(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "四段式",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:16",
            temperature = 90,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 45, instruction = "闷蒸30秒"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 25, targetWater = 50, instruction = "第一段: 注水至约25%"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 25, targetWater = 50, instruction = "第二段: 注水至约50%"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 25, targetWater = 50, instruction = "第三段: 注水至约75%"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 40, targetWater = 45, instruction = "第四段: 注水至100%，等待滴滤完成")
        )
    }

    private fun createChampion(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "冠军方案",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:16",
            temperature = 90,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 50, instruction = "50g水闷蒸"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 20, targetWater = 50, instruction = "第1次50g注水"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 20, targetWater = 50, instruction = "第2次50g注水"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 20, targetWater = 50, instruction = "第3次50g注水"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "POUR", duration = 20, targetWater = 50, instruction = "第4次50g注水"),
            RecipeStepEntity(recipeId = 0, sequence = 6, phase = "WAIT", duration = 30, targetWater = 50, instruction = "第5次50g注水，等待滴滤完成")
        )
    }
}