@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.ekhonavigator.core.designsystem.component.EkhoNavigationBar
import com.ekhonavigator.core.designsystem.component.EkhoNavigationBarItem
import com.ekhonavigator.core.designsystem.component.EkhoTopAppBar
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.core.navigation.rememberNavigationState
import com.ekhonavigator.core.navigation.toEntries
import com.ekhonavigator.feature.account.AccountScreen
import com.ekhonavigator.feature.account.navigation.AccountNavKey
import com.ekhonavigator.feature.account.navigation.navigateToAccount
import com.ekhonavigator.feature.calendar.CalendarScreen
import com.ekhonavigator.feature.calendar.DayScreen
import com.ekhonavigator.feature.calendar.navigation.CalendarNavKey
import com.ekhonavigator.feature.calendar.navigation.DayNavKey
import com.ekhonavigator.feature.calendar.navigation.navigateToDay
import com.ekhonavigator.feature.discover.DiscoverScreen
import com.ekhonavigator.feature.discover.navigation.DiscoverNavKey
import com.ekhonavigator.feature.event.CreateEventScreen
import com.ekhonavigator.feature.event.EventScreen
import com.ekhonavigator.feature.event.navigation.CreateEventNavKey
import com.ekhonavigator.feature.event.navigation.EventNavKey
import com.ekhonavigator.feature.event.navigation.navigateToCreateEvent
import com.ekhonavigator.feature.event.navigation.navigateToEvent
import com.ekhonavigator.feature.home.HomeScreen
import com.ekhonavigator.feature.home.navigation.HomeNavKey
import com.ekhonavigator.feature.map.MapScreen
import com.ekhonavigator.feature.map.navigation.MapNavKey
import com.ekhonavigator.feature.social.ChatScreen
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen
import com.ekhonavigator.feature.social.navigation.ChatNavKey
import com.ekhonavigator.feature.social.navigation.SocialNavKey
import com.ekhonavigator.feature.social.navigation.UserProfileNavKey
import com.ekhonavigator.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun EkhoNavigatorApp(
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val navigationState = rememberNavigationState(
        startKey = HomeNavKey,
        topLevelKeys = TOP_LEVEL_NAV_ITEMS.keys
    )
    val navigator = Navigator(navigationState)

    val currentKey = navigationState.currentKey
    val topLevelDestination =
        TOP_LEVEL_NAV_ITEMS.entries.find { (key, _) -> key::class == currentKey::class }?.value
    val isTopLevelDestination = topLevelDestination != null
    val isPureDestination = TOP_LEVEL_NAV_ITEMS.containsKey(currentKey)

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
                navigationIcon = if (isPureDestination) null else EkhoIcons.ArrowBack,
                actionIcon = EkhoIcons.AccountCircle,
                navigationIconContentDescription = if (isPureDestination) null else "Back",
                onNavigationClick = {
                    if (!isPureDestination) {
                        navigator.goBack()
                    }
                },
                onActionClick = {
                    navigator.navigateToAccount()
                }
            )
        },
        bottomBar = {
            if (isTopLevelDestination) {
                EkhoBottomBar(
                    onNavigateToNavKey = navigator::navigate,
                    // used currentKey so the bar knows exactly which screen is active
                    currentTopLevelKey = currentKey
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
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
                            CalendarScreen(
                                onEventClick = navigator::navigateToEvent,
                                onDayClick = navigator::navigateToDay,
                                onCreateEventClick = { epochDay ->
                                    navigator.navigateToCreateEvent(epochDay)
                                }
                            )
                        }
                    }

                    is DiscoverNavKey -> {
                        NavEntry(key) {
                            DiscoverScreen(
                                onEventClick = navigator::navigateToEvent,
                                onDayClick = navigator::navigateToDay,
                                onCreateEventClick = { epochDay ->
                                    navigator.navigateToCreateEvent(epochDay)
                                },
                                initialLocationFilter = key.initialLocationFilter
                            )
                        }
                    }

                    is DayNavKey -> {
                        NavEntry(key) {
                            DayScreen(
                                epochDay = key.epochDay,
                                onEventClick = navigator::navigateToEvent,
                                onCreateEventClick = { epochDay ->
                                    navigator.navigateToCreateEvent(epochDay)
                                },
                                sourceTypeNames = key.sourceTypes,
                                categoryNames = key.categories,
                            )
                        }
                    }

                    is CreateEventNavKey -> {
                        NavEntry(key) {
                            CreateEventScreen(
                                onBack = navigator::goBack,
                                initialEpochDay = key.initialEpochDay,
                            )
                        }
                    }

                    is MapNavKey -> {
                        NavEntry(key) {
                            MapScreen(
                                onEventClick = navigator::navigateToEvent,
                                onOpenDiscoverForLocation = { selectedCampusPlaceName ->
                                    navigator.navigate(DiscoverNavKey(initialLocationFilter = selectedCampusPlaceName))
                                },
                                onShareLocationToChat = { friendId, friendName, location ->
                                    navigator.navigate(
                                        ChatNavKey(
                                            friendUserId = friendId,
                                            friendDisplayName = friendName,
                                            friendAvatarId = "",
                                            sharedLocation = location
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is SocialNavKey -> {
                        NavEntry(key) {
                            SocialScreen(
                                onProfileClick = { userId ->
                                    navigator.navigate(UserProfileNavKey(userId))
                                },
                                onMessageClick = { friendUserId, friendDisplayName, friendAvatarId ->
                                    navigator.navigate(
                                        ChatNavKey(
                                            friendUserId = friendUserId,
                                            friendDisplayName = friendDisplayName,
                                            friendAvatarId = friendAvatarId,
                                        )
                                    )
                                },
                            )
                        }
                    }

                    is UserProfileNavKey -> {
                        NavEntry(key) {
                            UserProfileScreen(
                                userId = key.userId,
                            )
                        }
                    }

                    is ChatNavKey -> {
                        NavEntry(key) {
                            ChatScreen(
                                friendUserId = key.friendUserId,
                                friendDisplayName = key.friendDisplayName,
                                friendAvatarId = key.friendAvatarId,
                                sharedLocation = key.sharedLocation
                            )
                        }
                    }

                    is AccountNavKey -> {
                        NavEntry(key) {
                            AccountScreen(
                                onSignIn = onSignIn,
                                onSignOut = onSignOut,
                            )
                        }
                    }

                    is EventNavKey -> {
                        NavEntry(key) {
                            EventScreen(
                                eventId = key.id,
                                // this is here because we have custom event deletion
                                // so the standard back button is not enough here
                                // normally the top nav bar handles all back functionality
                                onBack = navigator::goBack,
                            )
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
            val selected =
                navKey::class == currentTopLevelKey::class     // matched by class so the icon highlights for any version of the tab
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