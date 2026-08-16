package com.example.texteditor.versioncontrol

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

class DiffEngine {
    fun computeDiff(base: String, current: String): String {
        val baseLines = base.lines()
        val currentLines = current.lines()
        val patch = DiffUtils.diff(baseLines, currentLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("base", "current", baseLines, patch, 0)
        return unifiedDiff.joinToString("\n")
    }

    fun applyPatch(base: String, patchString: String): String {
        val baseLines = base.lines()
        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchString.lines())
        val revisedLines = DiffUtils.patch(baseLines, patch)
        return revisedLines.joinToString("\n")
    }
}
