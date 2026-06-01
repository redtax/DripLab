package com.driplab.app.ui.recipe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: Long = 0,
    importedJson: String? = null,
    viewModel: RecipeEditViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    LaunchedEffect(recipeId) {
        if (recipeId > 0) viewModel.loadRecipe(recipeId)
    }

    LaunchedEffect(importedJson) {
        if (!importedJson.isNullOrBlank()) viewModel.loadImportedRecipe(importedJson)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    LaunchedEffect(state.exportText) {
        state.exportText?.let { text ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("recipe", text))
            Toast.makeText(context, "配方已复制到剪贴板", Toast.LENGTH_SHORT).show()
            viewModel.clearExportText()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.isEditing) "编辑配方" else "新建配方") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "导入",
                            tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { viewModel.exportRecipe() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.saveRecipe() }) {
                        Icon(Icons.Default.Save, contentDescription = "保存",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { BasicInfoSection(state, viewModel) }
                item { StepsSection(state, viewModel) }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入配方") },
            text = {
                Column {
                    Text("请粘贴滴落间Lab配方文本：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        placeholder = { Text("滴落间Lab配方\n---\n配方名: ...") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importText.isNotBlank()) {
                        viewModel.importRecipeText(importText.trim())
                        showImportDialog = false
                        importText = ""
                    }
                }) {
                    Text("导入并编辑")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    state.importResult?.let { result ->
        if (!result.success) {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportResult() },
                title = { Text("导入失败") },
                text = {
                    Column {
                        result.errors.forEach { error ->
                            Text("• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportResult() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicInfoSection(state: RecipeEditState, viewModel: RecipeEditViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("基础信息", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.recipe.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("配方名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            var methodExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it }
            ) {
                OutlinedTextField(
                    value = methodLabel(state.recipe.method),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("冲煮方式") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = methodExpanded,
                    onDismissRequest = { methodExpanded = false }
                ) {
                    listOf(BrewMethod.POUR_OVER, BrewMethod.IMMERSION, BrewMethod.ESPRESSO, BrewMethod.AERO_PRESS).forEach { method ->
                        DropdownMenuItem(
                            text = { Text(methodLabel(method)) },
                            onClick = {
                                viewModel.updateMethod(method)
                                methodExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (state.recipe.coffeeWeight > 0f) state.recipe.coffeeWeight.toInt().toString() else "",
                    onValueChange = { viewModel.updateCoffeeWeight(it.toFloatOrNull() ?: 0f) },
                    label = { Text("咖啡粉量(g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.recipe.waterRatio,
                    onValueChange = { viewModel.updateWaterRatio(it) },
                    label = { Text("粉水比") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("1:15") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = if (state.recipe.temperature > 0) state.recipe.temperature.toString() else "",
                onValueChange = { viewModel.updateTemperature(it.toIntOrNull() ?: 0) },
                label = { Text("水温(°C)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepsSection(state: RecipeEditState, viewModel: RecipeEditViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("冲煮步骤", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { viewModel.addStep() },
                    modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加步骤", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val ratioNum = state.recipe.waterRatio.replace("1:", "").toFloatOrNull() ?: 15f
            val totalWater = state.recipe.coffeeWeight * ratioNum

            state.recipe.steps.forEachIndexed { index, step ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("步骤 ${step.sequence}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium)
                            Row {
                                if (totalWater > 0f) {
                                    val ratio = step.targetWater.toFloat() / totalWater * 100f
                                    Text("${String.format("%.0f", ratio)}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeStep(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        var phaseExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = phaseExpanded,
                            onExpandedChange = { phaseExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = phaseLabel(step.phase),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("阶段") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = phaseExpanded,
                                onDismissRequest = { phaseExpanded = false }
                            ) {
                                listOf(BrewPhase.BLOOM, BrewPhase.POUR, BrewPhase.WAIT).forEach { phase ->
                                    DropdownMenuItem(
                                        text = { Text(phaseLabel(phase)) },
                                        onClick = {
                                            viewModel.updateStep(index, step.copy(phase = phase))
                                            phaseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = step.duration.toString(),
                                onValueChange = { v -> viewModel.updateStep(index, step.copy(duration = v.toIntOrNull() ?: 0)) },
                                label = { Text("时长(秒)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = if (step.targetWater > 0) step.targetWater.toString() else "",
                                onValueChange = { v -> viewModel.updateStep(index, step.copy(targetWater = v.toIntOrNull() ?: 0)) },
                                label = { Text("注水量(ml)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = step.instruction,
                            onValueChange = { v -> viewModel.updateStep(index, step.copy(instruction = v)) },
                            label = { Text("语音指令") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

private fun methodLabel(method: BrewMethod): String = when (method) {
    BrewMethod.POUR_OVER -> "手冲"
    BrewMethod.IMMERSION -> "浸泡"
    BrewMethod.ESPRESSO -> "意式"
    BrewMethod.AERO_PRESS -> "爱乐压"
    BrewMethod.FRENCH_PRESS -> "法压"
    BrewMethod.MOKA_POT -> "摩卡"
    BrewMethod.COLD_BREW -> "冷萃"
    BrewMethod.SIPHON -> "虹吸"
}

private fun phaseLabel(phase: BrewPhase): String = when (phase) {
    BrewPhase.BLOOM -> "闷蒸"
    BrewPhase.POUR -> "注水"
    BrewPhase.WAIT -> "滴滤"
    else -> phase.name
}