package com.example.urbaneye.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.ui.screens.BoundingBox
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val database: FirebaseDatabase,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    private var lastReportTime = 0L
    private val REPORT_COOLDOWN = 10000L // 10 seconds cooldown to avoid duplicate reports

    // Reference directly to the Realtime Database path
    private val potholesRef = database.getReference("artifacts")
        .child("urban-eye-app")
        .child("public")
        .child("data")
        .child("potholes")

    fun onPotholesDetected(results: List<BoundingBox>) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastReportTime < REPORT_COOLDOWN) return

        // Filter for potholes with high confidence
        val detectedPotholes = results.filter {
            it.clsName.contains("Pothole", ignoreCase = true) && it.cnf > 0.7f
        }

        if (detectedPotholes.isNotEmpty()) {
            val bestDetection = detectedPotholes.maxByOrNull { it.cnf }
            bestDetection?.let {
                reportPotholeToFirebase(it)
                lastReportTime = currentTime
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun reportPotholeToFirebase(detection: BoundingBox) {
        viewModelScope.launch {
            try {
                // 1. Get User Info
                val currentUser = auth.currentUser
                val userId = currentUser?.uid ?: "anonymous"
                val userName = currentUser?.displayName ?: "UrbanEye User"

                // 2. Get Location
                val location = try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).await() ?: fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    null
                }

                // 3. Save to Realtime Database
                location?.let { loc ->
                    val potholeData = hashMapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude,
                        "severity" to calculateSeverity(detection).name,
                        "reportedBy" to userName,
                        "reporterId" to userId,
                        "timestamp" to System.currentTimeMillis(),
                        "confidence" to detection.cnf
                    )

                    // Direct database write
                    potholesRef.push().setValue(potholeData).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateSeverity(box: BoundingBox): PotholeSeverity {
        val area = (box.x2 - box.x1) * (box.y2 - box.y1)
        return when {
            area > 0.15 -> PotholeSeverity.HIGH
            area > 0.05 -> PotholeSeverity.MEDIUM
            else -> PotholeSeverity.LOW
        }
    }
}