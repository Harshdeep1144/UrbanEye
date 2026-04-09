package com.example.urbaneye.ui.screens.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.ui.theme.UrbanEyeColors
import com.example.urbaneye.ui.utils.SetStatusBarColor
import com.example.urbaneye.ui.viewmodel.MapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HOME_PREFS   = "urbaneye_home_prefs"
private const val KEY_HISTORY  = "search_history"
private const val MAX_HISTORY  = 8
private enum class PanelStop { Collapsed, Half, Full }

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToDetection: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val view    = LocalView.current
    val scope   = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme   = MaterialTheme.colorScheme
    // Changed: routeLineArgb is now a specific Vivid Blue instead of using primary color
    val routeLineArgb = remember { Color(0xFF2563EB).toArgb() }

    // Edge-to-edge transparent status bar
    SetStatusBarColor(backgroundColor = Color.Transparent, darkIcons = true)

    // ── State ────────────────────────────────────────────────────
    var sourceAddress       by remember { mutableStateOf("") }
    var destinationAddress  by remember { mutableStateOf("") }
    var suggestions         by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentRoute        by remember { mutableStateOf<Polyline?>(null) }
    var destMarker          by remember { mutableStateOf<Marker?>(null) }
    var routeJob            by remember { mutableStateOf<Job?>(null) }
    var suggestionJob       by remember { mutableStateOf<Job?>(null) }
    var isLocating          by remember { mutableStateOf(false) }
    var searchHistory       by remember { mutableStateOf(loadSearchHistory(context)) }

    fun addHistory(entry: String) {
        val s = entry.trim().ifBlank { return }
        searchHistory = listOf(s) + searchHistory.filterNot { it.equals(s, true) }.take(MAX_HISTORY - 1)
        saveSearchHistory(context, searchHistory)
    }

    // ── Map setup ────────────────────────────────────────────────
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(30.5284, 76.7015))
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
        }
    }
    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() }
    }
    val potholeFolder = remember { FolderOverlay() }

    LaunchedEffect(uiState.potholes) {
        potholeFolder.items.clear()
        uiState.potholes.forEach { data ->
            val radius = when (data.pothole.severity) {
                PotholeSeverity.HIGH -> 100.0; PotholeSeverity.MEDIUM -> 60.0; PotholeSeverity.LOW -> 30.0
            }
            potholeFolder.add(Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(GeoPoint(data.pothole.latitude, data.pothole.longitude), radius)
                fillPaint.color = data.color; fillPaint.alpha = 90
                outlinePaint.color = data.color; outlinePaint.strokeWidth = 4f
                title = "Risk: ${data.pothole.severity}"
            })
        }
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        mapView.overlays.add(locationOverlay)
        mapView.overlays.add(potholeFolder)
        onDispose { locationOverlay.disableMyLocation(); mapView.onPause() }
    }

    fun clearRoute() {
        currentRoute?.let { mapView.overlays.remove(it) }
        destMarker?.let   { mapView.overlays.remove(it) }
        currentRoute = null; destMarker = null; mapView.invalidate()
    }

    val drawRoute: (String) -> Unit = { dest ->
        scope.launch(Dispatchers.IO) {
            try {
                val geo   = Geocoder(context, Locale.getDefault())
                val addr  = geo.getFromLocationName(dest, 1)?.firstOrNull() ?: return@launch
                val destPt = GeoPoint(addr.latitude, addr.longitude)
                val srcPt  = locationOverlay.myLocation ?: GeoPoint(30.5284, 76.7015)
                val url    = URL(
                    "https://router.project-osrm.org/route/v1/driving/" +
                            "${srcPt.longitude},${srcPt.latitude};${destPt.longitude},${destPt.latitude}" +
                            "?overview=full&geometries=geojson"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000; readTimeout = 10_000
                }
                val coords = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("routes").getJSONObject(0)
                    .getJSONObject("geometry").getJSONArray("coordinates")
                val pts = MutableList(coords.length()) { i ->
                    coords.getJSONArray(i).let { GeoPoint(it.getDouble(1), it.getDouble(0)) }
                }
                withContext(Dispatchers.Main) {
                    clearRoute()
                    val poly = Polyline().apply {
                        setPoints(pts)
                        outlinePaint.color       = routeLineArgb
                        outlinePaint.strokeWidth  = 12f
                        outlinePaint.strokeCap    = android.graphics.Paint.Cap.ROUND
                    }
                    mapView.overlays.add(poly); currentRoute = poly
                    val marker = Marker(mapView).apply {
                        position = destPt
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = dest
                    }
                    mapView.overlays.add(marker); destMarker = marker
                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(pts).increaseByScale(1.25f), true)
                    mapView.invalidate()
                }
            } catch (_: Exception) {}
        }
    }

    val locate = {
        isLocating = true; locationOverlay.enableMyLocation()
        locationOverlay.runOnFirstFix {
            val loc = locationOverlay.myLocation
            if (loc == null) { (context as? Activity)?.runOnUiThread { isLocating = false }; return@runOnFirstFix }
            (context as? Activity)?.runOnUiThread { mapView.controller.animateTo(loc); isLocating = false }
            scope.launch(Dispatchers.IO) {
                val line = try {
                    Geocoder(context, Locale.getDefault())
                        .getFromLocation(loc.latitude, loc.longitude, 1)
                        ?.firstOrNull()?.getAddressLine(0)
                } catch (_: Exception) { null }
                withContext(Dispatchers.Main) {
                    sourceAddress = line ?: "${loc.latitude.fmt(4)}, ${loc.longitude.fmt(4)}"
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) locate() }

    LaunchedEffect(destinationAddress) {
        suggestionJob?.cancel(); routeJob?.cancel()
        if (destinationAddress.length < 3) {
            suggestions = emptyList()
            if (destinationAddress.isBlank()) clearRoute()
            return@LaunchedEffect
        }
        suggestionJob = scope.launch(Dispatchers.IO) {
            delay(240)
            withContext(Dispatchers.Main) { suggestions = fetchSuggestions(context, destinationAddress) }
        }
        routeJob = scope.launch { delay(600); drawRoute(destinationAddress) }
    }

    // ── Layout ───────────────────────────────────────────────────
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density       = LocalDensity.current
        val screenHpx     = with(density) { maxHeight.toPx() }
        val collapsedPx   = screenHpx - with(density) { 160.dp.toPx() }
        val halfPx        = screenHpx * 0.46f
        val fullPx        = with(density) { 96.dp.toPx() }
        var panelTop      by remember { mutableFloatStateOf(halfPx) }
        var dragging      by remember { mutableStateOf(false) }
        val animPanelTop  by animateFloatAsState(
            targetValue  = panelTop,
            animationSpec = tween(if (dragging) 0 else 280),
            label        = "panel",
        )

        fun snap(px: Float): PanelStop = listOf(
            PanelStop.Collapsed to abs(px - collapsedPx),
            PanelStop.Half      to abs(px - halfPx),
            PanelStop.Full      to abs(px - fullPx),
        ).minBy { it.second }.first

        fun stopPx(s: PanelStop) = when (s) {
            PanelStop.Collapsed -> collapsedPx; PanelStop.Half -> halfPx; PanelStop.Full -> fullPx
        }

        // ── Map ──────────────────────────────────────────────────
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // ── Top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Logo pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 10.dp,
            ) {
                Row(
                    modifier           = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment  = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier          = Modifier.size(8.dp).clip(CircleShape)
                            .background(colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "UrbanEye",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color      = colorScheme.onSurface,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            // My location FAB
            Surface(
                shape           = CircleShape,
                color           = UrbanEyeColors.HoloPurple,
                shadowElevation = 10.dp,
                onClick         = {
                    val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (perm == PackageManager.PERMISSION_GRANTED) locate()
                    else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (isLocating) CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp, color = colorScheme.onPrimary
                    )
                    else Icon(Icons.Default.MyLocation, null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Expand FAB when collapsed ─────────────────────────────
        AnimatedVisibility(
            visible = snap(animPanelTop) == PanelStop.Collapsed,
            enter   = fadeIn() + slideInVertically { 40 },
            exit    = fadeOut() + slideOutVertically { 40 },
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            Surface(
                shape           = RoundedCornerShape(28.dp),
                color           = colorScheme.primary,
                shadowElevation = 14.dp,
                onClick         = { panelTop = stopPx(PanelStop.Half) },
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, null, tint = colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Search Route", style = MaterialTheme.typography.labelLarge, color = colorScheme.onPrimary)
                }
            }
        }

        // ── Bottom Sheet ─────────────────────────────────────────
        Surface(
            modifier        = Modifier
                .offset { IntOffset(0, animPanelTop.roundToInt()) }
                .fillMaxWidth()
                .height(maxHeight)
                .navigationBarsPadding(),
            shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color           = colorScheme.surface,
            shadowElevation = 24.dp,
        ) {
            Column(Modifier.fillMaxSize()) {

                // Drag handle area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state       = rememberDraggableState { delta ->
                                dragging = true
                                panelTop = (panelTop + delta).coerceIn(fullPx, collapsedPx)
                            },
                            onDragStopped = { dragging = false; panelTop = stopPx(snap(panelTop)) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(colorScheme.outlineVariant),
                    )
                }

                // Panel content
                SearchPanelContent(
                    source             = sourceAddress,
                    destination        = destinationAddress,
                    suggestions        = suggestions,
                    history            = searchHistory,
                    onSourceChange     = { sourceAddress = it; panelTop = stopPx(PanelStop.Full) },
                    onDestinationChange= { destinationAddress = it; panelTop = stopPx(PanelStop.Full) },
                    onSwap             = {
                        sourceAddress = destinationAddress.also { destinationAddress = sourceAddress }
                        if (destinationAddress.isNotBlank()) { addHistory(destinationAddress); drawRoute(destinationAddress) }
                        else clearRoute()
                    },
                    onDestFocused      = { panelTop = stopPx(PanelStop.Full) },
                    onSearch           = {
                        if (destinationAddress.isNotBlank()) { addHistory(destinationAddress); drawRoute(destinationAddress) }
                        panelTop = stopPx(PanelStop.Collapsed)
                    },
                    onSuggestionPick   = { s ->
                        destinationAddress = s; suggestions = emptyList()
                        addHistory(s); drawRoute(s)
                    },
                    onHistoryPick      = { s ->
                        destinationAddress = s; addHistory(s); drawRoute(s)
                        panelTop = stopPx(PanelStop.Half)
                    },
                    onUseCurrent       = {
                        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        if (perm == PackageManager.PERMISSION_GRANTED) locate()
                        else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    contentPadding     = contentPadding,
                )

                // CTA button
                AnimatedVisibility(visible = destinationAddress.isNotBlank()) {
                    Button(
                        onClick = { addHistory(destinationAddress); onNavigateToDetection() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .height(56.dp),
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor   = colorScheme.onPrimary,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Check Road Safety", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search panel content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchPanelContent(
    source: String, destination: String,
    suggestions: List<String>, history: List<String>,
    onSourceChange: (String) -> Unit, onDestinationChange: (String) -> Unit,
    onSwap: () -> Unit, onDestFocused: () -> Unit, onSearch: () -> Unit,
    onSuggestionPick: (String) -> Unit, onHistoryPick: (String) -> Unit,
    onUseCurrent: () -> Unit, contentPadding: PaddingValues,
) {
    val cs = MaterialTheme.colorScheme
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor     = cs.primary,
        unfocusedBorderColor   = cs.outline,
        focusedLabelColor      = cs.primary,
        cursorColor            = cs.primary,
        focusedLeadingIconColor   = cs.primary,
        unfocusedLeadingIconColor = cs.onSurfaceVariant,
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start  = 20.dp, end = 20.dp, top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        item {
            Text(
                text = "Where to?",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = cs.onSurface,
            )
        }

        // ── Route input card ─────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cs.surfaceVariant,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

                    // Source field
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.RadioButtonChecked, null,
                            tint = cs.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value         = source,
                            onValueChange = onSourceChange,
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(12.dp),
                            placeholder   = { Text("Your location", color = cs.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium) },
                            colors        = fieldColors,
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { onDestFocused() }),
                            trailingIcon  = {
                                if (source.isNotBlank()) IconButton({ onSourceChange("") }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            },
                        )
                    }

                    // Divider with swap
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(12.dp))
                        HorizontalDivider(Modifier.weight(1f), color = cs.outline.copy(alpha = 0.3f))
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape   = CircleShape,
                            onClick = onSwap,
                            color = cs.surfaceVariant
                        ) {
                            Icon(Icons.Default.SwapVert, "Swap",
                                tint = cs.primary,
                                modifier = Modifier.padding(0.dp).size(24.dp))
                        }
                    }

                    // Destination field
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null,
                            tint = UrbanEyeColors.RoyalEmerald, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(
                            value         = destination,
                            onValueChange = onDestinationChange,
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(12.dp),
                            placeholder   = { Text("Choose destination", color = cs.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium) },
                            colors        = fieldColors,
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                            trailingIcon  = {
                                if (destination.isNotBlank()) IconButton({ onDestinationChange("") }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            },
                        )
                    }
                }
            }
        }

        // ── Use current location chip ─────────────────────────────
        item {
            Surface(
                shape   = RoundedCornerShape(14.dp),
                color   = cs.primaryContainer.copy(alpha = 0.5f),
                onClick = onUseCurrent,
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier          = Modifier.size(34.dp).clip(CircleShape)
                            .background(cs.primary.copy(alpha = 0.15f)),
                        contentAlignment  = Alignment.Center,
                    ) {
                        Icon(Icons.Default.MyLocation, null, tint = cs.primary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Use current location", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text("Set as starting point", style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Suggestions ──────────────────────────────────────────
        if (suggestions.isNotEmpty()) {
            item {
                Text("SUGGESTIONS", style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant, letterSpacing = 1.sp)
            }
            items(suggestions.take(5)) { s ->
                SearchRow(text = s, isHistory = false, onClick = { onSuggestionPick(s) })
            }
        }

        // ── History ──────────────────────────────────────────────
        if (history.isNotEmpty() && suggestions.isEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("RECENT", style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant, letterSpacing = 1.sp)
            }
            items(history) { h ->
                SearchRow(text = h, isHistory = true, onClick = { onHistoryPick(h) })
            }
        }
    }
}

@Composable
private fun SearchRow(text: String, isHistory: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier          = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(CircleShape)
                .background(cs.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isHistory) Icons.Default.History else Icons.Default.Search,
                null, tint = cs.primary, modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, maxLines = 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun loadSearchHistory(ctx: Context): List<String> =
    ctx.getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_HISTORY, "").orEmpty()
        .split("|#|").map { it.trim() }.filter { it.isNotBlank() }.take(MAX_HISTORY)

private fun saveSearchHistory(ctx: Context, list: List<String>) =
    ctx.getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE).edit {
        putString(KEY_HISTORY, list.take(MAX_HISTORY).joinToString("|#|"))
    }

private suspend fun fetchSuggestions(ctx: Context, q: String): List<String> =
    withContext(Dispatchers.IO) {
        if (q.length < 3) return@withContext emptyList()
        try {
            Geocoder(ctx, Locale.getDefault())
                .getFromLocationName(q, 6)
                ?.mapNotNull { it.getAddressLine(0) }
                ?.distinct().orEmpty()
        } catch (_: Exception) { emptyList() }
    }

private fun Double.fmt(d: Int) = "%.${d}f".format(this)