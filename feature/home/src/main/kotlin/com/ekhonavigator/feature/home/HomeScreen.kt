package com.ekhonavigator.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventRow
import com.ekhonavigator.core.designsystem.component.EkhoEventRowState
import com.ekhonavigator.core.designsystem.component.EkhoSectionHeader
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val allEvents by viewModel.events.collectAsStateWithLifecycle()
    val showAll by viewModel.showAll.collectAsStateWithLifecycle()

    val zone = remember { ZoneId.of("America/Los_Angeles") }
    val today = remember { LocalDate.now(zone) }
    val tomorrow = remember { today.plusDays(1) }

    val eventsByDate = remember(allEvents, today) {
        val now = Instant.now()
        val cutoff = today.plusDays(7)

        val byDate = allEvents
            .filter { it.endTime.isAfter(now) }
            .filter { !it.startTime.atZone(zone).toLocalDate().isBefore(today) }
            .groupBy { it.startTime.atZone(zone).toLocalDate() }
            .toSortedMap()

        val result = linkedMapOf<LocalDate, List<CalendarEvent>>()
        var total = 0
        for ((date, events) in byDate) {
            if (date.isAfter(cutoff)) break
            result[date] = events
            total += events.size
            if (total >= 4) break
        }
        result
    }

    val dateHintFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d").withLocale(Locale.US)
    }
    val fullDayFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE").withLocale(Locale.US)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 24.dp),
    ) {
        WeatherSection(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
            )

            FilterChip(
                selected = !showAll,
                onClick = { viewModel.toggleShowAll() },
                label = { Text("Bookmarked") },
                leadingIcon = {
                    Icon(
                        imageVector = if (!showAll) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.tertiary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !showAll,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.tertiary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        if (eventsByDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (!showAll) "No bookmarked events" else "No upcoming events",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            eventsByDate.forEach { (date, dayEvents) ->
                val label = when (date) {
                    today -> "Today"
                    tomorrow -> "Tomorrow"
                    else -> date.format(fullDayFormatter)
                }
                val hint = date.format(dateHintFormatter).uppercase(Locale.US)
                val isImportant = date == today || date == tomorrow

                EkhoSectionHeader(
                    title = label,
                    dateHint = hint,
                    count = dayEvents.size,
                    isImportant = isImportant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                dayEvents.forEach { event ->
                    EkhoEventRow(
                        title = event.title,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        zone = zone,
                        location = event.location,
                        monograms = event.categories.map { it.monogram },
                        state = event.toRowState(),
                        isPending = event.myRsvpStatus == RsvpStatus.PENDING,
                        onClick = { onEventClick(event.id) },
                        onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun CalendarEvent.toRowState(): EkhoEventRowState = when {
    source != EventSource.ICAL_FEED -> EkhoEventRowState.PERSONAL
    isBookmarked -> EkhoEventRowState.BOOKMARKED
    else -> EkhoEventRowState.NONE
}
