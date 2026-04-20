package com.isharaai.isl.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.R
import com.isharaai.isl.speech.SpeechLanguage
import com.isharaai.isl.core.theme.TextLight

/**
 * The chat input bar with text field, gallery attach, camera, language toggle,
 * mic button and send button.
 */
@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isRecording: Boolean,
    partialTranscript: String,
    isGenerating: Boolean,
    speechLanguage: SpeechLanguage,
    onSend: (String) -> Unit,
    onAttachClick: () -> Unit,
    onCameraClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = if (isRecording) partialTranscript else inputText,
                onValueChange = { if (!isRecording) onInputChange(it) },
                modifier = Modifier.weight(1f),
                readOnly = isRecording,
                placeholder = {
                    Text(
                        if (isRecording) stringResource(R.string.listening)
                        else stringResource(R.string.chat_hint),
                        color = if (isRecording) Color(0xFFD32F2F) else TextLight
                    )
                },
                leadingIcon = {
                    IconButton(onClick = onAttachClick) {
                        // attach photo button
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach photo",
                            tint = Color.DarkGray
                        )
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Language toggle (EN / বাং)
                        TextButton(
                            onClick = onToggleLanguage,
                            modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = speechLanguage.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        // camera button
                        IconButton(onClick = onCameraClick) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                tint = Color.DarkGray
                            )
                        }
                        // mic button
                        IconButton(
                            onClick = {
                                if (isRecording) onStopRecording() else onStartRecording()
                            }
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop" else "Voice",
                                tint = if (isRecording) Color(0xFFD32F2F) else Color.DarkGray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF5F5F5)
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onSend(inputText)
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            val canSend = inputText.isNotBlank() && !isGenerating
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Color(0xFFD32F2F)
                        else TextLight.copy(alpha = 0.3f)
                    )
                    .then(
                        if (canSend) Modifier.clickable {
                            onSend(inputText)
                            focusManager.clearFocus()
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "➤",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
