package com.example.texteditor.data

import kotlinx.coroutines.flow.Flow

class FileRepository(private val fileDao: FileDao) {
    val recentFiles: Flow<List<FileEntity>> = fileDao.getRecentFiles()

    suspend fun addOrUpdateFile(name: String, uri: String) {
        val existing = fileDao.getFileByUri(uri)
        if (existing != null) {
            fileDao.updateFile(existing.copy(lastOpened = System.currentTimeMillis()))
        } else {
            fileDao.insertFile(FileEntity(name = name, uri = uri))
        }
    }
}
