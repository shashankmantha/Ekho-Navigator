package com.ekhonavigator.feature.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventCard
import com.ekhonavigator.core.designsystem.component.EkhoSectionHeader
import com.ekhonavigator.core.designsystem.component.sourceAccentColor
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.ScheduleSourceType
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
import java.time.format.DateTimeFormatter

/**
 * Internal tab indices for the Schedule screen pager.
 */
private enum class ScheduleTab(val title: String) {
    DAY("Day"),
    MONTH("Month"),
    DISCOVER("Discover"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val tabs = remember { ScheduleTab.entries }
    val pagerState = rememberPagerState(
        initialPage = ScheduleTab.DAY.ordinal,
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()
    var snapToTodayTrigger by remember { mutableStateOf(0) }
    var daySnapToTodayTrigger by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (pagerState.currentPage == ScheduleTab.MONTH.ordinal ||
                    pagerState.currentPage == ScheduleTab.DAY.ordinal
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (pagerState.currentPage == ScheduleTab.MONTH.ordinal) {
                                snapToTodayTrigger++
                            } else {
                                daySnapToTodayTrigger++
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = EkhoIcons.CalendarFilled,
                            contentDescription = "Jump to today",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (viewModel.isSignedIn) {
                    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                    FloatingActionButton(
                        onClick = {
                            val epochDay = if (pagerState.currentPage == ScheduleTab.DAY.ordinal) {
                                selectedDate.toEpochDay()
                            } else {
                                null
                            }
                            onCreateEventClick(epochDay)
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = EkhoIcons.Add,
                            contentDescription = "Create event",
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = pagerState.currentPage == tab.ordinal,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                        },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            // Pager content — tap-only tab switching, no swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
            ) { page ->
                when (tabs[page]) {
                    ScheduleTab.DAY -> DayTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                        snapToTodayTrigger = daySnapToTodayTrigger,
                    )
                    ScheduleTab.MONTH -> MonthTab(
                        viewModel = viewModel,
                        onDayClick = onDayClick,
                        snapToTodayTrigger = snapToTodayTrigger,
                    )
                    ScheduleTab.DISCOVER -> DiscoverTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════
// Day Tab — swipeable day view with events
// ══════════════════════════════════════════════════

@Composable
private fun DayTab(
    viewModel: ScheduleViewModel,
    onEventClick: (String) -> Unit,
    snapToTodayTrigger: Int = 0,
) {
    DayPager(
        initialDate = LocalDate.now(),
        viewModel = viewModel,
        onEventClick = onEventClick,
        snapToTodayTrigger = snapToTodayTrigger,
    )
}

// ══════════════════════════════════════════════════
// Month Tab — full-screen grid + color-coded filters
// ══════════════════════════════════════════════════

@Composable
private fun MonthTab(
    viewModel: ScheduleViewModel,
    onDayClick: (Long) -> Unit,
    snapToTodayTrigger: Int = 0,
) {
    val eventsForMonth by viewModel.eventsForMonth.collectAsStateWithLifecycle()
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

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
            // Source filter chips + category filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScheduleSourceFilterRow(
                    activeTypes = activeSourceTypes,
                    onToggle = viewModel::toggleSourceType,
                    modifier = Modifier.weight(1f),
                )

                // Category filter inline
                CategoryFilterButton(
                    selectedCategories = selectedCategories,
                    onToggleCategory = viewModel::toggleCategory,
                    onClearAll = viewModel::clearCategories,
                )
            }

            // Pinned day-of-week header (doesn't scroll)
            DaysOfWeekHeader(daysOfWeek = daysOfWeek)

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

// ══════════════════════════════════════════════════
// Discover Tab — event list with search/filters
// ══════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverTab(
    viewModel: ScheduleViewModel,
    onEventClick: (String) -> Unit,
) {
    val events by viewModel.discoverEvents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Search bar
        EventSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::setSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Source filter chips + category filter (shared with Month tab)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScheduleSourceFilterRow(
                activeTypes = activeSourceTypes,
                onToggle = viewModel::toggleSourceType,
                modifier = Modifier.weight(1f),
            )

            CategoryFilterButton(
                selectedCategories = selectedCategories,
                onToggleCategory = viewModel::toggleCategory,
                onClearAll = viewModel::clearCategories,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Event list
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedCategories.isNotEmpty()) {
                        "No matching pulses"
                    } else {
                        "Campus is quiet..."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val zone = remember { ZoneId.systemDefault() }
            val eventsByDate = remember(events) {
                events.groupBy { it.startTime.atZone(zone).toLocalDate() }
                    .toSortedMap()
            }

            val dayHeaderFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
            val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
            val today = remember { LocalDate.now() }
            val tomorrow = remember { today.plusDays(1) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                eventsByDate.forEach { (date, dayEvents) ->
                    stickyHeader(key = date.toString()) {
                        val headerLabel = when (date) {
                            today -> "Today"
                            tomorrow -> "Tomorrow"
                            else -> date.format(dayHeaderFormatter)
                        }
                        val isImportant = date == today || date == tomorrow

                        EkhoSectionHeader(
                            title = headerLabel,
                            isImportant = isImportant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp),
                        )
                    }

                    items(dayEvents, key = { it.id }) { event ->
                        val startTime = event.startTime.atZone(zone).format(timeFormatter)
                        val endTime = event.endTime.atZone(zone).format(timeFormatter)

                        EkhoEventCard(
                            title = event.title,
                            timeRange = "$startTime – $endTime",
                            location = event.location,
                            accentColor = sourceAccentColor(event.source.name, event.isBookmarked),
                            isBookmarked = event.isBookmarked,
                            showBookmark = event.source == EventSource.ICAL_FEED,
                            onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                            onClick = { onEventClick(event.id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════
// Discover tab filter composables
// ══════════════════════════════════════════════════

@Composable
private fun EventSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                "Search...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = EkhoIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}


// ══════════════════════════════════════════════════
// Category filter dropdown (Month tab — bottom end)
// ══════════════════════════════════════════════════

@Composable
private fun CategoryFilterButton(
    selectedCategories: Set<EventCategory>,
    onToggleCategory: (EventCategory) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasActiveFilter = selectedCategories.isNotEmpty()
    val colors = MaterialTheme.colorScheme

    Box(modifier = modifier) {
        FilterChip(
            selected = hasActiveFilter,
            onClick = { expanded = !expanded },
            label = {
                Icon(
                    imageVector = EkhoIcons.Grid3x3,
                    contentDescription = "Filter categories",
                    modifier = Modifier.size(16.dp),
                    tint = if (hasActiveFilter) colors.primary else colors.onSurfaceVariant,
                )
            },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.surfaceContainerHigh,
                containerColor = colors.surfaceContainerHigh,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = hasActiveFilter,
                borderColor = Color.Transparent,
                selectedBorderColor = colors.primary.copy(alpha = 0.3f),
                borderWidth = 1.dp,
                selectedBorderWidth = 1.dp,
            ),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surfaceContainerHigh),
        ) {
            // "All" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "All Categories",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!hasActiveFilter) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onClearAll()
                    expanded = false
                },
                trailingIcon = {
                    if (!hasActiveFilter) {
                        Icon(
                            imageVector = EkhoIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.primary,
                        )
                    }
                },
            )

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 2.dp),
            )

            EventCategory.entries.forEach { category ->
                val isSelected = category in selectedCategories

                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        )
                    },
                    onClick = { onToggleCategory(category) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = EkhoIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}
