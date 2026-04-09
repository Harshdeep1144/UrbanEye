package com.example.urbaneye.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.domain.model.RoadRating
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.random.Random

/**
 * UI-specific wrapper for Pothole to include visual styling data.
 */
data class PotholeDisplayData(
    val pothole: Pothole,
    val color: Int // Android color int
)

data class MapUiState(
    val potholes: List<PotholeDisplayData> = emptyList(),
    val roadRatings: List<RoadRating> = emptyList(),
    val isLoading: Boolean = false,
    val sourceAddress: String = "",
    val destinationAddress: String = "",
    val userLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val potholesRef = database.getReference("artifacts")
        .child("urban-eye-app")
        .child("public")
        .child("data")
        .child("potholes")

    private val potholesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val potholeList = mutableListOf<PotholeDisplayData>()

            // Log for debugging (check your Logcat for "MapViewModel")
            println("Firebase: Received ${snapshot.childrenCount} potholes")

            for (child in snapshot.children) {
                try {
                    val lat = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val severityStr = child.child("severity").getValue(String::class.java) ?: "LOW"
                    val confidence = child.child("confidence").getValue(Float::class.java) ?: 0f
                    val reportedBy = child.child("reportedBy").getValue(String::class.java) ?: "Anonymous"
                    val reporterId = child.child("reporterId").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val size = child.child("size").getValue(Double::class.java) ?: 0.0
                    val depth = child.child("depth").getValue(Double::class.java) ?: 0.0

                    val severity = try {
                        PotholeSeverity.valueOf(severityStr)
                    } catch (e: Exception) {
                        PotholeSeverity.LOW
                    }

                    val color = when (severity) {
                        PotholeSeverity.HIGH -> android.graphics.Color.RED
                        PotholeSeverity.MEDIUM -> android.graphics.Color.parseColor("#FFA500")
                        PotholeSeverity.LOW -> android.graphics.Color.YELLOW
                    }

                    potholeList.add(
                        PotholeDisplayData(
                            pothole = Pothole(
                                id = child.key ?: "",
                                latitude = lat,
                                longitude = lng,
                                severity = severity,
                                confidence = confidence,
                                reportedBy = reportedBy,
                                reporterId = reporterId,
                                timestamp = timestamp,
                                size = size,
                                depth = depth
                            ),
                            color = color
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // If the database is empty, seed it automatically for the demo
            if (snapshot.childrenCount == 0L) {
                seedMockPotholes()
            }

            _uiState.value = _uiState.value.copy(potholes = potholeList, isLoading = false)
        }

        override fun onCancelled(error: DatabaseError) {
            println("Firebase Error: ${error.message}")
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    init {
        _uiState.value = _uiState.value.copy(isLoading = true)
        potholesRef.addValueEventListener(potholesListener)
    }

    /**
     * Automatically generates random potholes precisely along the road
     * from Banur (Gian Sagar) to Chandigarh Airport using a HashMap
     * to ensure Firebase compatibility.
     */
    fun seedMockPotholes() {
        val currentTime = System.currentTimeMillis()

        // Expanded road path with more intermediate points for better density
        val roadPath = listOf(
            Pair(30.5284, 76.7015), // Gian Sagar Hospital
            Pair(30.5350, 76.7080), // Near Gian Sagar Dental
            Pair(30.5470, 76.7180), // Near Banur Barrier
            Pair(30.5650, 76.7280), // Highway stretch after Banur
            Pair(30.5750, 76.7350), // Midway to Chhat Bir
            Pair(30.5850, 76.7420), // Chhat Bir Zoo Area
            Pair(30.5975, 76.7485), // Chhat Bir Crossing
            Pair(30.6100, 76.7550), // Dayalpura
            Pair(30.6250, 76.7650), // Entering Zirakpur area
            Pair(30.6400, 76.7750), // Aerocity Road Junction
            Pair(30.6540, 76.7810), // PR7 Airport Road junction
            Pair(30.6650, 76.7850), // Mohali border stretch
            Pair(30.6725, 76.7885)  // Chandigarh Airport Entrance
        )

        val severities = PotholeSeverity.values()

        for (i in 0 until roadPath.size - 1) {
            val start = roadPath[i]
            val end = roadPath[i+1]
            val countInSegment = Random.nextInt(1, 3)

            repeat(countInSegment) {
                val fraction = Random.nextDouble(0.1, 0.9)
                val lat = start.first + (end.first - start.first) * fraction
                val lng = start.second + (end.second - start.second) * fraction

                val severity = severities[Random.nextInt(severities.size)]

                // Increased size and depth values to make markers more prominent
                val potholeData = hashMapOf(
                    "latitude" to lat,
                    "longitude" to lng,
                    "severity" to severity.name,
                    "reportedBy" to "System Demo",
                    "reporterId" to "demo_bot",
                    "timestamp" to currentTime - Random.nextLong(0, 1000000),
                    "confidence" to 0.85f + (Random.nextFloat() * 0.14f),
                    "size" to 5.0 + Random.nextDouble(0.0, 10.0), // Significantly increased size
                    "depth" to 1.5 + Random.nextDouble(0.0, 3.0)  // Significantly increased depth
                )

                potholesRef.push().setValue(potholeData)
                    .addOnFailureListener { e ->
                        println("Firebase Write Failed: ${e.message}")
                    }
            }
        }
    }

    fun onSourceAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(sourceAddress = address)
    }

    fun onDestinationAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(destinationAddress = address)
    }

    override fun onCleared() {
        super.onCleared()
        potholesRef.removeEventListener(potholesListener)
    }
}