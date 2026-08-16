package com.example.texteditor.history

import android.content.Context
import java.io.File

class AutoCacheManager(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "auto_cache")

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    fun saveCache(fileName: String, uri: String?, content: String) {
        val cacheFile = File(cacheDir, "${fileName.hashCode()}.tmp")
        val metaFile = File(cacheDir, "${fileName.hashCode()}.meta")
        
        cacheFile.writeText(content)
        metaFile.writeText("${fileName}\n${uri ?: ""}")
    }

    fun getRecoverableFiles(): List<RecoveredFile> {
        val files = mutableListOf<RecoveredFile>()
        cacheDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { cacheFile ->
            val hash = cacheFile.name.substringBefore(".tmp")
            val metaFile = File(cacheDir, "$hash.meta")
            if (metaFile.exists()) {
                val lines = metaFile.readLines()
                if (lines.size >= 2) {
                    files.add(RecoveredFile(
                        name = lines[0],
                        uri = lines[1].ifEmpty { null },
                        content = cacheFile.readText(),
                        hash = hash
                    ))
                }
            }
        }
        return files
    }

    fun clearCache(hash: String) {
        File(cacheDir, "$hash.tmp").delete()
        File(cacheDir, "$hash.meta").delete()
    }

    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}

data class RecoveredFile(
    val name: String,
    val uri: String?,
    val content: String,
    val hash: String
)
