package com.driplab.app.ui.recipe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driplab.app.domain.model.BrewPhase
import com.google.gson.GsonBuilder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: Long = 0,
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(recipeId) {
        if (recipeId > 0) {
            viewModel.loadRecipe(recipeId)
        }
    }

    DisposableEffect(context) {
        val instance = TextToSpeech(context) { _ -> }
        instance.language = Locale.CHINESE
        tts = instance
        viewModel.initTts(instance)
        onDispose { instance.shutdown() }
    }

    if (state.saveSuccess) {
        Toast.makeText(context, "配方已保存", Toast.LENGTH_SHORT).show()
        viewModel.dismissSaveSuccess()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) "新建手冲配方" else "编辑手冲配方",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveRecipe() },
                        enabled = !state.isSaving && state.name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("配方名称") },
                    placeholder = { Text("例如：我的专属手冲") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("咖啡粉量", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                                    .clickable { viewModel.adjustCoffeeDown() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "减少",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "${"%.1f".format(Locale.getDefault(), state.coffeeWeight)}g",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                                    .clickable { viewModel.adjustCoffeeUp() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "增加",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("粉水比", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.ratioPresets.forEachIndexed { index, preset ->
                                Button(
                                    onClick = { viewModel.selectRatioPreset(index) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.selectedRatioPreset == index && index < state.ratioPresets.size - 1) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Text(preset.label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                        if (state.selectedRatioPreset == state.ratioPresets.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            var customRatio by remember(state.waterRatio) { mutableFloatStateOf(state.waterRatio.toFloat()) }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("1:", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = customRatio,
                                    onValueChange = { customRatio = it },
                                    onValueChangeFinished = { viewModel.updateCustomRatio(customRatio.toInt()) },
                                    valueRange = 8f..25f,
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

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier.weight(7f).fillMaxHeight().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${state.temperature}°C",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("水温", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                                        .clickable { viewModel.adjustTempUp() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "升温",
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                                        .clickable { viewModel.adjustTempDown() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "降温",
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("分段策略", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("共${state.steps.size}段", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            itemsIndexed(state.steps) { index, step ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("第${index + 1}段", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { viewModel.moveStepUp(index) },
                                    enabled = index > 0, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移",
                                        modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.moveStepDown(index) },
                                    enabled = index < state.steps.size - 1, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移",
                                        modifier = Modifier.size(20.dp))
                                }
                                if (state.steps.size > 1) {
                                    IconButton(onClick = { viewModel.removeStep(index) },
                                        modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        var phaseExpanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { phaseExpanded = true }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(phaseDisplayName(step.phase))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = phaseExpanded,
                                onDismissRequest = { phaseExpanded = false }
                            ) {
                                val handPourPhases = listOf(BrewPhase.BLOOM, BrewPhase.POUR, BrewPhase.WAIT)
                                handPourPhases.forEach { phase ->
                                    DropdownMenuItem(
                                        text = { Text(phaseDisplayName(phase)) },
                                        onClick = {
                                            viewModel.updateStepPhase(index, phase)
                                            phaseExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (step.duration > 0) step.duration.toString() else "",
                                onValueChange = { v -> viewModel.updateStepDuration(index, v.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                                label = { Text("时长(秒)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (step.targetWater > 0) step.targetWater.toString() else "",
                                onValueChange = { v -> viewModel.updateStepWater(index, v.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                                label = { Text("注水量(ml)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            OutlinedTextField(
                                value = step.instruction,
                                onValueChange = { viewModel.updateStepInstruction(index, it) },
                                label = { Text("语音提示文本") },
                                placeholder = { Text("例如：注入45g水，中心画圈，闷蒸开始") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )
                            IconButton(
                                onClick = { viewModel.previewInstruction(index) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "预览语音",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.addStep() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加分段")
                }
            }

            item {
                Button(
                    onClick = {
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        val recipeDto = com.driplab.app.data.repository.RecipeRepositoryImpl.RecipeDto(
                            name = state.name,
                            method = "POUR_OVER",
                            coffeeWeight = state.coffeeWeight,
                            waterRatio = state.ratioLabel,
                            temperature = state.temperature,
                            steps = state.steps.map { step ->
                                com.driplab.app.data.repository.RecipeRepositoryImpl.StepDto(
                                    phase = step.phase.name,
                                    duration = step.duration,
                                    targetWater = step.targetWater,
                                    instruction = step.instruction.ifBlank { null }
                                )
                            }
                        )
                        val json = gson.toJson(ExportWrapper(
                            version = "1.0",
                            exportDate = System.currentTimeMillis(),
                            recipes = listOf(recipeDto)
                        ))
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("recipe", json))
                        Toast.makeText(context, "配方JSON已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.name.isNotBlank()
                ) {
                    Text("导出配方JSON")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private data class ExportWrapper(
    val version: String,
    val exportDate: Long,
    val recipes: List<com.driplab.app.data.repository.RecipeRepositoryImpl.RecipeDto>
)

private fun phaseDisplayName(phase: BrewPhase): String = when (phase) {
    BrewPhase.BLOOM -> "闷蒸"
    BrewPhase.POUR -> "注水"
    BrewPhase.WAIT -> "等待/滴滤"
    else -> phase.name
}