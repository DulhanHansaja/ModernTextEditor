package com.example.texteditor.data

import com.example.texteditor.versioncontrol.DiffEngine

class VersionRepository(
    private val fileDao: FileDao,
    private val versionDao: VersionDao,
    private val diffEngine: DiffEngine
) {
    suspend fun saveVersion(fileUri: String, currentContent: String, label: String, overrideBaseId: Int? = null) {
        val file = fileDao.getFileByUri(fileUri) ?: return
        val baseVersion = if (overrideBaseId != null) {
            versionDao.getVersionById(overrideBaseId)
        } else {
            versionDao.getLatestVersionForFile(file.id)
        }
        
        if (baseVersion == null) {
            // First version or invalid override: store full content
            versionDao.insertVersion(VersionEntity(
                fileId = file.id,
                versionLabel = label,
                patch = null,
                baseVersionId = null,
                fullContent = currentContent
            ))
        } else {
            // Compute diff against reconstructed base content
            val baseContent = reconstructVersion(baseVersion.id) ?: ""
            val patch = diffEngine.computeDiff(baseContent, currentContent)
            
            versionDao.insertVersion(VersionEntity(
                fileId = file.id,
                versionLabel = label,
                patch = patch,
                baseVersionId = baseVersion.id
            ))
        }
    }

    suspend fun getVersionsForFile(fileUri: String): List<VersionEntity> {
        val file = fileDao.getFileByUri(fileUri) ?: return emptyList()
        return versionDao.getVersionsForFile(file.id)
    }

    suspend fun reconstructVersion(versionId: Int): String? {
        val version = versionDao.getVersionById(versionId) ?: return null
        
        return if (version.patch == null) {
            // Base version
            version.fullContent
        } else {
            // Apply patch to base version
            val baseContent = version.baseVersionId?.let { reconstructVersion(it) } ?: ""
            diffEngine.applyPatch(baseContent, version.patch)
        }
    }
}
