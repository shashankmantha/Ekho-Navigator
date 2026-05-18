package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.repository.UserCourseRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.designsystem.component.LocationSuggestion
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.normalizeCourseLabel
import com.ekhonavigator.core.model.CourseColorChoice
import com.ekhonavigator.core.model.UserCourse
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.RecurrenceRule
import com.ekhonavigator.core.model.SharedLocation
import java.time.DayOfWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

private val EventZone: ZoneId = ZoneId.of("America/Los_Angeles")

data class CreateEventUiState(
    val type: EventType = EventType.EVENT,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val placeId: String? = null,
    // Captured at suggestion-pick time for custom (user-marker) locations so the
    // save denormalizes a SharedLocation onto the event for recipients.
    val pickedCustomLocation: SharedLocation? = null,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val category: EventCategory = EventCategory.GENERAL,
    // Raw user input; normalized via normalizeCourseLabel at save time.
    val courseLabel: String = "",
    // Carried through edit mode so save() doesn't reset existing completion.
    val isCompleted: Boolean = false,
    // CLASS_MEETING only — null on other types. recurrenceDays defaults to the
    // anchor date's weekday so the day chips look pre-selected once a date lands.
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val recurrenceEndDate: LocalDate? = null,
    val friends: List<FriendUser> = emptyList(),
    val selectedFriendUids: Set<String> = emptySet(),
    // Non-null when editing — also gates the one-shot load so Firestore syncs
    // can't clobber the user's in-progress edits.
    val editingEventId: String? = null,
    val existingAttendees: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val showValidationErrors: Boolean = false,
) {
    val canSave: Boolean
        get() = when (type) {
            EventType.ASSIGNMENT -> title.isNotBlank() && date != null && startTime != null
            EventType.CLASS_MEETING -> title.isNotBlank() && date != null && startTime != null &&
                endTime != null && !endBeforeStart && recurrenceDays.isNotEmpty() && recurrenceEndDate != null
            EventType.EVENT -> title.isNotBlank() && date != null && startTime != null && endTime != null && !endBeforeStart
        }

    // Shown live (not gated by a save attempt). ASSIGNMENT has no end; both EVENT and CLASS_MEETING do.
    val endBeforeStart: Boolean
        get() = type != EventType.ASSIGNMENT && startTime != null && endTime != null && !endTime.isAfter(startTime)

    // Soft warning, not a hard block — matches Google Calendar.
    val startsInPast: Boolean
        get() {
            val d = date ?: return false
            val t = startTime ?: return false
            return d.atTime(t).atZone(EventZone).toInstant().isBefore(Instant.now())
        }

    val titleError: Boolean get() = showValidationErrors && title.isBlank()
    val dateError: Boolean get() = showValidationErrors && date == null
    val startTimeError: Boolean get() = showValidationErrors && startTime == null
    val endTimeError: Boolean get() = showValidationErrors && type != EventType.ASSIGNMENT && endTime == null
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val placeRepository: PlaceRepository,
    private val userCourseRepository: UserCourseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    private var didLoadEvent = false

    val locationSuggestions: StateFlow<List<LocationSuggestion>> = placeRepository
        .observePlaces()
        .map { places -> places.map { it.toSuggestion() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Pulls from the user's course profile (Firestore) — survives PAT
    // disconnect and works without a Canvas connection at all.
    val courseSuggestions: StateFlow<List<String>> = userCourseRepository
        .observeCourses()
        .map { courses ->
            courses.filterNot { it.archived }
                .map { it.familyKey }
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadFriends()
    }

    // One-shot — later calls and Firestore updates are ignored so user edits stick.
    fun setEventId(eventId: String) {
        if (didLoadEvent) return
        didLoadEvent = true
        viewModelScope.launch {
            val event = calendarRepository.observeEventById(eventId).first { it != null } ?: return@launch
            val attendees = customEventRepository.observeAttendees(eventId).first()
            val attendeeMap = attendees.associate { it.userId to it.displayName }
            val zoned = event.startTime.atZone(EventZone)
            val zonedEnd = event.endTime.atZone(EventZone)
            _uiState.update {
                it.copy(
                    // event.id is the seed uid even when the nav passed a recurrence
                    // instance id (`seedUid__epochDay`) — keeps update writes on the
                    // real row instead of a synthetic one.
                    editingEventId = event.id,
                    // Any event carrying a recurrence lands on the Recurring tab,
                    // even if it was saved as plain EVENT (custom weekly meeting).
                    type = if (event.recurrence != null) EventType.CLASS_MEETING else event.type,
                    title = event.title,
                    description = event.description,
                    location = event.location,
                    placeId = event.placeId,
                    pickedCustomLocation = event.customLocation,
                    date = zoned.toLocalDate(),
                    startTime = zoned.toLocalTime(),
                    // ASSIGNMENT stores endTime == startTime; null out in state so the picker hides.
                    endTime = if (event.type == EventType.ASSIGNMENT) null else zonedEnd.toLocalTime(),
                    category = event.categories.firstOrNull() ?: EventCategory.GENERAL,
                    courseLabel = event.courseLabel.orEmpty(),
                    isCompleted = event.isCompleted,
                    recurrenceDays = event.recurrence?.daysOfWeek.orEmpty(),
                    recurrenceEndDate = event.recurrence?.endDate,
                    selectedFriendUids = attendeeMap.keys,
                    existingAttendees = attendeeMap,
                )
            }
        }
    }

    private fun loadFriends() {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                val friends = socialRepository.getFriends(uid)
                _uiState.update { it.copy(friends = friends) }
            } catch (_: Exception) {
                // Friends unavailable — sharing section just won't show.
            }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun setDate(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun setStartTime(value: LocalTime) = _uiState.update { it.copy(startTime = value) }
    fun setEndTime(value: LocalTime) = _uiState.update { it.copy(endTime = value) }
    fun setCategory(value: EventCategory) = _uiState.update { it.copy(category = value) }
    fun setCourseLabel(value: String) = _uiState.update { it.copy(courseLabel = value) }
    fun setType(value: EventType) = _uiState.update { state ->
        // Seed the recurrence chips so the first one matches the picked date.
        val seededDays = if (value == EventType.CLASS_MEETING && state.recurrenceDays.isEmpty()) {
            state.date?.dayOfWeek?.let(::setOf) ?: emptySet()
        } else state.recurrenceDays
        state.copy(type = value, recurrenceDays = seededDays)
    }

    fun toggleRecurrenceDay(day: DayOfWeek) = _uiState.update {
        it.copy(
            recurrenceDays = if (day in it.recurrenceDays) it.recurrenceDays - day else it.recurrenceDays + day,
        )
    }

    fun setRecurrenceEndDate(value: LocalDate) = _uiState.update { it.copy(recurrenceEndDate = value) }

    // Typing past a selected place unpins it.
    fun setLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null, pickedCustomLocation = null)
    }

    fun selectLocationSuggestion(suggestion: LocationSuggestion) = _uiState.update {
        // Local vals — cross-module properties don't smart-cast.
        val lat = suggestion.latitude
        val lng = suggestion.longitude
        val custom = if (suggestion.isCustom && lat != null && lng != null) {
            SharedLocation(suggestion.name, lat, lng)
        } else null
        it.copy(location = suggestion.name, placeId = suggestion.id, pickedCustomLocation = custom)
    }

    fun useCustomLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null, pickedCustomLocation = null)
    }

    fun toggleFriend(uid: String) {
        _uiState.update { state ->
            val current = state.selectedFriendUids
            state.copy(
                selectedFriendUids = if (uid in current) current - uid else current + uid,
            )
        }
    }

    fun setSelectedFriends(uids: Set<String>) {
        _uiState.update { it.copy(selectedFriendUids = uids) }
    }

    fun save() {
        val ownerUid = authRepository.getCurrentUserUid() ?: return
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        val startInstant = state.date!!.atTime(state.startTime!!).atZone(EventZone).toInstant()
        // ASSIGNMENTs collapse to one moment — end == start.
        val endInstant = if (state.type == EventType.ASSIGNMENT) {
            startInstant
        } else {
            state.date.atTime(state.endTime!!).atZone(EventZone).toInstant()
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            // Catches users who typed a known place name without tapping the suggestion.
            val resolvedPlaceId = state.placeId
                ?: state.location.takeIf { it.isNotBlank() }
                    ?.let { placeRepository.resolveFromText(it) }

            // Same idea — fetch marker coords now so recipients still get a snapshot.
            val resolvedCustomLocation = state.pickedCustomLocation
                ?: resolvedPlaceId
                    ?.takeIf { it.startsWith("marker_") }
                    ?.let { placeRepository.getPlace(it) }
                    ?.let { place ->
                        SharedLocation(place.name, place.latitude, place.longitude)
                    }

            // Auto-claim a user-course row for any new courseLabel — keeps the
            // event detail + calendar pill working even before the user opens
            // the My Courses screen to customize.
            ensureUserCourseForLabel(state.courseLabel)

            val editingId = state.editingEventId
            if (editingId != null) {
                applyEdit(editingId, ownerUid, state, startInstant, endInstant, resolvedPlaceId, resolvedCustomLocation)
            } else {
                applyCreate(ownerUid, state, startInstant, endInstant, resolvedPlaceId, resolvedCustomLocation)
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    /**
     * If the user typed a courseLabel that doesn't yet exist in their course
     * profile, create a row for it so the family-key shows up in autocomplete
     * and the calendar pill renders in a stable color. Skips silently when
     * the user is at the 20-active cap — old events still color via the
     * decorator's sort-position fallback.
     */
    private suspend fun ensureUserCourseForLabel(rawLabel: String) {
        val normalized = normalizeCourseLabel(rawLabel) ?: return
        val familyKey = CourseColorAssigner.familyKey(normalized)
        if (userCourseRepository.getByFamilyKey(familyKey) != null) return

        val active = userCourseRepository.observeCourses().first().count { !it.archived }
        if (active >= MAX_ACTIVE_USER_COURSES) return

        userCourseRepository.upsert(
            UserCourse(
                familyKey = familyKey,
                code = familyKey,
                displayName = normalized,
                colorChoice = CourseColorChoice.Palette(active % COURSE_PALETTE_SIZE),
                archived = false,
            )
        )
    }

    private suspend fun applyCreate(
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ) {
        val sharedWith = state.friends
            .filter { it.uid in state.selectedFriendUids }
            .associate { it.uid to it.displayName }

        val event = baseEvent(
            id = "",
            ownerUid = ownerUid,
            state = state,
            startInstant = startInstant,
            endInstant = endInstant,
            resolvedPlaceId = resolvedPlaceId,
            resolvedCustomLocation = resolvedCustomLocation,
        )
        customEventRepository.createEvent(event, sharedWith)
    }

    private suspend fun applyEdit(
        eventId: String,
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ) {
        val event = baseEvent(
            id = eventId,
            ownerUid = ownerUid,
            state = state,
            startInstant = startInstant,
            endInstant = endInstant,
            resolvedPlaceId = resolvedPlaceId,
            resolvedCustomLocation = resolvedCustomLocation,
        )
        customEventRepository.updateEvent(event)

        val toRemove = state.existingAttendees.keys - state.selectedFriendUids
        for (uid in toRemove) {
            customEventRepository.removeAttendee(eventId, uid)
        }

        val toAdd = (state.selectedFriendUids - state.existingAttendees.keys)
            .mapNotNull { uid ->
                state.friends.firstOrNull { it.uid == uid }?.let { uid to it.displayName }
            }
            .toMap()
        if (toAdd.isNotEmpty()) {
            customEventRepository.addAttendees(eventId, toAdd)
        }
    }

    private fun baseEvent(
        id: String,
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ): CalendarEvent {
        // SharedLocation snapshots only for custom markers — campus place IDs are
        // stable cross-user, so denormalizing them is just storage waste. Re-sync
        // the title on edits so renamed markers propagate to recipients.
        val customLocation = resolvedCustomLocation
            ?.takeIf { resolvedPlaceId?.startsWith("marker_") == true }
            ?.copy(title = state.location.trim())

        // Recurring tab is the "umbrella" — class meetings live here only when
        // the user actually labels a course. Bare recurring entries persist as
        // EVENT so the calendar treats them as customs that happen to repeat.
        val normalizedCourse = normalizeCourseLabel(state.courseLabel)
        val storedType = when {
            state.type == EventType.CLASS_MEETING && !normalizedCourse.isNullOrBlank() -> EventType.CLASS_MEETING
            state.type == EventType.CLASS_MEETING -> EventType.EVENT
            else -> state.type
        }
        return CalendarEvent(
            id = id,
            title = state.title.trim(),
            description = state.description.trim(),
            location = state.location.trim(),
            startTime = startInstant,
            endTime = endInstant,
            categories = listOf(state.category),
            url = "",
            status = "CONFIRMED",
            isBookmarked = true,
            lastSyncedAt = Instant.now(),
            source = EventSource.USER_CREATED,
            ownerUid = ownerUid,
            ownerDisplayName = authRepository.getCurrentUserDisplayName().orEmpty(),
            placeId = resolvedPlaceId,
            customLocation = customLocation,
            type = storedType,
            // Mirror onto dueAt so sites that prefer it don't need a fallback.
            dueAt = if (storedType == EventType.ASSIGNMENT) startInstant else null,
            courseLabel = normalizedCourse,
            isCompleted = state.isCompleted,
            recurrence = if (state.type == EventType.CLASS_MEETING &&
                state.recurrenceDays.isNotEmpty() &&
                state.recurrenceEndDate != null
            ) {
                RecurrenceRule(state.recurrenceDays, state.recurrenceEndDate)
            } else null,
        )
    }
}

private fun Place.toSuggestion(): LocationSuggestion = LocationSuggestion(
    id = id,
    name = name,
    isCustom = isCustom,
    subtitle = if (isCustom) "Your marker" else "",
    latitude = latitude.takeIf { isCustom },
    longitude = longitude.takeIf { isCustom },
)

// Mirrors CoursesViewModel.MaxActiveCourses — the auto-create path stops at
// the same cap rather than letting fresh courseLabels sneak past. Shared
// with ImportEventsViewModel so .ics bulk-imports honor the same ceiling.
internal const val MAX_ACTIVE_USER_COURSES = 20
internal const val COURSE_PALETTE_SIZE = 6
