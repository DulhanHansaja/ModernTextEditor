package com.example.texteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.texteditor.ui.screens.*
import com.example.texteditor.ui.theme.TextEditorTheme
import com.example.texteditor.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextEditorTheme {
                TextEditorMain(this.application as com.example.texteditor.TextEditorApp)
            }
        }
    }
}

@Composable
fun TextEditorMain(app: com.example.texteditor.TextEditorApp) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(app.repository))
    val editorViewModel: EditorViewModel = viewModel(factory = EditorViewModelFactory(app.repository))
    val vcViewModel: VersionControlViewModel = viewModel(factory = VersionControlViewModelFactory(app.versionRepository))
    val context = LocalContext.current

    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    pendingNavigation?.invoke()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onNewFile = {
                    val navigateAction = {
                        editorViewModel.setFile(null, null, "")
                        navController.navigate("editor")
                    }
                    if (editorViewModel.isDirty.value) {
                        pendingNavigation = navigateAction
                        showDiscardDialog = true
                    } else {
                        navigateAction()
                    }
                },
                onOpenFile = { name, uri, content ->
                    val navigateAction = {
                        if (content != null) {
                            editorViewModel.setFile(name, uri, content)
                        } else {
                            editorViewModel.loadFile(context, name, uri)
                        }
                        navController.navigate("editor")
                    }
                    if (editorViewModel.isDirty.value) {
                        pendingNavigation = navigateAction
                        showDiscardDialog = true
                    } else {
                        navigateAction()
                    }
                },
                onSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("editor") {
            EditorScreen(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() },
                onShowHistory = {
                    navController.navigate("history?uri=${editorViewModel.fileUri.value ?: ""}")
                },
                onSaveVersion = { label ->
                    editorViewModel.fileUri.value?.let { uri ->
                        vcViewModel.saveVersion(uri, editorViewModel.content.value, label, editorViewModel.baseVersionId.value)
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "history?uri={uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            VersionHistoryScreen(
                viewModel = vcViewModel,
                fileUri = uri,
                onBack = { navController.popBackStack() },
                onRollback = { content, versionId ->
                    editorViewModel.onRollback(content, versionId)
                },
                onCompare = { old, new ->
                    // For simplicity, we'll store these in the ViewModel and navigate
                    // because passing large strings in URIs is bad practice.
                    // But for this task, we can use a temporary state or just pass small strings.
                    // Actually, let's just pass them as arguments for now.
                    // Navigation arguments have limits, so better use a shared state in ViewModel.
                    vcViewModel.setDiffContents(old, new)
                    navController.navigate("diff")
                }
            )
        }
        composable("diff") {
            val oldContent by vcViewModel.oldDiffContent.collectAsState()
            val newContent by vcViewModel.newDiffContent.collectAsState()
            DiffScreen(
                oldContent = oldContent ?: "",
                newContent = newContent ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
