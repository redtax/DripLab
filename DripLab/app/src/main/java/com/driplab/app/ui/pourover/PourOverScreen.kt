package com.driplab.app.ui.pourover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driplab.app.domain.model.BrewPhase
import com.driplab.app.ui.components.CircularTimer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PourOverScreen(viewModel: PourOverViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手冲咖啡", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrewingTimerSection(state, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            ParameterSection(state, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            RatioSection(state, viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            TemperatureSection(state, viewModel)

            Spacer(modifier = Modifier.height(24.dp))

            BrewControlSection(state, viewModel)
        }
    }
}

@Composable
private fun BrewingTimerSection(
    state: PourOverUiState,
    viewModel: PourOverViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularTimer(
                progress = state.brewState.stepProgress,
                remainingSeconds = state.brewState.stepRemainingSeconds,
                currentPhase = state.brewState.currentPhase,
                size = 200.dp
            )

            if (state.brewState.currentPhase == BrewPhase.COMPLETE) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "冲煮完成！享受你的咖啡",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ParameterSection(
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
                Text("咖啡粉", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${state.coffeeWeight.toInt()}g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            var sliderValue by remember(state.coffeeWeight) { mutableFloatStateOf(state.coffeeWeight) }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.updateCoffeeWeight(sliderValue) },
                valueRange = 5f..40f,
                steps = 34,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("注水量", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${state.waterAmount.toInt()}ml",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("粉水比", style = MaterialTheme.typography.titleSmall)
                Text(state.ratioLabel, style = MaterialTheme.typography.titleMedium)
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
            Text(
                "粉水比预设",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
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
                        Text(
                            preset.label,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            if (state.selectedRatioPreset == state.ratioPresets.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
                var customRatio by remember(state.customRatio) { mutableFloatStateOf(state.customRatio) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                    Text(
                        "${customRatio.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("60°C", style = MaterialTheme.typography.labelSmall)
                Text("建议: ${state.suggestedTemp}°C", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("100°C", style = MaterialTheme.typography.labelSmall)
            }
        }
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
        if (!state.brewState.isRunning && state.brewState.currentPhase != BrewPhase.COMPLETE) {
            Button(
                onClick = { viewModel.startBrewing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("暂停")
                }

                Button(
                    onClick = { viewModel.skipStep() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("跳过")
                }

                Button(
                    onClick = { viewModel.stopBrewing() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止")
                }
            }
        }

        if (!state.brewState.isRunning && state.brewState.currentPhase != BrewPhase.IDLE && state.brewState.currentPhase != BrewPhase.COMPLETE) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.resumeBrewing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("继续冲煮", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state.brewState.currentPhase == BrewPhase.COMPLETE) {
            Button(
                onClick = { viewModel.stopBrewing() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("重新开始", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}