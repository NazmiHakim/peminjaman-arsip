package com.bpkpad.peminjaman.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// BPKPAD Primary: Old Green Theme
val BpkpadGreenDark = Color(0xFF0D631B)  // Primary
val BpkpadGreenLight = Color(0xFFACF4A4) // Primary Container / Accent 1
val BpkpadBlueAccent = Color(0xFFCFE6F2) // Accent 2

// Aliases to prevent compilation errors and map to the new aesthetic
val BpkpadBlue = BpkpadGreenDark
val BpkpadBlueDark = Color(0xFF071E27)
val BpkpadBlueLight = BpkpadGreenLight
val BpkpadAccent = BpkpadBlueAccent
val BpkpadGreen = Color(0xFF2E7D32) // Keep standard green for success states

val BpkpadGold = Color(0xFFF57F17)       // Warning / Overdue
val BpkpadRed = Color(0xFFC62828)        // Error / Rejected
val BpkpadOrange = Color(0xFFE65100)     // Bypass / Pending

val BpkpadSurface = Color(0xFFF9FAFA)    // Light surface
val BpkpadOnSurface = Color(0xFF071E27)
val BpkpadContainer = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = BpkpadGreenDark,
    onPrimary = Color.White,
    primaryContainer = BpkpadGreenLight,
    onPrimaryContainer = BpkpadGreenDark,
    secondary = BpkpadBlueAccent,
    onSecondary = BpkpadOnSurface,
    secondaryContainer = BpkpadBlueAccent,
    onSecondaryContainer = BpkpadOnSurface,
    tertiary = BpkpadGold,
    onTertiary = Color.White,
    error = BpkpadRed,
    onError = Color.White,
    background = BpkpadSurface,
    onBackground = BpkpadOnSurface,
    surface = BpkpadContainer,
    onSurface = BpkpadOnSurface,
    surfaceVariant = Color(0xFFE0E5E1),
    outline = Color(0xFFBFCABA)
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
