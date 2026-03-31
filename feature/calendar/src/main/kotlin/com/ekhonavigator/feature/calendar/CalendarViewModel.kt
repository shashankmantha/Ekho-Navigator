package com.ekhonavigator.feature.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
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

    // ---- User-driven state ----

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _sourceFilter = MutableStateFlow(EventSourceFilter.ALL)
    val sourceFilter: StateFlow<EventSourceFilter> = _sourceFilter.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    // ---- Derived event streams ----

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
    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = combine(
        rawEventsForSelectedDate,
        _sourceFilter,
        _selectedCategories,
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
    val availableCategories: StateFlow<List<EventCategory>> = combine(
        rawEventsForSelectedDate,
        _sourceFilter,
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

    // ---- Actions ----

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    fun setSourceFilter(filter: EventSourceFilter) {
        _sourceFilter.value = filter
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

    fun refresh() {
        SyncInitializer.requestImmediateSync(appContext, FEED_URL)
    }

    companion object {
        // TODO: Move to a shared configuration provider
        private const val FEED_URL =
            "https://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    }
}
