package com.example.texteditor

import android.app.Application
import com.example.texteditor.data.AppDatabase
import com.example.texteditor.data.FileRepository
import com.example.texteditor.data.VersionRepository
import com.example.texteditor.versioncontrol.DiffEngine

class TextEditorApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { FileRepository(database.fileDao()) }
    val versionRepository by lazy { 
        VersionRepository(database.fileDao(), database.versionDao(), DiffEngine()) 
    }
}
