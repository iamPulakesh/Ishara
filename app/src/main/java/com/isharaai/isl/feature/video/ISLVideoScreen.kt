package com.isharaai.isl.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.isharaai.isl.R
import com.isharaai.isl.feature.video.ISLVideoViewModel

@Composable
fun ISLVideoScreen(
    signId: String,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit,
    viewModel: ISLVideoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(signId) {
        viewModel.loadSign(signId)
    }

    LaunchedEffect(uiState.signEntity) {
        uiState.signEntity?.let { sign ->
            val resId = context.resources.getIdentifier(
                sign.videoResName, "raw", context.packageName
            )
            if (resId != 0) {
                viewModel.playVideo(context, resId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video player area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            uiState.player?.let { player ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Bottom info panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDD000000))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            uiState.signEntity?.let { sign ->
                Text(
                    text = sign.bengaliWord,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = sign.signId,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            // buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.btn_play_again))
                }

                Button(onClick = onBack) {
                    Text(stringResource(R.string.btn_back))
                }
            }
        }
    }
}
