package com.ekhonavigator.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.isPast
import com.ekhonavigator.core.model.matchesCategories
import com.ekhonavigator.core.model.matchesSourceTypes
import com.ekhonavigator.feature.calendar.component.DayDot
import com.ekhonavigator.feature.event.component.CourseFilterOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val canvasPlannerRepository: CanvasPlannerRepository,
    canvasCourseRepository: CanvasCourseRepository,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // CAMPUS excluded by default until class-calendar import lands.
    private val _activeSourceTypes = MutableStateFlow(
        EventSourceType.entries.toSet() - EventSourceType.CAMPUS,
    )
    val activeSourceTypes: StateFlow<Set<EventSourceType>> = _activeSourceTypes.asStateFlow()

    // Empty = no filter (show all). Same convention for course ids below.
    private val _selectedCategories = MutableStateFlow<Set<EventCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<EventCategory>> = _selectedCategories.asStateFlow()

    private val _selectedCourseIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCourseIds: StateFlow<Set<String>> = _selectedCourseIds.asStateFlow()

    private val eventIdToCourseId: StateFlow<Map<String, String>> = canvasPlannerRepository
        .observeAllItems()
        .map { items ->
            items.mapNotNull { item -> item.courseId?.let { item.id to it } }.toMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Repo filters to current-term; palette slots key on family, stable across renders.
    val availableCourses: StateFlow<List<CourseFilterOption>> = canvasCourseRepository
        .observeCourses()
        .map { courses ->
            if (courses.isEmpty()) return@map emptyList()
            val slots = CourseColorAssigner.assign(
                courses.map { CourseColorInput(id = it.id, code = it.code) },
            )
            courses.map { course ->
                CourseFilterOption(
                    id = course.id,
                    displayLabel = course.code,
                    paletteSlot = slots[course.id] ?: 0,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _visibleMonthRange = MutableStateFlow(
        YearMonth.now() to YearMonth.now()
    )

    init {
        // Init sits AFTER _visibleMonthRange — declaration-order init would NPE.
        // Canvas sync pulls -2/+3 months so past-due and far-future both surface.
        viewModelScope.launch {
            _visibleMonthRange.collect { (first, last) ->
                val zone = ZoneId.systemDefault()
                val start = first.minusMonths(2).atDay(1).atStartOfDay(zone).toInstant()
                val end = last.plusMonths(3).atDay(1).atStartOfDay(zone).toInstant()
                canvasPlannerRepository.sync(start, end)
            }
        }
    }

    // ±1 week bleed-in covers prev/next-month days visible at grid edges.
    private val rawEventsForMonth: StateFlow<List<CalendarEvent>> = _visibleMonthRange
        .flatMapLatest { (first, last) ->
            val zone = ZoneId.systemDefault()
            val start = first.atDay(1).minusWeeks(1).atStartOfDay(zone).toInstant()
            val end = last.plusMonths(1).atDay(1).plusWeeks(1).atStartOfDay(zone).toInstant()
            repository.observeEventsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val eventsForMonth: StateFlow<List<CalendarEvent>> = combine(
        rawEventsForMonth,
        _activeSourceTypes,
        _selectedCategories,
        _selectedCourseIds,
        eventIdToCourseId,
    ) { events, activeTypes, categories, courseIds, eventCourseMap ->
        events.applyVisibilityFilters(activeTypes, categories, courseIds, eventCourseMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = _selectedDate
        .flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            combine(
                repository.observeEventsByDateRange(start, end),
                _activeSourceTypes,
                _selectedCategories,
                _selectedCourseIds,
                eventIdToCourseId,
            ) { events, activeTypes, categories, courseIds, eventCourseMap ->
                events.applyVisibilityFilters(activeTypes, categories, courseIds, eventCourseMap)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedWeekStart = MutableStateFlow(weekStartFor(LocalDate.now()))

    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()

    val eventsForWeek: StateFlow<List<CalendarEvent>> = _selectedWeekStart
        .flatMapLatest { weekStart ->
            val zone = ZoneId.systemDefault()
            val start = weekStart.atStartOfDay(zone).toInstant()
            val end = weekStart.plusDays(7).atStartOfDay(zone).toInstant()
            combine(
                repository.observeEventsByDateRange(start, end),
                _activeSourceTypes,
                _selectedCategories,
                _selectedCourseIds,
                eventIdToCourseId,
            ) { events, activeTypes, categories, courseIds, eventCourseMap ->
                events.applyVisibilityFilters(activeTypes, categories, courseIds, eventCourseMap)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _miniCalendarMonth = MutableStateFlow(YearMonth.now())

    private val courseSlotsById: StateFlow<Map<String, Int>> = canvasCourseRepository
        .observeCourses()
        .map { courses ->
            CourseColorAssigner.assign(
                courses.map { CourseColorInput(id = it.id, code = it.code) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Per-course slot for Canvas-with-courseId; source-type fallback otherwise.
    // Distinct set per day so dense days don't repeat dots.
    val miniCalendarDayDots: StateFlow<Map<LocalDate, Set<DayDot>>> =
        _miniCalendarMonth
            .flatMapLatest { month ->
                val zone = ZoneId.systemDefault()
                val start = month.atDay(1).atStartOfDay(zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
                combine(
                    repository.observeEventsByDateRange(start, end),
                    _activeSourceTypes,
                    _selectedCategories,
                    _selectedCourseIds,
                    eventIdToCourseId,
                    courseSlotsById,
                ) { args ->
                    @Suppress("UNCHECKED_CAST")
                    val events = args[0] as List<CalendarEvent>
                    @Suppress("UNCHECKED_CAST")
                    val activeTypes = args[1] as Set<EventSourceType>
                    @Suppress("UNCHECKED_CAST")
                    val categories = args[2] as Set<EventCategory>
                    @Suppress("UNCHECKED_CAST")
                    val courseIds = args[3] as Set<String>
                    @Suppress("UNCHECKED_CAST")
                    val eventCourseMap = args[4] as Map<String, String>
                    @Suppress("UNCHECKED_CAST")
                    val slotsByCourseId = args[5] as Map<String, Int>

                    events
                        .applyVisibilityFilters(activeTypes, categories, courseIds, eventCourseMap)
                        .groupBy { it.startTime.atZone(zone).toLocalDate() }
                        .mapValues { (_, dayEvents) ->
                            dayEvents.map { event ->
                                val slot = eventCourseMap[event.id]?.let { slotsByCourseId[it] }
                                if (event.source == EventSource.CANVAS && slot != null) {
                                    DayDot.CourseSlot(slot)
                                } else {
                                    DayDot.Source(event.toSourceType())
                                }
                            }.toSet()
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    fun toggleCourse(courseId: String) {
        val current = _selectedCourseIds.value
        _selectedCourseIds.value = if (courseId in current) current - courseId else current + courseId
    }

    fun clearCourses() {
        _selectedCourseIds.value = emptySet()
    }

    fun initializeFilters(
        sourceTypes: Set<EventSourceType>,
        categories: Set<EventCategory>,
    ) {
        // Empty sourceTypes = keep defaults. Empty categories = show all (always apply).
        if (sourceTypes.isNotEmpty()) {
            _activeSourceTypes.value = sourceTypes
        }
        _selectedCategories.value = categories
    }

    fun toggleSourceType(type: EventSourceType) {
        val current = _activeSourceTypes.value
        // At least one source type must stay active.
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

// Past pending invites are hidden here; the Invites screen has its own "Show past" toggle.
private fun List<CalendarEvent>.applyVisibilityFilters(
    activeTypes: Set<EventSourceType>,
    categories: Set<EventCategory>,
    selectedCourseIds: Set<String>,
    eventIdToCourseId: Map<String, String>,
): List<CalendarEvent> {
    val now = Instant.now()
    return filter { event ->
        val isStalePendingInvite = event.source == EventSource.SHARED &&
            event.myRsvpStatus == RsvpStatus.PENDING &&
            event.isPast(now)
        // Course filter scopes to ASSIGNMENTs only; unbridged ones drop when set is non-empty.
        val coursePasses = selectedCourseIds.isEmpty() ||
            event.type != EventType.ASSIGNMENT ||
            eventIdToCourseId[event.id] in selectedCourseIds
        !isStalePendingInvite &&
            event.matchesSourceTypes(activeTypes) &&
            event.matchesCategories(categories) &&
            coursePasses
    }
}

private fun CalendarEvent.toSourceType(): EventSourceType = when {
    source == EventSource.ICAL_FEED && isBookmarked -> EventSourceType.BOOKMARKED
    source == EventSource.ICAL_FEED -> EventSourceType.CAMPUS
    source == EventSource.USER_CREATED || source == EventSource.SHARED -> EventSourceType.CUSTOM
    source == EventSource.CANVAS -> EventSourceType.CANVAS
    else -> EventSourceType.CAMPUS
}

internal fun weekStartFor(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
