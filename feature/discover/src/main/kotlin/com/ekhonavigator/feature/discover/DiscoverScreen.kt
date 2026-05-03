@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.feature.event.component.FilterSheetContent
import com.ekhonavigator.feature.study.StudyScreen
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long, Set<EventSourceType>, Set<EventCategory>) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    onViewLibraryOnMap: () -> Unit = {},
    focusPlaceId: String? = null,
    initialTab: DiscoverTab = DiscoverTab.STUDY,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()

    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val focusedPlace by viewModel.focusedPlace.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()

    val onDayHeaderClick: (Long) -> Unit = { epochDay ->
        onDayClick(epochDay, activeSourceTypes, selectedCategories)
    }

    LaunchedEffect(focusPlaceId) {
        viewModel.setFocusedPlaceId(focusPlaceId)
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
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
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

                    if (isSignedIn) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    focusedPlace = focusedPlace,
                    onClearFocusedPlace = { viewModel.setFocusedPlaceId(null) },
                    onOpenFilters = { showFilterSheet = true },
                    onEventClick = onEventClick,
                    onDayClick = onDayHeaderClick,
                    listState = listState,
                )

                DiscoverTab.STUDY -> StudyScreen(
                    onViewLibraryOnMap = onViewLibraryOnMap,
                )
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
    STUDY("Study"),
    EVENTS("Events"),
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
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                    )
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
    focusedPlace: Place?,
    onClearFocusedPlace: () -> Unit,
    onOpenFilters: () -> Unit,
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit,
    listState: LazyListState,
) {
    val showSearch = listState.isScrollingUp()

    AnimatedVisibility(visible = showSearch) {
        Column {
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
        }
    }

    focusedPlace?.let { place ->
        FocusedPlaceChip(
            place = place,
            onClear = onClearFocusedPlace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    DiscoverEventsList(
        viewModel = viewModel,
        onEventClick = onEventClick,
        onDayClick = onDayClick,
        listState = listState,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FocusedPlaceChip(
    place: Place,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(place.name) },
        leadingIcon = {
            Icon(
                imageVector = EkhoIcons.Place,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = EkhoIcons.Close,
                contentDescription = "Clear location filter",
                modifier = Modifier.size(18.dp),
            )
        },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) {
        mutableIntStateOf(firstVisibleItemIndex)
    }

    var previousScrollOffset by remember(this) {
        mutableIntStateOf(firstVisibleItemScrollOffset)
    }

    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}