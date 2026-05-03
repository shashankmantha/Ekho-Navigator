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
import com.ekhonavigator.core.data.route.RouteRepository
import com.ekhonavigator.core.data.route.TravelMode
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val markerRepository: MarkerRepository,
    private val socialRepository: com.ekhonavigator.core.data.social.SocialRepository,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    val droppedMarkers = mutableStateListOf<UserMarker>()

    private val _activeRoutePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val activeRoutePoints = _activeRoutePoints.asStateFlow()

    private val _isRouteLoading = MutableStateFlow(false)
    val isRouteLoading = _isRouteLoading.asStateFlow()

    val friends = androidx.compose.runtime.mutableStateListOf<FriendInfo>()


    var searchTextForFriendPicker by mutableStateOf("")
        private set

    private var activeFriendsListener: kotlinx.coroutines.Job? = null

    fun updateSearchTextForFriendPicker(newText: String) {
        searchTextForFriendPicker = newText
    }

    val friendsListMatchingSearchQuery: List<FriendInfo>
        get() = if (searchTextForFriendPicker.isBlank()) {
            friends
        } else {
            friends.filter { friend ->
                friend.name.contains(searchTextForFriendPicker, ignoreCase = true)
            }
        }

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
        activeFriendsListener?.cancel()

        activeFriendsListener = viewModelScope.launch {
            socialRepository.observeFriends(userId).collect { latestFriendsFromFirebase ->
                friends.clear()
                friends.addAll(latestFriendsFromFirebase.map { friend ->
                    FriendInfo(friend.uid, friend.displayName)
                })
            }
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

    fun getDirectionsToDestination(
        destination: LatLng,
        travelMode: TravelMode,
        userLocation: LatLng?
    ) {
        viewModelScope.launch {
            _isRouteLoading.value = true

            // Use user's real location if available, otherwise fallback to CSUCI center
            val origin = userLocation ?: LatLng(34.162134342787105, -119.04400892418893)

            val routePath = routeRepository.fetchRouteBetweenPoints(
                startLocation = origin,
                endLocation = destination,
                travelMode = travelMode
            )

            if (routePath.isNotEmpty()) {
                _activeRoutePoints.value = routePath
            } else {
                _activeRoutePoints.value = listOf(origin, destination) // Fallback
            }

            _isRouteLoading.value = false
        }
    }

    fun clearActiveRoute() {
        _activeRoutePoints.value = emptyList()
    }
}