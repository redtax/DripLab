package com.driplab.app.ui.pourover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.domain.model.Recipe
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PourOverScreen(viewModel: PourOverViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val methodName = methodDisplayName(state.currentMethod)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = methodName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.height(40.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrewControlSection(state, viewModel)

            Spacer(modifier = Modifier.height(8.dp))

            BrewingTimerSection(state)

            Spacer(modifier = Modifier.height(8.dp))

            if (state.voicePrompt.isNotEmpty() && state.brewState.isRunning) {
                VoicePromptBanner(state.voicePrompt)
                Spacer(modifier = Modifier.height(8.dp))
            }

            CoffeeAndWaterSection(state, viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            RatioSection(state, viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            TemperatureSection(state, viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            RecipeSelectorSection(state, viewModel)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (state.showRecipeConfirmDialog && state.pendingRecipe != null) {
        RecipeConfirmDialog(
            recipe = state.pendingRecipe!!,
            onConfirm = { viewModel.confirmPendingRecipe() },
            onDismiss = { viewModel.dismissRecipeConfirmDialog() }
        )
    }
}

@Composable
private fun BrewControlSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.brewState.currentPhase == BrewPhase.IDLE && !state.brewState.isComplete) {
            Button(
                onClick = { viewModel.startBrewing() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始冲煮", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state.brewState.isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.pauseBrewing() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("暂停")
                }
                Button(
                    onClick = { viewModel.advanceToNextStep() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下一步")
                }
                Button(
                    onClick = { viewModel.stopBrewing() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止")
                }
            }
        }

        if (state.brewState.isPaused) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.resumeBrewing() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("继续", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = { viewModel.stopBrewing() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("停止", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (state.brewState.currentPhase == BrewPhase.COMPLETE) {
            Button(
                onClick = { viewModel.stopBrewing() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("重新开始", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BrewingTimerSection(
    state: PourOverUiState
) {
    val bs = state.brewState
    val isActive = bs.isRunning || bs.isPaused

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "计时区",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularClock(
                    value = formatElapsedTime(bs.elapsedSeconds),
                    label = "总用时",
                    isActive = isActive,
                    accentColor = MaterialTheme.colorScheme.onSurface
                )
                CircularClock(
                    value = if (isActive) formatElapsedTime(bs.stepRemainingSeconds) else "--:--",
                    label = "本段倒计时",
                    isActive = isActive,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = phaseDisplayName(bs.currentPhase),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${bs.currentStepTargetWater}ml",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "本段注水量",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${bs.targetWater}ml",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "需注水到",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (bs.currentPhase == BrewPhase.IDLE && !bs.isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "准备冲煮",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "设定参数，点击上方开始按钮",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bs.currentPhase == BrewPhase.COMPLETE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "冲煮完成！总用时: ${formatElapsedTime(bs.elapsedSeconds)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "笔记已自动保存",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CircularClock(
    value: String,
    label: String,
    isActive: Boolean,
    accentColor: Color
) {
    val bgColor = if (isActive) {
        accentColor.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val borderColor = if (isActive) {
        accentColor.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VoicePromptBanner(prompt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "\uD83D\uDD0A", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoffeeAndWaterSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(3f).fillMaxHeight().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${String.format("%.1f", state.coffeeWeight)}g",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "咖啡粉",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ScrollWheelSelector(
                    value = state.gearValue,
                    onValueChange = { newValue ->
                        val rounded = (newValue * 10).toInt() / 10f
                        viewModel.updateCoffeeWeight(rounded)
                    },
                    onValueChangeEnd = { value ->
                        viewModel.lockGearValue(value)
                        viewModel.playClickSound()
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.waterAmount.toInt()}ml",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "总注水量",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.ratioLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "粉水比",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScrollWheelSelector(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeEnd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val currentValue by rememberUpdatedState(value)
    val textColor = MaterialTheme.colorScheme.onSurface
    val textColorDim = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val labelStyle = TextStyle(
        fontSize = 9.sp,
        color = textColorDim,
        textAlign = TextAlign.Center
    )
    val valueStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { onValueChangeEnd(currentValue) }
                ) { _, dragAmount ->
                    val delta = -(dragAmount * 0.03f)
                    val newValue = (currentValue + delta).coerceIn(5f, 150f)
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val wheelWidth = size.width
            val wheelHeight = size.height
            val centerX = wheelWidth / 2f
            val centerY = wheelHeight / 2f

            val tickSpacingDp = 6.dp
            val tickSpacing = tickSpacingDp.toPx()
            val longTickWidth = 14.dp.toPx()
            val shortTickWidth = 7.dp.toPx()

            val valueRange = 150f
            val totalTicks = (valueRange * 10).toInt()
            val normalizedPos = (value - 5f) / valueRange
            val wheelOffset = ((0.5f - normalizedPos) * totalTicks * tickSpacing)

            val minGrams = 5
            val maxGrams = 150

            drawRect(
                color = accentColor.copy(alpha = 0.12f),
                topLeft = Offset(0f, centerY - tickSpacing + 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(wheelWidth, (tickSpacing * 2f) - 2.dp.toPx())
            )

            drawLine(
                color = accentColor.copy(alpha = 0.6f),
                start = Offset(0f, centerY - tickSpacing),
                end = Offset(wheelWidth, centerY - tickSpacing),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = accentColor.copy(alpha = 0.6f),
                start = Offset(0f, centerY + tickSpacing),
                end = Offset(wheelWidth, centerY + tickSpacing),
                strokeWidth = 1.dp.toPx()
            )

            for (gram in minGrams..maxGrams) {
                val tickIndex = ((gram - 5f) * 10).toInt()
                val tickY = centerY + wheelOffset + tickIndex * tickSpacing

                if (tickY < -tickSpacing * 5 || tickY > wheelHeight + tickSpacing * 5) continue

                val isMajorTick = true
                val isInCenter = kotlin.math.abs(tickY - centerY) < tickSpacing

                val textResult = textMeasurer.measure(
                    text = "${gram}",
                    style = if (isInCenter) valueStyle else labelStyle
                )

                val tickX = centerX - longTickWidth - 4.dp.toPx()
                drawLine(
                    color = if (isInCenter) accentColor else tickColor,
                    start = Offset(tickX, tickY),
                    end = Offset(tickX + longTickWidth, tickY),
                    strokeWidth = if (isInCenter) 1.5.dp.toPx() else 1.dp.toPx()
                )

                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        tickX + longTickWidth + 6.dp.toPx(),
                        tickY - textResult.size.height / 2f
                    )
                )

                for (sub in 1..9) {
                    val subTickY = tickY - sub * tickSpacing / 10f
                    if (subTickY < -tickSpacing || subTickY > wheelHeight + tickSpacing) continue
                    val subIsInCenter = kotlin.math.abs(subTickY - centerY) < tickSpacing / 2f
                    drawLine(
                        color = if (subIsInCenter) accentColor.copy(alpha = 0.5f) else tickColor.copy(alpha = 0.4f),
                        start = Offset(centerX, subTickY),
                        end = Offset(centerX + shortTickWidth, subTickY),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
private fun RatioSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("粉水比预设", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.ratioPresets.forEachIndexed { index, preset ->
                    FilledTonalButton(
                        onClick = { viewModel.selectRatioPreset(index) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (state.selectedRatioPreset == index && index < state.ratioPresets.size - 1) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(preset.label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            if (state.selectedRatioPreset == state.ratioPresets.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
                var customRatio by remember(state.customRatio) { mutableFloatStateOf(state.customRatio) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("自定义比例: 1:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = customRatio,
                        onValueChange = { customRatio = it },
                        onValueChangeFinished = { viewModel.updateCustomRatio(customRatio) },
                        valueRange = 10f..25f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("${customRatio.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TemperatureSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("水温", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${state.temperature}°C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            var tempValue by remember(state.temperature) { mutableFloatStateOf(state.temperature.toFloat()) }
            Slider(
                value = tempValue,
                onValueChange = { tempValue = it },
                onValueChangeFinished = { viewModel.updateTemperature(tempValue.toInt()) },
                valueRange = 60f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("60°C", style = MaterialTheme.typography.labelSmall)
                Text("建议: ${state.suggestedTemp}°C", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("100°C", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RecipeSelectorSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("冲煮配方", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Box {
                    Row(
                        modifier = Modifier.clickable { viewModel.toggleRecipeDropdown() }.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.selectedRecipe?.name ?: "请选择配方",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(
                        expanded = state.showRecipeDropdown,
                        onDismissRequest = { viewModel.toggleRecipeDropdown() }
                    ) {
                        state.allPourOverRecipes.forEach { recipe ->
                            DropdownMenuItem(
                                text = { Text(recipe.name) },
                                onClick = { viewModel.showRecipeConfirmDialog(recipe) }
                            )
                        }
                        if (state.allPourOverRecipes.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("暂无配方") },
                                onClick = { viewModel.toggleRecipeDropdown() }
                            )
                        }
                    }
                }
            }

            state.selectedRecipe?.let { recipe ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TagChip("${recipe.coffeeWeight}g")
                    TagChip(recipe.waterRatio)
                    TagChip("${recipe.temperature}°C")
                }
                Spacer(modifier = Modifier.height(8.dp))
                recipe.steps.forEach { step ->
                    Text(
                        text = "${step.sequence}. ${step.instruction} (${step.duration}s, ${step.targetWater}ml)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun RecipeConfirmDialog(
    recipe: Recipe,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择冲煮配方") },
        text = {
            Column {
                Text("是否将「${recipe.name}」设置为当前冲煮配方？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "粉量: ${recipe.coffeeWeight}g | 比例: ${recipe.waterRatio} | 水温: ${recipe.temperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun methodDisplayName(method: BrewMethod): String = when (method) {
    BrewMethod.POUR_OVER -> "手冲咖啡"
    BrewMethod.FRENCH_PRESS -> "法压壶"
    BrewMethod.AERO_PRESS -> "爱乐压"
    BrewMethod.MOKA_POT -> "摩卡壶"
    BrewMethod.COLD_BREW -> "冷萃"
    BrewMethod.SIPHON -> "虹吸壶"
}

private fun phaseDisplayName(phase: BrewPhase): String = when (phase) {
    BrewPhase.IDLE -> "准备开始"
    BrewPhase.BLOOM -> "闷蒸"
    BrewPhase.POUR -> "注水"
    BrewPhase.WAIT -> "等待滴滤"
    BrewPhase.STEEP -> "浸泡"
    BrewPhase.PRESS -> "压滤"
    BrewPhase.COMPLETE -> "完成"
}

private fun formatElapsedTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatPhaseTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d分%02d秒".format(minutes, seconds)
}