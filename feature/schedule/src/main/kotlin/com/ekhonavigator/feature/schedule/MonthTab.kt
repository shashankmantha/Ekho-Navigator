package com.ekhonavigator.feature.schedule

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.feature.schedule.component.CalendarTitle
import com.ekhonavigator.feature.schedule.component.DayContent
import com.ekhonavigator.feature.schedule.component.DaysOfWeekHeader
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
internal fun MonthTab(
    viewModel: ScheduleViewModel,
    onDayClick: (Long) -> Unit,
    snapToTodayTrigger: Int = 0,
) {
    val eventsForMonth by viewModel.eventsForMonth.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val scope = rememberCoroutineScope()

    val eventsByDay = remember(eventsForMonth) {
        eventsForMonth.groupBy { event ->
            event.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(12),
        endMonth = YearMonth.now().plusMonths(12),
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfRow,
    )

    LaunchedEffect(calendarState) {
        snapshotFlow {
            calendarState.firstVisibleMonth.yearMonth to
                    calendarState.lastVisibleMonth.yearMonth
        }.collect { (first, last) ->
            viewModel.setVisibleMonthRange(first, last)
        }
    }

    LaunchedEffect(snapToTodayTrigger) {
        if (snapToTodayTrigger > 0) {
            calendarState.animateScrollToMonth(YearMonth.from(today))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned day-of-week header (doesn't scroll)
            DaysOfWeekHeader(
                daysOfWeek = daysOfWeek,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )

            // Vertically scrolling calendar grid — horizontal swipe jumps months
            VerticalCalendar(
                state = calendarState,
                modifier = Modifier.pointerInput(Unit) {
                    var accumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulator = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            accumulator += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 100f
                            if (accumulator > threshold) {
                                scope.launch {
                                    calendarState.animateScrollToMonth(
                                        calendarState.firstVisibleMonth.yearMonth.minusMonths(1),
                                    )
                                }
                            } else if (accumulator < -threshold) {
                                scope.launch {
                                    calendarState.animateScrollToMonth(
                                        calendarState.firstVisibleMonth.yearMonth.plusMonths(1),
                                    )
                                }
                            }
                        },
                    )
                },
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
                },
                dayContent = { day ->
                    DayContent(
                        day = day,
                        isSelected = day.date == today,
                        isToday = day.date == today,
                        events = eventsByDay[day.date].orEmpty(),
                        onClick = { onDayClick(day.date.toEpochDay()) },
                    )
                },
            )
        }
    }
}
