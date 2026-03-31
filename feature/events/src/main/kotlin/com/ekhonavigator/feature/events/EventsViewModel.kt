package com.ekhonavigator.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: Boolean
        get() = authRepository.getCurrentUserUid() != null

    // ---- Filter state ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sourceFilter = MutableStateFlow(EventSourceFilter.ALL)
    val sourceFilter: StateFlow<EventSourceFilter> = _sourceFilter.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    // ---- Derived event list ----

    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _searchQuery,
        _sourceFilter,
        _selectedCategories,
    ) { allEvents, query, source, selected ->
        val now = LocalDate.now(ZoneId.of("America/Los_Angeles"))
            .atStartOfDay(ZoneId.of("America/Los_Angeles"))
            .toInstant()

        allEvents.filter { event ->
            val notPast = event.startTime >= now

            val matchesSource = when (source) {
                EventSourceFilter.ALL -> true
                EventSourceFilter.CAMPUS -> event.source == EventSource.ICAL_FEED
                EventSourceFilter.PERSONAL -> event.source != EventSource.ICAL_FEED
            }

            val matchesQuery = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            val matchesCategory = selected.isEmpty() ||
                    event.categories.any { it in selected }

            notPast && matchesSource && matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Categories available in the currently source-filtered events (for reactive chip row). */
    val availableCategories: StateFlow<List<EventCategory>> = combine(
        repository.observeEvents(),
        _sourceFilter,
    ) { allEvents, source ->
        val now = LocalDate.now(ZoneId.of("America/Los_Angeles"))
            .atStartOfDay(ZoneId.of("America/Los_Angeles"))
            .toInstant()

        allEvents
            .filter { event ->
                event.startTime >= now && when (source) {
                    EventSourceFilter.ALL -> true
                    EventSourceFilter.CAMPUS -> event.source == EventSource.ICAL_FEED
                    EventSourceFilter.PERSONAL -> event.source != EventSource.ICAL_FEED
                }
            }
            .flatMap { it.categories }
            .distinct()
            .sortedBy { it.ordinal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Actions ----

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSourceFilter(filter: EventSourceFilter) {
        _sourceFilter.value = filter
        // Clear category selection when switching source — categories change
        _selectedCategories.value = emptySet()
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

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }
}
