
package com.rines.esp32c3flasher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RinesDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D9FF),
    onPrimary = Color(0xFF002B35),

    primaryContainer = Color(0xFF004B5A),
    onPrimaryContainer = Color(0xFFB8F3FF),

    secondary = Color(0xFF42A5F5),
    onSecondary = Color(0xFF002B50),

    secondaryContainer = Color(0xFF123B5A),
    onSecondaryContainer = Color(0xFFD1E8FF),

    tertiary = Color(0xFF00E5A8),
    onTertiary = Color(0xFF00382A),

    background = Color(0xFF080D14),
    onBackground = Color(0xFFE6F1FA),

    surface = Color(0xFF111A24),
    onSurface = Color(0xFFE6F1FA),

    surfaceVariant = Color(0xFF1C2936),
    onSurfaceVariant = Color(0xFFB5C7D8),

    outline = Color(0xFF526779),
    outlineVariant = Color(0xFF304253),

    error = Color(0xFFFF5252),
    onError = Color(0xFF3B0000),

    errorContainer = Color(0xFF5C1717),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun RinesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RinesDarkColorScheme,
        content = content
    )
}
