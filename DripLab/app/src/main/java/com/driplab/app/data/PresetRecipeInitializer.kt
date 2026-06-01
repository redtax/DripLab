package com.driplab.app.data

import android.content.Context
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
            dao.deleteAllPresets()
            val presets = listOf(
                createOnePour(),
                createThreeStage(),
                createFourStage(),
                createKasuya46(),
                createWangCeFourStage()
            )
            presets.forEach { (recipe, steps) ->
                val recipeId = dao.insertRecipe(recipe)
                val totalDuration = steps.sumOf { it.duration }
                val stepsWithRatio = steps.map { step ->
                    step.copy(
                        recipeId = recipeId,
                        waterRatio = calculateWaterRatio(step.targetWater, recipe),
                        durationRatio = if (totalDuration > 0) step.duration.toFloat() / totalDuration * 100f else 0f
                    )
                }
                dao.insertSteps(stepsWithRatio)
            }
        }
    }

    private fun calculateWaterRatio(targetWater: Int, recipe: RecipeEntity): Float {
        val ratioNum = recipe.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
        val totalWater = recipe.coffeeWeight * ratioNum
        if (totalWater <= 0f) return 0f
        return (targetWater.toFloat() / totalWater * 100f)
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
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 40, instruction = "闷蒸阶段开始，缓慢注入40g水，让咖啡粉充分浸润"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 120, targetWater = 200, instruction = "一次性持续注水至目标水量，保持水流稳定，等待滴滤完成")
        )
    }

    private fun createThreeStage(): Pair<RecipeEntity, List<RecipeStepEntity>> {
        return RecipeEntity(
            name = "三段式手冲",
            method = "POUR_OVER",
            coffeeWeight = 15f,
            waterRatio = "1:16",
            temperature = 90,
            isDefault = true
        ) to listOf(
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 45, instruction = "闷蒸开始，缓慢注入45g水，等待30秒，让咖啡粉释放气体"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 35, targetWater = 70, instruction = "第一段注水，绕圈注入70g水，形成咖啡液的骨架结构"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 35, targetWater = 70, instruction = "第二段注水，继续绕圈注入70g水，萃取咖啡的甜感与层次"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "WAIT", duration = 45, targetWater = 55, instruction = "最后一段注水，注入55g水，等待咖啡液自然滴落完成")
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
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 30, targetWater = 45, instruction = "闷蒸开始，注入45g水，等待30秒"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 25, targetWater = 50, instruction = "第一段注水，绕圈注入50g水，为萃取建立基础"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 25, targetWater = 50, instruction = "第二段注水，继续注入50g水，释放咖啡的香气与甜感"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 25, targetWater = 50, instruction = "第三段注水，注入50g水，丰富口感层次"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 40, targetWater = 45, instruction = "最后一段注水，注入45g水，等待咖啡液完全滴落")
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
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "POUR", duration = 45, targetWater = 45, instruction = "第一段注水，注入45g水，这决定了咖啡的酸度与甜度的平衡"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 45, targetWater = 45, instruction = "第二段注水，注入45g水，这决定了咖啡的甜度表现"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 45, targetWater = 30, instruction = "第三段注水，注入30g水，决定咖啡的口感强度"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 45, targetWater = 30, instruction = "第四段注水，注入30g水，后段低温注水，减少杂味"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 45, targetWater = 75, instruction = "最后一段注入75g水，等待咖啡液滴滤完成")
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
            RecipeStepEntity(recipeId = 0, sequence = 1, phase = "BLOOM", duration = 35, targetWater = 35, instruction = "闷蒸开始，注入35g水，等待35秒，唤醒咖啡风味"),
            RecipeStepEntity(recipeId = 0, sequence = 2, phase = "POUR", duration = 45, targetWater = 50, instruction = "第一段注水，绕圈注入50g水，提升香气的释放"),
            RecipeStepEntity(recipeId = 0, sequence = 3, phase = "POUR", duration = 45, targetWater = 60, instruction = "第二段注水，注入60g水，支撑咖啡的整体结构"),
            RecipeStepEntity(recipeId = 0, sequence = 4, phase = "POUR", duration = 45, targetWater = 40, instruction = "第三段注水，注入40g水，完成层次收尾"),
            RecipeStepEntity(recipeId = 0, sequence = 5, phase = "WAIT", duration = 45, targetWater = 40, instruction = "最后一段注水，注入40g水，让水流自然滴落完毕")
        )
    }
}