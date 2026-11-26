package com.sonora.remote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SonoraColors = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF211307),
    secondary = Color(0xFFB5C2D8),
    background = Color(0xFF090B10),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF151923),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFB9C0CC)
)

@Composable
fun SonoraRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonoraColors,
        content = content
    )
}
