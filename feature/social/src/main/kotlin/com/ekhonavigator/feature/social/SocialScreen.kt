package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ekhonavigator.core.designsystem.R as DesignR
import com.ekhonavigator.core.model.OnlineStatus
import com.ekhonavigator.feature.account.AccountScreen

private enum class SocialTab(
    val label: String,
) {
    Chats("Chats"),
    Friends("Friends"),
}

@Composable
fun SocialScreen(
    onProfileClick: (String) -> Unit,
    onMessageClick: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = SocialTab.entries
    val selectedTab = tabs[selectedTabIndex]
    val isSearching = uiState.searchQuery.trim().length >= 2

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            viewModel.loadSocialData()
        }
    }

    if (!uiState.isSignedIn) {
        AccountScreen(
            onSignIn = {
                viewModel.loadSocialData()
            },
            modifier = modifier,
            forceSignedOutUi = true,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        SocialTabStrip(
            selected = selectedTab,
            onSelect = { tab ->
                selectedTabIndex = tabs.indexOf(tab)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.weight(1f),
                label = { Text("Search users") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onSearchQueryChange("")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )

        if (isSearching) {
            SearchPeopleTab(
                uiState = uiState,
                onProfileClick = onProfileClick,
                onSendFriendRequest = viewModel::sendFriendRequest,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            when (selectedTab) {
                SocialTab.Chats -> {
                    ChatsTab(
                        uiState = uiState,
                        onMessageClick = onMessageClick,
                        onProfileClick = onProfileClick,
                        onRemoveFriendClick = viewModel::removeFriend,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                SocialTab.Friends -> {
                    FriendsTab(
                        uiState = uiState,
                        onProfileClick = onProfileClick,
                        onMessageClick = onMessageClick,
                        onAcceptFriendRequest = viewModel::acceptFriendRequest,
                        onDenyFriendRequest = viewModel::denyFriendRequest,
                        onRemoveFriend = viewModel::removeFriend,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialTabStrip(
    selected: SocialTab,
    onSelect: (SocialTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        SocialTab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = {
                    onSelect(tab)
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = SocialTab.entries.size,
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
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun ChatsTab(
    uiState: SocialUiState,
    onMessageClick: (String, String, String) -> Unit,
    onProfileClick: (String) -> Unit,
    onRemoveFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chatFriends = uiState.friends
        .filter { friend ->
            friend.lastMessage.isNotBlank() || friend.hasUnreadMessages
        }
        .sortedWith(
            compareByDescending<com.ekhonavigator.core.data.social.FriendUser> {
                it.hasUnreadMessages
            }.thenByDescending {
                it.lastMessageTimestamp
            },
        )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (chatFriends.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No chats yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(
                items = chatFriends,
                key = { "chat_${it.uid}" },
            ) { friend ->
                ChatRow(
                    chatId = friend.uid,
                    title = friend.displayName,
                    avatarId = friend.avatarId,
                    online = friend.online,
                    onlineStatus = friend.onlineStatus,
                    showOnlineStatus = friend.showOnlineStatus,
                    lastMessage = friend.lastMessage,
                    hasUnreadMessages = friend.hasUnreadMessages,
                    unreadCount = friend.unreadCount,
                    onChatClick = {
                        onMessageClick(friend.uid, friend.displayName, friend.avatarId)
                    },
                    onProfileClick = {
                        onProfileClick(friend.uid)
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun FriendsTab(
    uiState: SocialUiState,
    onProfileClick: (String) -> Unit,
    onMessageClick: (String, String, String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onDenyFriendRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (uiState.incomingRequests.isNotEmpty()) {
            item {
                SectionTitle("Friend Requests")
            }

            items(
                items = uiState.incomingRequests,
                key = { "request_${it.uid}" },
            ) { request ->
                ListItem(
                    modifier = Modifier.clickable {
                        onProfileClick(request.uid)
                    },
                    headlineContent = {
                        Text(request.displayName)
                    },
                    supportingContent = {
                        if (request.major.isNotBlank()) {
                            Text(request.major)
                        }
                    },
                    trailingContent = {
                        Row {
                            TextButton(
                                onClick = {
                                    onAcceptFriendRequest(request.uid)
                                },
                            ) {
                                Text("Accept")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    onDenyFriendRequest(request.uid)
                                },
                            ) {
                                Text("Deny")
                            }
                        }
                    },
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }

        item {
            SectionTitle("Friends")
        }

        if (uiState.friends.isEmpty()) {
            item {
                Text(
                    text = "No friends yet",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = uiState.friends,
                key = { "friend_${it.uid}" },
            ) { friend ->
                FriendRow(
                    uid = friend.uid,
                    displayName = friend.displayName,
                    avatarId = friend.avatarId,
                    major = friend.major,
                    online = friend.online,
                    onlineStatus = friend.onlineStatus,
                    showOnlineStatus = friend.showOnlineStatus,
                    lastMessage = friend.lastMessage,
                    hasUnreadMessages = friend.hasUnreadMessages,
                    unreadCount = friend.unreadCount,
                    onMessageClick = onMessageClick,
                    onViewProfileClick = onProfileClick,
                    onRemoveFriendClick = onRemoveFriend,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
private fun SearchPeopleTab(
    uiState: SocialUiState,
    onProfileClick: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            SectionTitle("Search Results")
        }

        when {
            uiState.errorMessage != null -> {
                item {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            uiState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            uiState.users.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No users found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                items(
                    items = uiState.users,
                    key = { "user_${it.id}" },
                ) { user ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onProfileClick(user.id)
                        },
                        headlineContent = {
                            Text(user.displayName)
                        },
                        supportingContent = {
                            val subtitle = buildString {
                                if (user.major.isNotBlank()) {
                                    append(user.major)
                                }

                                if (user.email.isNotBlank()) {
                                    if (isNotEmpty()) {
                                        append(" • ")
                                    }

                                    append(user.email)
                                }
                            }

                            if (subtitle.isNotBlank()) {
                                Text(subtitle)
                            }
                        },
                        trailingContent = {
                            val isFriend = uiState.friends.any {
                                it.uid == user.id
                            }

                            val isPending = user.id in uiState.outgoingRequestIds

                            val hasIncomingRequest = uiState.incomingRequests.any {
                                it.uid == user.id
                            }

                            when {
                                isFriend -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                    ) {
                                        Text("Friended")
                                    }
                                }

                                isPending -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                    ) {
                                        Text("Pending")
                                    }
                                }

                                hasIncomingRequest -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                    ) {
                                        Text("Requested You")
                                    }
                                }

                                else -> {
                                    Button(
                                        onClick = {
                                            onSendFriendRequest(user.id)
                                        },
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 8.dp,
        ),
    )
}
@Composable
private fun ChatRow(
    chatId: String,
    title: String,
    avatarId: String,
    online: Boolean,
    onlineStatus: OnlineStatus,
    showOnlineStatus: Boolean,
    lastMessage: String,
    hasUnreadMessages: Boolean,
    unreadCount: Int,
    onChatClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onChatClick()
            },
        leadingContent = {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            onProfileClick()
                        },
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    val resId = when (avatarId) {
                        "avatar_dolphin" -> DesignR.drawable.avatar_dolphin
                        "avatar_whale" -> DesignR.drawable.avatar_whale
                        "avatar_turtle" -> DesignR.drawable.avatar_turtle
                        else -> DesignR.drawable.avatar_default
                    }

                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }

                if (online && showOnlineStatus) {
                    val statusColor = when (onlineStatus) {
                        OnlineStatus.ONLINE -> Color(0xFF4CAF50)
                        OnlineStatus.AWAY -> Color(0xFFFFC107)
                        OnlineStatus.BUSY -> Color(0xFFF44336)
                    }

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (hasUnreadMessages) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
            )
        },
        supportingContent = {
            if (lastMessage.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3)),
                        )
                    }

                    val fadeBrush = Brush.horizontalGradient(
                        colors = listOf(
                            if (hasUnreadMessages) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            Color.Transparent,
                        ),
                        startX = 0f,
                        endX = 600f,
                    )

                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            brush = fadeBrush,
                            fontWeight = if (hasUnreadMessages) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        },
        trailingContent = {
            if (hasUnreadMessages) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$unreadCount",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    )
}
@Composable
private fun FriendRow(
    uid: String,
    displayName: String,
    avatarId: String,
    major: String,
    online: Boolean,
    onlineStatus: OnlineStatus,
    showOnlineStatus: Boolean,
    lastMessage: String,
    hasUnreadMessages: Boolean,
    unreadCount: Int,
    onMessageClick: (String, String, String) -> Unit,
    onViewProfileClick: (String) -> Unit,
    onRemoveFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onMessageClick(uid, displayName, avatarId)
                },
            leadingContent = {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                onViewProfileClick(uid)
                            },
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        val resId = when (avatarId) {
                            "avatar_dolphin" -> DesignR.drawable.avatar_dolphin
                            "avatar_whale" -> DesignR.drawable.avatar_whale
                            "avatar_turtle" -> DesignR.drawable.avatar_turtle
                            else -> DesignR.drawable.avatar_default
                        }

                        Image(
                            painter = painterResource(resId),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    if (online && showOnlineStatus) {
                        val statusColor = when (onlineStatus) {
                            OnlineStatus.ONLINE -> Color(0xFF4CAF50)
                            OnlineStatus.AWAY -> Color(0xFFFFC107)
                            OnlineStatus.BUSY -> Color(0xFFF44336)
                        }

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(statusColor),
                            )
                        }
                    }
                }
            },
            headlineContent = {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column {
                    if (major.isNotBlank()) {
                        Text(
                            text = major,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (lastMessage.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasUnreadMessages) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3)),
                                )
                            }

                            val fadeBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    if (hasUnreadMessages) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    Color.Transparent,
                                ),
                                startX = 0f,
                                endX = 600f,
                            )

                            Text(
                                text = lastMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    brush = fadeBrush,
                                    fontWeight = if (hasUnreadMessages) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+$unreadCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onMessageClick(uid, displayName, avatarId)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "Message",
                            tint = if (hasUnreadMessages) {
                                Color(0xFF2196F3)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            },
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = {
                showMenu = false
            },
        ) {
            DropdownMenuItem(
                text = {
                    Text("View Profile")
                },
                onClick = {
                    showMenu = false
                    onViewProfileClick(uid)
                },
            )

            DropdownMenuItem(
                text = {
                    Text("Remove Friend")
                },
                onClick = {
                    showMenu = false
                    onRemoveFriendClick(uid)
                },
            )
        }
    }
}