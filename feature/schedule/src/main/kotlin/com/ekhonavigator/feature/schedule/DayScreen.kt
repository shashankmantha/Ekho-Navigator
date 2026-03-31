package com.ekhonavigator.feature.schedule

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.ScheduleSourceType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Day detail screen showing events for a specific day.
 * Inherits the active source filter from the month tab.
 * Placeholder for the vertical timeline (Phase 2 of Week timetable work).
 */
@Composable
fun DayScreen(
    epochDay: Long,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val eventsForDay by viewModel.eventsForSelectedDate.collectAsStateWithLifecycle()

    // Set the selected date so the VM queries events for this day
    LaunchedEffect(date) {
        viewModel.selectDate(date)
    }

    val zone = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

    val today = remember { LocalDate.now() }

    Column(modifier = modifier.fillMaxSize()) {
        // Day header
        Text(
            text = when (date) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> date.format(dayFormatter)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (date == today) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Color-coded source filter row (inherited from month, locally overridable)
        ScheduleSourceFilterRow(
            activeTypes = activeSourceTypes,
            onToggle = viewModel::toggleSourceType,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Event list (placeholder — will become vertical timeline)
        if (eventsForDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                items(eventsForDay, key = { it.id }) { event ->
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
                    )
                }
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
