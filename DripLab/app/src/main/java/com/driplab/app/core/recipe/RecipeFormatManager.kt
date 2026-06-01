package com.driplab.app.core.recipe

import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import com.driplab.app.domain.model.RecipeStep
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedRecipeStep(
    val sequence: Int,
    val phase: BrewPhase,
    val duration: Int,
    val targetWater: Int,
    val waterRatio: Float,
    val instruction: String
)

data class ImportResult(
    val success: Boolean,
    val recipe: Recipe? = null,
    val steps: List<ParsedRecipeStep> = emptyList(),
    val errors: List<String> = emptyList()
)

@Singleton
class RecipeFormatManager @Inject constructor() {

    private val df = DecimalFormat("#.#")

    fun exportRecipe(recipe: Recipe, steps: List<RecipeStep>): String {
        val sb = StringBuilder()
        sb.appendLine("滴落间Lab配方")
        sb.appendLine("---")
        sb.appendLine("配方名: ${recipe.name}")
        sb.appendLine("冲煮方式: ${recipe.method.name}")
        sb.appendLine("咖啡粉量: ${df.format(recipe.coffeeWeight)}g")
        sb.appendLine("粉水比: ${recipe.waterRatio}")
        sb.appendLine("水温: ${recipe.temperature}°C")

        val ratioNum = recipe.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
        val totalWater = (recipe.coffeeWeight * ratioNum).toInt()
        sb.appendLine("总注水量: ${totalWater}ml")

        val stepRatios = calculateRatios(steps, totalWater)
        val ratioSum = stepRatios.sumOf { it.toDouble() }.toFloat()
        sb.appendLine("注水比例合计: ${df.format(ratioSum)}%")
        sb.appendLine()

        steps.forEachIndexed { index, step ->
            val ratio = stepRatios.getOrElse(index) { 0f }
            sb.appendLine("[${step.sequence}] ${phaseLabel(step.phase)} | ${step.duration}秒 | ${step.targetWater}ml | ${df.format(ratio)}% | ${step.instruction}")
        }

        return sb.toString()
    }

    fun parseRecipe(text: String): ImportResult {
        val errors = mutableListOf<String>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }

        if (lines.isEmpty() || lines.first() != "滴落间Lab配方") {
            return ImportResult(false, errors = listOf("格式错误：第一行应为「滴落间Lab配方」"))
        }

        val headerEnd = lines.indexOf("---")
        if (headerEnd < 0) {
            return ImportResult(false, errors = listOf("格式错误：缺少分隔符「---」"))
        }

        val headerLines = lines.take(headerEnd + 1)
        val stepLines = lines.drop(headerEnd + 1).filter { it.startsWith("[") }

        val name = extractValue(headerLines, "配方名")
        val methodStr = extractValue(headerLines, "冲煮方式")
        val coffeeWeightStr = extractValue(headerLines, "咖啡粉量")
        val waterRatioStr = extractValue(headerLines, "粉水比")
        val temperatureStr = extractValue(headerLines, "水温")
        val totalWaterStr = extractValue(headerLines, "总注水量")

        if (name.isNullOrBlank()) errors.add("缺少配方名")
        if (methodStr.isNullOrBlank()) errors.add("缺少冲煮方式")
        if (coffeeWeightStr.isNullOrBlank()) errors.add("缺少咖啡粉量")
        if (waterRatioStr.isNullOrBlank()) errors.add("缺少粉水比")
        if (temperatureStr.isNullOrBlank()) errors.add("缺少水温")

        val method = when (methodStr?.uppercase()) {
            "POUR_OVER" -> BrewMethod.POUR_OVER
            "IMMERSION" -> BrewMethod.IMMERSION
            "ESPRESSO" -> BrewMethod.ESPRESSO
            "AERO_PRESS" -> BrewMethod.AERO_PRESS
            else -> {
                errors.add("不支持的冲煮方式: $methodStr")
                BrewMethod.POUR_OVER
            }
        }

        val coffeeWeight = coffeeWeightStr?.replace("g", "")?.toFloatOrNull() ?: 0f
        val temperature = temperatureStr?.replace("°C", "")?.replace("℃", "")?.toIntOrNull() ?: 0
        val totalWater = totalWaterStr?.replace("ml", "")?.toIntOrNull() ?: 0

        if (coffeeWeight <= 0f) errors.add("咖啡粉量无效")
        if (waterRatioStr.isNullOrBlank() || !waterRatioStr.matches(Regex("1:\\d+(\\.\\d+)?"))) errors.add("粉水比格式无效，应为1:xx")
        if (temperature <= 0) errors.add("水温无效")

        val ratioNum = waterRatioStr?.replace("1:", "")?.toFloatOrNull() ?: 0f
        val expectedTotalWater = (coffeeWeight * ratioNum).toInt()
        if (expectedTotalWater > 0 && totalWater > 0 && Math.abs(expectedTotalWater - totalWater) > 5) {
            errors.add("总注水量不匹配：咖啡粉量${coffeeWeight}g × 粉水比${waterRatioStr} = ${expectedTotalWater}ml，但声明为${totalWater}ml")
        }

        val parsedSteps = mutableListOf<ParsedRecipeStep>()
        val stepPattern = Regex("""\[(\d+)]\s*(.+?)\s*\|\s*(\d+)秒\s*\|\s*(\d+)ml\s*\|\s*([\d.]+)%\s*\|\s*(.+)""")

        for ((idx, line) in stepLines.withIndex()) {
            val match = stepPattern.matchEntire(line)
            if (match == null) {
                errors.add("第${idx + 1}步格式错误: $line")
                continue
            }
            val (seqStr, phaseStr, durationStr, waterStr, ratioStr, instruction) = match.destructured
            val phase = when (phaseStr.trim()) {
                "闷蒸" -> BrewPhase.BLOOM
                "注水" -> BrewPhase.POUR
                "等待", "滴滤" -> BrewPhase.WAIT
                else -> {
                    errors.add("第${idx + 1}步阶段类型无效: $phaseStr")
                    BrewPhase.POUR
                }
            }
            val seq = seqStr.toIntOrNull() ?: 0
            val duration = durationStr.toIntOrNull() ?: 0
            val targetWater = waterStr.toIntOrNull() ?: 0
            val waterRatio = ratioStr.toFloatOrNull() ?: 0f

            if (totalWater > 0 && targetWater > 0) {
                val calcRatio = targetWater.toFloat() / totalWater * 100f
                if (Math.abs(calcRatio - waterRatio) > 2f) {
                    errors.add("第${idx + 1}步：注水量${targetWater}ml / 总水量${totalWater}ml = ${df.format(calcRatio)}%，但声明为${waterRatio}%")
                }
            }

            parsedSteps.add(
                ParsedRecipeStep(
                    sequence = seq,
                    phase = phase,
                    duration = duration,
                    targetWater = targetWater,
                    waterRatio = waterRatio,
                    instruction = instruction.trim()
                )
            )
        }

        if (parsedSteps.isEmpty()) {
            errors.add("没有解析到任何步骤")
        } else {
            val ratioSum = parsedSteps.sumOf { it.waterRatio.toDouble() }.toFloat()
            if (Math.abs(ratioSum - 100f) > 3f) {
                errors.add("注水比例合计${df.format(ratioSum)}%，偏离100%超过3%")
            }
        }

        if (errors.isNotEmpty()) {
            return ImportResult(false, errors = errors)
        }

        val recipe = Recipe(
            name = name ?: "导入配方",
            method = method,
            coffeeWeight = coffeeWeight,
            waterRatio = waterRatioStr ?: "1:15",
            temperature = temperature,
            isDefault = false
        )

        return ImportResult(true, recipe = recipe, steps = parsedSteps)
    }

    private fun phaseLabel(phase: BrewPhase): String = when (phase) {
        BrewPhase.BLOOM -> "闷蒸"
        BrewPhase.POUR -> "注水"
        BrewPhase.WAIT -> "等待"
        else -> phase.name
    }

    private fun calculateRatios(steps: List<RecipeStep>, totalWater: Int): List<Float> {
        if (totalWater <= 0) return steps.map { 0f }
        return steps.map { step ->
            val ratio = step.targetWater.toFloat() / totalWater * 100f
            if (step.waterRatio > 0f) step.waterRatio else ratio
        }
    }

    private fun extractValue(lines: List<String>, key: String): String? {
        for (line in lines) {
            if (line.startsWith("$key: ")) {
                return line.removePrefix("$key: ").trim()
            }
        }
        return null
    }
}