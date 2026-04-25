package com.ekhonavigator.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.isPast
import com.ekhonavigator.core.model.matchesCategories
import com.ekhonavigator.core.model.matchesSourceTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
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

    // ══════════════════════════════════════════════════
    // Shared state (used across Month + Day screens)
    // ══════════════════════════════════════════════════

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Multi-select source filter: all types active by default. */
    // calendar excluded until class calendar import is implemented
    private val _activeSourceTypes = MutableStateFlow(
        EventSourceType.entries.toSet() - EventSourceType.SCHEDULE - EventSourceType.CAMPUS,
    )
    val activeSourceTypes: StateFlow<Set<EventSourceType>> = _activeSourceTypes.asStateFlow()

    /** Multi-select category filter: empty = show all categories. */
    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    // ══════════════════════════════════════════════════
    // Month tab state
    // ══════════════════════════════════════════════════

    private val _visibleMonthRange = MutableStateFlow(
        YearMonth.now() to YearMonth.now()
    )

    /**
     * All events visible on the calendar grid (unfiltered).
     * Spans from one week before the first visible month to one week
     * after the last visible month, covering all bleed-in days.
     */
    private val rawEventsForMonth: StateFlow<List<CalendarEvent>> = _visibleMonthRange
        .flatMapLatest { (first, last) ->
            val zone = ZoneId.systemDefault()
            val start = first.atDay(1).minusWeeks(1).atStartOfDay(zone).toInstant()
            val end = last.plusMonths(1).atDay(1).plusWeeks(1).atStartOfDay(zone).toInstant()
            repository.observeEventsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Events for the month, filtered by source types + categories. */
    val eventsForMonth: StateFlow<List<CalendarEvent>> = combine(
        rawEventsForMonth,
        _activeSourceTypes,
        _selectedCategories,
    ) { events, activeTypes, categories ->
        events.applyVisibilityFilters(activeTypes, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Events for the selected date, filtered by source types + categories (for DayScreen). */
    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = _selectedDate
        .flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            combine(
                repository.observeEventsByDateRange(start, end),
                _activeSourceTypes,
                _selectedCategories,
            ) { events, activeTypes, categories ->
                events.applyVisibilityFilters(activeTypes, categories)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ══════════════════════════════════════════════════
    // Week tab state
    // ══════════════════════════════════════════════════

    private val _selectedWeekStart = MutableStateFlow(weekStartFor(LocalDate.now()))

    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()

    /** Events for the selected week (Sunday–Saturday), filtered. */
    val eventsForWeek: StateFlow<List<CalendarEvent>> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            val zone = ZoneId.systemDefault()
            val start = weekStart.atStartOfDay(zone).toInstant()
            val end = weekStart.plusDays(7).atStartOfDay(zone).toInstant()
            combine(
                repository.observeEventsByDateRange(start, end),
                _activeSourceTypes,
                _selectedCategories,
            ) { events, activeTypes, categories ->
                events.applyVisibilityFilters(activeTypes, categories)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ══════════════════════════════════════════════════
    // Mini-calendar state (Day tab / Week tab dot indicators)
    // ══════════════════════════════════════════════════

    private val _miniCalendarMonth = MutableStateFlow(YearMonth.now())

    /** Source types per date in the mini-calendar's visible month, for colored dot indicators. */
    val miniCalendarDaySourceTypes: StateFlow<Map<LocalDate, Set<EventSourceType>>> =
        _miniCalendarMonth
            .flatMapLatest { month ->
                val zone = ZoneId.systemDefault()
                val start = month.atDay(1).atStartOfDay(zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
                combine(
                    repository.observeEventsByDateRange(start, end),
                    _activeSourceTypes,
                    _selectedCategories,
                ) { events, activeTypes, categories ->
                    events
                        .applyVisibilityFilters(activeTypes, categories)
                        .groupBy { it.startTime.atZone(zone).toLocalDate() }
                        .mapValues { (_, dayEvents) ->
                            dayEvents.map { it.toSourceType() }.toSet()
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ══════════════════════════════════════════════════
    // Actions — calendar source filter (Month + Day)
    // ══════════════════════════════════════════════════

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setVisibleMonthRange(first: YearMonth, last: YearMonth) {
        _visibleMonthRange.value = first to last
    }

    fun selectWeek(dateInWeek: LocalDate) {
        _selectedWeekStart.value = weekStartFor(dateInWeek)
    }

    fun setMiniCalendarMonth(month: YearMonth) {
        _miniCalendarMonth.value = month
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

    /**
     * One-shot initializer: sets the source-type and category filters to match
     * the parent calendar screen's active selections. Called when the DayScreen
     * is first composed with filter params from the nav key.
     */
    fun initializeFilters(
        sourceTypes: Set<EventSourceType>,
        categories: Set<EventCategory>,
    ) {
        if (sourceTypes.isNotEmpty()) {
            _activeSourceTypes.value = sourceTypes
        }
        // Empty categories = show all, which is already the default,
        // so we always apply what was passed.
        _selectedCategories.value = categories
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

    // ══════════════════════════════════════════════════
    // Actions — Shared
    // ══════════════════════════════════════════════════

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }
}

/**
 * Applies the calendar's visibility rules. Past pending invites are hidden so
 * stale "going?" cards don't clutter days the user already lived through; the
 * Invites screen still surfaces them via its own "Show past" toggle.
 */
private fun List<CalendarEvent>.applyVisibilityFilters(
    activeTypes: Set<EventSourceType>,
    categories: Set<EventCategory>,
): List<CalendarEvent> {
    val now = Instant.now()
    return filter { event ->
        val isStalePendingInvite = event.source == EventSource.SHARED &&
            event.myRsvpStatus == RsvpStatus.PENDING &&
            event.isPast(now)
        !isStalePendingInvite &&
            event.matchesSourceTypes(activeTypes) &&
            event.matchesCategories(categories)
    }
}

/** Map a [CalendarEvent] to its display [EventSourceType] for dot coloring. */
private fun CalendarEvent.toSourceType(): EventSourceType = when {
    source == EventSource.ICAL_FEED && isBookmarked -> EventSourceType.BOOKMARKED
    source == EventSource.ICAL_FEED -> EventSourceType.CAMPUS
    source == EventSource.USER_CREATED || source == EventSource.SHARED -> EventSourceType.CUSTOM
    else -> EventSourceType.SCHEDULE
}

/** Get the Sunday that starts the week containing [date]. */
internal fun weekStartFor(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
