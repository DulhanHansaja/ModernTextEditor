package com.example.texteditor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.texteditor.data.VersionEntity
import com.example.texteditor.data.VersionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionControlViewModel(private val repository: VersionRepository) : ViewModel() {
    private val _versions = MutableStateFlow<List<VersionEntity>>(emptyList())
    val versions: StateFlow<List<VersionEntity>> = _versions.asStateFlow()

    private val _diffContent = MutableStateFlow<String?>(null)
    val diffContent: StateFlow<String?> = _diffContent.asStateFlow()

    private val _oldDiffContent = MutableStateFlow<String?>(null)
    val oldDiffContent: StateFlow<String?> = _oldDiffContent.asStateFlow()

    private val _newDiffContent = MutableStateFlow<String?>(null)
    val newDiffContent: StateFlow<String?> = _newDiffContent.asStateFlow()

    fun setDiffContents(old: String, new: String) {
        _oldDiffContent.value = old
        _newDiffContent.value = new
    }

    fun loadVersions(fileUri: String) {
        viewModelScope.launch {
            _versions.value = repository.getVersionsForFile(fileUri)
        }
    }

    fun saveVersion(fileUri: String, content: String, label: String, baseVersionId: Int? = null) {
        viewModelScope.launch {
            repository.saveVersion(fileUri, content, label, baseVersionId)
            loadVersions(fileUri)
        }
    }

    fun reconstructVersion(versionId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val content = repository.reconstructVersion(versionId)
            content?.let { onResult(it) }
        }
    }
}

class VersionControlViewModelFactory(private val repository: VersionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VersionControlViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VersionControlViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
