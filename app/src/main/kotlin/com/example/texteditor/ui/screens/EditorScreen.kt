package com.example.texteditor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.texteditor.ui.viewmodel.EditorViewModel
import com.example.texteditor.editor.SyntaxHighlighter

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onShowHistory: () -> Unit,
    onSaveVersion: (String) -> Unit,
    onFileRenamed: (String, String) -> Unit
) {
    val fileName by viewModel.fileName.collectAsState()
    val content by viewModel.content.collectAsState()
    val isReadOnly by viewModel.isReadOnly.collectAsState()
    val isWordWrap by viewModel.isWordWrap.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val isMarkdown by viewModel.isMarkdown.collectAsState()
    val keywords by viewModel.keywords.collectAsState()
    val searchIndices by viewModel.searchResultIndices.collectAsState()
    val currentSearchIndex by viewModel.currentSearchIndex.collectAsState()
    val isCaseSensitive by viewModel.isCaseSensitive.collectAsState()
    val activeSearchQuery by viewModel.searchQuery.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var versionLabel by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadKeywords(context)
    }

    DisposableEffect(Unit) {
        viewModel.startAutoCache(context)
        onDispose {
            viewModel.stopAutoCache()
        }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
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
            viewModel.saveFile(context, it.toString())
            viewModel.setFile(name, it.toString(), viewModel.content.value)
            onFileRenamed(name, it.toString())
        }
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Save Version") },
            text = {
                TextField(
                    value = versionLabel,
                    onValueChange = { versionLabel = it },
                    label = { Text("Version Label") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveVersion(versionLabel)
                    showVersionDialog = false
                    versionLabel = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "${fileName ?: "New File"}${if (isDirty) "*" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isReadOnly) {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                        IconButton(onClick = { viewModel.saveFile(context) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (!isReadOnly) {
                                DropdownMenuItem(
                                    text = { Text("Save Version") },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                    onClick = {
                                        showVersionDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Version History") },
                                leadingIcon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                                onClick = {
                                    onShowHistory()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showSearch) "Hide Search" else "Search & Replace") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    showSearch = !showSearch
                                    showMenu = false
                                }
                            )
                            if (isMarkdown) {
                                DropdownMenuItem(
                                    text = { Text(if (showPreview) "Edit Mode" else "Markdown Preview") },
                                    leadingIcon = { Icon(if (showPreview) Icons.Default.Edit else Icons.Default.Visibility, contentDescription = null) },
                                    onClick = {
                                        showPreview = !showPreview
                                        showMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isWordWrap) "Disable Word Wrap" else "Enable Word Wrap") },
                                onClick = {
                                    viewModel.toggleWordWrap()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isReadOnly) "Unlock File" else "Lock (Read-Only)") },
                                onClick = {
                                    viewModel.toggleReadOnly()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save As") },
                                onClick = {
                                    saveAsLauncher.launch(fileName ?: "Untitled.txt")
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (showPreview && isMarkdown) {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            )
                        )
                    }
                }
            } else {
                if (showSearch) {
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .shadow(4.dp, MaterialTheme.shapes.medium),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    SearchReplaceBar(
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        replaceQuery = replaceQuery,
                        onReplaceQueryChange = { replaceQuery = it }
                    )
                }
            }
            
                val scrollState = rememberScrollState()
                BasicTextField(
                    value = content,
                    onValueChange = { if (!isReadOnly) viewModel.onContentChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .let { 
                            if (!isWordWrap) it.horizontalScroll(scrollState) else it 
                        },
                    readOnly = isReadOnly,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = SyntaxHighlighter(
                        keywords = keywords,
                        isMarkdown = isMarkdown,
                        searchQuery = activeSearchQuery,
                        searchIndices = searchIndices,
                        currentSearchIndex = currentSearchIndex
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (content.isEmpty()) {
                                Text("Start typing...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SearchReplaceBar(
    viewModel: EditorViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit
) {
    val searchResults by viewModel.searchResultIndices.collectAsState()
    val currentIndex by viewModel.currentSearchIndex.collectAsState()
    val isCaseSensitive by viewModel.isCaseSensitive.collectAsState()

    Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    viewModel.search(it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("Search") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleCaseSensitive() }) {
                        Icon(
                            if (isCaseSensitive) Icons.Default.FormatSize else Icons.Default.TextFields,
                            contentDescription = "Case Sensitive",
                            tint = if (isCaseSensitive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            IconButton(onClick = { viewModel.previousSearchMatch() }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
            }
            IconButton(onClick = { viewModel.nextSearchMatch() }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
            }
            Text("${if (currentIndex != -1) currentIndex + 1 else 0}/${searchResults.size}")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = replaceQuery,
                onValueChange = onReplaceQueryChange,
                modifier = Modifier.weight(1f),
                label = { Text("Replace") },
                singleLine = true
            )
            Button(onClick = { viewModel.replace(searchQuery, replaceQuery) }) {
                Text("Replace")
            }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { viewModel.replaceAll(searchQuery, replaceQuery) }) {
                Text("All")
            }
        }
    }
}
