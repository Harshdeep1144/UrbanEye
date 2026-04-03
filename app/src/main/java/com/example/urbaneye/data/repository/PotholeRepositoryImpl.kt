package com.example.urbaneye.data.repository

import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.model.PotholeSeverity
import com.example.urbaneye.domain.model.RoadRating
import com.example.urbaneye.domain.repository.PotholeRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotholeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PotholeRepository {

    private val potholesCollection = firestore.collection("potholes")

    override fun getPotholes(): Flow<List<Pothole>> = callbackFlow {
        val subscription = potholesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val potholes = snapshot.documents.mapNotNull { doc ->
                        try {
                            Pothole(
                                id = doc.id,
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                severity = PotholeSeverity.valueOf(doc.getString("severity") ?: "MEDIUM"),
                                size = doc.getDouble("size") ?: 0.0,
                                depth = doc.getDouble("depth") ?: 0.0,
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(potholes)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getRoadRatings(): Flow<List<RoadRating>> = callbackFlow {
        // For simplicity, we can derive ratings or have a separate collection
        // For now, let's keep it empty or implement logic later
        trySend(emptyList<RoadRating>())
        awaitClose { }
    }

    override suspend fun reportPothole(pothole: Pothole) {
        val data = hashMapOf(
            "latitude" to pothole.latitude,
            "longitude" to pothole.longitude,
            "severity" to pothole.severity.name,
            "size" to pothole.size,
            "depth" to pothole.depth,
            "timestamp" to pothole.timestamp
        )
        potholesCollection.add(data).await()
    }
}
