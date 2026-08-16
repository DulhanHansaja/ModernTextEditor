package com.example.texteditor.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlighter(
    private val keywords: List<String>,
    private val isMarkdown: Boolean = false,
    private val searchQuery: String = "",
    private val searchIndices: List<Int> = emptyList(),
    private val currentSearchIndex: Int = -1
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = if (isMarkdown) highlightMarkdown(text.text) else highlightKotlin(text.text)
        val finalAnnotatedString = highlightSearch(highlighted, text.text)
        return TransformedText(finalAnnotatedString, OffsetMapping.Identity)
    }

    private fun highlightSearch(annotatedString: AnnotatedString, rawText: String): AnnotatedString = buildAnnotatedString {
        append(annotatedString)
        if (searchQuery.isNotEmpty()) {
            searchIndices.forEachIndexed { index, start ->
                val end = start + searchQuery.length
                if (end <= rawText.length) {
                    val isCurrent = index == currentSearchIndex
                    addStyle(
                        style = SpanStyle(
                            background = if (isCurrent) Color(0xFFFFCC00) else Color(0xFFFFFF00),
                            color = Color.Black
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }

    private fun highlightKotlin(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        
        // Keywords
        keywords.forEach { keyword ->
            val regex = Regex("\\b$keyword\\b")
            regex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = Color(0xFFCF8E6D), fontWeight = FontWeight.Bold),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
        
        // Strings
        Regex("\".*?\"").findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF6A8759)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Comments
        Regex("//.*|/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF808080)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }

    private fun highlightMarkdown(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        
        // Headers
        Regex("^#+.*", RegexOption.MULTILINE).findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF507873), fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Bold
        Regex("\\*\\*.*?\\*\\*").findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Code
        Regex("`.*?`").findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF6A8759), background = Color(0xFFF0F0F0)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}
