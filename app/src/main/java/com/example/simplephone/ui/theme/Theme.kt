package com.example.simplephone.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// High contrast dark mode colors for better accessibility
private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7), // Lighter blue for dark mode
    onPrimary = AccessibleBlack,
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFFCE93D8),
    background = AccessibleBlack,
    onBackground = AccessibleWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = AccessibleWhite,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242) // Darker gray for disabled states
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = AccessibleBlack,
    onBackground = AccessibleWhite,
    surface = AccessibleBlack,
    onSurface = AccessibleWhite
)

private val LightColorScheme = lightColorScheme(
    primary = HighContrastBlue,
    onPrimary = AccessibleWhite,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = AccessibleWhite,
    onBackground = AccessibleBlack,
    surface = AccessibleWhite,
    onSurface = AccessibleBlack,
    outlineVariant = Color(0xFF424242) // Darker gray for disabled states (was light gray)

    /* Other default colors to override
    surfaceStart = ...
    surfaceEnd = ...
    */
)

@Composable
fun SimplePhoneTheme(
    darkThemeOption: Int = 0, // 0=System, 1=Light, 2=Dark
    useHighContrastDark: Boolean = true, // Use high contrast dark mode
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // DISABLED for consistent High Contrast
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkThemeOption) {
        1 -> false // Force Light
        2 -> true  // Force Dark
        else -> systemDark // Follow System
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> if (useHighContrastDark) HighContrastDarkColorScheme else DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
