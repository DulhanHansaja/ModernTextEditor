package com.example.texteditor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.texteditor.data.VersionEntity
import com.example.texteditor.ui.viewmodel.VersionControlViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    viewModel: VersionControlViewModel,
    fileUri: String,
    onBack: () -> Unit,
    onRollback: (String, Int) -> Unit,
    onCompare: (String, String) -> Unit
) {
    val versions by viewModel.versions.collectAsState()
    var selectedVersionIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(fileUri) {
        viewModel.loadVersions(fileUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Version History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedVersionIds.size == 2) {
                        Button(
                            onClick = {
                                val ids = selectedVersionIds.toList()
                                viewModel.reconstructVersion(ids[0]) { old ->
                                    viewModel.reconstructVersion(ids[1]) { new ->
                                        onCompare(old, new)
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Difference, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Compare")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (versions.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No versions saved yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(versions) { version ->
                    VersionCard(
                        version = version,
                        isSelected = selectedVersionIds.contains(version.id),
                        onSelect = {
                            selectedVersionIds = if (selectedVersionIds.contains(version.id)) {
                                selectedVersionIds - version.id
                            } else {
                                if (selectedVersionIds.size < 2) selectedVersionIds + version.id else selectedVersionIds
                            }
                        },
                        onRollback = {
                            viewModel.reconstructVersion(version.id) { content ->
                                onRollback(content, version.id)
                                onBack()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VersionCard(
    version: VersionEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRollback: () -> Unit
) {
    OutlinedCard(
        onClick = onSelect,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected, 
                onCheckedChange = { onSelect() }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.versionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val date = Date(version.timestamp)
                val format = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                Text(
                    text = format.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRollback) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Rollback")
            }
        }
    }
}
