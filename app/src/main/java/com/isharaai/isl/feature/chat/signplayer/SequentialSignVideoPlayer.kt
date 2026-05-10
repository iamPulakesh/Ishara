package com.isharaai.isl.feature.chat.signplayer

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

internal class SequentialSignVideoPlayerState {
    var currentIndex by mutableIntStateOf(0)
        private set

    var isFinished by mutableStateOf(false)
        private set

    internal var playbackRequest by mutableIntStateOf(0)
        private set

    fun replay() {
        currentIndex = 0
        isFinished = false
        playbackRequest++
    }

    fun moveTo(index: Int, totalItems: Int) {
        if (totalItems <= 0) return
        val targetIndex = index.coerceIn(0, totalItems - 1)
        if (targetIndex == currentIndex && !isFinished) return
        currentIndex = targetIndex
        isFinished = false
        playbackRequest++
    }

    internal fun advance(totalItems: Int) {
        if (currentIndex < totalItems - 1) {
            currentIndex++
            isFinished = false
        } else {
            isFinished = true
        }
    }
}

@Composable
internal fun SequentialSignVideoPlayer(
    wordVideoMap: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    playerModifier: Modifier = Modifier.fillMaxSize(),
    playerAlignment: Alignment? = null,
    replayTrigger: Int = 0,
    enableSwipe: Boolean = false,
    onClick: (() -> Unit)? = null,
    overlay: @Composable BoxScope.(SequentialSignVideoPlayerState) -> Unit = {}
) {
    if (wordVideoMap.isEmpty()) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val state = remember(wordVideoMap) { SequentialSignVideoPlayerState() }
    val activeState by rememberUpdatedState(state)
    val activeVideoCount by rememberUpdatedState(wordVideoMap.size)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(replayTrigger) {
        if (replayTrigger > 0) state.replay()
    }

    LaunchedEffect(wordVideoMap, state.currentIndex, state.isFinished, state.playbackRequest) {
        if (state.isFinished) return@LaunchedEffect
        val entry = wordVideoMap.getOrNull(state.currentIndex) ?: return@LaunchedEffect
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(entry.second)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    activeState.advance(activeVideoCount)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val swipeModifier = if (enableSwipe) {
        Modifier.pointerInput(wordVideoMap.size) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onDragEnd = {
                    when {
                        totalDrag < -80 -> state.moveTo(state.currentIndex + 1, wordVideoMap.size)
                        totalDrag > 80 -> state.moveTo(state.currentIndex - 1, wordVideoMap.size)
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                }
            )
        }
    } else {
        Modifier
    }

    val clickModifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier

    Box(modifier = modifier.then(swipeModifier).then(clickModifier)) {
        val resolvedPlayerModifier = playerAlignment?.let { alignment ->
            playerModifier.align(alignment)
        } ?: playerModifier
        AndroidView(
            factory = { PlayerView(it).apply { useController = false } },
            update = { it.player = exoPlayer },
            modifier = resolvedPlayerModifier
        )
        overlay(state)
    }
}
