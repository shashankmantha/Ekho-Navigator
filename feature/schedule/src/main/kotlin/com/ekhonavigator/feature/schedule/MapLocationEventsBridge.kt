package com.ekhonavigator.feature.schedule

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun MapLocationEventsBridge(
    selectedLocationFilter: String?,
    scheduleViewModel: ScheduleViewModel,
    schedulePagerState: PagerState,
    discoverPageIndex: Int
) {
    LaunchedEffect(selectedLocationFilter) {
        // only triggers if the location name is not null or blank
        selectedLocationFilter?.takeIf { it.isNotBlank() }?.let { selectedLocationName ->
            scheduleViewModel.setSearchQuery(selectedLocationName)
            schedulePagerState.scrollToPage(discoverPageIndex)
        }
    }
}