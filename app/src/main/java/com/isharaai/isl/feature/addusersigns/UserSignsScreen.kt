package com.isharaai.isl.feature.addusersigns

import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.isharaai.isl.R
import com.isharaai.isl.core.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSignsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var signs by remember { mutableStateOf(UserSignManager.getAllUserSigns(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var wordInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    // Temp file for camera recording
    val tempFile = remember { File(context.cacheDir, "temp_sign.mp4") }
    val tempUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    }
    var pendingWord by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && tempFile.exists()) {
            val saved = UserSignManager.saveSign(context, pendingWord, tempFile.inputStream())
            if (saved) {
                signs = UserSignManager.getAllUserSigns(context)
                Toast.makeText(context, context.getString(R.string.my_signs_saved, pendingWord), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_signs_title), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppGreen)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { wordInput = ""; errorMsg = null; showDialog = true },
                containerColor = AppGreen, contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Add Sign") }
        },
        containerColor = BackgroundCream
    ) { padding ->
        if (signs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.my_signs_empty), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.my_signs_empty_hint), fontSize = 14.sp, color = TextLight)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(signs) { word ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(AppGreen.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🤟", fontSize = 20.sp) }
                            Spacer(Modifier.width(12.dp))
                            Text(word, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = TextDark)
                            IconButton(onClick = { deleteTarget = word }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add sign dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.my_signs_add_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = wordInput,
                        onValueChange = { wordInput = it.filter { c -> c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' }; errorMsg = null },
                        placeholder = { Text(stringResource(R.string.my_signs_placeholder), color = TextLight) },
                        singleLine = true,
                        isError = errorMsg != null
                    )
                    errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.my_signs_camera_hint), fontSize = 12.sp, color = TextLight)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val word = wordInput.trim().lowercase()
                    when {
                        word.isBlank() -> errorMsg = context.getString(R.string.my_signs_error_empty)
                        word.length < 2 -> errorMsg = context.getString(R.string.my_signs_error_short)
                        UserSignManager.bundledSignExists(context, word) -> errorMsg = context.getString(R.string.my_signs_error_bundled, word)
                        UserSignManager.userSignExists(context, word) -> errorMsg = context.getString(R.string.my_signs_error_exists, word)
                        else -> {
                            pendingWord = word
                            showDialog = false
                            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10)
                                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
                                putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
                            }
                            cameraLauncher.launch(intent)
                        }
                    }
                }) { Text(stringResource(R.string.my_signs_record), fontWeight = FontWeight.Bold, color = AppGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.my_signs_cancel)) }
            }
        )
    }

    // Delete confirmation
    deleteTarget?.let { word ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.my_signs_delete_title)) },
            text = { Text(stringResource(R.string.my_signs_delete_confirm, word)) },
            confirmButton = {
                TextButton(onClick = {
                    UserSignManager.deleteSign(context, word.lowercase())
                    signs = UserSignManager.getAllUserSigns(context)
                    deleteTarget = null
                }) { Text(stringResource(R.string.my_signs_delete), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.my_signs_cancel)) }
            }
        )
    }
}
