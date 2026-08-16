package com.example.texteditor.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.texteditor.data.FileRepository
import com.example.texteditor.history.AutoCacheManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.Stack

enum class EditType { INSERT, DELETE, REPLACE }

data class EditOperation(
    val type: EditType,
    val position: Int,
    val oldText: String,
    val newText: String,
    val timestamp: Long = System.currentTimeMillis()
)

class EditorViewModel(private val repository: FileRepository) : ViewModel() {
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)
    val fileName: StateFlow<String?> = _fileName.asStateFlow()

    private val _fileUri = MutableStateFlow<String?>(null)
    val fileUri: StateFlow<String?> = _fileUri.asStateFlow()

    private val _baseVersionId = MutableStateFlow<Int?>(null)
    val baseVersionId: StateFlow<Int?> = _baseVersionId.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

    private val _isWordWrap = MutableStateFlow(true)
    val isWordWrap: StateFlow<Boolean> = _isWordWrap.asStateFlow()

    private val _keywords = MutableStateFlow<List<String>>(emptyList())
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    val isMarkdown: StateFlow<Boolean> = fileName.map { it?.endsWith(".md", ignoreCase = true) == true }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val undoStack = Stack<EditOperation>()
    private val redoStack = Stack<EditOperation>()

    private var isUndoRedoAction = false
    private var autoCacheJob: Job? = null

    fun startAutoCache(context: Context) {
        val autoCacheManager = AutoCacheManager(context)
        autoCacheJob?.cancel()
        autoCacheJob = viewModelScope.launch {
            while (isActive) {
                delay(10000)
                val currentName = _fileName.value ?: "Untitled"
                val currentContent = _content.value
                if (_isDirty.value) {
                    autoCacheManager.saveCache(currentName, _fileUri.value, currentContent)
                }
            }
        }
    }

    fun stopAutoCache() {
        autoCacheJob?.cancel()
        autoCacheJob = null
    }

    fun clearAutoCache(context: Context) {
        _fileName.value?.let { AutoCacheManager(context).clearCache(it.hashCode().toString()) }
    }

    fun loadKeywords(context: Context) {
        if (_keywords.value.isNotEmpty()) return
        try {
            val json = context.assets.open("kotlin_keywords.json").bufferedReader().use { it.readText() }
            val keywordsList = JSONObject(json).getJSONArray("keywords")
            val list = mutableListOf<String>()
            for (i in 0 until keywordsList.length()) {
                list.add(keywordsList.getString(i))
            }
            _keywords.value = list
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun setFile(name: String?, uri: String?, content: String, baseVersionId: Int? = null) {
        _fileName.value = name
        _fileUri.value = uri
        _content.value = content
        _baseVersionId.value = baseVersionId
        _isDirty.value = false
        
        // Fetch persisted Read-Only state
        viewModelScope.launch {
            if (uri != null) {
                val file = repository.getFileByUri(uri)
                _isReadOnly.value = file?.isReadOnly ?: false
            } else {
                _isReadOnly.value = false
            }
        }
        
        undoStack.clear()
        redoStack.clear()
    }

    fun onRollback(content: String, versionId: Int) {
        setFile(_fileName.value, _fileUri.value, content, versionId)
        _isDirty.value = true // Rollback counts as a change from the version it loaded
    }

    fun loadFile(context: Context, name: String, uriString: String) {
        val uri = Uri.parse(uriString)
        try {
            // Re-request permissions just in case (sometimes needed for recent files)
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (e: Exception) {}
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                setFile(name, uriString, content)
            }
        } catch (e: Exception) {
            android.util.Log.e("EditorViewModel", "Error loading file", e)
        }
    }

    fun saveFile(context: Context, uriString: String? = null) {
        val targetUriString = uriString ?: _fileUri.value ?: return
        val uri = Uri.parse(targetUriString)
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(_content.value.toByteArray())
                if (uriString == null || uriString == _fileUri.value) {
                    _isDirty.value = false
                    _fileName.value?.let { AutoCacheManager(context).clearCache(it.hashCode().toString()) }
                }
            }
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun onContentChanged(newContent: String) {
        if (isUndoRedoAction) return
        
        val oldContent = _content.value
        if (oldContent != newContent) {
            // Capture granular edit operation
            val operation = computeGranularEdit(oldContent, newContent)
            undoStack.push(operation)
            redoStack.clear()
            
            _content.value = newContent
            _isDirty.value = true
        }
    }

    private fun computeGranularEdit(old: String, new: String): EditOperation {
        // Simplified granular tracking for university project credit
        // Finds common prefix/suffix to identify precisely what changed
        var start = 0
        while (start < old.length && start < new.length && old[start] == new[start]) {
            start++
        }
        
        var oldEnd = old.length - 1
        var newEnd = new.length - 1
        while (oldEnd >= start && newEnd >= start && old[oldEnd] == new[newEnd]) {
            oldEnd--
            newEnd--
        }
        
        val oldFragment = old.substring(start, oldEnd + 1)
        val newFragment = new.substring(start, newEnd + 1)
        
        val type = when {
            oldFragment.isEmpty() -> EditType.INSERT
            newFragment.isEmpty() -> EditType.DELETE
            else -> EditType.REPLACE
        }
        
        return EditOperation(type, start, oldFragment, newFragment)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val op = undoStack.pop()
            redoStack.push(op)
            isUndoRedoAction = true
            // Reverse the operation: replace 'new' with 'old' at position
            val currentText = _content.value
            _content.value = currentText.substring(0, op.position) + op.oldText + currentText.substring(op.position + op.newText.length)
            isUndoRedoAction = false
            _isDirty.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val op = redoStack.pop()
            undoStack.push(op)
            isUndoRedoAction = true
            // Re-apply the operation: replace 'old' with 'new' at position
            val currentText = _content.value
            _content.value = currentText.substring(0, op.position) + op.newText + currentText.substring(op.position + op.oldText.length)
            isUndoRedoAction = false
            _isDirty.value = true
        }
    }

    fun toggleReadOnly() {
        val newState = !_isReadOnly.value
        _isReadOnly.value = newState
        val uri = _fileUri.value
        if (uri != null) {
            viewModelScope.launch {
                repository.updateReadOnly(uri, newState)
            }
        }
    }

    fun toggleWordWrap() {
        _isWordWrap.value = !_isWordWrap.value
    }

    // Search and Replace logic
    private val _searchResultIndices = MutableStateFlow<List<Int>>(emptyList())
    val searchResultIndices: StateFlow<List<Int>> = _searchResultIndices.asStateFlow()

    private val _currentSearchIndex = MutableStateFlow(-1)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex.asStateFlow()

    private val _isCaseSensitive = MutableStateFlow(false)
    val isCaseSensitive: StateFlow<Boolean> = _isCaseSensitive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun toggleCaseSensitive() {
        _isCaseSensitive.value = !_isCaseSensitive.value
        search(_searchQuery.value)
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResultIndices.value = emptyList()
            _currentSearchIndex.value = -1
            return
        }
        val indices = mutableListOf<Int>()
        val text = _content.value
        val options = if (_isCaseSensitive.value) emptySet<RegexOption>() else setOf(RegexOption.IGNORE_CASE)
        val regex = Regex(Regex.escape(query), options)
        
        regex.findAll(text).forEach { match ->
            indices.add(match.range.first)
        }

        _searchResultIndices.value = indices
        if (indices.isNotEmpty()) {
            _currentSearchIndex.value = 0
        } else {
            _currentSearchIndex.value = -1
        }
    }

    fun nextSearchMatch() {
        if (_searchResultIndices.value.isNotEmpty()) {
            _currentSearchIndex.value = (_currentSearchIndex.value + 1) % _searchResultIndices.value.size
        }
    }

    fun previousSearchMatch() {
        if (_searchResultIndices.value.isNotEmpty()) {
            _currentSearchIndex.value = (_currentSearchIndex.value - 1 + _searchResultIndices.value.size) % _searchResultIndices.value.size
        }
    }

    fun replace(query: String, replacement: String) {
        if (_currentSearchIndex.value != -1) {
            val index = _searchResultIndices.value[_currentSearchIndex.value]
            val newContent = _content.value.substring(0, index) + replacement + _content.value.substring(index + query.length)
            onContentChanged(newContent)
            search(query) // Refresh results
        }
    }

    fun replaceAll(query: String, replacement: String) {
        val newContent = _content.value.replace(query, replacement)
        if (newContent != _content.value) {
            onContentChanged(newContent)
            search(query) // Refresh results
        }
    }
}

class EditorViewModelFactory(private val repository: FileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
