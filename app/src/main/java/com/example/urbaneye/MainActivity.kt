package com.example.urbaneye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.urbaneye.ui.screens.*
import com.example.urbaneye.ui.screens.agent.AgentScreen
import com.example.urbaneye.ui.screens.detection.DetectionScreen
import com.example.urbaneye.ui.screens.home.HomeScreen
import com.example.urbaneye.ui.screens.profile.ProfileScreen
import com.example.urbaneye.ui.theme.UrbanEyeTheme
import com.example.urbaneye.ui.theme.LocalIsDarkTheme
import com.example.urbaneye.ui.theme.LocalThemeToggle
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash    : Screen("splash",    "Splash",    Icons.Rounded.Home)
    object Home      : Screen("home",      "Home",      Icons.Rounded.Map)
    object Detection : Screen("detection", "Scan",      Icons.Rounded.PhotoCamera)
    object Agent     : Screen("agent",     "AI",        Icons.Rounded.AutoAwesome)
    object Profile   : Screen("profile",   "Profile",   Icons.Rounded.Person)
}

private val BottomNavScreens = listOf(Screen.Home, Screen.Detection, Screen.Agent, Screen.Profile)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sharedPrefs = remember { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("dark_mode", systemDark)) }

            val toggleTheme: (Boolean) -> Unit = { isDark ->
                isDarkTheme = isDark
                sharedPrefs.edit().putBoolean("dark_mode", isDark).apply()
            }

            CompositionLocalProvider(
                LocalThemeToggle provides toggleTheme,
                LocalIsDarkTheme provides isDarkTheme
            ) {
                UrbanEyeTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    val backstackEntry by navController.currentBackStackEntryAsState()
                    val currentDest    = backstackEntry?.destination
                val showNav        = currentDest?.route in BottomNavScreens.map { it.route }

                Scaffold(
                    modifier       = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar      = {
                        AnimatedVisibility(
                            visible = showNav,
                            enter   = fadeIn(tween(200)) + slideInVertically { it },
                            exit    = fadeOut(tween(200)) + slideOutVertically { it },
                        ) {
                            NavigationBar(
                                containerColor  = MaterialTheme.colorScheme.surface,
                                tonalElevation  = 0.dp,
                            ) {
                                BottomNavScreens.forEach { screen ->
                                    val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                screen.icon, null,
                                                modifier = Modifier.size(if (selected) 24.dp else 22.dp),
                                            )
                                        },
                                        label    = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                                        selected = selected,
                                        colors   = NavigationBarItemDefaults.colors(
                                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        ),
                                        onClick  = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController    = navController,
                        startDestination = Screen.Splash.route,
                        modifier         = Modifier.fillMaxSize(),
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(
                                contentPadding       = innerPadding,
                                onNavigateToDetection = { navController.navigate(Screen.Detection.route) },
                            )
                        }
                        composable(Screen.Detection.route) {
                            DetectionScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.Agent.route)   { AgentScreen() }
                        composable(Screen.Profile.route) { ProfileScreen() }
                    }
                }
            }
        }
    }
}
}