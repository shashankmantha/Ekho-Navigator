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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.designsystem.theme.LocalSignedIn
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.feature.calendar.component.MiniMonthCalendar
import com.ekhonavigator.feature.calendar.component.TimelineGrid
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Navigational DayScreen — shows a single day timeline for a specific date.
 * Reached from month day cell click or week view "+N" overflow tap.
 */
@Composable
fun DayScreen(
    epochDay: Long,
    onEventClick: (String) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    sourceTypeNames: List<String> = emptyList(),
    categoryNames: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val initialDate = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    // Initialize filters from the parent calendar screen's active selections (one-shot)
    LaunchedEffect(Unit) {
        val sourceTypes = sourceTypeNames.mapNotNull { name ->
            EventSourceType.entries.find { it.name == name }
        }.toSet()
        val categories = categoryNames.mapNotNull { name ->
            EventCategory.entries.find { it.name == name }
        }.toSet()
        viewModel.initializeFilters(sourceTypes, categories)
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.selectDate(LocalDate.now()) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = EkhoIcons.CalendarFilled,
                        contentDescription = "Jump to today",
                        modifier = Modifier.size(20.dp),
                    )
                }

                val signedIn = LocalSignedIn.current
                // Greyed when signed-out (matches CalendarScreen's pattern). The
                // create-event flow needs an ownerUid; the FAB stays visible as a
                // discovery hint but the click is suppressed.
                FloatingActionButton(
                    onClick = {
                        if (!signedIn) return@FloatingActionButton
                        onCreateEventClick(selectedDate.toEpochDay())
                    },
                    containerColor = if (signedIn) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (signedIn) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                ) {
                    Icon(
                        imageVector = EkhoIcons.Add,
                        contentDescription = if (signedIn) "Create event" else "Sign in to create events",
                    )
                }
            }
        },
    ) { paddingValues ->
        DayTimelineContent(
            initialDate = initialDate,
            viewModel = viewModel,
            onEventClick = onEventClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}

/**
 * Day timeline content — used by both the Day tab and the routed DayScreen.
 * Shows a date header with collapsible mini-month and a swipeable single-column timeline.
 */
private const val DAY_PAGE_RANGE = 365

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTimelineContent(
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialDate: LocalDate = LocalDate.now(),
    snapToTodayTrigger: Int = 0,
) {
    val eventsForDay by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val miniCalendarDayDots by viewModel.miniCalendarDayDots.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

    val initialOffset = remember(initialDate) {
        (initialDate.toEpochDay() - today.toEpochDay()).toInt()
    }

    val pagerState = rememberPagerState(
        initialPage = DAY_PAGE_RANGE + initialOffset,
        pageCount = { DAY_PAGE_RANGE * 2 + 1 },
    )
    val scope = rememberCoroutineScope()

    // Update ViewModel when page changes (swipe)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val offset = page - DAY_PAGE_RANGE
            viewModel.selectDate(today.plusDays(offset.toLong()))
        }
    }

    // Snap to today when triggered
    LaunchedEffect(snapToTodayTrigger) {
        if (snapToTodayTrigger > 0) {
            pagerState.animateScrollToPage(DAY_PAGE_RANGE)
        }
    }

    // Mini-month expand/collapse state
    var miniMonthExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Day header with chevron toggle for mini-month
        val currentDate = remember(pagerState.currentPage) {
            today.plusDays((pagerState.currentPage - DAY_PAGE_RANGE).toLong())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { miniMonthExpanded = !miniMonthExpanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = when (currentDate) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    today.minusDays(1) -> "Yesterday"
                    else -> currentDate.format(dayFormatter)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (currentDate == today || currentDate == today.plusDays(1)) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
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

        // Collapsible mini-month calendar
        AnimatedVisibility(
            visible = miniMonthExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            MiniMonthCalendar(
                selectedDate = selectedDate,
                dayDots = miniCalendarDayDots,
                onDayClick = { date ->
                    val page = DAY_PAGE_RANGE + (date.toEpochDay() - today.toEpochDay()).toInt()
                    scope.launch { pagerState.animateScrollToPage(page) }
                },
                onMonthChanged = viewModel::setMiniCalendarMonth,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // Swipeable day pages — single-column timeline
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            if (page == pagerState.currentPage) {
                val pageDate = remember(page) {
                    today.plusDays((page - DAY_PAGE_RANGE).toLong())
                }
                TimelineGrid(
                    columnCount = 1,
                    columnDates = listOf(pageDate),
                    events = eventsForDay,
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
