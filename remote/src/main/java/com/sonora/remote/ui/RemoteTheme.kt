package com.sonora.remote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OriginalColors = darkColorScheme(
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

private fun palette(themeId: String) = when (themeId) {
    "aurora" -> darkColorScheme(
        primary = Color(0xFF75E8D0), onPrimary = Color(0xFF062A28),
        secondary = Color(0xFFB8A3FF), background = Color(0xFF08151C),
        onBackground = Color(0xFFE9FAF6), surface = Color(0xFF142630),
        onSurface = Color(0xFFE9FAF6), surfaceVariant = Color(0xFF203844),
        onSurfaceVariant = Color(0xFFB1C8CC)
    )
    "ocean" -> darkColorScheme(
        primary = Color(0xFF68C8FF), onPrimary = Color(0xFF06243B),
        secondary = Color(0xFF7E9DFF), background = Color(0xFF071525),
        onBackground = Color(0xFFEAF5FF), surface = Color(0xFF12263B),
        onSurface = Color(0xFFEAF5FF), surfaceVariant = Color(0xFF1C3550),
        onSurfaceVariant = Color(0xFFB1C7DC)
    )
    "sunset" -> darkColorScheme(
        primary = Color(0xFFFFA878), onPrimary = Color(0xFF35170D),
        secondary = Color(0xFFFF85B5), background = Color(0xFF1B101D),
        onBackground = Color(0xFFFFF1EA), surface = Color(0xFF2A1B2B),
        onSurface = Color(0xFFFFF1EA), surfaceVariant = Color(0xFF3A2638),
        onSurfaceVariant = Color(0xFFD5B9C6)
    )
    "forest" -> darkColorScheme(
        primary = Color(0xFFA6E5A5), onPrimary = Color(0xFF102B18),
        secondary = Color(0xFF58C99B), background = Color(0xFF091A16),
        onBackground = Color(0xFFECF8EE), surface = Color(0xFF152820),
        onSurface = Color(0xFFECF8EE), surfaceVariant = Color(0xFF21392E),
        onSurfaceVariant = Color(0xFFB6CCBE)
    )
    "rose" -> darkColorScheme(
        primary = Color(0xFFFF91BE), onPrimary = Color(0xFF351225),
        secondary = Color(0xFFB9A0FF), background = Color(0xFF1B111D),
        onBackground = Color(0xFFFFF0F8), surface = Color(0xFF2A1B2D),
        onSurface = Color(0xFFFFF0F8), surfaceVariant = Color(0xFF3A2740),
        onSurfaceVariant = Color(0xFFD5BED6)
    )
    else -> OriginalColors
}

@Composable
fun SonoraRemoteTheme(themeId: String = "original", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = palette(themeId),
        content = content
    )
}
