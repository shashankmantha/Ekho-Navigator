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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton

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
    onConversationClick: (ConversationUiModel) -> Unit,
    onNewChatClick: () -> Unit = {},
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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

        SearchBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

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
                            onConversationClick = onConversationClick,
                            onProfileClick = onProfileClick,
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

        if (selectedTab == SocialTab.Chats && !isSearching) {
            FloatingActionButton(
                onClick = onNewChatClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New chat",
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            label = {
                Text("Search users")
            },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
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
    onConversationClick: (ConversationUiModel) -> Unit,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations = uiState.conversations

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        uiState.errorMessage?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (conversations.isEmpty()) {
            item {
                EmptyStateMessage(
                    text = "No chats yet",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                )
            }
        } else {
            items(
                items = conversations,
                key = { conversation ->
                    "conversation_${conversation.conversationId}"
                },
            ) { conversation ->
                ChatRow(
                    title = conversation.title,
                    avatarId = conversation.avatarId,
                    online = conversation.online,
                    onlineStatus = conversation.onlineStatus,
                    showOnlineStatus = conversation.showOnlineStatus && !conversation.isGroup,
                    lastMessage = conversation.lastMessage,
                    hasUnreadMessages = conversation.hasUnreadMessages,
                    unreadCount = conversation.unreadCount,
                    onChatClick = {
                        onConversationClick(conversation)
                    },
                    onProfileClick = if (!conversation.isGroup && conversation.directFriendUserId.isNotBlank()) {
                        {
                            onProfileClick(conversation.directFriendUserId)
                        }
                    } else {
                        null
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
        uiState.errorMessage?.let { error ->
            item {
                Text(
                    text = error,
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
                key = { request ->
                    "request_${request.uid}"
                },
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
                key = { friend ->
                    "friend_${friend.uid}"
                },
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
                    EmptyStateMessage(
                        text = "No users found",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }

            else -> {
                items(
                    items = uiState.users,
                    key = { user ->
                        "user_${user.id}"
                    },
                ) { user ->
                    SearchUserRow(
                        user = user,
                        uiState = uiState,
                        onProfileClick = onProfileClick,
                        onSendFriendRequest = onSendFriendRequest,
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
private fun SearchUserRow(
    user: com.ekhonavigator.core.data.social.SocialUser,
    uiState: SocialUiState,
    onProfileClick: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
) {
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
            val isFriend = uiState.friends.any { friend ->
                friend.uid == user.id
            }

            val isPending = user.id in uiState.outgoingRequestIds

            val hasIncomingRequest = uiState.incomingRequests.any { request ->
                request.uid == user.id
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
private fun EmptyStateMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatRow(
    title: String,
    avatarId: String,
    online: Boolean,
    onlineStatus: OnlineStatus,
    showOnlineStatus: Boolean,
    lastMessage: String,
    hasUnreadMessages: Boolean,
    unreadCount: Int,
    onChatClick: () -> Unit,
    onProfileClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onChatClick()
            },
        leadingContent = {
            ChatAvatar(
                avatarId = avatarId,
                online = online,
                onlineStatus = onlineStatus,
                showOnlineStatus = showOnlineStatus,
                onClick = onProfileClick,
            )
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
            LastMessagePreview(
                lastMessage = lastMessage,
                hasUnreadMessages = hasUnreadMessages,
            )
        },
        trailingContent = {
            UnreadBadge(
                visible = hasUnreadMessages,
                unreadCount = unreadCount,
            )
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
                ChatAvatar(
                    avatarId = avatarId,
                    online = online,
                    onlineStatus = onlineStatus,
                    showOnlineStatus = showOnlineStatus,
                    onClick = {
                        onViewProfileClick(uid)
                    },
                )
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

                    LastMessagePreview(
                        lastMessage = lastMessage,
                        hasUnreadMessages = hasUnreadMessages,
                    )
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UnreadBadge(
                        visible = hasUnreadMessages,
                        unreadCount = unreadCount,
                    )

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

@Composable
private fun ChatAvatar(
    avatarId: String,
    online: Boolean,
    onlineStatus: OnlineStatus,
    showOnlineStatus: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable {
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ),
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
}

@Composable
private fun LastMessagePreview(
    lastMessage: String,
    hasUnreadMessages: Boolean,
    modifier: Modifier = Modifier,
) {
    if (lastMessage.isBlank()) return

    Row(
        modifier = modifier,
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

@Composable
private fun UnreadBadge(
    visible: Boolean,
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Box(
        modifier = modifier
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