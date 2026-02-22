package com.ekhonavigator.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.feature.calendar.component.CalendarTitle
import com.ekhonavigator.feature.calendar.component.DayContent
import com.ekhonavigator.feature.calendar.component.DaysOfWeekHeader
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Month-view calendar with event indicator dots and a list of events
 * for the selected day below.
 *
 * Data flow:
 *   CalendarViewModel → StateFlow → collectAsStateWithLifecycle → Compose state
 *
 * collectAsStateWithLifecycle converts a Kotlin Flow into Compose State that
 * triggers recomposition when the value changes. The "WithLifecycle" part means
 * it stops collecting when the screen isn't visible (saves battery).
 */
@Composable
fun CalendarScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // Collect ViewModel StateFlows as Compose state
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val eventsForSelectedDate by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
    val eventsForMonth by viewModel.eventsForMonth.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val scope = rememberCoroutineScope()

    // Group month events by day so each day cell can show indicator dots
    val eventsByDay = remember(eventsForMonth) {
        eventsForMonth.groupBy { event ->
            event.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    // Kizitonwose calendar state — controls which months are scrollable
    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(12),
        endMonth = YearMonth.now().plusMonths(12),
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = daysOfWeek.first(),
    )

    // When the user swipes to a new month, tell the ViewModel so it
    // fetches events for that month range.
    // snapshotFlow bridges Compose state reads into a Kotlin Flow.
    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .collect { month -> viewModel.setMonth(month) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ---- Calendar grid ----
        HorizontalCalendar(
            state = calendarState,
            monthHeader = { month ->
                CalendarTitle(
                    month = month.yearMonth,
                    onPreviousMonth = {
                        scope.launch {
                            calendarState.animateScrollToMonth(
                                month.yearMonth.minusMonths(1),
                            )
                        }
                    },
                    onNextMonth = {
                        scope.launch {
                            calendarState.animateScrollToMonth(
                                month.yearMonth.plusMonths(1),
                            )
                        }
                    },
                )
                DaysOfWeekHeader(daysOfWeek = daysOfWeek)
            },
            dayContent = { day ->
                DayContent(
                    day = day,
                    isSelected = day.date == selectedDate,
                    isToday = day.date == today,
                    events = eventsByDay[day.date].orEmpty(),
                    onClick = { viewModel.selectDate(day.date) },
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ---- Events for selected date ----
        if (eventsForSelectedDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No events on this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(eventsForSelectedDate, key = { it.id }) { event ->
                    EventListItem(
                        event = event,
                        onClick = { onEventClick(event.id) },
                    )
                }
            }
        }
    }
}

/**
 * A compact event card shown in the list below the calendar.
 * Left color bar indicates the event category.
 */
@Composable
private fun EventListItem(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Category color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(event.primaryCategory.color)),
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val startTime = event.startTime.atZone(zone).format(timeFormatter)
                val endTime = event.endTime.atZone(zone).format(timeFormatter)
                Text(
                    text = "$startTime – $endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
