package com.example.urbaneye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UrbanEyeTheme {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Home,
                    Screen.Detection,
                    Screen.Agent,
                    Screen.Profile
                )
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = currentDestination?.route in items.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = Color.White,
                                tonalElevation = 0.dp
                            ) {
                                items.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = when (screen) {
                                                    Screen.Home -> Icons.Filled.Home
                                                    Screen.Detection -> Icons.Filled.Search
                                                    Screen.Agent -> Icons.Filled.Build
                                                    Screen.Profile -> Icons.Filled.Person
                                                    else -> Icons.Filled.Home
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        },
                                        label = { Text(screen.title) },
                                        selected = selected,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = Color.Black,
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray,
                                            indicatorColor = Color.Transparent
                                        ),
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // Removed padding(innerPadding) to allow content to go behind status/nav bars
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(onNavigateToMain = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            })
                        }
                        composable(Screen.Home.route) {
                            // Pass innerPadding to screens that need to avoid the bottom bar
                            HomeScreen(
                                contentPadding = innerPadding,
                                onNavigateToDetection = {
                                    navController.navigate(Screen.Detection.route)
                                }
                            )
                        }
                        composable(Screen.Detection.route) {
                            DetectionScreen()
                        }
                        composable(Screen.Agent.route) {
                            AgentScreen()
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home")
    object Detection : Screen("detection", "Detection")
    object Agent : Screen("agent", "Agent")
    object Profile : Screen("profile", "Profile")
}
