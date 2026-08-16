package com.example.texteditor.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.texteditor.data.FileEntity
import com.example.texteditor.data.FileRepository
import com.example.texteditor.history.AutoCacheManager
import com.example.texteditor.history.RecoveredFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: FileRepository) : ViewModel() {

    val recentFiles: StateFlow<List<FileEntity>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _recoveredFiles = MutableStateFlow<List<RecoveredFile>>(emptyList())
    val recoveredFiles: StateFlow<List<RecoveredFile>> = _recoveredFiles.asStateFlow()

    fun checkForRecoveredFiles(context: Context) {
        viewModelScope.launch {
            val files = AutoCacheManager(context).getRecoverableFiles()
            android.util.Log.d("HomeViewModel", "Detected ${files.size} recovered files")
            _recoveredFiles.value = files
        }
    }

    fun discardRecoveredFile(context: Context, recoveredFile: RecoveredFile) {
        AutoCacheManager(context).clearCache(recoveredFile.hash)
        _recoveredFiles.value = _recoveredFiles.value.filter { it.hash != recoveredFile.hash }
    }

    fun addRecentFile(name: String, uri: String) {
        viewModelScope.launch {
            repository.addOrUpdateFile(name, uri)
        }
    }
}

class HomeViewModelFactory(private val repository: FileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
