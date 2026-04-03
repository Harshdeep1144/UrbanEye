package com.example.urbaneye.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.domain.repository.PotholeRepository
import com.example.urbaneye.ui.screens.BoundingBox
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val repository: PotholeRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private var lastReportTime = 0L
    private val REPORT_COOLDOWN = 5000L // 5 seconds cooldown to avoid duplicate reports

    fun onPotholesDetected(results: List<BoundingBox>) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastReportTime < REPORT_COOLDOWN) return

        val potholes = results.filter { it.clsName.equals("Pothole", ignoreCase = true) && it.cnf > 0.6f }
        if (potholes.isNotEmpty()) {
            reportPotholesWithLocation()
            lastReportTime = currentTime
        }
    }

    @SuppressLint("MissingPermission")
    private fun reportPotholesWithLocation() {
        viewModelScope.launch {
            try {
                val result = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).run {
                    // In some environments, getCurrentLocation might be tricky, fall back to lastLocation
                    val loc = com.google.android.gms.tasks.Tasks.await(this)
                    loc ?: com.google.android.gms.tasks.Tasks.await(fusedLocationClient.lastLocation)
                }

                result?.let { location ->
                    val pothole = Pothole(
                        id = UUID.randomUUID().toString(),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        severity = PotholeSeverity.HIGH, // Default to HIGH for now, could be calculated from bbox size
                        size = 0.0,
                        depth = 0.0,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.reportPothole(pothole)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
