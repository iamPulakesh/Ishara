package com.isharaai.isl.feature.chat.signplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.core.theme.TextDark

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class ISLBlock(val words: List<String>) : MessageSegment()
}

fun parseISLMessage(raw: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val pattern = Regex("""\[\[ISL:\s*(.+?)\]\]""")

    var lastEnd = 0
    for (match in pattern.findAll(raw)) {
        if (match.range.first > lastEnd) {
            val textBefore = raw.substring(lastEnd, match.range.first).trim()
            if (textBefore.isNotEmpty()) segments.add(MessageSegment.Text(textBefore))
        }
        val words = match.groupValues[1].trim().split("\\s+".toRegex()).map { it.uppercase() }.filter { it.isNotEmpty() }
        if (words.isNotEmpty()) segments.add(MessageSegment.ISLBlock(words))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < raw.length) {
        val trailing = raw.substring(lastEnd).trim()
        if (trailing.isNotEmpty()) segments.add(MessageSegment.Text(trailing))
    }
    if (segments.isEmpty()) segments.add(MessageSegment.Text(raw))
    return segments
}

@Composable
fun ISLMessageContent(text: String, modifier: Modifier = Modifier) {
    val segments = remember(text) { parseISLMessage(text) }
    Column(modifier = modifier) {
        for (segment in segments) {
            when (segment) {
                is MessageSegment.Text -> Text(segment.content, color = TextDark, fontSize = 15.sp, lineHeight = 21.sp)
                is MessageSegment.ISLBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    ISLSignPlayerCard(words = segment.words)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
