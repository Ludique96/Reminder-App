package com.example.arrivalreminders

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6B5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0DF),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8DE),
    onSecondaryContainer = Color(0xFF06201A),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF6FAF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6C2),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFFB7F0DF),
    secondary = Color(0xFFB1CCC2),
    onSecondary = Color(0xFF1C352E),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCCE8DE),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDEE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF89938F),
)

/** Follows the phone's light/dark setting. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
