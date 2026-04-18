package com.ekhonavigator.feature.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventRow
import com.ekhonavigator.core.designsystem.component.EkhoEventRowState
import com.ekhonavigator.core.designsystem.component.EkhoSectionHeader
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedCategories.isNotEmpty()) {
                        "No matching events"
                    } else {
                        "No scheduled events"
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

            val dateHintFormatter = remember {
                DateTimeFormatter.ofPattern("MMM d").withLocale(Locale.US)
            }
            val fullDayFormatter = remember {
                DateTimeFormatter.ofPattern("EEEE").withLocale(Locale.US)
            }
            val today = remember { LocalDate.now() }
            val tomorrow = remember { today.plusDays(1) }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                eventsByDate.forEach { (date, dayEvents) ->
                    stickyHeader(key = date.toString()) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onDayClick(date.toEpochDay()) }
                                .padding(horizontal = 16.dp),
                        )
                    }

                    items(dayEvents, key = { it.id }) { event ->
                        EkhoEventRow(
                            title = event.eventName.ifEmpty { event.title },
                            startTime = event.startTime,
                            endTime = event.endTime,
                            zone = zone,
                            location = event.location,
                            monograms = event.categories.map { it.monogram },
                            state = event.toRowState(),
                            isPending = event.myRsvpStatus == RsvpStatus.PENDING,
                            organization = event.organization,
                            onClick = { onEventClick(event.id) },
                            onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}

internal fun CalendarEvent.toRowState(): EkhoEventRowState = when {
    source != EventSource.ICAL_FEED -> EkhoEventRowState.PERSONAL
    isBookmarked -> EkhoEventRowState.BOOKMARKED
    else -> EkhoEventRowState.NONE
}
