package com.isharaai.isl.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.R
import com.isharaai.isl.core.speech.SpeechLanguage
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.feature.onboarding.registerTarget

/**
 * The chat input bar with text field, gallery attach, camera,
 * mic button (with language picker popup) and send button.
 */
@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isRecording: Boolean,
    partialTranscript: String,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onAttachClick: () -> Unit,
    onCameraClick: () -> Unit,
    onStartRecording: (SpeechLanguage) -> Unit,
    onStopRecording: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showLanguagePicker by remember { mutableStateOf(false) }

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
                modifier = Modifier.weight(1f).onGloballyPositioned { registerTarget("chat_input", it) },
                readOnly = isRecording,
                placeholder = {
                    Text(
                        if (isRecording) stringResource(R.string.listening)
                        else stringResource(R.string.chat_hint),
                        color = if (isRecording) ActiveRed else TextLight
                    )
                },
                leadingIcon = {
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier.onGloballyPositioned { registerTarget("attach_btn", it) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach photo",
                            tint = Color.DarkGray
                        )
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Camera button
                        IconButton(
                            onClick = onCameraClick,
                            modifier = Modifier.onGloballyPositioned { registerTarget("camera_btn", it) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                tint = Color.DarkGray
                            )
                        }
                        // Mic button with language picker popup
                        Box {
                            IconButton(
                                onClick = {
                                    if (isRecording) onStopRecording()
                                    else showLanguagePicker = true
                                },
                                modifier = Modifier.onGloballyPositioned { registerTarget("mic_btn", it) }
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop" else "Voice",
                                    tint = if (isRecording) ActiveRed else Color.DarkGray
                                )
                            }
                            DropdownMenu(
                                expanded = showLanguagePicker,
                                onDismissRequest = { showLanguagePicker = false }
                            ) {
                                SpeechLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = lang.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showLanguagePicker = false
                                            onStartRecording(lang)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = InputFieldBg
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
                    .padding(bottom = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) ActiveRed
                        else TextLight.copy(alpha = 0.3f)
                    )
                    .then(
                        if (canSend) Modifier.clickable {
                            onSend(inputText)
                            focusManager.clearFocus()
                        } else Modifier
                    )
                    .onGloballyPositioned { registerTarget("send_btn", it) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
