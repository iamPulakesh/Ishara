package com.isharaai.isl.feature.chat

import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.R
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.feature.chat.isl.ISLMessageContent

// Chat Bubble

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) stringResource(R.string.chat_sender_you) else stringResource(R.string.chat_sender_ai),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isUser) PrimaryPurple.copy(alpha = 0.7f) else PrimaryPink.copy(alpha = 0.7f)
        )

        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) PrimaryPurple else CardWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isUser) 0.dp else 1.dp
            )
        ) {
            Column {
                // Show image if present
                message.imagePath?.let { path ->
                    val bitmap = remember(path) {
                        BitmapFactory.decodeFile(path)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Captured photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Show text — delegate to ISLMessageContent for AI messages
                val displayText = if (message.stringResId != 0) {
                    stringResource(message.stringResId)
                } else {
                    message.text
                }

                if (displayText.isNotEmpty()) {
                    if (!isUser) {
                        // AI messages - parse for ISL tags and render inline videos
                        ISLMessageContent(
                            text = displayText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    } else {
                        // User messages - plain text
                        Text(
                            text = displayText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    }
                } else if (!isUser) {
                    InlineTypingDots()
                }
            }
        }

        // Timestamp
        Text(
            text = formatTime(message.timestamp),
            fontSize = 10.sp,
            color = TextLight,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
        )
    }
}

// Inline Typing Dots

@Composable
fun InlineTypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "inlineTyping")

    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 180),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "inlineDot$index"
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = alpha))
            )
        }
    }
}

// Helpers for chat bubble

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
