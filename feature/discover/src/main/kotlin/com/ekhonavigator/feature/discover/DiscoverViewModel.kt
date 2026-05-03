package com.ekhonavigator.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.matchesCategories
import com.ekhonavigator.core.model.matchesSourceTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val authRepository: AuthRepository,
    private val customEventRepository: CustomEventRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    private var customEventSyncJob: Job? = null

    private val _isSignedIn = MutableStateFlow(authRepository.getCurrentUserUid() != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    // SCHEDULE excluded until class schedule import is implemented.
    // CANVAS excluded by default — Discover is for browsing campus-authored events;
    // Canvas assignments belong on Calendar / the future Campus tab, not Discover.
    private val _activeSourceTypes = MutableStateFlow(
        EventSourceType.entries.toSet() - EventSourceType.SCHEDULE - EventSourceType.CANVAS,
    )
    val activeSourceTypes: StateFlow<Set<EventSourceType>> = _activeSourceTypes.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _focusedPlaceId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            authRepository.userFlow().collect { userId ->
                val signedIn = userId != null
                _isSignedIn.value = signedIn

                if (signedIn) {
                    startCustomEventSync()
                } else {
                    stopCustomEventSync()
                    clearUserState()
                }
            }
        }
    }

    val focusedPlace: StateFlow<Place?> = combine(
        _focusedPlaceId,
        placeRepository.observePlaces()
            .catch {
                emit(emptyList())
            },
    ) { id, places ->
        id?.let { targetId ->
            places.firstOrNull { place ->
                place.id == targetId
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val discoverEvents: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents()
            .catch {
                emit(emptyList())
            },
        _searchQuery,
        _activeSourceTypes,
        _selectedCategories,
        _focusedPlaceId,
    ) { allEvents, query, activeTypes, categories, focusedPid ->
        val now = LocalDate.now(ZoneId.of("America/Los_Angeles"))
            .atStartOfDay(ZoneId.of("America/Los_Angeles"))
            .toInstant()

        allEvents.filter { event ->
            val notPast = event.startTime >= now

            val matchesQuery = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            val matchesPlace = focusedPid == null || event.placeId == focusedPid

            notPast &&
                    event.matchesSourceTypes(activeTypes) &&
                    event.matchesCategories(categories) &&
                    matchesQuery &&
                    matchesPlace
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFocusedPlaceId(id: String?) {
        _focusedPlaceId.value = id
    }

    fun toggleCategory(category: EventCategory) {
        val current = _selectedCategories.value

        _selectedCategories.value = if (category in current) {
            current - category
        } else {
            current + category
        }
    }

    fun clearCategories() {
        _selectedCategories.value = emptySet()
    }

    fun toggleSourceType(type: EventSourceType) {
        val current = _activeSourceTypes.value

        if (type in current && current.size > 1) {
            _activeSourceTypes.value = current - type
        } else if (type !in current) {
            _activeSourceTypes.value = current + type
        }
    }

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            runCatching {
                repository.toggleBookmark(eventId)
            }
        }
    }

    private fun startCustomEventSync() {
        if (customEventSyncJob?.isActive == true) return

        customEventSyncJob = viewModelScope.launch {
            runCatching {
                customEventRepository.startSync(this)
            }
        }
    }

    private fun stopCustomEventSync() {
        customEventSyncJob?.cancel()
        customEventSyncJob = null
    }

    private fun clearUserState() {
        _searchQuery.value = ""
        _selectedCategories.value = emptySet()
        _focusedPlaceId.value = null
        _activeSourceTypes.value = EventSourceType.entries.toSet() - EventSourceType.SCHEDULE
    }

    override fun onCleared() {
        stopCustomEventSync()
        super.onCleared()
    }
}