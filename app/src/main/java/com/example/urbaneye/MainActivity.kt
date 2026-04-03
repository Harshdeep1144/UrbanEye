package com.example.urbaneye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.urbaneye.ui.screens.DetectionScreen
import com.example.urbaneye.ui.screens.MapScreen
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
                    Screen.Map,
                    Screen.Detection
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            if (screen == Screen.Map) Icons.Filled.Home else Icons.Filled.Search,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Map.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Map.route) {
                            MapScreen(onNavigateToDetection = {
                                navController.navigate(Screen.Detection.route)
                            })
                        }
                        composable(Screen.Detection.route) {
                            DetectionScreen()
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Map : Screen("map")
    object Detection : Screen("detection")
}
