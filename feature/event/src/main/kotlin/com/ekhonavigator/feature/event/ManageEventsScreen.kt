package com.ekhonavigator.feature.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageEventsScreen(
    modifier: Modifier = Modifier,
    viewModel: ManageEventsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()

    var pendingBulkDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Manage events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Bulk delete events you created. Canvas-bridged and campus events are managed elsewhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ManageEventBucket.entries.forEach { bucket ->
                    val selected = bucket in uiState.activeBuckets
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleBucket(bucket) },
                        label = {
                            Text(text = bucket.displayName, style = MaterialTheme.typography.labelSmall)
                        },
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ManageDateRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.dateRange == range,
                        onClick = { viewModel.setDateRange(range) },
                        label = {
                            Text(text = range.displayName, style = MaterialTheme.typography.labelSmall)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // Selection state line — also doubles as the "select all visible" toggle.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.selectedIds.isEmpty()) {
                        "${events.size} matching"
                    } else {
                        "${uiState.selectedIds.size} selected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        if (uiState.selectedIds.isEmpty()) viewModel.selectAllVisible()
                        else viewModel.clearSelection()
                    },
                    enabled = events.isNotEmpty(),
                ) {
                    Text(
                        if (uiState.selectedIds.isEmpty()) "Select all" else "Clear",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(events, key = { it.id }) { event ->
                EventRow(
                    event = event,
                    selected = event.id in uiState.selectedIds,
                    onToggle = { viewModel.toggleSelection(event.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
            if (events.isEmpty()) {
                item {
                    Text(
                        text = "Nothing matches the current filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = viewModel::deleteSelected,
                    enabled = uiState.selectedIds.isNotEmpty() && !uiState.isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Delete selected (${uiState.selectedIds.size})")
                }
                TextButton(
                    onClick = { pendingBulkDelete = true },
                    enabled = events.isNotEmpty() && !uiState.isDeleting,
                ) {
                    Text("Delete all matching", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pendingBulkDelete) {
        AlertDialog(
            onDismissRequest = { pendingBulkDelete = false },
            title = { Text("Delete every matching event?") },
            text = {
                Text(
                    "${events.size} events will be removed. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingBulkDelete = false
                        viewModel.deleteAllFiltered()
                    },
                ) { Text("Delete all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulkDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EventRow(
    event: CalendarEvent,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(),
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title.ifBlank { "(untitled)" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = event.startTime.atZone(zone).format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .height(20.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = bucketLabel(event),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Same logic as ManageEventsViewModel.matchesBucket — kept here so the row
// can display its own classification without round-tripping through the VM.
private fun bucketLabel(event: CalendarEvent): String = when {
    event.recurrence != null -> "Recurring"
    event.type == EventType.ASSIGNMENT -> "Assignment"
    else -> "Event"
}
