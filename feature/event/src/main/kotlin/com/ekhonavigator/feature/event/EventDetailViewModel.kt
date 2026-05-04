package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
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

    /** Resolves to a place id the user can navigate to. Falls through three checks:
     *   1. Direct match (campus or owner's own marker).
     *   2. Coord match against any of the user's own markers — covers recipients who already
     *      saved this customLocation as a marker; their local marker id won't equal the event's
     *      original `marker_<ownerId>` but the coordinates do, so the WHERE row navs straight
     *      to their copy without re-prompting "Save to map?".
     *   3. Otherwise null — WHERE row degrades to un-linked styling. */
    val effectivePlaceId: StateFlow<String?> = combine(event, placeRepository.observePlaces()) { ev, places ->
        ev?.resolveTargetPlaceId(places)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Non-null when the event was pinned to a custom marker the current user does NOT own
     *  (recipient case, or owner whose marker was deleted) AND the user has not already saved
     *  the customLocation as a personal marker. Drives the one-shot "Save to map?" prompt. */
    val customLocationOffer: StateFlow<SharedLocation?> = combine(event, placeRepository.observePlaces()) { ev, places ->
        val pid = ev?.placeId ?: return@combine null
        val custom = ev.customLocation ?: return@combine null
        if (!pid.startsWith(MARKER_ID_PREFIX)) return@combine null
        val targetResolved = ev.resolveTargetPlaceId(places) != null
        if (targetResolved) null else custom
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _navigateToMarker = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val navigateToMarker: SharedFlow<String> = _navigateToMarker.asSharedFlow()

    /** Current user's RSVP status for this event, or null if not an attendee. */
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

    /** Same gate as [canDelete] today — owner of a user-created event. Kept as a distinct
     *  property so a future policy change (e.g. allow edit but not delete) doesn't have to
     *  fork the call sites. */
    val canEdit: Boolean
        get() = canDelete

    /** True for any custom event with attendees — shows attendee section for both owner and invitees. */
    val hasAttendees: Boolean
        get() {
            val e = event.value ?: return false
            return e.source == EventSource.SHARED || (e.source == EventSource.USER_CREATED && attendees.value.isNotEmpty())
        }

    /** True when the current user is an invitee (not the owner) — shows RSVP buttons. */
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

    /** Personal ASSIGNMENT events get a manual completion toggle. Canvas-derived
     *  ASSIGNMENT events get their completion from `submitted/graded/excused`
     *  on the planner item, so we don't expose a manual toggle for those. */
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
        _eventId.value = id
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.syncAttendees(id)
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
                // No explicit navigation — the Room Flow emits null after delete,
                // which the screen's hadEvent detection catches and calls onBack().
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

    /** Saves the event's customLocation as a personal marker, dedup-by-coords (mirrors
     *  ChatViewModel.saveSharedLocationToMap), then emits a focusPlaceId so the screen can
     *  navigate to the map and pan to it. */
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
                // Friends list unavailable — sheet just shows empty state
            }
        }
    }
}

// Must match DefaultPlaceRepository's namespacing scheme for user-marker Place ids.
private const val MARKER_ID_PREFIX = "marker_"

private fun CalendarEvent.resolveTargetPlaceId(places: List<com.ekhonavigator.core.model.Place>): String? {
    val pid = placeId ?: return null
    if (places.any { it.id == pid }) return pid
    // Coord-match fallback: matches the recipient's saved-marker copy of a shared customLocation.
    val custom = customLocation ?: return null
    return places.firstOrNull {
        it.isCustom && it.latitude == custom.latitude && it.longitude == custom.longitude
    }?.id
}
