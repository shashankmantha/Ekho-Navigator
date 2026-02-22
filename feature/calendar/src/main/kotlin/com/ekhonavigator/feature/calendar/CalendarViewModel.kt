package com.ekhonavigator.feature.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.sync.SyncInitializer
import com.ekhonavigator.core.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

/**
 * Drives the Calendar screen.
 *
 * Holds the currently selected date and displayed month. When either changes,
 * the corresponding event lists re-query the repository automatically via
 * flatMapLatest — a Flow operator that cancels the previous collector and
 * switches to a new one whenever the upstream value changes (like switching
 * radio channels: you only hear the latest station).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // ---- User-driven state ----

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // ---- Derived event streams ----

    /**
     * Events that fall on [selectedDate]. Updates automatically when the
     * user taps a different day.
     *
     * flatMapLatest: whenever _selectedDate emits a new date, this cancels
     * the old Room query Flow and subscribes to a new one for the new range.
     */
    val eventsForSelectedDate: StateFlow<List<CalendarEvent>> = _selectedDate
        .flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            repository.observeEventsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * All events in [currentMonth], used to render indicator dots on the
     * calendar grid (so you can see at a glance which days have events).
     */
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

    /**
     * Enqueues a one-time WorkManager sync. The actual network call happens
     * in CalendarSyncWorker on a background thread — this just drops the
     * request into WorkManager's queue and returns immediately.
     */
    fun refresh() {
        SyncInitializer.requestImmediateSync(appContext, FEED_URL)
    }

    companion object {
        // TODO: Move to a shared configuration provider
        private const val FEED_URL =
            "https://25livepub.collegenet.com/calendars/CSUCI_EVENTS.ics"
    }
}
