package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.model.PlannerKind
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.canvas.CanvasAssignmentRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMarker
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.SharedLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository,
    private val markerRepository: MarkerRepository,
    private val canvasPlannerRepository: CanvasPlannerRepository,
    private val canvasAssignmentRepository: CanvasAssignmentRepository,
) : ViewModel() {

    private val _eventId = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val event: StateFlow<CalendarEvent?> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> repository.observeEventById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendees: StateFlow<List<EventAttendee>> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> customEventRepository.observeAttendees(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Drives the EventScreen "Canvas details" section — fields the bridged
    // calendar_events row doesn't carry but the planner table does.
    @OptIn(ExperimentalCoroutinesApi::class)
    val canvasContext: StateFlow<PlannerItem?> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> canvasPlannerRepository.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Per-courseId one-shot — backfills the assignment description (which the
    // planner DTO doesn't carry) the first time an event tap lands here.
    private val syncedCourseIds = mutableSetOf<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val assignmentContext: StateFlow<CanvasAssignment?> = canvasContext
        .flatMapLatest { item ->
            if (item == null || item.kind != PlannerKind.ASSIGNMENT) {
                kotlinx.coroutines.flow.flowOf<CanvasAssignment?>(null)
            } else {
                val courseId = item.courseId
                if (courseId != null && syncedCourseIds.add(courseId)) {
                    viewModelScope.launch {
                        runCatching { canvasAssignmentRepository.sync(courseId) }
                    }
                }
                canvasAssignmentRepository.observeById(item.plannableId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Direct-id match, then coord-match against the user's own markers, then null.
    // Coord-match handles recipients who saved this shared location as their own marker.
    val effectivePlaceId: StateFlow<String?> = combine(event, placeRepository.observePlaces()) { ev, places ->
        ev?.resolveTargetPlaceId(places)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Drives the one-shot "Save to map?" prompt — fires only when the user
    // doesn't already own the underlying marker or a coord-matching copy.
    val customLocationOffer: StateFlow<SharedLocation?> = combine(event, placeRepository.observePlaces()) { ev, places ->
        val pid = ev?.placeId ?: return@combine null
        val custom = ev.customLocation ?: return@combine null
        if (!pid.startsWith(MARKER_ID_PREFIX)) return@combine null
        val targetResolved = ev.resolveTargetPlaceId(places) != null
        if (targetResolved) null else custom
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _navigateToMarker = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val navigateToMarker: SharedFlow<String> = _navigateToMarker.asSharedFlow()

    val currentUserRsvp: StateFlow<RsvpStatus?> = attendees
        .map { list ->
            val uid = authRepository.getCurrentUserUid() ?: return@map null
            list.find { it.userId == uid }?.rsvpStatus
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _friends = MutableStateFlow<List<FriendUser>>(emptyList())
    val friends: StateFlow<List<FriendUser>> = _friends.asStateFlow()

    private val _shareSheetVisible = MutableStateFlow(false)
    val shareSheetVisible: StateFlow<Boolean> = _shareSheetVisible.asStateFlow()

    val canDelete: Boolean
        get() {
            val event = event.value ?: return false
            val uid = authRepository.getCurrentUserUid() ?: return false
            return event.source == EventSource.USER_CREATED && event.ownerUid == uid
        }

    // Kept distinct from canDelete so a future "edit but not delete" policy lands cheaply.
    val canEdit: Boolean
        get() = canDelete

    val hasAttendees: Boolean
        get() {
            val e = event.value ?: return false
            return e.source == EventSource.SHARED || (e.source == EventSource.USER_CREATED && attendees.value.isNotEmpty())
        }

    val canRsvp: Boolean
        get() = event.value?.source == EventSource.SHARED

    val isOwner: Boolean
        get() {
            val e = event.value ?: return false
            val uid = authRepository.getCurrentUserUid() ?: return false
            return e.ownerUid == uid
        }

    val canShare: Boolean
        get() = isOwner && event.value?.source == EventSource.USER_CREATED

    // Canvas-derived assignments source completion from planner submission state;
    // only personal assignments need the manual toggle.
    val canMarkComplete: Boolean
        get() {
            val e = event.value ?: return false
            return canEdit && e.type == com.ekhonavigator.core.model.EventType.ASSIGNMENT
        }

    fun toggleCompleted() {
        val e = event.value ?: return
        if (!canMarkComplete) return
        viewModelScope.launch {
            customEventRepository.updateEvent(e.copy(isCompleted = !e.isCompleted))
        }
    }

    fun setEventId(id: String) {
        // Recurrence taps pass `seedUid__epochDay`; downstream writes (delete,
        // bookmark, complete) need the seed row, so strip the suffix once here.
        val seedId = id.substringBefore("__")
        _eventId.value = seedId
        if (seedId.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.syncAttendees(seedId)
            }
        }
    }

    fun toggleBookmark() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                repository.toggleBookmark(id)
            }
        }
    }

    fun rsvp(status: RsvpStatus) {
        val id = _eventId.value
        val uid = authRepository.getCurrentUserUid() ?: return
        val displayName = authRepository.getCurrentUserDisplayName() ?: ""
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.rsvp(id, uid, displayName, status)
            }
        }
    }

    fun deleteEvent() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.deleteEvent(id)
                // No nav call — Room emits null, screen's hadEvent watcher fires onBack().
            }
        }
    }

    fun openShareSheet() {
        if (!canShare) return
        loadFriendsIfNeeded()
        _shareSheetVisible.value = true
    }

    fun dismissShareSheet() {
        _shareSheetVisible.value = false
    }

    fun shareWith(newUids: Set<String>) {
        val eventId = _eventId.value.takeIf { it.isNotEmpty() } ?: return
        val toAdd = _friends.value
            .filter { it.uid in newUids }
            .associate { it.uid to it.displayName }
        _shareSheetVisible.value = false
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            customEventRepository.addAttendees(eventId, toAdd)
        }
    }

    // Dedup-by-coords (same pattern as ChatViewModel.saveSharedLocationToMap).
    fun saveCustomLocationToMyMarkers() {
        val offer = customLocationOffer.value ?: return
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            val existing = markerRepository.getUserMarkers(uid)
                .firstOrNull { it.latitude == offer.latitude && it.longitude == offer.longitude }
            val markerId = existing?.id ?: System.currentTimeMillis().toString().also { newId ->
                markerRepository.saveMarker(
                    uid,
                    UserDroppedMarker(
                        id = newId,
                        latitude = offer.latitude,
                        longitude = offer.longitude,
                        comment = offer.title,
                    ),
                )
            }
            _navigateToMarker.emit("$MARKER_ID_PREFIX$markerId")
        }
    }

    private fun loadFriendsIfNeeded() {
        if (_friends.value.isNotEmpty()) return
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                _friends.value = socialRepository.getFriends(uid)
            } catch (_: Exception) {
                // Friends unavailable — sheet shows empty state.
            }
        }
    }
}

// Must match DefaultPlaceRepository's user-marker namespacing.
private const val MARKER_ID_PREFIX = "marker_"

private fun CalendarEvent.resolveTargetPlaceId(places: List<com.ekhonavigator.core.model.Place>): String? {
    val pid = placeId ?: return null
    if (places.any { it.id == pid }) return pid
    // Coord-match fallback — recipient may have saved this shared location locally.
    val custom = customLocation ?: return null
    return places.firstOrNull {
        it.isCustom && it.latitude == custom.latitude && it.longitude == custom.longitude
    }?.id
}
