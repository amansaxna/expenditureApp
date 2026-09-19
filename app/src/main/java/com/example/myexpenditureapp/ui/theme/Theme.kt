package com.example.myexpenditureapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    inversePrimary = VioletPrimary,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = GoldAccent,
    onTertiary = ObsidianDark,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = ObsidianDark,
    onBackground = TextWhite,
    surface = ObsidianSurface,
    onSurface = TextWhite,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextGrayLight,
    surfaceTint = IndigoPrimary,
    inverseSurface = AlpineBackground,
    inverseOnSurface = TextDark,
    surfaceContainerLowest = Color(0xFF05080E),
    surfaceContainerLow = ObsidianDark,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianCard,
    surfaceContainerHighest = ObsidianCardElevated,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1E293B),
    scrim = Color(0xFF000000),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    inversePrimary = Color(0xFF818CF8),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFFBEB),
    onTertiaryContainer = Color(0xFF78350F),
    background = AlpineBackground,
    onBackground = TextDark,
    surface = AlpineSurface,
    onSurface = TextDark,
    surfaceVariant = AlpineCard,
    onSurfaceVariant = TextDarkMuted,
    surfaceTint = Color(0xFF4F46E5),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = AlpineBackground,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = AlpineBackground,
    surfaceContainer = AlpineCard,
    surfaceContainerHigh = AlpineCardElevated,
    surfaceContainerHighest = AlpineBorder,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color(0xFF000000),
    error = ExpenseRedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun MyExpenditureAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
