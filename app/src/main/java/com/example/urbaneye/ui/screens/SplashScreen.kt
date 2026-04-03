package com.example.urbaneye.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.urbaneye.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    val view = LocalView.current
    val scale = remember { Animatable(0f) }

    // Configure status bar for Splash (Transparent with Light Icons)
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 0.8f,
            animationSpec = tween(
                durationMillis = 800,
                easing = {
                    OvershootInterpolator(4f).getInterpolation(it)
                }
            )
        )
        delay(1500L)
        onNavigateToMain()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Uber Black
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale.value)
            )
            Text(
                text = "URBAN EYE",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 4.sp
            )
        }
    }
}

class OvershootInterpolator(private val tension: Float) {
    fun getInterpolation(t: Float): Float {
        var tValue = t
        tValue -= 1.0f
        return tValue * tValue * ((tension + 1) * tValue + tension) + 1.0f
    }
}
