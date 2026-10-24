package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = Color(0xFF003554),
    primaryContainer = Color(0xFF004D74),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF73F8E7),
    tertiary = AmberTertiary,
    background = SlateBgDark,
    surface = SlateCardDark,
    surfaceVariant = SlateBoxDark,
    onBackground = SlateTextDark,
    onSurface = SlateTextDark,
    onSurfaceVariant = SlateMutedDark,
    outline = SlateBorderDark,
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = TealSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = AmberTertiary,
    background = SlateBgLight,
    surface = SlateCardLight,
    surfaceVariant = SlateBoxLight,
    onBackground = SlateTextLight,
    onSurface = SlateTextLight,
    onSurfaceVariant = SlateMutedLight,
    outline = SlateBorderLight,
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent engineering palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
