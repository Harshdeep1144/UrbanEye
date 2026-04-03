package com.example.urbaneye.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UberColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = DarkGrey,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    error = Color(0xFFB00020),
    onError = White
)

@Composable
fun UrbanEyeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UberColorScheme,
        typography = Typography,
        content = content
    )
}
