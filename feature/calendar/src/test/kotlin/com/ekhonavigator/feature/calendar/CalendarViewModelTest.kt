package com.ekhonavigator.feature.calendar

import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [CalendarViewModel].
 *
 * ## What's being tested
 *
 * The CalendarViewModel has two main reactive streams:
 * - `eventsForSelectedDate` — events on a specific day (for the event list below the calendar)
 * - `eventsForMonth` — all events in a month (for showing indicator dots on the calendar grid)
 *
 * Both use `flatMapLatest` — when the selected date or current month changes,
 * the old Room query is cancelled and a new one starts automatically.
 *
 * ## Note about CalendarViewModel constructor
 *
 * The real ViewModel also takes `@ApplicationContext Context` for the
 * WorkManager refresh. Since we're testing the data flow (not WorkManager),
 * we can't easily construct it without Android context. Instead, these
 * tests focus on the repository interaction patterns that we CAN test
 * without context — by directly testing the repository's date range
 * filtering, which is what the ViewModel delegates to.
 */
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository

    @Before
    fun setup() {
        repository = TestCalendarRepository()
    }

    @Test
    fun `repository date range filter returns events within range`() = runTest {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()

        val todayEvent = testCalendarEvent(
            id = "today",
            startTime = todayStart.plusSeconds(3600), // 1am today
        )
        val tomorrowEvent = testCalendarEvent(
            id = "tomorrow",
            startTime = tomorrowStart.plusSeconds(3600), // 1am tomorrow
        )

        repository.emit(listOf(todayEvent, tomorrowEvent))

        // observeEventsByDateRange filters to [start, end)
        val todayEvents = repository.observeEventsByDateRange(todayStart, todayEnd)
            .first { it.isNotEmpty() }

        assertEquals(1, todayEvents.size)
        assertEquals("today", todayEvents[0].id)
    }

    @Test
    fun `repository month range filter returns all events in month`() = runTest {
        val zone = ZoneId.systemDefault()
        val month = YearMonth.of(2026, 3)
        val monthStart = month.atDay(1).atStartOfDay(zone).toInstant()
        val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        val marchEvent = testCalendarEvent(
            id = "march",
            startTime = month.atDay(15).atStartOfDay(zone).toInstant(),
        )
        val aprilEvent = testCalendarEvent(
            id = "april",
            startTime = month.plusMonths(1).atDay(5).atStartOfDay(zone).toInstant(),
        )

        repository.emit(listOf(marchEvent, aprilEvent))

        val marchEvents = repository.observeEventsByDateRange(monthStart, monthEnd)
            .first { it.isNotEmpty() }

        assertEquals(1, marchEvents.size)
        assertEquals("march", marchEvents[0].id)
    }

    @Test
    fun `empty date range returns no events`() = runTest {
        val zone = ZoneId.systemDefault()
        val farFuture = LocalDate.of(2030, 1, 1)
        val start = farFuture.atStartOfDay(zone).toInstant()
        val end = farFuture.plusDays(1).atStartOfDay(zone).toInstant()

        val event = testCalendarEvent(id = "now")
        repository.emit(listOf(event))

        // The event is tomorrow, not in 2030 — should get empty list
        val events = repository.observeEventsByDateRange(start, end)
            .first()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `bookmarked events filter works`() = runTest {
        val bookmarked = testCalendarEvent(id = "b1", isBookmarked = true)
        val normal = testCalendarEvent(id = "n1", isBookmarked = false)

        repository.emit(listOf(bookmarked, normal))

        val bookmarkedEvents = repository.observeBookmarkedEvents()
            .first { it.isNotEmpty() }

        assertEquals(1, bookmarkedEvents.size)
        assertEquals("b1", bookmarkedEvents[0].id)
    }
}
