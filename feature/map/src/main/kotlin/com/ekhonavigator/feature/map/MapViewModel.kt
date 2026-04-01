package com.ekhonavigator.feature.map

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMapMarker
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val markerRepository: MarkerRepository
) : ViewModel() {

    // list of markers the map will draw on screen.
    // here so the markers won't disappear if phone rotates.
    val droppedMarkers = mutableStateListOf<UserMarker>()

    // figures out who is signed in now using the Auth logic.
    private val currentAuthenticatedUserId: String?
        get() = authRepository.getCurrentUserUid()

    init {
        // as soon as the Map starts up, tries to load markers from Firebase.
        loadMarkersFromFirebase()
    }

    // Grabs all markers the current user has saved and puts them on the map.
    private fun loadMarkersFromFirebase() {
        val activeUserFirebaseUid = currentAuthenticatedUserId ?: return
        viewModelScope.launch {
            val markersFetchedFromFirestore = markerRepository.getUserMarkers(activeUserFirebaseUid)

                 // Clears the local list and fills it with the ones from the database
            droppedMarkers.clear()
            droppedMarkers.addAll(markersFetchedFromFirestore.map { firestoreMarker -> firestoreMarker.toUserMarker() })
        }
    }

    // called when a user long-presses the map, which adds dropped marker to map and saves to Firebase.
    fun addMarker(droppedMarkerLocation: LatLng) {
        val activeUserFirebaseUid = currentAuthenticatedUserId ?: return
        val newGeneratedMarkerId = System.currentTimeMillis().toString()

        val newCreatedMapMarker = UserDroppedMapMarker(
            id = newGeneratedMarkerId,
            latitude = droppedMarkerLocation.latitude,
            longitude = droppedMarkerLocation.longitude,
            comment = ""
        )

        // shows dropped marker on the map immediately
        droppedMarkers.add(newCreatedMapMarker.toUserMarker())

        // saves dropped marker in the background.
        viewModelScope.launch {
            markerRepository.saveMarker(activeUserFirebaseUid, newCreatedMapMarker)
        }
    }

    // called when user updates the label/comment, and syncs the change to Firebase.
    fun updateMarkerLabel(targetMarkerUniqueId: Long, updatedMarkerLabelComment: String) {
        val activeUserFirebaseUid = currentAuthenticatedUserId ?: return
        val targetMarkerListIndex = droppedMarkers.indexOfFirst { existingMarker -> existingMarker.id == targetMarkerUniqueId }

        if (targetMarkerListIndex != -1) {
            val updatedMarker = droppedMarkers[targetMarkerListIndex].copy(markerLabelComment = updatedMarkerLabelComment)
            droppedMarkers[targetMarkerListIndex] = updatedMarker

            // Updates record in the database
            viewModelScope.launch {
                markerRepository.saveMarker(activeUserFirebaseUid, updatedMarker.toFirestoreMarker())
            }
        }
    }

    // removes a marker from the map and wipes it from database.
    fun removeMarker(markerSelectedForRemoval: UserMarker) {
        val activeUserFirebaseUid = currentAuthenticatedUserId ?: return

        droppedMarkers.remove(markerSelectedForRemoval)

        viewModelScope.launch {
            markerRepository.deleteMarker(activeUserFirebaseUid, markerSelectedForRemoval.id.toString())
        }
    }

    // These convert between UI data and Database data.
    private fun UserDroppedMapMarker.toUserMarker() = UserMarker(
        id = id.toLongOrNull() ?: 0L,
        droppedMarkerLocation = LatLng(latitude, longitude),
        markerLabelComment = comment
    )

    private fun UserMarker.toFirestoreMarker() = UserDroppedMapMarker(
        id = id.toString(),
        latitude = droppedMarkerLocation.latitude,
        longitude = droppedMarkerLocation.longitude,
        comment = markerLabelComment
    )
}