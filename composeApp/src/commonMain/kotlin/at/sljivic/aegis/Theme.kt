package com.example.compose

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = darkForeground,
            onPrimary = darkBackground,
            background = darkBackground,
            onBackground = darkForeground,
            surface = darkBackground,
            onSurface = darkForeground
        )
    } else {
        lightColorScheme(
            primary = lightForeground,
            onPrimary = lightBackground,
            background = lightBackground,
            onBackground = lightForeground,
            surface = lightBackground,
            onSurface = lightForeground
        )
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
