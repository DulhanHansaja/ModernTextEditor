package com.example.texteditor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.texteditor.data.FileEntity
import com.example.texteditor.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewFile: () -> Unit,
    onOpenFile: (name: String, uri: String, content: String?) -> Unit,
    onSettings: () -> Unit
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val recoveredFiles by viewModel.recoveredFiles.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkForRecoveredFiles(context)
    }

    if (recoveredFiles.isNotEmpty()) {
        val recoveredFile = recoveredFiles.first()
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Recovered File Found") },
            text = { Text("A temporary copy of '${recoveredFile.name}' was found. Would you like to restore it?") },
            confirmButton = {
                Button(onClick = {
                    onOpenFile(recoveredFile.name, recoveredFile.uri ?: "", recoveredFile.content)
                    viewModel.discardRecoveredFile(context, recoveredFile)
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardRecoveredFile(context, recoveredFile)
                }) {
                    Text("Discard")
                }
            }
        )
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val name = try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else "Unknown"
                } ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
            viewModel.addRecentFile(name, it.toString())
            onOpenFile(name, it.toString(), null)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Text Editor", fontWeight = FontWeight.Bold)
                        Text("Draft your ideas anywhere", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewFile,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New File") }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedButton(
                onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Document")
            }
            
            Text(
                "Recent Files",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (recentFiles.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No recent files",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(recentFiles) { file ->
                        RecentFileCard(file = file, onClick = { onOpenFile(file.name, file.uri, null) })
                    }
                }
            }
        }
    }
}

@Composable
fun RecentFileCard(file: FileEntity, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.name.endsWith(".md", true)) Icons.Default.Description else Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                val date = Date(file.lastOpened)
                val format = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                Text(
                    text = "Opened ${format.format(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
