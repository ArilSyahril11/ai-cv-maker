package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    secondary = Sky400,
    tertiary = Amber400,
    background = Slate900,
    surface = Slate950,
    surfaceVariant = Slate800,
    onPrimary = Color.White,
    onSecondary = Slate900,
    onTertiary = Slate900,
    onBackground = Slate300,
    onSurface = Slate300,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

@Composable
fun MyApplicationTheme(
    // Force dark theme and ignore dynamic color to maintain the Tailwind brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Slate950.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
