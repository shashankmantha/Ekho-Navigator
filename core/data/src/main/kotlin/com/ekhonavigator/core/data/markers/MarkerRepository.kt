package com.ekhonavigator.core.data.markers

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// used to save the markers people drop on the map.
// it keeps track of where it is (lat/long) and whatever label/note the user wrote.
data class UserDroppedMapMarker(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val comment: String = ""               // stores the label or details the user types in
)

class MarkerRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()

    // saves a new marker or updates an old one for a specific user.
    // it goes into: users -> (userId) -> droppedMarkers -> (markerId)
    suspend fun saveMarker(userId: String, marker: UserDroppedMapMarker) {
        firestore.collection("users")
            .document(userId)
            .collection("droppedMarkers")
            .document(marker.id)
            .set(marker)
            .await()
    }

    // grabs all the markers the specific user has saved so we can show them on map.
    suspend fun getUserMarkers(userId: String): List<UserDroppedMapMarker> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("droppedMarkers")
                .get()
                .await()

            // firebase auto turns the database data back into our list of markers
            snapshot.toObjects(UserDroppedMapMarker::class.java)
        } catch (e: Exception) {
            emptyList()               // if there's an error or no markers, we return an empty list
        }
    }

    // deletes a marker from the database so it's gone for good.
    suspend fun deleteMarker(userId: String, markerId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("droppedMarkers")
            .document(markerId)
            .delete()
            .await()
    }
}