package com.example.texteditor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VersionDao {
    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY timestamp DESC")
    suspend fun getVersionsForFile(fileId: Int): List<VersionEntity>

    @Insert
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM versions WHERE id = :id")
    suspend fun getVersionById(id: Int): VersionEntity?

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestVersionForFile(fileId: Int): VersionEntity?
}
