package com.example.texteditor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val uri: String,
    val isReadOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpened: Long = System.currentTimeMillis()
)
