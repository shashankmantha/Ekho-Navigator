@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.designsystem.component.EkhoNavigationBar
import com.ekhonavigator.core.designsystem.component.EkhoNavigationBarItem
import com.ekhonavigator.core.designsystem.component.EkhoTopAppBar
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.core.navigation.rememberNavigationState
import com.ekhonavigator.core.navigation.toEntries
import com.ekhonavigator.feature.calendar.CalendarScreen
import com.ekhonavigator.feature.calendar.navigation.CalendarNavKey
import com.ekhonavigator.feature.map.MapScreen
import com.ekhonavigator.feature.map.navigation.MapNavKey
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.navigation.SocialNavKey
import com.ekhonavigator.feature.events.EventsScreen
import com.ekhonavigator.feature.events.navigation.EventsNavKey
import com.ekhonavigator.feature.event.EventScreen
import com.ekhonavigator.feature.event.navigation.EventNavKey
import com.ekhonavigator.feature.event.navigation.navigateToEvent
import com.ekhonavigator.feature.home.HomeScreen
import com.ekhonavigator.feature.home.navigation.HomeNavKey
import com.ekhonavigator.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun EkhoNavigatorApp() {
    val navigationState = rememberNavigationState(
        startKey = HomeNavKey,
        topLevelKeys = TOP_LEVEL_NAV_ITEMS.keys
    )
    val navigator = Navigator(navigationState)
    
    val currentKey = navigationState.currentKey
    val topLevelDestination = TOP_LEVEL_NAV_ITEMS[currentKey]
    val isTopLevelDestination = topLevelDestination != null

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    // Reset TopAppBar scroll state whenever the destination changes
    LaunchedEffect(currentKey) {
        topAppBarState.heightOffset = 0f
        topAppBarState.contentOffset = 0f
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val titleRes = topLevelDestination?.titleRes ?: R.string.app_name
            EkhoTopAppBar(
                titleRes = titleRes,
                scrollBehavior = scrollBehavior,
                navigationIcon = if (isTopLevelDestination) null else EkhoIcons.ArrowBack,
                navigationIconContentDescription = if (isTopLevelDestination) null else "Back",
                onNavigationClick = {
                    if (!isTopLevelDestination) {
                        navigator.goBack()
                    }
                },
            )
        },
        bottomBar = {
            if (isTopLevelDestination) {
                EkhoBottomBar(
                    onNavigateToNavKey = navigator::navigate,
                    currentTopLevelKey = navigationState.currentTopLevelKey
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            modifier = Modifier.padding(paddingValues),
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith 
                        slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn() togetherWith 
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn() togetherWith 
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            },
            entries = navigationState.toEntries { key ->
                when (key) {
                    is HomeNavKey -> {
                        NavEntry(key) {
                            HomeScreen(onEventClick = navigator::navigateToEvent)
                        }
                    }
                    is CalendarNavKey -> {
                        NavEntry(key) {
                            CalendarScreen(onEventClick = navigator::navigateToEvent)
                        }
                    }
                    is EventsNavKey -> {
                        NavEntry(key) {
                            EventsScreen(onEventClick = navigator::navigateToEvent)
                        }
                    }
                    is MapNavKey -> {
                        NavEntry(key) {
                            MapScreen(onEventClick = navigator::navigateToEvent)
                        }
                    }
                    is SocialNavKey -> {
                        NavEntry(key) {
                            SocialScreen(onEventClick = navigator::navigateToEvent)
                        }
                    }
                    is EventNavKey -> {
                        NavEntry(key) {
                            EventScreen(eventId = key.id)
                        }
                    }
                    else -> {
                        NavEntry(key) {
                            PlaceholderScreen(key.toString())
                        }
                    }
                }
            },
            onBack = { navigator.goBack() }
        )
    }
}

@Composable
private fun EkhoBottomBar(
    onNavigateToNavKey: (NavKey) -> Unit,
    currentTopLevelKey: NavKey,
) {
    EkhoNavigationBar {
        TOP_LEVEL_NAV_ITEMS.forEach { (navKey, item) ->
            val selected = navKey == currentTopLevelKey
            EkhoNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToNavKey(navKey) },
                icon = {
                    Icon(
                        imageVector = item.unselectedIcon,
                        contentDescription = null,
                    )
                },
                selectedIcon = {
                    Icon(
                        imageVector = item.selectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun PlaceholderScreen(featureName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$featureName feature coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
