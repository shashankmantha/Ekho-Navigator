package com.ekhonavigator.feature.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventCard
import com.ekhonavigator.core.designsystem.component.EkhoSectionHeader
import com.ekhonavigator.core.designsystem.component.sourceAccentColor
import com.ekhonavigator.core.model.EventSource
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiscoverEventsList(
    viewModel: DiscoverViewModel,
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val events by viewModel.discoverEvents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
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
                state = listState,
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
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onDayClick(date.toEpochDay()) }
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
