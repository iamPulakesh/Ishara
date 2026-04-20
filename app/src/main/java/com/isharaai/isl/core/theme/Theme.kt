package com.isharaai.isl.core.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun IsharaAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = CameraBlue,
            secondary = MicGreen,
            tertiary = TypeOrange,
            background = BackgroundCream,
            surface = CardWhite,
            onPrimary = CardWhite,
            onBackground = TextDark,
            onSurface = TextDark,
            error = ErrorRed,
        ),
        typography = IsharaTypography,
        content = content
    )
}
