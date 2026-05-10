package com.isharaai.isl.feature.history
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.isharaai.isl.R
import com.isharaai.isl.core.db.ChatSessionEntity
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.core.ui.IsharaListCard
import com.isharaai.isl.core.ui.IsharaListIcon
import com.isharaai.isl.core.ui.IsharaTopAppBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit,
    currentSessionId: String,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

    Scaffold(
        topBar = {
            IsharaTopAppBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
                actions = {
                    if (sessions.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete all",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        containerColor = BackgroundCream
    ) { padding ->
        if (sessions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextMedium,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.history_empty),
                        color = TextMedium,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        isCurrentSession = session.id == currentSessionId,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }

    // Delete single session dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.history_delete_yes), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.btn_back))
                }
            }
        )
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.history_delete_all_title)) },
            text = { Text(stringResource(R.string.history_delete_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllSessions(currentSessionId)
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.history_delete_yes), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.btn_back))
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: ChatSessionEntity,
    isCurrentSession: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    IsharaListCard(
        onClick = onClick,
        cornerRadius = 14.dp,
        elevation = 1.dp,
        contentPadding = 16.dp
    ) {
        IsharaListIcon(
            icon = Icons.Default.ChatBubbleOutline,
            iconTint = PrimaryPurple,
            iconSize = 22.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDate(session.updatedAt),
                fontSize = 12.sp,
                color = TextMedium
            )
        }
        if (!isCurrentSession) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
