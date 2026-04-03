package com.example.urbaneye.ui.screens.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToDetection: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // --- Status Bar Configuration ---
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    // State Management
    var sourceAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    val historyItems = listOf(
        HistoryItem("Home", "123 Maple Street, Bangalore", Icons.Default.Home),
        HistoryItem("Office", "Tech Park Tower B, Whitefield", Icons.Default.Home),
        HistoryItem("Starbucks", "Koramangala 5th Block", Icons.Default.Home),
        HistoryItem("Gym", "Power Fitness, Indiranagar", Icons.Default.Home)
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Initialize Map
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(12.9716, 77.5946)) // Default Bangalore
        }
    }

    // Lifecycle management for OSMDroid (Crucial for physical devices)
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    // Helper: Fetch location and Reverse Geocode
    val requestLocationUpdate = {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(geoPoint)

                    // Reverse Geocode to get address string
                    scope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            val addressText = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0).split(",")[0] // Short address
                            } else {
                                "Current Location"
                            }
                            withContext(Dispatchers.Main) {
                                sourceAddress = addressText
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                sourceAddress = "Current Location"
                            }
                        }
                    }
                }
            }
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocationUpdate()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 320.dp + contentPadding.calculateBottomPadding(),
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = Color.White,
        sheetShadowElevation = 10.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color.Black.copy(alpha = 0.1f), CircleShape)
            )
        },
        sheetContent = {
            Box(modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())) {
                SearchAndHistorySheet(
                    source = sourceAddress,
                    onSourceChange = { sourceAddress = it },
                    destination = destinationAddress,
                    onDestinationChange = { destinationAddress = it },
                    history = historyItems,
                    onCheckSafety = onNavigateToDetection
                )
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Top Header Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        onClick = { /* Open Drawer */ }
                    ) {
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
                        }
                    }

                    // --- Updated Locate Me Button ---
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                requestLocationUpdate()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Use Current Location",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndHistorySheet(
    source: String,
    onSourceChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    history: List<HistoryItem>,
    onCheckSafety: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Where to, Harsh?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Pickup Input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(18.dp)
                    )
                    TextField(
                        value = source,
                        onValueChange = onSourceChange,
                        placeholder = { Text("Enter pickup location", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 1.dp,
                    color = Color.LightGray.copy(alpha = 0.3f)
                )

                // Destination Input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                    TextField(
                        value = destination,
                        onValueChange = onDestinationChange,
                        placeholder = {
                            Text("Enter destination", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(history) { item ->
                HistoryRow(item) {
                    onDestinationChange(item.subtitle)
                }
            }
        }

        AnimatedVisibility(
            visible = destination.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = onCheckSafety,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text("Check Road Safety", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFFF3F3F3)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
            }
        }

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

data class HistoryItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)