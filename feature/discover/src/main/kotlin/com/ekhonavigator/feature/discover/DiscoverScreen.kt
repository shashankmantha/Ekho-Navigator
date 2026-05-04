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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.designsystem.theme.LocalCanvasConnected
import com.ekhonavigator.core.designsystem.theme.LocalSignedIn
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.feature.canvas.courses.MyCoursesGrid
import com.ekhonavigator.feature.event.component.FilterSheetContent
import com.ekhonavigator.feature.study.StudyScreen
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long, Set<EventSourceType>, Set<EventCategory>) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    onViewLibraryOnMap: () -> Unit = {},
    onCourseClick: (courseId: String) -> Unit = {},
    focusPlaceId: String? = null,
    initialTab: DiscoverTab = DiscoverTab.COURSES,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    var showFilterSheet by remember { mutableStateOf(false) }
    // skipPartiallyExpanded so the sheet uses content's intrinsic height and re-measures
    // when collapsible sections grow — otherwise the partial-expand peek height is locked
    // to the initial composition and later expansions get clipped.
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val availableCourses by viewModel.availableCourses.collectAsStateWithLifecycle()
    val selectedCourseIds by viewModel.selectedCourseIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val focusedPlace by viewModel.focusedPlace.collectAsStateWithLifecycle()

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

                    val signedIn = LocalSignedIn.current
                    // Greyed when signed-out (matches Calendar/Day pattern). Discover
                    // is reachable signed-out so the FAB stays visible as a discovery
                    // hint; the click is suppressed without an ownerUid.
                    FloatingActionButton(
                        onClick = {
                            if (!signedIn) return@FloatingActionButton
                            onCreateEventClick(null)
                        },
                        containerColor = if (signedIn) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (signedIn) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                    ) {
                        Icon(
                            imageVector = EkhoIcons.Add,
                            contentDescription = if (signedIn) "Create event" else "Sign in to create events",
                        )
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
            // Courses tab is gated on Canvas connection — when not connected
            // it disappears entirely (the ConnectCanvas screen is still
            // reachable from Account/Settings as the entry point). Per A3 the
            // tab bar will collapse into a single scrolling Campus layout;
            // for A1 we keep the tab pattern intact and add Courses alongside.
            val canvasConnected = LocalCanvasConnected.current
            val visibleTabs = remember(canvasConnected) {
                if (canvasConnected) DiscoverTab.entries else DiscoverTab.entries - DiscoverTab.COURSES
            }
            // If Canvas disconnects while the user is sitting on the Courses
            // tab, snap them back to Study — otherwise the tab strip drops
            // the Courses pill while the content area still renders the
            // Courses branch (now empty), which reads as "the tab broke."
            LaunchedEffect(canvasConnected) {
                if (!canvasConnected && selectedTab == DiscoverTab.COURSES) {
                    selectedTab = DiscoverTab.STUDY
                }
            }

            DiscoverTabStrip(
                selected = selectedTab,
                tabs = visibleTabs,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            when (selectedTab) {
                DiscoverTab.COURSES -> CoursesTabContent(onCourseClick = onCourseClick)

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
                courses = availableCourses,
                selectedCourseIds = selectedCourseIds,
                onToggleCourse = viewModel::toggleCourse,
                onClearCourses = viewModel::clearCourses,
            )
        }
    }
}

enum class DiscoverTab(val title: String) {
    COURSES("Courses"),
    STUDY("Study"),
    EVENTS("Events"),
}

@Composable
private fun DiscoverTabStrip(
    selected: DiscoverTab,
    tabs: List<DiscoverTab>,
    onSelect: (DiscoverTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = tabs.size,
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
private fun CoursesTabContent(
    onCourseClick: (courseId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "My Courses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        MyCoursesGrid(onCourseClick = onCourseClick)
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