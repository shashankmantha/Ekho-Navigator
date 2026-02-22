package com.ekhonavigator.feature.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Campus events list screen — shows all synced events with search and
 * category filtering. Unlike CalendarScreen which is a month-view
 * calendar, this is a straight scrollable list of all events.
 */
@Composable
fun EventsScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // ---- Search bar ----
        EventSearchBar(
            query = searchQuery,
            onQueryChange = viewModel::setSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // ---- Category filter chips ----
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = viewModel::setCategory,
            modifier = Modifier.fillMaxWidth(),
        )

        // ---- Event list ----
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank() || selectedCategory != null) {
                        "No matching events"
                    } else {
                        "No events yet"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event.id) },
                        onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                    )
                }
            }
        }
    }
}

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
        placeholder = { Text("Search events\u2026") },
        leadingIcon = {
            Icon(
                imageVector = EkhoIcons.Search,
                contentDescription = "Search",
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = EkhoIcons.Close,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: EventCategory?,
    onCategorySelected: (EventCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
            )
        }

        // One chip per category
        items(EventCategory.entries.toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == category) null else category,
                    )
                },
                label = { Text(category.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(category.color).copy(alpha = 0.15f),
                    selectedLabelColor = Color(category.color),
                ),
            )
        }
    }
}

/**
 * Event card with category color bar, time, location, and bookmark toggle.
 */
@Composable
private fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Category color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
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

                val startZoned = event.startTime.atZone(zone)
                val date = startZoned.format(dateFormatter)
                val startTime = startZoned.format(timeFormatter)
                val endTime = event.endTime.atZone(zone).format(timeFormatter)
                Text(
                    text = "$date \u00B7 $startTime \u2013 $endTime",
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

            // Bookmark toggle
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (event.isBookmarked) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                    contentDescription = if (event.isBookmarked) "Remove bookmark" else "Bookmark",
                    tint = if (event.isBookmarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
