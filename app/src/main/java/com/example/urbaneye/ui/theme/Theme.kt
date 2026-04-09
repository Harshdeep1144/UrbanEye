package com.example.urbaneye.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark Color Scheme ──────────────────────────────────────────────────────
// Inspired by the "Okay you spend" screen with deep blacks and vibrant white text.

private val DarkColors = darkColorScheme(
    primary                = UrbanEyeColors.PureWhite,
    onPrimary              = UrbanEyeColors.DeepBlack,
    primaryContainer       = UrbanEyeColors.Gray800,
    onPrimaryContainer     = UrbanEyeColors.PureWhite,

    secondary              = UrbanEyeColors.Mint,
    onSecondary            = UrbanEyeColors.DeepBlack,

    background             = UrbanEyeColors.DeepBlack,
    onBackground           = UrbanEyeColors.PureWhite,
    surface                = UrbanEyeColors.SurfaceDark,
    onSurface              = UrbanEyeColors.PureWhite,
    surfaceVariant         = UrbanEyeColors.CardDark,
    onSurfaceVariant       = UrbanEyeColors.Gray400,

    outline                = UrbanEyeColors.Gray600,
    error                  = UrbanEyeColors.ErrorRed,
)

// ─── Light Color Scheme ─────────────────────────────────────────────────────
// Inspired by the Lavender splash and the Bill payment screens.

private val LightColors = lightColorScheme(
    primary                = UrbanEyeColors.BankBlack,
    onPrimary              = UrbanEyeColors.PureWhite,
    primaryContainer       = UrbanEyeColors.Gray200,
    onPrimaryContainer     = UrbanEyeColors.BankBlack,

    secondary              = UrbanEyeColors.HoloPurple,
    onSecondary            = UrbanEyeColors.PureWhite,

    background             = UrbanEyeColors.Lavender,
    onBackground           = UrbanEyeColors.BankBlack,
    surface                = UrbanEyeColors.PureWhite,
    onSurface              = UrbanEyeColors.BankBlack,
    surfaceVariant         = UrbanEyeColors.Gray100,
    onSurfaceVariant       = UrbanEyeColors.Gray600,

    outline                = UrbanEyeColors.Gray200,
    error                  = UrbanEyeColors.ErrorRed,
)

@Composable
fun UrbanEyeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val colorScheme = if (darkTheme) DarkColors else LightColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                // In light theme, we want dark icons. In dark theme, light icons.
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}

val LocalThemeToggle = androidx.compose.runtime.staticCompositionLocalOf<(Boolean) -> Unit> { {} }
val LocalIsDarkTheme = androidx.compose.runtime.staticCompositionLocalOf<Boolean> { true }