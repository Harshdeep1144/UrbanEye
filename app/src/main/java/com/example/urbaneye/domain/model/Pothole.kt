package com.example.urbaneye.domain.model

/**
 * Represents the severity level of a detected pothole based on size and impact.
 */
enum class PotholeSeverity {
    LOW, MEDIUM, HIGH
}

/**
 * Data model for a Pothole detection report.
 * This includes geographic location, reporter details, and detection metadata.
 */
data class Pothole(
    val id: String = "",
    val latitude: Double,
    val longitude: Double,
    val severity: PotholeSeverity,
    val reportedBy: String,
    val reporterId: String,
    val timestamp: Long,
    val confidence: Float = 0f,
    val size: Double = 0.0,
    val depth: Double = 0.0
)