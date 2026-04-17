@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator.feature.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

/**
 * Internal tab indices for the Calendar screen pager.
 */
private enum class CalendarTab(val title: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long, Set<EventSourceType>, Set<EventCategory>) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val tabs = remember { CalendarTab.entries }
    val pagerState = rememberPagerState(
        initialPage = CalendarTab.DAY.ordinal,
        pageCount = { tabs.size },
    )

    val scope = rememberCoroutineScope()
    var daySnapTrigger by remember { mutableIntStateOf(0) }
    var weekSnapTrigger by remember { mutableIntStateOf(0) }
    var monthSnapTrigger by remember { mutableIntStateOf(0) }

    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()
    val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    // Wrap onDayClick to forward current filter state to the DayScreen
    val onDayClickWithFilters: (Long) -> Unit = { epochDay ->
        onDayClick(epochDay, activeSourceTypes, selectedCategories)
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        when (pagerState.currentPage) {
                            CalendarTab.DAY.ordinal -> daySnapTrigger++
                            CalendarTab.WEEK.ordinal -> weekSnapTrigger++
                            CalendarTab.MONTH.ordinal -> monthSnapTrigger++
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

                if (viewModel.isSignedIn) {
                    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                    FloatingActionButton(
                        onClick = {
                            val epochDay = if (pagerState.currentPage == CalendarTab.DAY.ordinal) {
                                selectedDate.toEpochDay()
                            } else {
                                null
                            }
                            onCreateEventClick(epochDay)
                        },
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
        },
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Unified Header Row - Matches ScheduleScreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) // Fixed height for consistency
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp), // Match SearchBar height
                ) {
                    tabs.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabs.size,
                            ),
                            icon = { /* no checkmark */ },
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

                Spacer(modifier = Modifier.width(8.dp))

                // Filter icon — opens bottom sheet
                val allSourcesActive = activeSourceTypes.size == EventSourceType.entries.size
                val hasActiveFilters = selectedCategories.isNotEmpty() || !allSourcesActive
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.size(48.dp) // Fixed touch target size
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

            // Pager content — tap-only tab switching, no swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
            ) { page ->
                when (tabs[page]) {
                    CalendarTab.DAY -> DayTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                        snapToTodayTrigger = daySnapTrigger,
                    )

                    CalendarTab.WEEK -> WeekTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                        onDayClick = onDayClickWithFilters,
                        snapToTodayTrigger = weekSnapTrigger,
                    )

                    CalendarTab.MONTH -> MonthTab(
                        viewModel = viewModel,
                        onDayClick = onDayClickWithFilters,
                        snapToTodayTrigger = monthSnapTrigger,
                    )
                }
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

@Composable
private fun DayTab(
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit,
    snapToTodayTrigger: Int = 0,
) {
    DayTimelineContent(
        viewModel = viewModel,
        onEventClick = onEventClick,
        snapToTodayTrigger = snapToTodayTrigger,
    )
}
