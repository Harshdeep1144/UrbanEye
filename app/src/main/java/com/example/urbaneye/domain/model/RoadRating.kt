package com.example.urbaneye.domain.model

data class RoadRating(
    val segmentId: String,
    val rating: Int, // 1-5 or 1-10 scale
    val averageSeverity: PotholeSeverity,
    val potholeCount: Int,
    val geometry: List<Pair<Double, Double>> // List of Lat/Lng for the road segment
)