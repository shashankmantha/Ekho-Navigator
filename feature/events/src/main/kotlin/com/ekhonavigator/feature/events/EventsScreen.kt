package com.ekhonavigator.feature.events

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoEventCard
import com.ekhonavigator.core.designsystem.component.EkhoSectionHeader
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventsScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ---- Search and Filter Section ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            EventSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            CategoryFilterRow(
                selectedCategories = selectedCategories,
                onToggleCategory = viewModel::toggleCategory,
                onClearAll = viewModel::clearCategories,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- Event list ----
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
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .padding(horizontal = 16.dp)
                        )
                    }

                    items(dayEvents, key = { it.id }) { event ->
                        val startTime = event.startTime.atZone(zone).format(timeFormatter)
                        val endTime = event.endTime.atZone(zone).format(timeFormatter)
                        
                        EkhoEventCard(
                            title = event.title,
                            timeRange = "$startTime – $endTime",
                            location = event.location,
                            categoryColors = event.categories.map { Color(it.color) },
                            isBookmarked = event.isBookmarked,
                            onBookmarkClick = { viewModel.toggleBookmark(event.id) },
                            onClick = { onEventClick(event.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
        placeholder = { 
            Text(
                "Search...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ) 
        },
        leadingIcon = {
            Icon(
                imageVector = EkhoIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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

@Composable
private fun CategoryFilterRow(
    selectedCategories: Set<EventCategory>,
    onToggleCategory: (EventCategory) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategories.isEmpty(),
                onClick = onClearAll,
                label = { 
                    Text("All", style = MaterialTheme.typography.labelSmall) 
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null
            )
        }

        items(EventCategory.entries.toList()) { category ->
            val isSelected = category in selectedCategories
            val categoryColor = Color(category.color)
            
            FilterChip(
                selected = isSelected,
                onClick = { onToggleCategory(category) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                } else Modifier
                            )
                    )
                },
                label = { 
                    Text(
                        text = category.displayName, 
                        style = MaterialTheme.typography.labelSmall
                    ) 
                },
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
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}
