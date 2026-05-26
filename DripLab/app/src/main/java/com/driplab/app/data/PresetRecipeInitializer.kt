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
                createKasuya46(),
                createWangCeFourStage()
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

    private fun createKasuya46(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "粕谷哲·46手冲法",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:15",
            temperature = 92,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "POUR", duration = 45, targetWater = 45, instruction = "第一段(40%): 注入45g水，决定了酸度与甜度的平衡"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 45, targetWater = 45, instruction = "第二段(20%): 注入45g水，决定了咖啡的甜度"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 45, targetWater = 30, instruction = "第三段(10%): 注入30g水，决定咖啡的口感强度"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 45, targetWater = 30, instruction = "第四段(10%): 注入30g水，后段低温减少杂味"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 45, targetWater = 75, instruction = "第五段(20%): 注入75g水，等待滴滤完成")
        )
    }

    private fun createWangCeFourStage(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "王策·四段式手法",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:15",
            temperature = 92,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 35, targetWater = 35, instruction = "闷蒸: 注入35g水，闷蒸35秒"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 45, targetWater = 50, instruction = "第一段: 绕圈注入50g水，待水位下降到底部"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 45, targetWater = 60, instruction = "第二段: 注入60g水，提香与支撑"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 45, targetWater = 40, instruction = "第三段: 注入40g水，收尾与层次"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 45, targetWater = 40, instruction = "第四段: 注入40g水，让水流自然滴落完毕")
        )
    }
}