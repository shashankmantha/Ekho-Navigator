@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator.feature.discover

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.feature.event.component.FilterSheetContent
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long, Set<EventSourceType>, Set<EventCategory>) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    initialLocationFilter: String? = null,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(DiscoverTab.EVENTS) }

    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val onDayHeaderClick: (Long) -> Unit = { epochDay ->
        onDayClick(epochDay, activeSourceTypes, selectedCategories)
    }

    LaunchedEffect(initialLocationFilter) {
        initialLocationFilter?.takeIf { it.isNotBlank() }?.let { selectedLocationName ->
            viewModel.setSearchQuery(selectedLocationName)
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (selectedTab == DiscoverTab.EVENTS) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch { listState.animateScrollToItem(0) }
                        },
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
                            onClick = { onCreateEventClick(null) },
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
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            DiscoverTabStrip(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            when (selectedTab) {
                DiscoverTab.EVENTS -> EventsTabContent(
                    viewModel = viewModel,
                    searchQuery = searchQuery,
                    activeSourceTypes = activeSourceTypes,
                    selectedCategories = selectedCategories,
                    onOpenFilters = { showFilterSheet = true },
                    onEventClick = onEventClick,
                    onDayClick = onDayHeaderClick,
                    listState = listState,
                )

                DiscoverTab.ROOMS -> RoomsTabPlaceholder()
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            FilterSheetContent(
                activeSourceTypes = activeSourceTypes,
                selectedCategories = selectedCategories,
                onToggleSourceType = viewModel::toggleSourceType,
                onToggleCategory = viewModel::toggleCategory,
                onClearCategories = viewModel::clearCategories,
            )
        }
    }
}

enum class DiscoverTab(val title: String) {
    EVENTS("Events"),
    ROOMS("Rooms"),
}

@Composable
private fun DiscoverTabStrip(
    selected: DiscoverTab,
    onSelect: (DiscoverTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        DiscoverTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DiscoverTab.entries.size,
                ),
                icon = { },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                label = {
                    Text(tab.title, style = MaterialTheme.typography.labelMedium)
                },
            )
        }
    }
}

@Composable
private fun EventsTabContent(
    viewModel: DiscoverViewModel,
    searchQuery: String,
    activeSourceTypes: Set<EventSourceType>,
    selectedCategories: Set<EventCategory>,
    onOpenFilters: () -> Unit,
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit,
    listState: LazyListState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.weight(1f),
            label = { Text("Search events") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.width(8.dp))

        val allSourcesActive = activeSourceTypes.size == EventSourceType.entries.size
        val hasActiveFilters = selectedCategories.isNotEmpty() || !allSourcesActive
        IconButton(
            onClick = onOpenFilters,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = EkhoIcons.Tune,
                contentDescription = "Filters",
                tint = if (hasActiveFilters) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )

    DiscoverEventsList(
        viewModel = viewModel,
        onEventClick = onEventClick,
        onDayClick = onDayClick,
        listState = listState,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun RoomsTabPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = EkhoIcons.Place,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Study room availability",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

