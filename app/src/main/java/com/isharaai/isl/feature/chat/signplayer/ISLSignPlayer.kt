package com.isharaai.isl.feature.chat.signplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.feature.usersigns.UserSignManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Resolves a sign key to a URI string — checks bundled res/raw first, then user signs. */
private fun resolveSignUri(context: android.content.Context, key: String): String {
    return UserSignManager.resolveSignUri(context, key)
}

@Composable
fun ISLSignPlayerCard(words: List<String>) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }

    // Map each word (or compound/fingerspell) to its video URI string ("" = no video)
    val wordVideoMap = remember(words) {
        val mapped = mutableListOf<Pair<String, String>>()
        var i = 0
        while (i < words.size) {
            var matched = false
            // Try longest compound first (up to 3 words), then shrink
            for (len in minOf(3, words.size - i) downTo 1) {
                val compound = words.subList(i, i + len)
                val key = compound.joinToString("_") { it.lowercase() }
                val uri = resolveSignUri(context, key)
                if (uri.isNotEmpty()) {
                    mapped.add(compound.joinToString(" ") to uri)
                    i += len
                    matched = true
                    break
                }
            }
            // Fallback: fingerspell the unmatched word
            if (!matched) {
                for (char in words[i]) {
                    if (char.isLetterOrDigit()) {
                        val uri = resolveSignUri(context, char.lowercaseChar().toString())
                        if (uri.isNotEmpty()) mapped.add(char.uppercase() to uri)
                    }
                }
                i++
            }
        }
        mapped
    }
    val hasAnyVideo = wordVideoMap.any { it.second.isNotEmpty() }
    var replayTrigger by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ISLCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (hasAnyVideo) {
                ISLSequentialPlayer(
                    wordVideoMap = wordVideoMap.filter { it.second.isNotEmpty() },
                    replayTrigger = replayTrigger,
                    onClick = { showFullscreen = true }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for ((word, uri) in wordVideoMap) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uri.isNotEmpty()) AppGreen else TextLight,
                        contentColor = Color.White
                    ) {
                        Text(word, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (hasAnyVideo) {
                    IconButton(onClick = { replayTrigger++ }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Replay, "Replay", tint = AppGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showFullscreen = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Fullscreen, "Fullscreen", tint = AppGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showFullscreen && hasAnyVideo) {
        ISLFullscreenDialog(
            wordVideoMap = wordVideoMap.filter { it.second.isNotEmpty() },
            allWords = words,
            onDismiss = { showFullscreen = false }
        )
    }
}

// this function plays the videos in sequence
@Composable
fun ISLSequentialPlayer(wordVideoMap: List<Pair<String, String>>, replayTrigger: Int = 0, onClick: () -> Unit = {}) {
    if (wordVideoMap.isEmpty()) return
    SequentialSignVideoPlayer(
        wordVideoMap = wordVideoMap,
        replayTrigger = replayTrigger,
        enableSwipe = true,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) { state ->
        if (wordVideoMap.size > 1) {
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.6f)) {
                Text("${state.currentIndex + 1} / ${wordVideoMap.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
