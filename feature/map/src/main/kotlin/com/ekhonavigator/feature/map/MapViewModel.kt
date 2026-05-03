package com.ekhonavigator.feature.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMarker
import com.ekhonavigator.core.data.social.SocialRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val markerRepository: MarkerRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    val droppedMarkers = mutableStateListOf<UserMarker>()
    val friends = mutableStateListOf<FriendInfo>()

    var searchTextForFriendPicker by mutableStateOf("")
        private set

    private var activeFriendsListener: Job? = null
    private var activeMarkersListener: Job? = null

    private val currentUserId: String?
        get() = authRepository.getCurrentUserUid()

    val friendsListMatchingSearchQuery: List<FriendInfo>
        get() = if (searchTextForFriendPicker.isBlank()) {
            friends
        } else {
            friends.filter { friend ->
                friend.name.contains(searchTextForFriendPicker, ignoreCase = true)
            }
        }

    init {
        viewModelScope.launch {
            authRepository.userFlow().collect { userId ->
                if (userId != null) {
                    startUserListeners(userId)
                } else {
                    stopUserListeners()
                    clearUserData()
                }
            }
        }
    }

    fun updateSearchTextForFriendPicker(newText: String) {
        searchTextForFriendPicker = newText
    }

    private fun startUserListeners(userId: String) {
        loadUserMarkers(userId)
        loadFriends(userId)
    }

    private fun stopUserListeners() {
        activeFriendsListener?.cancel()
        activeFriendsListener = null

        activeMarkersListener?.cancel()
        activeMarkersListener = null
    }

    private fun clearUserData() {
        droppedMarkers.clear()
        friends.clear()
        searchTextForFriendPicker = ""
    }

    private fun loadFriends(userId: String) {
        activeFriendsListener?.cancel()

        activeFriendsListener = viewModelScope.launch {
            socialRepository.observeFriends(userId)
                .catch {
                    friends.clear()
                }
                .collect { latestFriendsFromFirebase ->
                    friends.clear()
                    friends.addAll(
                        latestFriendsFromFirebase.map { friend ->
                            FriendInfo(
                                id = friend.uid,
                                name = friend.displayName,
                            )
                        }
                    )
                }
        }
    }

    private fun loadUserMarkers(userId: String) {
        activeMarkersListener?.cancel()

        activeMarkersListener = viewModelScope.launch {
            markerRepository.observeUserMarkers(userId)
                .catch {
                    droppedMarkers.clear()
                }
                .collect { remoteMarkers ->
                    droppedMarkers.clear()
                    droppedMarkers.addAll(
                        remoteMarkers.map { marker ->
                            marker.toUserMarker()
                        }
                    )
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
            comment = "",
        )

        droppedMarkers.add(newDroppedMarker.toUserMarker())

        viewModelScope.launch {
            runCatching {
                markerRepository.saveMarker(userId, newDroppedMarker)
            }
        }
    }

    fun updateMarkerLabel(markerIdToUpdate: Long, newMarkerCommentText: String) {
        val userId = currentUserId ?: return

        val markerIndexToUpdate = droppedMarkers.indexOfFirst { currentMarker ->
            currentMarker.id == markerIdToUpdate
        }

        if (markerIndexToUpdate == -1) return

        val updatedMarker = droppedMarkers[markerIndexToUpdate].copy(
            markerLabelComment = newMarkerCommentText,
        )

        droppedMarkers[markerIndexToUpdate] = updatedMarker

        viewModelScope.launch {
            runCatching {
                markerRepository.saveMarker(userId, updatedMarker.toRemoteMarker())
            }
        }
    }

    fun removeMarker(markerForRemoval: UserMarker) {
        val userId = currentUserId ?: return

        droppedMarkers.remove(markerForRemoval)

        viewModelScope.launch {
            runCatching {
                markerRepository.deleteMarker(userId, markerForRemoval.id.toString())
            }
        }
    }

    override fun onCleared() {
        stopUserListeners()
        super.onCleared()
    }

    private fun UserDroppedMarker.toUserMarker() = UserMarker(
        id = id.toLongOrNull() ?: 0L,
        droppedMarkerLocation = LatLng(latitude, longitude),
        markerLabelComment = comment,
    )

    private fun UserMarker.toRemoteMarker() = UserDroppedMarker(
        id = id.toString(),
        latitude = droppedMarkerLocation.latitude,
        longitude = droppedMarkerLocation.longitude,
        comment = markerLabelComment,
    )
}