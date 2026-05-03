package com.ekhonavigator.feature.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.feature.calendar.component.MiniMonthCalendar
import com.ekhonavigator.feature.calendar.component.TimelineGrid
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val WEEK_PAGE_RANGE = 52

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WeekTab(
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit = {},
    snapToTodayTrigger: Int = 0,
) {
    val eventsForWeek by viewModel.eventsForWeek.collectAsStateWithLifecycle()
    val miniCalendarDayDots by viewModel.miniCalendarDayDots.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val todayWeekStart = remember { weekStartFor(today) }

    val pagerState = rememberPagerState(
        initialPage = WEEK_PAGE_RANGE,
        pageCount = { WEEK_PAGE_RANGE * 2 + 1 },
    )

    // Update ViewModel when page changes (swipe)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val weekOffset = page - WEEK_PAGE_RANGE
            viewModel.selectWeek(todayWeekStart.plusWeeks(weekOffset.toLong()))
        }
    }

    // Snap to current week when triggered
    LaunchedEffect(snapToTodayTrigger) {
        if (snapToTodayTrigger > 0) {
            pagerState.animateScrollToPage(WEEK_PAGE_RANGE)
        }
    }

    val scope = rememberCoroutineScope()

    var miniMonthExpanded by remember { mutableStateOf(false) }

    // Current week's dates derived from pager position
    val currentWeekStart = remember(pagerState.currentPage) {
        todayWeekStart.plusWeeks((pagerState.currentPage - WEEK_PAGE_RANGE).toLong())
    }
    val columnDates = remember(currentWeekStart) {
        (0L until 7L).map { currentWeekStart.plusDays(it) }
    }

    // Week label: "Mar 30 – Apr 5"
    val weekLabel = remember(currentWeekStart) {
        val end = currentWeekStart.plusDays(6)
        val startFmt = DateTimeFormatter.ofPattern("MMM d")
        val endFmt = if (currentWeekStart.month == end.month) {
            DateTimeFormatter.ofPattern("d")
        } else {
            DateTimeFormatter.ofPattern("MMM d")
        }
        "${currentWeekStart.format(startFmt)} – ${end.format(endFmt)}"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week header with chevron toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { miniMonthExpanded = !miniMonthExpanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Icon(
                imageVector = EkhoIcons.ChevronRight,
                contentDescription = if (miniMonthExpanded) "Collapse calendar" else "Expand calendar",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (miniMonthExpanded) 270f else 90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Collapsible mini-month
        AnimatedVisibility(
            visible = miniMonthExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            MiniMonthCalendar(
                selectedDate = currentWeekStart,
                dayDots = miniCalendarDayDots,
                onDayClick = { date ->
                    viewModel.selectWeek(date)
                    viewModel.setMiniCalendarMonth(java.time.YearMonth.from(date))
                    val targetWeekStart = weekStartFor(date)
                    val weekOffset =
                        ((targetWeekStart.toEpochDay() - todayWeekStart.toEpochDay()) / 7).toInt()
                    scope.launch { pagerState.animateScrollToPage(WEEK_PAGE_RANGE + weekOffset) }
                },
                onMonthChanged = viewModel::setMiniCalendarMonth,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // Campus event counts per day (unbookmarked iCal feed events)
        val zone = remember { ZoneId.systemDefault() }
        val campusCountByDate = remember(eventsForWeek) {
            eventsForWeek
                .filter { it.source == EventSource.ICAL_FEED && !it.isBookmarked }
                .groupBy { it.startTime.atZone(zone).toLocalDate() }
                .mapValues { (_, events) -> events.size }
        }

        // Day-of-week column headers — clickable to navigate to day view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp), // align with timeline grid (TimeLabelWidth)
        ) {
            columnDates.forEach { date ->
                val isToday = date == today
                val campusCount = campusCountByDate[date] ?: 0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDayClick(date.toEpochDay()) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                    )
                    if (campusCount > 0) {
                        Text(
                            text = "+$campusCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Swipeable week pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            if (page == pagerState.currentPage) {
                // Exclude unbookmarked campus events — shown as +N in header instead
                val timelineEvents = remember(eventsForWeek) {
                    eventsForWeek.filter { event ->
                        !(event.source == EventSource.ICAL_FEED && !event.isBookmarked)
                    }
                }

                TimelineGrid(
                    columnCount = 7,
                    columnDates = columnDates,
                    events = timelineEvents,
                    onEventClick = onEventClick,
                    onDayClick = onDayClick,
                    maxVisibleOverlaps = 2,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
