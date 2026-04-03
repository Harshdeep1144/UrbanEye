package com.example.urbaneye.domain.model

data class Pothole(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val severity: PotholeSeverity,
    val size: Double, // in cm or relative scale
    val depth: Double, // estimated depth
    val timestamp: Long
)

enum class PotholeSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}