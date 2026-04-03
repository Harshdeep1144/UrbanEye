package com.example.urbaneye.domain.repository

import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.RoadRating
import kotlinx.coroutines.flow.Flow

interface PotholeRepository {
    fun getPotholes(): Flow<List<Pothole>>
    fun getRoadRatings(): Flow<List<RoadRating>>
    suspend fun reportPothole(pothole: Pothole)
}