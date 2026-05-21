package com.driplab.app.ui.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("冲煮笔记", fontWeight = FontWeight.Bold) },
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
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedMethod == null,
                    onClick = { viewModel.filterByMethod(null) },
                    label = { Text("全部") }
                )
                BrewMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.selectedMethod == method,
                        onClick = { viewModel.filterByMethod(method) },
                        label = { Text(method.displayName) }
                    )
                }
            }

            val filteredNotes = viewModel.getFilteredNotes()

            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "暂无冲煮记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "完成手冲冲煮后会自动记录笔记",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(note = note, onDelete = { viewModel.deleteNote(note) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: BrewNote,
    onDelete: () -> Unit
) {
    val dateFormat = rememberDateFormat()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    note.method.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                dateFormat.format(Date(note.brewDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "冲泡用时: ${formatDuration(note.totalTime)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (note.acidity != null || note.sweetness != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val flavors = buildString {
                    note.acidity?.let { append("酸度: $it ") }
                    note.sweetness?.let { append("甜度: $it ") }
                    note.bitterness?.let { append("苦度: $it ") }
                    note.body?.let { append("醇厚: $it") }
                }
                Text(flavors, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (note.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return androidx.compose.runtime.remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}分${seconds}秒"
}