package com.ekhonavigator.feature.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.sync.SyncInitializer
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val authRepository: AuthRepository,
    private val customEventRepository: CustomEventRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    val isSignedIn: Boolean
        get() = authRepository.getCurrentUserUid() != null

    init {
        if (isSignedIn) {
            customEventRepository.startSync(viewModelScope)
        }
    }

    // ══════════════════════════════════════════════════
    // Shared state (used across tabs)
    // ══════════════════════════════════════════════════

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // ══════════════════════════════════════════════════
    // Month tab state (ported from CalendarViewModel)
    // ══════════════════════════════════════════════════

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _monthSourceFilter = MutableStateFlow(EventSourceFilter.ALL)
    val monthSourceFilter: StateFlow<EventSourceFilter> = _monthSourceFilter.asStateFlow()

    private val _monthSelectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val monthSelectedCategories: StateFlow<Set<EventCategory>> = _monthSelectedCategories.asStateFlow()

    /** All events in the selected date range (unfiltered by source/category). */
    private val rawEventsForSelectedDate: StateFlow<List<CalendarEvent>> = _selectedDate
        .flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            repository.observeEventsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Events for the selected date, filtered by source and category. */
    val monthEventsForSelectedDate: StateFlow<List<CalendarEvent>> = combine(
        rawEventsForSelectedDate,
        _monthSourceFilter,
        _monthSelectedCategories,
    ) { events, source, selected ->
        events.filter { event ->
            val matchesSource = when (source) {
                EventSourceFilter.ALL -> true
                EventSourceFilter.CAMPUS -> event.source == EventSource.ICAL_FEED
                EventSourceFilter.PERSONAL -> event.source != EventSource.ICAL_FEED
            }
            val matchesCategory = selected.isEmpty() ||
                    event.categories.any { it in selected }
            matchesSource && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Categories available in the source-filtered events for the selected date. */
    val monthAvailableCategories: StateFlow<List<EventCategory>> = combine(
        rawEventsForSelectedDate,
        _monthSourceFilter,
    ) { events, source ->
        events
            .filter { event ->
                when (source) {
                    EventSourceFilter.ALL -> true
                    EventSourceFilter.CAMPUS -> event.source == EventSource.ICAL_FEED
                    EventSourceFilter.PERSONAL -> event.source != EventSource.ICAL_FEED
                }
            }
            .flatMap { it.categories }
            .distinct()
            .sortedBy { it.ordinal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All events in the month (unfiltered, for calendar dot indicators). */
    val eventsForMonth: StateFlow<List<CalendarEvent>> = _currentMonth
        .flatMapLatest { month ->
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
            repository.observeEventsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ══════════════════════════════════════════════════
    // Discover tab state (ported from EventsViewModel)
    // ══════════════════════════════════════════════════

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _discoverSourceFilter = MutableStateFlow(EventSourceFilter.ALL)
    val discoverSourceFilter: StateFlow<EventSourceFilter> = _discoverSourceFilter.asStateFlow()

    private val _discoverSelectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val discoverSelectedCategories: StateFlow<Set<EventCategory>> = _discoverSelectedCategories.asStateFlow()

    /** Filtered event list for the Discover tab. */
    val discoverEvents: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _searchQuery,
        _discoverSourceFilter,
        _discoverSelectedCategories,
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

    /** Categories available in the currently source-filtered discover events. */
    val discoverAvailableCategories: StateFlow<List<EventCategory>> = combine(
        repository.observeEvents(),
        _discoverSourceFilter,
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

    // ══════════════════════════════════════════════════
    // Actions — Month tab
    // ══════════════════════════════════════════════════

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    fun setMonthSourceFilter(filter: EventSourceFilter) {
        _monthSourceFilter.value = filter
        _monthSelectedCategories.value = emptySet()
    }

    fun toggleMonthCategory(category: EventCategory) {
        val current = _monthSelectedCategories.value
        _monthSelectedCategories.value = if (category in current) {
            current - category
        } else {
            current + category
        }
    }

    fun clearMonthCategories() {
        _monthSelectedCategories.value = emptySet()
    }

    // ══════════════════════════════════════════════════
    // Actions — Discover tab
    // ══════════════════════════════════════════════════

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDiscoverSourceFilter(filter: EventSourceFilter) {
        _discoverSourceFilter.value = filter
        _discoverSelectedCategories.value = emptySet()
    }

    fun toggleDiscoverCategory(category: EventCategory) {
        val current = _discoverSelectedCategories.value
        _discoverSelectedCategories.value = if (category in current) {
            current - category
        } else {
            current + category
        }
    }

    fun clearDiscoverCategories() {
        _discoverSelectedCategories.value = emptySet()
    }

    // ══════════════════════════════════════════════════
    // Actions — Shared
    // ══════════════════════════════════════════════════

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }

    fun refresh() {
        SyncInitializer.requestImmediateSync(appContext, FEED_URL)
    }

    companion object {
        private const val FEED_URL =
            "https://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    }
}
