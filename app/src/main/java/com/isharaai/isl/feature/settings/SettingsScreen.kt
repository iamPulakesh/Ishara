package com.isharaai.isl.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.R
import com.isharaai.isl.core.theme.*
import com.isharaai.isl.core.language.LanguageManager
import com.isharaai.isl.core.ui.IsharaListCard
import com.isharaai.isl.core.ui.IsharaListIcon
import com.isharaai.isl.core.ui.IsharaTopAppBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHistoryClick: () -> Unit = {},
    onMySignsClick: () -> Unit = {},
    onReplayTutorial: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentLang by remember { mutableStateOf(LanguageManager.getCurrentLanguage(context)) }

    Scaffold(
        topBar = {
            IsharaTopAppBar(
                title = stringResource(R.string.settings),
                onBack = onBack
            )
        },
        containerColor = BackgroundCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Chat History
            SettingsItem(
                icon = Icons.Default.History,
                iconTint = PrimaryPurple,
                title = stringResource(R.string.history_title),
                description = stringResource(R.string.history_desc),
                onClick = onHistoryClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // My Signs
            SettingsItem(
                icon = Icons.Default.Videocam,
                iconTint = AppGreen,
                title = stringResource(R.string.my_signs_title),
                description = stringResource(R.string.my_signs_desc),
                onClick = onMySignsClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // English option
                    LanguageOption(
                        label = "English",
                        flag = "\uD83C\uDDFA\uD83C\uDDF8",
                        isSelected = currentLang == LanguageManager.LANG_ENGLISH,
                        onClick = {
                            currentLang = LanguageManager.LANG_ENGLISH
                            LanguageManager.setLanguage(context, LanguageManager.LANG_ENGLISH)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bengali option
                    LanguageOption(
                        label = "Bengali(\u09AC\u09BE\u0982\u09B2\u09BE)",
                        flag = "\uD83C\uDDEE\uD83C\uDDF3",
                        isSelected = currentLang == LanguageManager.LANG_BENGALI,
                        onClick = {
                            currentLang = LanguageManager.LANG_BENGALI
                            LanguageManager.setLanguage(context, LanguageManager.LANG_BENGALI)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Replay Tutorial
            SettingsItem(
                icon = Icons.Default.Refresh,
                iconTint = AppGreen,
                title = stringResource(R.string.tutorial_replay),
                description = stringResource(R.string.tutorial_replay_desc),
                onClick = onReplayTutorial
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Attribution Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.attribution_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.attribution_islrtc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// Reusable settings menu item card
@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    IsharaListCard(onClick = onClick) {
        IsharaListIcon(
            icon = icon,
            iconTint = iconTint,
            contentDescription = title
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMedium
            )
        }
    }
}

//Languages Option
@Composable
private fun LanguageOption(
    label: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryPurple.copy(alpha = 0.1f) else CardElevated,
        label = "langBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryPurple else TextDark,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
        }
    }
}
