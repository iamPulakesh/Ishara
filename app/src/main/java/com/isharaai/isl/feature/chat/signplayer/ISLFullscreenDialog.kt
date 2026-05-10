package com.isharaai.isl.feature.chat.signplayer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ISLFullscreenDialog(wordVideoMap: List<Pair<String, String>>, allWords: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var replayTrigger by remember { mutableIntStateOf(0) }

        SequentialSignVideoPlayer(
            wordVideoMap = wordVideoMap,
            replayTrigger = replayTrigger,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            playerModifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            playerAlignment = Alignment.Center
        ) { state ->
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
            ) { Icon(Icons.Default.Close, "Close", tint = Color.White) }

            // Current word + progress
            Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                wordVideoMap.getOrNull(state.currentIndex)?.let {
                    Text(it.first, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (wordVideoMap.size > 1) {
                    Text("${state.currentIndex + 1} / ${wordVideoMap.size}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // Bottom: replay + word chips
            Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = state.isFinished, enter = fadeIn(), exit = fadeOut()) {
                    Button(
                        onClick = { replayTrigger++ },
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
                        val isCurrent = wordVideoMap.getOrNull(state.currentIndex)?.first == word
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
