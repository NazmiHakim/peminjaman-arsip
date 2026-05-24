package com.bpkpad.peminjaman.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// BPKPAD Primary: Deep Government Blue
val BpkpadBlue = Color(0xFF1565C0)       // Primary
val BpkpadBlueDark = Color(0xFF0D47A1)   // Primary Dark
val BpkpadBlueLight = Color(0xFF1976D2)  // Primary Light
val BpkpadAccent = Color(0xFF00BCD4)     // Cyan Accent
val BpkpadGold = Color(0xFFF57F17)       // Warning / Overdue
val BpkpadGreen = Color(0xFF2E7D32)      // Success / Returned
val BpkpadRed = Color(0xFFC62828)        // Error / Rejected
val BpkpadOrange = Color(0xFFE65100)     // Bypass / Pending
val BpkpadSurface = Color(0xFFF8FAFF)    // Light surface
val BpkpadOnSurface = Color(0xFF1A1C1E)
val BpkpadContainer = Color(0xFFE3F2FD)  // Blue container

private val LightColorScheme = lightColorScheme(
    primary = BpkpadBlue,
    onPrimary = Color.White,
    primaryContainer = BpkpadContainer,
    onPrimaryContainer = BpkpadBlueDark,
    secondary = BpkpadAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = BpkpadGold,
    onTertiary = Color.White,
    error = BpkpadRed,
    onError = Color.White,
    background = BpkpadSurface,
    onBackground = BpkpadOnSurface,
    surface = Color.White,
    onSurface = BpkpadOnSurface,
    surfaceVariant = Color(0xFFDEE3EB),
    outline = Color(0xFF73777F)
)

@Composable
fun BpkpadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force light theme for government app consistency
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = BpkpadTypography,
        content = content
    )
}
