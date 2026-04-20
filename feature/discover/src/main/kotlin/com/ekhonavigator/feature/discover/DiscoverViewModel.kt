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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val isSignedIn: Boolean
        get() = authRepository.getCurrentUserUid() != null

    init {
        if (isSignedIn) {
            customEventRepository.startSync(viewModelScope)
        }
    }

    // SCHEDULE excluded until class schedule import is implemented
    private val _activeSourceTypes = MutableStateFlow(
        EventSourceType.entries.toSet() - EventSourceType.SCHEDULE,
    )
    val activeSourceTypes: StateFlow<Set<EventSourceType>> = _activeSourceTypes.asStateFlow()

    /** Multi-select category filter: empty = show all categories. */
    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _focusedPlaceId = MutableStateFlow<String?>(null)

    val focusedPlace: StateFlow<Place?> = combine(
        _focusedPlaceId,
        placeRepository.observePlaces(),
    ) { id, places ->
        id?.let { targetId -> places.firstOrNull { it.id == targetId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val discoverEvents: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
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

            notPast && event.matchesSourceTypes(activeTypes) &&
                    event.matchesCategories(categories) && matchesQuery && matchesPlace
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        // Don't allow deselecting all — at least one must stay active
        if (type in current && current.size > 1) {
            _activeSourceTypes.value = current - type
        } else if (type !in current) {
            _activeSourceTypes.value = current + type
        }
    }

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }
}
