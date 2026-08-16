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
        metaFile.writeText("${fileName}\n${uri ?: "NULL_URI"}")
    }

    fun getRecoverableFiles(): List<RecoveredFile> {
        val files = mutableListOf<RecoveredFile>()
        cacheDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { cacheFile ->
            val hash = cacheFile.name.substringBefore(".tmp")
            val metaFile = File(cacheDir, "$hash.meta")
            if (metaFile.exists()) {
                val lines = metaFile.readLines()
                if (lines.isNotEmpty()) {
                    files.add(RecoveredFile(
                        name = lines[0],
                        uri = if (lines.size > 1 && lines[1] != "NULL_URI") lines[1] else null,
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
