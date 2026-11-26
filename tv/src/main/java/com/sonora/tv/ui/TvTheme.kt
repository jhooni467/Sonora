package com.sonora.tv.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SonoraTvColors = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF211307),
    secondary = Color(0xFFB5C2D8),
    background = Color(0xFF080A0F),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF141923),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFB9C0CC)
)

@Composable
fun SonoraTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonoraTvColors,
        content = content
    )
}
