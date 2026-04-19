package com.ekhonavigator.feature.map

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMarker
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val markerRepository: MarkerRepository,
    private val socialRepository: com.ekhonavigator.core.data.social.SocialRepository
) : ViewModel() {

    val droppedMarkers = mutableStateListOf<UserMarker>()

    val friends = androidx.compose.runtime.mutableStateListOf<FriendInfo>()

    private val currentUserId: String?
        get() = authRepository.getCurrentUserUid()

    init {
        viewModelScope.launch {
            authRepository.userFlow().collect { userId ->
                if (userId != null) {
                    loadUserMarkers()
                    loadFriends(userId) // Fetch friends when user is logged in
                } else {
                    droppedMarkers.clear()
                    friends.clear()
                }
            }
        }
    }

    private fun loadFriends(userId: String) {
        viewModelScope.launch {
            // SocialRepository.getFriends returns a List, so we just call it.
            val friendList = socialRepository.getFriends(userId)
            friends.clear()
            friends.addAll(friendList.map { FriendInfo(it.uid, it.displayName) })
        }
    }

    private fun loadUserMarkers() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            markerRepository.observeUserMarkers(userId).collect { remoteMarkers ->
                droppedMarkers.clear()
                droppedMarkers.addAll(remoteMarkers.map { it.toUserMarker() })
            }
        }
    }

    fun addMarker(newMarkerLocation: LatLng) {
        val userId = currentUserId ?: return
        val newMarkerId = System.currentTimeMillis().toString()

        val newDroppedMarker = UserDroppedMarker(
            id = newMarkerId,
            latitude = newMarkerLocation.latitude,
            longitude = newMarkerLocation.longitude,
            comment = ""
        )

        droppedMarkers.add(newDroppedMarker.toUserMarker())

        viewModelScope.launch {
            markerRepository.saveMarker(userId, newDroppedMarker)
        }
    }

    fun updateMarkerLabel(markerIdToUpdate: Long, newMarkerCommentText: String) {
        val userId = currentUserId ?: return
        val markerIndexToUpdate =
            droppedMarkers.indexOfFirst { currentMarker -> currentMarker.id == markerIdToUpdate }

        if (markerIndexToUpdate != -1) {
            val updatedMarker =
                droppedMarkers[markerIndexToUpdate].copy(markerLabelComment = newMarkerCommentText)
            droppedMarkers[markerIndexToUpdate] = updatedMarker

            viewModelScope.launch {
                markerRepository.saveMarker(userId, updatedMarker.toRemoteMarker())
            }
        }
    }

    fun removeMarker(markerForRemoval: UserMarker) {
        val userId = currentUserId ?: return

        droppedMarkers.remove(markerForRemoval)

        viewModelScope.launch {
            markerRepository.deleteMarker(userId, markerForRemoval.id.toString())
        }
    }

    private fun UserDroppedMarker.toUserMarker() = UserMarker(
        id = id.toLongOrNull() ?: 0L,
        droppedMarkerLocation = LatLng(latitude, longitude),
        markerLabelComment = comment
    )

    private fun UserMarker.toRemoteMarker() = UserDroppedMarker(
        id = id.toString(),
        latitude = droppedMarkerLocation.latitude,
        longitude = droppedMarkerLocation.longitude,
        comment = markerLabelComment
    )
}