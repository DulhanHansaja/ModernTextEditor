package com.example.texteditor.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "versions",
    foreignKeys = [ForeignKey(
        entity = FileEntity::class,
        parentColumns = ["id"],
        childColumns = ["fileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileId: Int,
    val versionLabel: String,
    val patch: String?,
    val baseVersionId: Int?,
    val timestamp: Long = System.currentTimeMillis(),
    val fullContent: String? = null // Optional: store full content occasionally or for base
)
