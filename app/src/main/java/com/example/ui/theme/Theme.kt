package com.example.ui.theme

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

// Sleek Midnight Dark Mode
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,                  // Electric Indigo #6366F1
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,              // Neon Teal #10B981
    onSecondary = Color.Black,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = ElectricCyan,
    onTertiary = Color.Black,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,            // Deep navy/black #0B0F19
    onBackground = DarkTextPrimary,         // Off-white #F1F5F9
    surface = DarkSurface,                  // Dark charcoal #1E293B
    onSurface = DarkTextPrimary,            // Off-white #F1F5F9
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,   // Cool gray #94A3B8
    outline = DarkOutline,                  // Border #334155
    outlineVariant = DarkOutlineSubtle,
    error = DarkError,                      // #EF4444
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

// Modern Corporate Blue (Light Mode)
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,                 // Royal Blue #2563EB
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,             // Cyan #06B6D4
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = VibrantIndigo,
    onTertiary = Color.White,
    tertiaryContainer = LightPrimaryContainer,
    onTertiaryContainer = LightOnPrimaryContainer,
    background = LightBackground,           // Off-white #F8FAFC
    onBackground = LightTextPrimary,        // Dark slate #0F172A
    surface = LightSurface,                 // Pure white #FFFFFF
    onSurface = LightTextPrimary,           // Dark slate #0F172A
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,  // Muted gray #64748B
    outline = LightOutline,                 // Border #E2E8F0
    outlineVariant = LightOutlineSubtle,
    error = LightError,                     // #DC2626
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun DigitalHabitsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
