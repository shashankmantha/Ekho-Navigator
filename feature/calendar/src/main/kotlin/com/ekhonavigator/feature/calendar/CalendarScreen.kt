package com.ekhonavigator.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventCard
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import androidx.compose.ui.draw.clip
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventSourceFilter
import com.ekhonavigator.feature.calendar.component.CalendarTitle
import com.ekhonavigator.feature.calendar.component.DayContent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import com.ekhonavigator.feature.calendar.component.DaysOfWeekHeader
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onEventClick: (String) -> Unit,
    onCreateEventClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val eventsForSelectedDate by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()
    val eventsForMonth by viewModel.eventsForMonth.collectAsStateWithLifecycle()
    val sourceFilter by viewModel.sourceFilter.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val scope = rememberCoroutineScope()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }

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
    )

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .collect { month -> viewModel.setMonth(month) }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (viewModel.isSignedIn) {
                FloatingActionButton(
                    onClick = onCreateEventClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = EkhoIcons.Add,
                        contentDescription = "Create event",
                    )
                }
            }
        },
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )

        // Source filter row
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventSourceFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == sourceFilter,
                    onClick = { viewModel.setSourceFilter(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                EventSourceFilter.ALL -> "All"
                                EventSourceFilter.CAMPUS -> "Campus"
                                EventSourceFilter.PERSONAL -> "My Events"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                )
            }
        }

        // Reactive category filter
        if (availableCategories.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategories.isEmpty(),
                        onClick = viewModel::clearCategories,
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = null,
                    )
                }
                items(availableCategories) { category ->
                    val isSelected = category in selectedCategories
                    val categoryColor = Color(category.color)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleCategory(category) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                                    .then(
                                        if (isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        else Modifier
                                    )
                            )
                        },
                        label = { Text(category.displayName, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = categoryColor.copy(alpha = 0.15f),
                            selectedLabelColor = categoryColor,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = categoryColor.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(eventsForSelectedDate, key = { it.id }) { event ->
                    val startTime = event.startTime.atZone(zone).format(timeFormatter)
                    val endTime = event.endTime.atZone(zone).format(timeFormatter)

                    EkhoEventCard(
                        title = event.title,
                        timeRange = "$startTime – $endTime",
                        location = event.location,
                        categoryColors = event.categories.map { Color(it.color) },
                        isBookmarked = event.isBookmarked,
                        showBookmark = event.source == EventSource.ICAL_FEED,
                        onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
    }
    } // Scaffold
}
