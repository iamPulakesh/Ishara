package com.isharaai.isl.feature.chat

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.isharaai.isl.core.theme.TextDark

/**
 * Represents a segment of an AI message — either plain text or an ISL sign block.
 */
sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class ISLBlock(val words: List<String>) : MessageSegment()
}

/**
 * Parses a raw AI response into segments of plain text and ISL blocks.
 *
 * Looks for [[ISL: WORD1, WORD2, WORD3]] patterns and splits them out.
 */
fun parseISLMessage(raw: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val pattern = Regex("""\[\[ISL:\s*(.+?)\]\]""") //

    var lastEnd = 0
    for (match in pattern.findAll(raw)) {
        if (match.range.first > lastEnd) {
            val textBefore = raw.substring(lastEnd, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                segments.add(MessageSegment.Text(textBefore))
            }
        }
        // Extract the words from the ISL tag
        val wordsRaw = match.groupValues[1]
        val words = wordsRaw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        if (words.isNotEmpty()) {
            segments.add(MessageSegment.ISLBlock(words))
        }

        lastEnd = match.range.last + 1
    }
    // Add any remaining text after the last ISL tag
    if (lastEnd < raw.length) {
        val trailing = raw.substring(lastEnd).trim()
        if (trailing.isNotEmpty()) {
            segments.add(MessageSegment.Text(trailing))
        }
    }
    // If no ISL tags were found, add the entire raw text as a single text segment
    if (segments.isEmpty()) {
        segments.add(MessageSegment.Text(raw))
    }

    return segments
}

/**
 * Renders an AI message with ISL tag interception.
 */
@Composable
fun ISLMessageContent(
    text: String,
    modifier: Modifier = Modifier
) {
    val segments = remember(text) { parseISLMessage(text) }

    Column(modifier = modifier) {
        for (segment in segments) {
            when (segment) {
                is MessageSegment.Text -> {
                    Text(
                        text = segment.content,
                        color = TextDark,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )
                }
                is MessageSegment.ISLBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    ISLSignPlayerCard(words = segment.words)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * A compact card that plays ISL sign videos sequentially.
 */
@Composable
fun ISLSignPlayerCard(words: List<String>) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }

    // Map each word to its video resource ID
    val wordVideoMap = remember(words) {
        words.map { word ->
            val resName = "sign_${word.lowercase()}"
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            word to resId
        }
    }
    // Check if any of the words have corresponding video resources
    val hasAnyVideo = wordVideoMap.any { it.second != 0 }

    // Shared replay trigger — incremented to force replay
    var replayTrigger by remember { mutableIntStateOf(0) }

    // Main card container
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
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

            // Bottom bar: word chips, replay and fullscreen
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word chips
                for ((word, resId) in wordVideoMap) {
                    val hasVideo = resId != 0
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasVideo) Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = word,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Replay button
                if (hasAnyVideo) {
                    IconButton(
                        onClick = { replayTrigger++ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Fullscreen button
                    IconButton(
                        onClick = { showFullscreen = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Dialog
    if (showFullscreen && hasAnyVideo) {
        ISLFullscreenDialog(
            wordVideoMap = wordVideoMap.filter { it.second != 0 },
            allWords = words,
            onDismiss = { showFullscreen = false }
        )
    }
}

/**
 * Clean inline video player with a small progress counter if there are multiple videos.
 */
@Composable
fun ISLSequentialPlayer(
    wordVideoMap: List<Pair<String, Int>>,
    replayTrigger: Int = 0,
    onClick: () -> Unit = {}
) {
    if (wordVideoMap.isEmpty()) return

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF 
        }
    }

    // Replay: reset index when trigger changes
    LaunchedEffect(replayTrigger) {
        if (replayTrigger > 0) {
            currentIndex = 0
        }
    }

    // Load the current video
    LaunchedEffect(currentIndex, replayTrigger) {
        val entry = wordVideoMap.getOrNull(currentIndex) ?: return@LaunchedEffect
        val uri = Uri.parse("android.resource://${context.packageName}/${entry.second}")
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Auto-advance on video end
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if (currentIndex < wordVideoMap.size - 1) {
                        currentIndex++
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Video player
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable { onClick() }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Small progress counter if multiple videos
        if (wordVideoMap.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${wordVideoMap.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Fullscreen Dialog

@Composable
fun ISLFullscreenDialog(
    wordVideoMap: List<Pair<String, Int>>,
    allWords: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        var currentIndex by remember { mutableIntStateOf(0) }
        var isFinished by remember { mutableStateOf(false) }
        var replayTrigger by remember { mutableIntStateOf(0) }

        // Single ExoPlayer instance
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }

        // Load the current video 
        LaunchedEffect(currentIndex, replayTrigger) {
            val entry = wordVideoMap.getOrNull(currentIndex) ?: return@LaunchedEffect
            val uri = Uri.parse("android.resource://${context.packageName}/${entry.second}")
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            isFinished = false
        }

        // Auto-advance
        DisposableEffect(Unit) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        if (currentIndex < wordVideoMap.size - 1) {
                            currentIndex++
                        } else {
                            isFinished = true
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Video 
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
            )

            // Close button (top-left)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Current word + progress (top-center)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                wordVideoMap.getOrNull(currentIndex)?.let {
                    Text(
                        text = it.first,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (wordVideoMap.size > 1) {
                    Text(
                        text = "${currentIndex + 1} / ${wordVideoMap.size}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Word chips + Replay (bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Replay button — always visible once finished
                AnimatedVisibility(
                    visible = isFinished,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            currentIndex = 0
                            isFinished = false
                            replayTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Word chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (word in allWords) {
                        val isCurrent = wordVideoMap.getOrNull(currentIndex)?.first == word
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isCurrent -> Color(0xFF4CAF50)
                                else -> Color.White.copy(alpha = 0.2f)
                            },
                            contentColor = Color.White
                        ) {
                            Text(
                                text = word,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
