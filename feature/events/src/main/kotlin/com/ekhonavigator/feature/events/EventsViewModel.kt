package com.ekhonavigator.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Drives the Events list screen.
 *
 * Combines three reactive streams — the full event list from Room, the
 * user's search query, and the selected category filter — into a single
 * filtered list. Whenever ANY of those three change, the combine operator
 * re-evaluates the filter lambda and emits a new list.
 *
 * Think of combine like a spreadsheet formula: =FILTER(allEvents, query, category).
 * Change any input cell and the output recalculates automatically.
 */
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    // ---- Filter state ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null means "show all categories" */
    private val _selectedCategory = MutableStateFlow<EventCategory?>(null)
    val selectedCategory: StateFlow<EventCategory?> = _selectedCategory.asStateFlow()

    // ---- Derived event list ----

    /**
     * The filtered event list. [combine] merges three Flows into one:
     * whenever the repo emits new data OR the user changes the query/category,
     * the filter lambda runs again and downstream (the UI) gets the new list.
     *
     * [stateIn] converts the cold Flow into a hot StateFlow that the Compose
     * UI can collect. WhileSubscribed(5_000) keeps it alive for 5 seconds
     * after the last collector disappears (survives quick config changes
     * like rotation without re-querying Room).
     */
    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _searchQuery,
        _selectedCategory,
    ) { allEvents, query, category ->
        // Start of today in California time — hide events that have already ended
        val now = LocalDate.now(ZoneId.of("America/Los_Angeles"))
            .atStartOfDay(ZoneId.of("America/Los_Angeles"))
            .toInstant()

        allEvents.filter { event ->
            val notPast = event.startTime >= now
            val matchesQuery = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)
            val matchesCategory = category == null || category in event.categories
            notPast && matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Actions ----

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: EventCategory?) {
        _selectedCategory.value = category
    }

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }
}
