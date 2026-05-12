package com.isharaai.isl.feature.chat

import android.Manifest
import android.net.Uri
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isharaai.isl.R
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.feature.onboarding.registerTarget

@Composable
fun ChatScreen(
    onSettingsClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDownloadClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Microphone permission request
    var pendingLanguage by remember { mutableStateOf<com.isharaai.isl.speech.SpeechLanguage?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingLanguage?.let { viewModel.startRecording(it) }
        }
        pendingLanguage = null
    }

    // Gallery photo picker
    val scope = rememberCoroutineScope()
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val filePath = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(it)
                        val file = File(context.filesDir, "gallery_${System.currentTimeMillis()}.jpg")
                        inputStream?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        file.absolutePath
                    }
                    viewModel.sendImage(filePath)
                } catch (e: Exception) {
                    Log.e("ChatScreen", "Failed to load gallery image", e)
                }
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        val targetIndex = uiState.messages.size - 1 + if (uiState.isGenerating) 1 else 0
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .imePadding()
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp)
                .background(AppGreen)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // App title
            Text(
                text = stringResource(R.string.app_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            // New Chat + Settings buttons
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(
                    onClick = { showNewChatDialog = true },
                    modifier = Modifier.onGloballyPositioned { registerTarget("new_chat_btn", it) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Chat",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.onGloballyPositioned { registerTarget("settings_btn", it) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Download Banner (when model not ready)
        if (!uiState.isModelReady) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // model needed banner
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.model_needed),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Text(
                            text = stringResource(R.string.model_needed_desc),
                            fontSize = 11.sp,
                            color = TextMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDownloadClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            stringResource(R.string.btn_download),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        // Input Bar
        ChatInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            isRecording = uiState.isRecording,
            partialTranscript = uiState.partialTranscript,
            isGenerating = uiState.isGenerating,
            onSend = { text ->
                viewModel.sendMessage(text)
                inputText = ""
            },
            onAttachClick = { galleryLauncher.launch("image/*") },
            onCameraClick = onCameraClick,
            onStartRecording = { lang ->
                pendingLanguage = lang
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onStopRecording = { viewModel.stopRecording() }
        )
    }

    // New Chat name dialog
    if (showNewChatDialog) {
        NewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onConfirm = { name ->
                showNewChatDialog = false
                viewModel.startNewSession(name)
            }
        )
    }
}
