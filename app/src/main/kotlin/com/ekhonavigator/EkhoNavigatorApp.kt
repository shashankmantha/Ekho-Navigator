@file:OptIn(ExperimentalMaterial3Api::class)

package com.ekhonavigator

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.ekhonavigator.core.designsystem.component.EkhoAppBarIcon
import com.ekhonavigator.core.designsystem.component.EkhoNavigationSuiteScaffold
import com.ekhonavigator.core.designsystem.component.EkhoTopAppBar
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.core.navigation.rememberNavigationState
import com.ekhonavigator.core.navigation.toEntries
import com.ekhonavigator.feature.account.AccountScreen
import com.ekhonavigator.feature.account.SettingsScreen
import com.ekhonavigator.feature.account.navigation.AccountNavKey
import com.ekhonavigator.feature.account.navigation.SettingsNavKey
import com.ekhonavigator.feature.account.navigation.navigateToAccount
import com.ekhonavigator.feature.account.navigation.navigateToSettings
import com.ekhonavigator.feature.calendar.CalendarScreen
import com.ekhonavigator.feature.calendar.DayScreen
import com.ekhonavigator.feature.calendar.navigation.CalendarNavKey
import com.ekhonavigator.feature.calendar.navigation.DayNavKey
import com.ekhonavigator.feature.calendar.navigation.navigateToDay
import com.ekhonavigator.feature.discover.DiscoverScreen
import com.ekhonavigator.feature.discover.DiscoverTab
import com.ekhonavigator.feature.discover.navigation.DiscoverNavKey
import com.ekhonavigator.feature.event.CreateEventScreen
import com.ekhonavigator.feature.event.EventScreen
import com.ekhonavigator.feature.event.InvitesActionIcon
import com.ekhonavigator.feature.event.InvitesScreen
import com.ekhonavigator.feature.event.navigation.CreateEventNavKey
import com.ekhonavigator.feature.event.navigation.EventNavKey
import com.ekhonavigator.feature.event.navigation.InvitesNavKey
import com.ekhonavigator.feature.event.navigation.navigateToCreateEvent
import com.ekhonavigator.feature.event.navigation.navigateToEvent
import com.ekhonavigator.feature.event.navigation.navigateToInvites
import com.ekhonavigator.feature.home.HomeScreen
import com.ekhonavigator.feature.home.navigation.HomeNavKey
import com.ekhonavigator.feature.map.CampusPlacesData
import com.ekhonavigator.feature.map.MapScreen
import com.ekhonavigator.feature.map.navigation.MapNavKey
import com.ekhonavigator.feature.social.ChatOptionsScreen
import com.ekhonavigator.feature.social.ChatScreen
import com.ekhonavigator.feature.social.NewChatScreen
import com.ekhonavigator.feature.social.SocialActionViewModel
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen
import com.ekhonavigator.feature.social.navigation.ChatNavKey
import com.ekhonavigator.feature.social.navigation.ChatOptionsNavKey
import com.ekhonavigator.feature.social.navigation.NewChatNavKey
import com.ekhonavigator.feature.social.navigation.SocialNavKey
import com.ekhonavigator.feature.social.navigation.UserProfileNavKey
import com.ekhonavigator.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun EkhoNavigatorApp(
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    notificationChatRequest: NotificationChatRequest? = null,
    onNotificationChatRequestHandled: () -> Unit = {},
    socialActionViewModel: SocialActionViewModel = hiltViewModel(),
) {
    val hasUnreadMessages by socialActionViewModel.hasUnreadMessages.collectAsStateWithLifecycle()

    val navigationState = rememberNavigationState(
        startKey = HomeNavKey,
        topLevelKeys = TOP_LEVEL_NAV_ITEMS.keys,
    )

    val navigator = Navigator(navigationState)

    LaunchedEffect(notificationChatRequest) {
        val request = notificationChatRequest ?: return@LaunchedEffect

        if (request.isGroup) {
            navigator.navigate(
                ChatNavKey(
                    conversationId = request.conversationId,
                    chatTitle = request.chatTitle,
                    isGroup = true,
                ),
            )
        } else {
            navigator.navigate(
                ChatNavKey(
                    conversationId = request.conversationId,
                    friendUserId = request.friendUserId,
                    friendDisplayName = request.friendDisplayName,
                    friendAvatarId = request.friendAvatarId,
                    chatTitle = request.chatTitle.ifBlank {
                        request.friendDisplayName
                    },
                    isGroup = false,
                ),
            )
        }

        onNotificationChatRequestHandled()
    }

    val currentKey = navigationState.currentKey
    val topLevelDestination = TOP_LEVEL_NAV_ITEMS.entries
        .find { (key, _) ->
            key::class == currentKey::class
        }
        ?.value

    val isDefaultTopLevel = TOP_LEVEL_NAV_ITEMS.containsKey(currentKey)

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    LaunchedEffect(currentKey) {
        topAppBarState.heightOffset = 0f
        topAppBarState.contentOffset = 0f
    }

    EkhoNavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                item(
                    selected = navKey::class == currentKey::class,
                    onClick = {
                        navigator.navigate(navKey)
                    },
                    icon = {
                        val icon = if (navKey::class == currentKey::class) {
                            navItem.selectedIcon
                        } else {
                            navItem.unselectedIcon
                        }

                        BadgedBox(
                            badge = {
                                if (navItem.label == "Social" && hasUnreadMessages) {
                                    Badge(
                                        containerColor = Color.Blue,
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    },
                    label = {
                        Text(navItem.label)
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                val titleRes = topLevelDestination?.titleRes ?: R.string.app_name

                EkhoTopAppBar(
                    titleRes = titleRes,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        if (!isDefaultTopLevel) {
                            EkhoAppBarIcon(
                                icon = EkhoIcons.ArrowBack,
                                contentDescription = "Back",
                                onClick = {
                                    navigator.goBack()
                                },
                            )
                        }
                    },
                    actions = {
                        InvitesActionIcon(
                            onClick = {
                                navigator.navigateToInvites()
                            },
                        )

                        EkhoAppBarIcon(
                            icon = EkhoIcons.AccountCircle,
                            contentDescription = null,
                            onClick = {
                                navigator.navigateToAccount()
                            },
                        )
                    },
                )
            },
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
                                HomeScreen(
                                    onEventClick = navigator::navigateToEvent,
                                )
                            }
                        }

                        is CalendarNavKey -> {
                            NavEntry(key) {
                                CalendarScreen(
                                    onEventClick = navigator::navigateToEvent,
                                    onDayClick = navigator::navigateToDay,
                                    onCreateEventClick = { epochDay ->
                                        navigator.navigateToCreateEvent(epochDay)
                                    },
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
                                    onViewLibraryOnMap = {
                                        navigator.navigateAsDetour(
                                            MapNavKey(
                                                focusPlaceId = CampusPlacesData.BROOME_LIBRARY_ID,
                                            ),
                                        )
                                    },
                                    focusPlaceId = key.focusPlaceId,
                                    initialTab = key.initialTab,
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
                                    onOpenDiscoverForPlace = { placeId ->
                                        navigator.navigateAsTabSwitch(
                                            DiscoverNavKey(
                                                focusPlaceId = placeId,
                                                initialTab = DiscoverTab.EVENTS,
                                            ),
                                        )
                                    },
                                    onShareLocationToChat = { friendId, friendName, location ->
                                        navigator.navigate(
                                            ChatNavKey(
                                                conversationId = null,
                                                friendUserId = friendId,
                                                friendDisplayName = friendName,
                                                friendAvatarId = "",
                                                chatTitle = friendName,
                                                isGroup = false,
                                                sharedLocation = location,
                                            ),
                                        )
                                    },
                                    focusPlaceId = key.focusPlaceId,
                                )
                            }
                        }

                        is SocialNavKey -> {
                            NavEntry(key) {
                                SocialScreen(
                                    onProfileClick = { userId ->
                                        navigator.navigate(
                                            UserProfileNavKey(
                                                userId = userId,
                                            ),
                                        )
                                    },
                                    onMessageClick = { friendUserId, friendDisplayName, friendAvatarId ->
                                        navigator.navigate(
                                            ChatNavKey(
                                                conversationId = null,
                                                friendUserId = friendUserId,
                                                friendDisplayName = friendDisplayName,
                                                friendAvatarId = friendAvatarId,
                                                chatTitle = friendDisplayName,
                                                isGroup = false,
                                            ),
                                        )
                                    },
                                    onConversationClick = { conversation ->
                                        navigator.navigate(
                                            ChatNavKey(
                                                conversationId = conversation.conversationId,
                                                friendUserId = conversation.directFriendUserId,
                                                friendDisplayName = conversation.directFriendDisplayName,
                                                friendAvatarId = conversation.directFriendAvatarId,
                                                chatTitle = conversation.title,
                                                isGroup = conversation.isGroup,
                                                groupParticipantNames = conversation.participantNames,
                                                groupParticipantAvatarIds = conversation.groupParticipantAvatarIds,
                                            ),
                                        )
                                    },
                                    onChatOptionsClick = { conversationId ->
                                        navigator.navigate(
                                            ChatOptionsNavKey(
                                                conversationId = conversationId,
                                            ),
                                        )
                                    },
                                    onNewChatClick = {
                                        navigator.navigate(NewChatNavKey)
                                    },
                                )
                            }
                        }

                        is NewChatNavKey -> {
                            NavEntry(key) {
                                NewChatScreen(
                                    onDirectChatSelected = { friend ->
                                        navigator.goBack()

                                        navigator.navigate(
                                            ChatNavKey(
                                                conversationId = null,
                                                friendUserId = friend.uid,
                                                friendDisplayName = friend.displayName,
                                                friendAvatarId = friend.avatarId,
                                                chatTitle = friend.displayName,
                                                isGroup = false,
                                            ),
                                        )
                                    },
                                    onGroupDraftCreated = { groupTitle, selectedFriends ->
                                        navigator.goBack()

                                        navigator.navigate(
                                            ChatNavKey(
                                                conversationId = null,
                                                friendUserId = "",
                                                friendDisplayName = "",
                                                friendAvatarId = "",
                                                chatTitle = groupTitle.trim(),
                                                isGroup = true,
                                                groupParticipantNames = selectedFriends.associate { friend ->
                                                    friend.uid to friend.displayName
                                                },
                                                groupParticipantAvatarIds = selectedFriends.associate { friend ->
                                                    friend.uid to friend.avatarId
                                                },
                                            ),
                                        )
                                    },
                                )
                            }
                        }

                        is UserProfileNavKey -> {
                            NavEntry(key) {
                                UserProfileScreen(
                                    userId = key.userId,
                                    onBack = navigator::goBack,
                                )
                            }
                        }

                        is ChatOptionsNavKey -> {
                            NavEntry(key) {
                                ChatOptionsScreen(
                                    conversationId = key.conversationId,
                                    onBack = {
                                        navigator.goBack()
                                    },
                                    onParticipantClick = { userId ->
                                        navigator.navigate(
                                            UserProfileNavKey(
                                                userId = userId,
                                            ),
                                        )
                                    },
                                    onLeaveConversation = {
                                        navigator.goBack()
                                        navigator.goBack()
                                    },
                                )
                            }
                        }

                        is ChatNavKey -> {
                            NavEntry(key) {
                                ChatScreen(
                                    conversationId = key.conversationId,
                                    friendUserId = key.friendUserId,
                                    friendDisplayName = key.friendDisplayName,
                                    friendAvatarId = key.friendAvatarId,
                                    chatTitle = key.chatTitle,
                                    isGroup = key.isGroup,
                                    groupParticipantNames = key.groupParticipantNames,
                                    groupParticipantAvatarIds = key.groupParticipantAvatarIds,
                                    sharedLocation = key.sharedLocation,
                                    onNavigateToMap = {
                                        navigator.navigate(MapNavKey())
                                    },
                                    onOpenChatOptions = { conversationId ->
                                        navigator.navigate(
                                            ChatOptionsNavKey(
                                                conversationId = conversationId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }

                        is AccountNavKey -> {
                            NavEntry(key) {
                                AccountScreen(
                                    onSignIn = onSignIn,
                                    onSignOut = onSignOut,
                                    onSettingsClick = navigator::navigateToSettings,
                                )
                            }
                        }

                        is SettingsNavKey -> {
                            NavEntry(key) {
                                SettingsScreen()
                            }
                        }

                        is EventNavKey -> {
                            NavEntry(key) {
                                EventScreen(
                                    eventId = key.id,
                                    onBack = navigator::goBack,
                                    onLocationClick = { placeId ->
                                        navigator.navigateAsDetour(
                                            MapNavKey(
                                                focusPlaceId = placeId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }

                        is InvitesNavKey -> {
                            NavEntry(key) {
                                InvitesScreen(
                                    onEventClick = navigator::navigateToEvent,
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
                onBack = {
                    navigator.goBack()
                },
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