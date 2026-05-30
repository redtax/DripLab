package com.driplab.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.domain.model.AlertMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            ThemeSection(state.theme, state.darkTheme, viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            AlertModeSection(state.alertMode, state.bgMusicUri, viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            TtsSettingsSection(ttsManager = viewModel.ttsEngineManager)
            Spacer(modifier = Modifier.height(16.dp))
            AboutSection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSection(
    selectedTheme: DripTheme,
    darkTheme: Boolean,
    viewModel: SettingsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("主题配色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DripTheme.entries.forEach { theme ->
                    ThemeChip(
                        theme = theme,
                        isSelected = theme == selectedTheme,
                        onClick = { viewModel.selectTheme(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("深色模式", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme() }
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    theme: DripTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = when (theme) {
        DripTheme.CLASSIC -> androidx.compose.ui.graphics.Color(0xFF8B5A2B)
        DripTheme.DARK_ROAST -> androidx.compose.ui.graphics.Color(0xFF1A1A2E)
        DripTheme.MINT_COLD_BREW -> androidx.compose.ui.graphics.Color(0xFF2C5F5D)
        DripTheme.OAT_LATTE -> androidx.compose.ui.graphics.Color(0xFF6B5B4F)
        DripTheme.MOCHA_GRADIENT -> androidx.compose.ui.graphics.Color(0xFF4A2C2A)
    }

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.height(20.dp).width(20.dp)) {
                drawCircle(color = chipColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(theme.displayName, style = MaterialTheme.typography.bodyMedium)
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, contentDescription = null,
                    modifier = Modifier.height(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AlertModeSection(
    selectedMode: AlertMode,
    bgMusicUri: String,
    viewModel: SettingsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("提示模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            AlertMode.entries.forEach { mode ->
                val desc = buildString {
                    append(if (mode.hasSound) "有声音" else "无声音")
                    append(" · ")
                    append(if (mode.hasVibration) "有震动" else "无震动")
                    if (mode.hasBgMusic) append(" · 背景音")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAlertMode(mode) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (mode == selectedMode) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (selectedMode.hasBgMusic) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("背景音", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val bgOptions = listOf("咖啡厅环境音", "雨声白噪音", "轻柔爵士")
                var selectedBg by remember { mutableStateOf(if (bgMusicUri.isEmpty()) -1 else -1) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bgOptions.forEachIndexed { index, label ->
                        AssistChip(
                            onClick = {
                                selectedBg = index
                                viewModel.setBgMusicUri("preset:$label")
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedBg == index) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val filePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { viewModel.setBgMusicUri(it.toString()) }
                }
                FilledTonalButton(onClick = { filePicker.launch("audio/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传自定义背景音")
                }
                if (bgMusicUri.startsWith("file") || bgMusicUri.startsWith("content")) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("已选择自定义背景音", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("滴落间Lab v1.0.0", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("DripLab - 咖啡冲煮助手、Redtax 制作", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("专为咖啡爱好者打造的多方法冲煮辅助工具",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}