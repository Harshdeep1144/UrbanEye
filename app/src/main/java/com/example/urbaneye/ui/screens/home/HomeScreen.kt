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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
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

    // --- State Management ---
    var sourceAddress by remember { mutableStateOf("") }
    var destinationAddress by remember { mutableStateOf("") }
    var currentRouteOverlay by remember { mutableStateOf<Polyline?>(null) }
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }

    // NEW: Controls whether the bottom sheet is visible (hidden when destination is focused)
    var isSheetVisible by remember { mutableStateOf(true) }

    // NEW: Whether we are in "navigation planning" mode (destination field active, sheet hidden)
    var isDestinationFocused by remember { mutableStateOf(false) }

    // NEW: Track if location has been fetched (for the pulsing dot state)
    var isLocating by remember { mutableStateOf(false) }

    val historyItems = listOf(
        HistoryItem("Home", "123 Maple Street, Bangalore", Icons.Default.Home),
        HistoryItem("Office", "Tech Park Tower B, Whitefield", Icons.Default.Place),
        HistoryItem("Starbucks", "Koramangala 5th Block", Icons.Default.Favorite),
        HistoryItem("Gym", "Power Fitness, Indiranagar", Icons.Default.Star)
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
            controller.setCenter(GeoPoint(12.9716, 77.5946))
        }
    }

    // Blue Dot Location Overlay
    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            // Do NOT enableFollowLocation() by default — we control this manually
        }
    }

    // --- Draw Route (OSRM) ---
    val drawRoute = { destName: String ->
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val destAddresses = geocoder.getFromLocationName(destName, 1)

                if (!destAddresses.isNullOrEmpty()) {
                    val destPoint = GeoPoint(destAddresses[0].latitude, destAddresses[0].longitude)
                    val startPoint = locationOverlay.myLocation ?: GeoPoint(12.9716, 77.5946)

                    val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                            "${startPoint.longitude},${startPoint.latitude};" +
                            "${destPoint.longitude},${destPoint.latitude}?overview=full&geometries=geojson"

                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val routes = json.getJSONArray("routes")

                    if (routes.length() > 0) {
                        val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val routePoints = mutableListOf<GeoPoint>()

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            routePoints.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }

                        withContext(Dispatchers.Main) {
                            currentRouteOverlay?.let { mapView.overlays.remove(it) }
                            destinationMarker?.let { mapView.overlays.remove(it) }

                            val polyline = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#4285F4")
                                outlinePaint.strokeWidth = 12f
                            }
                            mapView.overlays.add(polyline)
                            currentRouteOverlay = polyline

                            val marker = Marker(mapView).apply {
                                position = destPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = destName
                            }
                            mapView.overlays.add(marker)
                            destinationMarker = marker

                            val box = BoundingBox.fromGeoPoints(routePoints)
                            mapView.zoomToBoundingBox(box.increaseByScale(1.3f), true)
                            mapView.invalidate()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Location Update: Snap to dot, reverse-geocode into source field ---
    val requestLocationUpdate = {
        isLocating = true
        locationOverlay.enableMyLocation()

        // NEW: Snap to location once, do NOT keep following (like Google Maps)
        locationOverlay.runOnFirstFix {
            val myLocation = locationOverlay.myLocation
            myLocation?.let { geoPoint ->
                (context as? Activity)?.runOnUiThread {
                    // Animate camera to location, then STOP following (user can pan freely)
                    mapView.controller.animateTo(geoPoint)
                    locationOverlay.disableFollowLocation()
                    isLocating = false
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                        val addressText = if (!addresses.isNullOrEmpty()) {
                            addresses[0].getAddressLine(0).split(",")[0]
                        } else {
                            "Current Location"
                        }
                        withContext(Dispatchers.Main) {
                            // NEW: Fills the source field in real-time
                            sourceAddress = addressText
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            sourceAddress = "Current Location"
                            isLocating = false
                        }
                    }
                }
            }
        }

        // Also use FusedLocation for faster first fix
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(geoPoint)

                    scope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            val addressText = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0).split(",")[0]
                            } else {
                                "Current Location"
                            }
                            withContext(Dispatchers.Main) {
                                sourceAddress = addressText
                                isLocating = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                sourceAddress = "Current Location"
                                isLocating = false
                            }
                        }
                    }
                }
            }
    }

    // Lifecycle for OSMDroid
    DisposableEffect(mapView) {
        mapView.onResume()
        mapView.overlays.add(locationOverlay)
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocationUpdate()
        }
    }

    // --- Root Layout: Map + overlaid UI ---
    Box(modifier = Modifier.fillMaxSize()) {

        // Full-screen Map
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // --- Top Header: Always visible ---
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
                // Menu button (only when sheet is visible)
                AnimatedVisibility(visible = !isDestinationFocused) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        onClick = { /* Open Drawer */ }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Black)
                        }
                    }
                }

                // Back button when destination is focused
                AnimatedVisibility(visible = isDestinationFocused) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        onClick = {
                            // Close destination mode: clear route, restore sheet
                            isDestinationFocused = false
                            isSheetVisible = true
                            destinationAddress = ""
                            currentRouteOverlay?.let { mapView.overlays.remove(it) }
                            destinationMarker?.let { mapView.overlays.remove(it) }
                            currentRouteOverlay = null
                            destinationMarker = null
                            mapView.invalidate()
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                    }
                }

                // "Use Current Location" pill button
                AnimatedVisibility(visible = !isDestinationFocused) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
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
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isLocating) "Locating..." else "Use Current Location",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // When destination is focused: show inline destination search bar at top
                AnimatedVisibility(visible = isDestinationFocused) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 6.dp
                    ) {
                        TextField(
                            value = destinationAddress,
                            onValueChange = { value ->
                                destinationAddress = value
                                if (value.length > 3) drawRoute(value)
                            },
                            placeholder = {
                                Text(
                                    "Search destination",
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEA4335),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (destinationAddress.isNotEmpty()) {
                                    IconButton(onClick = {
                                        destinationAddress = ""
                                        currentRouteOverlay?.let { mapView.overlays.remove(it) }
                                        destinationMarker?.let { mapView.overlays.remove(it) }
                                        currentRouteOverlay = null
                                        destinationMarker = null
                                        mapView.invalidate()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }
        }

        // --- Check Safety floating button (shown when route exists in destination mode) ---
        AnimatedVisibility(
            visible = isDestinationFocused && destinationAddress.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp + contentPadding.calculateBottomPadding())
        ) {
            Button(
                onClick = onNavigateToDetection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Check Road Safety", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // --- Bottom Sheet (hidden when destination is focused) ---
        AnimatedVisibility(
            visible = isSheetVisible && !isDestinationFocused,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color.Black.copy(alpha = 0.1f), CircleShape)
                        )
                    }

                    Box(modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())) {
                        SearchAndHistorySheet(
                            source = sourceAddress,
                            onSourceChange = { sourceAddress = it },
                            destination = destinationAddress,
                            onDestinationChange = { destinationAddress = it },
                            // NEW: When destination field is tapped, collapse sheet and enter destination mode
                            onDestinationFocused = {
                                isDestinationFocused = true
                                isSheetVisible = false
                            },
                            history = historyItems,
                            onCheckSafety = onNavigateToDetection
                        )
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
    onDestinationFocused: () -> Unit,  // NEW callback
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
                // Source Field
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

                // Destination Field — tapping collapses sheet
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDestinationFocused() }  // Intercept tap
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                    ) {
                        if (destination.isEmpty()) {
                            Text(
                                text = "Enter destination",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = destination,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        }
                    }
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
                    // Tapping history also enters destination mode with pre-filled text
                    onDestinationChange(item.subtitle)
                    onDestinationFocused()
                }
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
                Icon(
                    item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
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