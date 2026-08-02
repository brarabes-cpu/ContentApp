package com.example.contentapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    primaryContainer = AccentBlueContainer,
    background = SurfaceBackground,
    surface = SurfaceBackground,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ContentAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
