package com.ekhonavigator.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceType
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
class ScheduleViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val authRepository: AuthRepository,
    private val customEventRepository: CustomEventRepository,
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

    /** Upcoming events, filtered by source, category, and search query. */
    val discoverEvents: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _searchQuery,
        _activeSourceTypes,
        _selectedCategories,
    ) { allEvents, query, activeTypes, categories ->
        val now = LocalDate.now(ZoneId.of("America/Los_Angeles"))
            .atStartOfDay(ZoneId.of("America/Los_Angeles"))
            .toInstant()

        allEvents.filter { event ->
            val notPast = event.startTime >= now

            val matchesQuery = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            notPast && event.matchesSourceTypes(activeTypes) &&
                    event.matchesCategories(categories) && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

/**
 * Check if a [CalendarEvent] matches any of the active [EventSourceType]s.
 * An event matches if ANY active type includes it.
 */
private fun CalendarEvent.matchesSourceTypes(activeTypes: Set<EventSourceType>): Boolean {
    for (type in activeTypes) {
        when (type) {
            EventSourceType.SCHEDULE -> {
                // CLASS_SCHEDULE source (future — not yet in EventSource)
            }

            EventSourceType.CUSTOM -> {
                if (source == EventSource.USER_CREATED || source == EventSource.SHARED) return true
            }

            EventSourceType.CAMPUS -> {
                if (source == EventSource.ICAL_FEED) return true
            }

            EventSourceType.BOOKMARKED -> {
                if (source == EventSource.ICAL_FEED && isBookmarked) return true
            }
        }
    }
    return false
}

/**
 * Check if a [CalendarEvent] matches the selected categories.
 * Empty set = no filter = all events pass.
 */
private fun CalendarEvent.matchesCategories(selected: Set<EventCategory>): Boolean =
    selected.isEmpty() || categories.any { it in selected }
