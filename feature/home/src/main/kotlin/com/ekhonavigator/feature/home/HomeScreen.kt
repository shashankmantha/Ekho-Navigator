package com.ekhonavigator.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val allEvents by viewModel.events.collectAsStateWithLifecycle()
    val showAll by viewModel.showAll.collectAsStateWithLifecycle()

    // ---- Date filtering ----
    val zone = remember { ZoneId.of("America/Los_Angeles") }
    val today = remember { LocalDate.now(zone) }
    val tomorrow = remember { today.plusDays(1) }

    // Accumulate days of events until we reach a minimum or exhaust the lookahead
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
            if (total >= 3) break
        }
        result
    }

    val allDayEvents = remember(eventsByDate) { eventsByDate.values.flatten() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dayHeaderFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    val sectionLabel = remember(eventsByDate, today) {
        if (eventsByDate.isEmpty()) {
            "UPCOMING"
        } else if (eventsByDate.size == 1) {
            when (eventsByDate.keys.first()) {
                today -> "TODAY ON CAMPUS"
                tomorrow -> "TOMORROW ON CAMPUS"
                else -> "UPCOMING — ${eventsByDate.keys.first().format(dateFormatter).uppercase()}"
            }
        } else {
            "UPCOMING ON CAMPUS"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        WeatherSection(
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // ---- Header row: label + filter chip ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(sectionLabel)
            FilterChip(
                selected = !showAll,
                onClick = { viewModel.toggleShowAll() },
                label = { Text("Bookmarked") },
                leadingIcon = {
                    Icon(
                        imageVector = if (!showAll) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                        contentDescription = null,
                    )
                },
            )
        }

        // ---- Event list ----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            if (allDayEvents.isEmpty()) {
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
                val showDayHeaders = eventsByDate.size > 1

                Column {
                    eventsByDate.entries.forEachIndexed { dayIndex, (date, dayEvents) ->
                        if (showDayHeaders) {
                            if (dayIndex > 0) {
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = when (date) {
                                    today -> "Today"
                                    tomorrow -> "Tomorrow"
                                    else -> date.format(dayHeaderFormatter)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 4.dp,
                                ),
                            )
                        }
                        dayEvents.forEachIndexed { index, event ->
                            TodayEventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                            )
                            if (dayIndex < eventsByDate.size - 1 || index < dayEvents.lastIndex) {
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun TodayEventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
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
                    maxLines = 2,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = EkhoIcons.Place,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
}
