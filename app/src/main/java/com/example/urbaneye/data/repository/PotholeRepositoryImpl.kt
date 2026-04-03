package com.example.urbaneye.data.repository

import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.domain.model.RoadRating
import com.example.urbaneye.domain.repository.PotholeRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotholeRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : PotholeRepository {

    /**
     * Path structure for UrbanEye data in Realtime Database.
     */
    private val potholesRef = database.getReference("artifacts")
        .child("urban-eye-app")
        .child("public")
        .child("data")
        .child("potholes")

    override fun getPotholes(): Flow<List<Pothole>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val potholes = snapshot.children.mapNotNull { child ->
                    try {
                        Pothole(
                            id = child.key ?: "",
                            latitude = child.child("latitude").getValue(Double::class.java) ?: 0.0,
                            longitude = child.child("longitude").getValue(Double::class.java) ?: 0.0,
                            severity = PotholeSeverity.valueOf(
                                child.child("severity").getValue(String::class.java) ?: "MEDIUM"
                            ),
                            reportedBy = child.child("reportedBy").getValue(String::class.java) ?: "Unknown User",
                            reporterId = child.child("reporterId").getValue(String::class.java) ?: "",
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L,
                            confidence = child.child("confidence").getValue(Float::class.java) ?: 0f
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.reversed()

                trySend(potholes)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        potholesRef.addValueEventListener(listener)
        awaitClose { potholesRef.removeEventListener(listener) }
    }

    override suspend fun reportPothole(pothole: Pothole) {
        try {
            val data = hashMapOf(
                "latitude" to pothole.latitude,
                "longitude" to pothole.longitude,
                "severity" to pothole.severity.name,
                "reportedBy" to pothole.reportedBy,
                "reporterId" to pothole.reporterId,
                "timestamp" to pothole.timestamp,
                "confidence" to pothole.confidence
            )

            // Generate a unique ID using push() and save the data
            potholesRef.push().setValue(data).await()
        } catch (e: Exception) {
            throw e
        }
    }

    override fun getRoadRatings(): Flow<List<RoadRating>> = callbackFlow {
        trySend(emptyList())
        awaitClose { }
    }
}