package com.ekhonavigator.feature.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventCard
import com.ekhonavigator.core.designsystem.component.sourceAccentColor
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.ScheduleSourceType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Navigational DayScreen — wraps [DayPager] for use as a routed screen
 * (e.g. from month day cell click). Supports system back gesture.
 */
@Composable
fun DayScreen(
    epochDay: Long,
    onEventClick: (String) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val initialDate = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    var snapToTodayTrigger by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { snapToTodayTrigger++ },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = EkhoIcons.CalendarFilled,
                        contentDescription = "Jump to today",
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (viewModel.isSignedIn) {
                    FloatingActionButton(
                        onClick = { onCreateEventClick(selectedDate.toEpochDay()) },
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
        DayPager(
            initialDate = initialDate,
            viewModel = viewModel,
            onEventClick = onEventClick,
            snapToTodayTrigger = snapToTodayTrigger,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        )
    }
}

/**
 * Shared swipeable day pager used by both the Day tab and the routed DayScreen.
 * Swipe left/right to navigate between days.
 */
private const val DAY_PAGE_RANGE = 365

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayPager(
    initialDate: LocalDate,
    viewModel: ScheduleViewModel,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    snapToTodayTrigger: Int = 0,
) {
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val eventsForDay by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val initialOffset = remember(initialDate) {
        (initialDate.toEpochDay() - today.toEpochDay()).toInt()
    }

    val pagerState = rememberPagerState(
        initialPage = DAY_PAGE_RANGE + initialOffset,
        pageCount = { DAY_PAGE_RANGE * 2 + 1 },
    )

    // Update ViewModel when page changes (swipe or initial)
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

    val zone = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

    Column(modifier = modifier.fillMaxSize()) {
        // Day header — updates based on current page
        val currentDate = remember(pagerState.currentPage) {
            today.plusDays((pagerState.currentPage - DAY_PAGE_RANGE).toLong())
        }

        Text(
            text = when (currentDate) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                today.minusDays(1) -> "Yesterday"
                else -> currentDate.format(dayFormatter)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (currentDate == today) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Color-coded source filter row
        ScheduleSourceFilterRow(
            activeTypes = activeSourceTypes,
            onToggle = viewModel::toggleSourceType,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Swipeable day pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            if (page == pagerState.currentPage) {
                DayPageContent(
                    events = eventsForDay,
                    zone = zone,
                    timeFormatter = timeFormatter,
                    onEventClick = onEventClick,
                    onBookmarkClick = viewModel::toggleBookmark,
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun DayPageContent(
    events: List<com.ekhonavigator.core.model.CalendarEvent>,
    zone: ZoneId,
    timeFormatter: DateTimeFormatter,
    onEventClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Free as a dolphin",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(events, key = { it.id }) { event ->
                val startTime = event.startTime.atZone(zone).format(timeFormatter)
                val endTime = event.endTime.atZone(zone).format(timeFormatter)

                EkhoEventCard(
                    title = event.title,
                    timeRange = "$startTime – $endTime",
                    location = event.location,
                    accentColor = sourceAccentColor(event.source.name, event.isBookmarked),
                    isBookmarked = event.isBookmarked,
                    showBookmark = event.source == EventSource.ICAL_FEED,
                    onBookmarkClick = { onBookmarkClick(event.id) },
                    onClick = { onEventClick(event.id) },
                )
            }
        }
    }
}

/**
 * Color-coded multi-select source filter row.
 * Used by both the Month tab and the Day screen.
 *
 * All colors sourced from [MaterialTheme.colorScheme] — no hardcoded hex.
 * Each source type maps to a theme slot:
 * - Schedule → primary (SchoolRed)
 * - Custom → secondary (DolphinCyan)
 * - Campus → surfaceContainerHighest (neutral)
 * - Bookmarked → tertiary (CampusAmber) with bookmark icon
 */
@Composable
fun ScheduleSourceFilterRow(
    activeTypes: Set<ScheduleSourceType>,
    onToggle: (ScheduleSourceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScheduleSourceType.entries.forEach { type ->
            val isActive = type in activeTypes
            val (accentColor, onAccentColor) = sourceTypeThemeColors(type, colors)

            FilterChip(
                selected = isActive,
                onClick = { onToggle(type) },
                leadingIcon = if (isActive && type != ScheduleSourceType.BOOKMARKED) {
                    {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .drawBehind { drawCircle(accentColor) },
                        )
                    }
                } else {
                    null
                },
                label = {
                    if (type == ScheduleSourceType.BOOKMARKED) {
                        androidx.compose.material3.Icon(
                            imageVector = com.ekhonavigator.core.designsystem.icon.EkhoIcons.Bookmark,
                            contentDescription = "Bookmarked",
                            modifier = Modifier.size(16.dp),
                            tint = if (isActive) accentColor else colors.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                },
                modifier = Modifier
                    .then(
                        if (type != ScheduleSourceType.BOOKMARKED) Modifier.weight(1f)
                        else Modifier
                    )
                    .height(32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.12f),
                    selectedLabelColor = accentColor,
                    containerColor = colors.surfaceContainerHigh,
                    labelColor = colors.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isActive,
                    borderColor = Color.Transparent,
                    selectedBorderColor = accentColor.copy(alpha = 0.3f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

/**
 * Maps a [ScheduleSourceType] to its (accent, onAccent) color pair from the theme.
 */
private fun sourceTypeThemeColors(
    type: ScheduleSourceType,
    colors: androidx.compose.material3.ColorScheme,
): Pair<Color, Color> = when (type) {
    ScheduleSourceType.SCHEDULE -> colors.primary to colors.onPrimary
    ScheduleSourceType.CUSTOM -> colors.secondary to colors.onSecondary
    ScheduleSourceType.CAMPUS -> colors.onSurfaceVariant to colors.onSurface
    ScheduleSourceType.BOOKMARKED -> colors.tertiary to colors.onTertiary
}
