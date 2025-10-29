package com.cs407.festify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,

    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    surfaceVariant = Color(0xFF3A3A3C),

    onBackground = Color(0xFFF2F2F7),
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93),

    primaryContainer = Color(0xFF4A5568),
    secondaryContainer = Color(0xFF5A6C7D),
    tertiaryContainer = Color(0xFF6B8E7F),

    onPrimaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFFD1D1D6),

    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF9F9F9),

    onBackground = Color(0xFF2E2E2E),
    onSurface = Color(0xFF2E2E2E),
    onSurfaceVariant = Color.Gray,

    primaryContainer = Color(0xFFA7C7E7),
    secondaryContainer = Color(0xFFB8E0D2),
    tertiaryContainer = Color(0xFFEAEAEA),

    onPrimaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFF2E2E2E),

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0)
)

val LocalDarkMode = compositionLocalOf { mutableStateOf(false) }

@Composable
fun FestifyTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkModeState = LocalDarkMode.current
    val darkTheme by darkModeState

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}