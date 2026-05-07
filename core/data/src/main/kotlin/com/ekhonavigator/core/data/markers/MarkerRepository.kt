package com.ekhonavigator.core.data.markers

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserDroppedMarker(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val comment: String = ""
)

class MarkerRepository @Inject constructor() {
    // Lazy so constructing this class doesn't touch FirebaseApp — JVM unit tests
    // can instantiate the repo without bringing up Firebase as long as they never
    // call a method that actually hits Firestore.
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // The Firestore path for markers is: users -> (userId) -> droppedMarkers -> (markerId)
    private fun getUserMarkersCollection(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("droppedMarkers")

    suspend fun saveMarker(userId: String, marker: UserDroppedMarker) {
        getUserMarkersCollection(userId)
            .document(marker.id)
            .set(marker)
            .await()
    }

    fun observeUserMarkers(userId: String): Flow<List<UserDroppedMarker>> = callbackFlow {
        val registration = getUserMarkersCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val markers = snapshot.toObjects(UserDroppedMarker::class.java)
                trySend(markers)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getUserMarkers(userId: String): List<UserDroppedMarker> {
        return try {
            val markersSnapshot = getUserMarkersCollection(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            markersSnapshot.toObjects(UserDroppedMarker::class.java)
        } catch (exception: Exception) {
            emptyList()
        }
    }

    suspend fun deleteMarker(userId: String, markerId: String) {
        getUserMarkersCollection(userId)
            .document(markerId)
            .delete()
            .await()
    }
}