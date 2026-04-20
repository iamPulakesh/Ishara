package com.isharaai.isl.feature.video

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ExoPlayer instance for playing ISL sign language video clips
 * from app's raw resources.
 */
@Singleton
class ISLVideoPlayer @Inject constructor(private val context: Context) {

    private var player: ExoPlayer? = null

    fun prepareAndPlay(@RawRes videoResId: Int, onComplete: () -> Unit) {
        release()

        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer

        // prepare and play video
        val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
        val mediaItem = MediaItem.fromUri(uri)

        exoPlayer.apply {
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onComplete()
                    }
                }
            })
            prepare()
            play()
        }
    }

    fun getPlayer(): ExoPlayer? = player

    fun release() {
        player?.release()
        player = null
    }
}
