package com.example.texteditor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.difflib.DiffUtils
import com.github.difflib.text.DiffRowGenerator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(
    oldContent: String,
    newContent: String,
    onBack: () -> Unit
) {
    val generator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .oldTag { f -> if (f) "\u001b[" else "\u001b]" }
        .newTag { f -> if (f) "\u001b{" else "\u001b}" }
        .build()
    
    val rows = generator.generateDiffRows(oldContent.lines(), newContent.lines())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Diff Comparison", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rows) { row ->
                DiffRowCard(row)
            }
        }
    }
}

@Composable
fun DiffRowCard(row: com.github.difflib.text.DiffRow) {
    if (row.tag == com.github.difflib.text.DiffRow.Tag.EQUAL) {
        Text(
            text = row.oldLine,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    } else {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                if (row.oldLine.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("-", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                        Text(
                            text = parseDiffLine(row.oldLine, Color.Red, Color(0xFFFFCDD2), "\u001b[", "\u001b]"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE).copy(alpha = 0.5f))
                        )
                    }
                }
                if (row.newLine.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                        Text("+", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                        Text(
                            text = parseDiffLine(row.newLine, Color(0xFF1B5E20), Color(0xFFC8E6C9), "\u001b{", "\u001b}"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9).copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

private fun parseDiffLine(line: String, textColor: Color, highlightColor: Color, startTag: String, endTag: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    var startIndex = line.indexOf(startTag)
    
    while (startIndex != -1) {
        append(line.substring(lastIndex, startIndex))
        val endIndex = line.indexOf(endTag, startIndex + 1)
        if (endIndex != -1) {
            val innerText = line.substring(startIndex + 1, endIndex)
            pushStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold, color = textColor))
            append(innerText)
            pop()
            lastIndex = endIndex + 1
        } else {
            append(line.substring(startIndex))
            lastIndex = line.length
            break
        }
        startIndex = line.indexOf(startTag, lastIndex)
    }
    if (lastIndex < line.length) {
        append(line.substring(lastIndex))
    }
}
