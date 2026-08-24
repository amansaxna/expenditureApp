package com.example.myexpenditureapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDeep,
    primaryContainer = GoldDark,
    onPrimaryContainer = Color.White,
    secondary = Slate,
    onSecondary = Color.White,
    tertiary = NavyLight,
    onTertiary = Color.White,
    background = NavyDeep,
    onBackground = OffWhite,
    surface = NavyLight,
    onSurface = OffWhite,
    error = ExpenseRed
)

private val LightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = GoldDark,
    onSecondary = NavyDeep,
    tertiary = Gold,
    onTertiary = NavyDeep,
    background = Color(0xFFF8F9FA),
    onBackground = NavyDeep,
    surface = Color.White,
    onSurface = NavyDeep,
    error = ExpenseRed
)

@Composable
fun MyExpenditureAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
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
