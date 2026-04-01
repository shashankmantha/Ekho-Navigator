package com.ekhonavigator.feature.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ekhonavigator.feature.schedule.component.CategoryFilterButton
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Internal tab indices for the Schedule screen pager.
 */
private enum class ScheduleTab(val title: String) {
    DAY("Day"),
    MONTH("Month"),
    DISCOVER("Discover"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    onEventClick: (String) -> Unit,
    onDayClick: (Long) -> Unit,
    onCreateEventClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val tabs = remember { ScheduleTab.entries }
    val pagerState = rememberPagerState(
        initialPage = ScheduleTab.DAY.ordinal,
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()
    var monthSnapTrigger by remember { mutableStateOf(0) }
    var daySnapTrigger by remember { mutableStateOf(0) }
    var discoverSnapTrigger by remember { mutableStateOf(0) }

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
                            ScheduleTab.DAY.ordinal -> daySnapTrigger++
                            ScheduleTab.MONTH.ordinal -> monthSnapTrigger++
                            ScheduleTab.DISCOVER.ordinal -> discoverSnapTrigger++
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
                            val epochDay = if (pagerState.currentPage == ScheduleTab.DAY.ordinal) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = pagerState.currentPage == tab.ordinal,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                        },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            // Shared filter chip row — always visible, identical across tabs
            val activeSourceTypes by viewModel.activeSourceTypes.collectAsStateWithLifecycle()
            val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScheduleSourceFilterRow(
                    activeTypes = activeSourceTypes,
                    onToggle = viewModel::toggleSourceType,
                    modifier = Modifier.weight(1f),
                )

                CategoryFilterButton(
                    selectedCategories = selectedCategories,
                    onToggleCategory = viewModel::toggleCategory,
                    onClearAll = viewModel::clearCategories,
                )
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
                    ScheduleTab.DAY -> DayTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                        snapToTodayTrigger = daySnapTrigger,
                    )
                    ScheduleTab.MONTH -> MonthTab(
                        viewModel = viewModel,
                        onDayClick = onDayClick,
                        snapToTodayTrigger = monthSnapTrigger,
                    )
                    ScheduleTab.DISCOVER -> DiscoverTab(
                        viewModel = viewModel,
                        onEventClick = onEventClick,
                        snapToTodayTrigger = discoverSnapTrigger,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTab(
    viewModel: ScheduleViewModel,
    onEventClick: (String) -> Unit,
    snapToTodayTrigger: Int = 0,
) {
    DayPager(
        initialDate = LocalDate.now(),
        viewModel = viewModel,
        onEventClick = onEventClick,
        snapToTodayTrigger = snapToTodayTrigger,
    )
}
