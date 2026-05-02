package com.isharaai.isl.feature.chat.isl

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ISLSignPlayerCard(words: List<String>) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }

    val wordVideoMap = remember(words) {
        val mapped = mutableListOf<Pair<String, Int>>()
        for (word in words) {
            val resId = context.resources.getIdentifier("sign_${word.lowercase()}", "raw", context.packageName)
            if (resId != 0) {
                mapped.add(word to resId)
            } else {
                for (char in word) {
                    if (char.isLetterOrDigit()) {
                        val charResId = context.resources.getIdentifier("sign_${char.lowercaseChar()}", "raw", context.packageName)
                        if (charResId != 0) mapped.add(char.uppercase() to charResId)
                    }
                }
            }
        }
        mapped
    }
    val hasAnyVideo = wordVideoMap.any { it.second != 0 }
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
                    wordVideoMap = wordVideoMap.filter { it.second != 0 },
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
                for ((word, resId) in wordVideoMap) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (resId != 0) AppGreen else TextLight,
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
            wordVideoMap = wordVideoMap.filter { it.second != 0 },
            allWords = words,
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
fun ISLSequentialPlayer(wordVideoMap: List<Pair<String, Int>>, replayTrigger: Int = 0, onClick: () -> Unit = {}) {
    if (wordVideoMap.isEmpty()) return
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF } }

    LaunchedEffect(replayTrigger) { if (replayTrigger > 0) currentIndex = 0 }

    LaunchedEffect(currentIndex, replayTrigger) {
        val entry = wordVideoMap.getOrNull(currentIndex) ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${entry.second}")))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && currentIndex < wordVideoMap.size - 1) currentIndex++
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(Color.Black).clickable { onClick() }
    ) {
        AndroidView(factory = { PlayerView(it).apply { player = exoPlayer; useController = false } }, modifier = Modifier.fillMaxSize())
        if (wordVideoMap.size > 1) {
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.6f)) {
                Text("${currentIndex + 1} / ${wordVideoMap.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
