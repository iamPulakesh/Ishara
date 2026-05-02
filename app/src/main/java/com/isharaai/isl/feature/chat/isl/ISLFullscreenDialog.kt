package com.isharaai.isl.feature.chat.isl

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ISLFullscreenDialog(wordVideoMap: List<Pair<String, Int>>, allWords: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val context = LocalContext.current
        var currentIndex by remember { mutableIntStateOf(0) }
        var isFinished by remember { mutableStateOf(false) }
        var replayTrigger by remember { mutableIntStateOf(0) }

        val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF } }

        LaunchedEffect(currentIndex, replayTrigger) {
            val entry = wordVideoMap.getOrNull(currentIndex) ?: return@LaunchedEffect
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${entry.second}")))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            isFinished = false
        }

        DisposableEffect(Unit) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        if (currentIndex < wordVideoMap.size - 1) currentIndex++ else isFinished = true
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer; useController = false } },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).align(Alignment.Center)
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
            ) { Icon(Icons.Default.Close, "Close", tint = Color.White) }

            // Current word + progress
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                wordVideoMap.getOrNull(currentIndex)?.let {
                    Text(it.first, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (wordVideoMap.size > 1) {
                    Text("${currentIndex + 1} / ${wordVideoMap.size}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // Bottom: replay + word chips
            Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = isFinished, enter = fadeIn(), exit = fadeOut()) {
                    Button(
                        onClick = { currentIndex = 0; isFinished = false; replayTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Replay, "Replay", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (word in allWords) {
                        val isCurrent = wordVideoMap.getOrNull(currentIndex)?.first == word
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) MicGreen else Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ) {
                            Text(word, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
