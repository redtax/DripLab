package com.driplab.app.core.calculator

data class BrewCalculation(
    val coffeeWeight: Float,
    val ratio: Float,
    val waterAmount: Float,
    val ratioLabel: String,
    val suggestedTemp: Int
)

object BrewCalculator {

    data class RatioPreset(
        val label: String,
        val ratio: Float,
        val description: String
    )

    val ratioPresets = listOf(
        RatioPreset("1:15", 15f, "浅烘推荐"),
        RatioPreset("1:16", 16f, "中烘推荐"),
        RatioPreset("1:17", 17f, "深烘推荐"),
        RatioPreset("自定义", 16f, "自行设定比例")
    )

    data class TempRange(val min: Int, val max: Int, val label: String)

    val tempRanges = listOf(
        TempRange(90, 93, "浅烘 90-93°C"),
        TempRange(88, 91, "中烘 88-91°C"),
        TempRange(85, 88, "深烘 85-88°C")
    )

    fun calculate(coffeeWeight: Float, ratio: Float): BrewCalculation {
        val waterAmount = coffeeWeight * ratio
        val ratioLabel = "1:${ratio.toInt()}"
        val suggestedTemp = when {
            ratio <= 15f -> 92
            ratio <= 16f -> 90
            else -> 87
        }
        return BrewCalculation(
            coffeeWeight = coffeeWeight,
            ratio = ratio,
            waterAmount = waterAmount,
            ratioLabel = ratioLabel,
            suggestedTemp = suggestedTemp
        )
    }

    fun calculateWaterForStep(totalWater: Float, stepRatio: Float): Float {
        return totalWater * stepRatio / 100f
    }
}