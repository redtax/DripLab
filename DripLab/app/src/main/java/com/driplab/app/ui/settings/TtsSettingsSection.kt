package com.driplab.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driplab.app.core.tts.TtsEngineInfo
import com.driplab.app.core.tts.TtsEngineManager

private enum class TestStatus { IDLE, TESTING, SUCCESS, FAILED }

private class EngineTestState(
    val engine: TtsEngineInfo,
    var status: TestStatus = TestStatus.IDLE,
    var errorDetail: String? = null
)

@Composable
fun TtsSettingsSection(ttsManager: TtsEngineManager) {
    val engines = remember { ttsManager.getAvailableEngines() }
    val selectedEngine = remember { ttsManager.getSelectedEngine() }
    val testStates = remember {
        val list = mutableStateListOf<EngineTestState>()
        engines.forEach { list.add(EngineTestState(it)) }
        if (selectedEngine != null && engines.none { it.packageName == selectedEngine }) {
            list.add(EngineTestState(TtsEngineInfo(selectedEngine, selectedEngine, false)))
        }
        list.toList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "TTS语音引擎",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "选择并测试语音引擎，测试成功后勾选即可用于冲煮时的语音提示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (testStates.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "未发现任何TTS引擎，请安装语音引擎应用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                testStates.forEachIndexed { index, testState ->
                    val isSelected = selectedEngine == testState.engine.packageName

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (testState.status == TestStatus.SUCCESS || isSelected) {
                                    val newEngine = if (isSelected) null else testState.engine.packageName
                                    ttsManager.setSelectedEngine(newEngine)
                                }
                            },
                            enabled = testState.status == TestStatus.SUCCESS || isSelected,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    testState.engine.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (testState.engine.isSystemDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("默认", fontSize = 10.sp) },
                                        modifier = Modifier.height(20.dp),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                            Text(
                                testState.engine.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        TestButton(testState, ttsManager)
                    }

                    if (testState.status == TestStatus.FAILED && testState.errorDetail != null) {
                        Row(modifier = Modifier.padding(start = 40.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                testState.errorDetail ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (index < testStates.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestButton(testState: EngineTestState, ttsManager: TtsEngineManager) {
    var localStatus by remember { mutableStateOf(testState.status) }

    when (localStatus) {
        TestStatus.IDLE -> {
            AssistChip(
                onClick = {
                    localStatus = TestStatus.TESTING
                    testState.status = TestStatus.TESTING
                    ttsManager.testEngine(testState.engine.packageName) { success, detail ->
                        localStatus = if (success) TestStatus.SUCCESS else TestStatus.FAILED
                        testState.status = localStatus
                        testState.errorDetail = detail
                    }
                },
                label = { Text("测试", fontSize = 12.sp) },
                modifier = Modifier.height(28.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
        TestStatus.TESTING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(28.dp).padding(horizontal = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("测试中", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TestStatus.SUCCESS -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(28.dp).padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("通过", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        TestStatus.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(28.dp).padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("失败", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}