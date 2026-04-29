package com.isharaai.isl.feature.onboarding

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.feature.settings.LanguageManager
import java.util.Locale

@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
    val context = LocalContext.current
    val isBengali = LanguageManager.getCurrentLanguage(context) == LanguageManager.LANG_BENGALI
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = TUTORIAL_STEPS
    val step = steps[stepIndex]
    val desc = if (isBengali) step.descBn else step.descEn

    // TTS
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) ttsReady = true
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    // Speak on step change or when TTS becomes ready
    LaunchedEffect(stepIndex, ttsReady) {
        if (ttsReady) {
            tts?.language = if (isBengali) Locale("bn", "IN") else Locale.US
            tts?.speak(desc, TextToSpeech.QUEUE_FLUSH, null, "step_$stepIndex")
        }
    }

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    )

    val targetRect = tutorialTargets[step.targetKey]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawBehind {
                // Dark scrim
                drawRect(Color.Black.copy(alpha = 0.7f))
                // Cut out spotlight
                targetRect?.let { rect ->
                    val center = rect.center
                    val radius = maxOf(rect.width, rect.height) / 2f + 16.dp.toPx()
                    drawCircle(Color.Transparent, radius, center, blendMode = BlendMode.Clear)
                    // Pulsing ring
                    drawCircle(
                        Color(0xFF4CAF50).copy(alpha = pulseAlpha),
                        radius * pulseScale,
                        center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
    ) {
        // Tooltip
        targetRect?.let { rect ->
            val screenHeightPx = constraints.maxHeight.toFloat()
            val showBelow = rect.center.y < screenHeightPx / 2f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(if (showBelow) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(top = if (showBelow) (rect.bottom / LocalContext.current.resources.displayMetrics.density + 16).dp else 0.dp,
                             bottom = if (!showBelow) ((screenHeightPx - rect.top) / LocalContext.current.resources.displayMetrics.density + 16).dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(desc, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (stepIndex < steps.size - 1) {
                                OutlinedButton(onClick = { tts?.stop(); onFinish() }) { Text("Skip") }
                                Button(
                                    onClick = { stepIndex++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) { Text("Next") }
                            } else {
                                Button(
                                    onClick = { tts?.stop(); onFinish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) { Text("Done") }
                            }
                        }
                    }
                }
            }
        }
    }
}
