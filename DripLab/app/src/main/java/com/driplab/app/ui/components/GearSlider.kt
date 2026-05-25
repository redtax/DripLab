package com.driplab.app.ui.components

import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GearSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    steps: Int = 20
) {
    var currentValue by remember { mutableFloatStateOf(value) }

    val normalized = (currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val sweepAngle = normalized * 300f

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.toPx() / 2, size.toPx() / 2)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y
                        val angle = Math.toDegrees(
                            Math.atan2(dy.toDouble(), dx.toDouble())
                        ).toFloat()
                        val adjustedAngle = ((angle + 210f + 360f) % 360f).coerceIn(0f, 300f)
                        val normalizedValue = adjustedAngle / 300f
                        val raw = valueRange.start + normalizedValue * (valueRange.endInclusive - valueRange.start)
                        val stepped = (raw * steps).roundToInt() / steps.toFloat()
                        currentValue = stepped.coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(currentValue)
                    }
                }
        ) {
            val strokeWidth = size.toPx() * 0.08f
            val radius = (size.toPx() - strokeWidth) / 2
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = surfaceVariant,
                startAngle = 210f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            drawArc(
                color = primaryColor,
                startAngle = 210f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            val indicatorAngle = Math.toRadians((210f + sweepAngle).toDouble())
            val indicatorX = center.x + (radius - strokeWidth / 4) * Math.cos(indicatorAngle)
            val indicatorY = center.y + (radius - strokeWidth / 4) * Math.sin(indicatorAngle)
            drawCircle(
                color = primaryColor,
                radius = strokeWidth * 0.8f,
                center = Offset(indicatorX.toFloat(), indicatorY.toFloat())
            )
        }
    }
}