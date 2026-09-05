package com.example.myexpenditureapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentVibrant,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = OffWhite,
    secondary = AccentSuccess,
    onSecondary = Color.White,
    secondaryContainer = NavyLighter,
    onSecondaryContainer = AccentSuccess,
    tertiary = Gold,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = OffWhite,
    surface = NavyLight,
    onSurface = OffWhite,
    surfaceVariant = NavyLighter,
    onSurfaceVariant = Slate,
    outline = NavyAccent,
    error = AccentError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentVibrant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AccentSuccess,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = GoldDark,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = NavyDeep,
    surface = Color.White,
    onSurface = NavyDeep,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = AccentError,
    onError = Color.White
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun MyExpenditureAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
