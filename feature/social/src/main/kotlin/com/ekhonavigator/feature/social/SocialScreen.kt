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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ekhonavigator.core.model.OnlineStatus
import com.ekhonavigator.core.designsystem.R as DesignR

@Composable
fun SocialScreen(
    onProfileClick: (String) -> Unit,
    onMessageClick: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSocialData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Social",
            style = MaterialTheme.typography.headlineMedium,
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search users") },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.errorMessage != null) {
                item {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (uiState.incomingRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Friend Requests",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                items(uiState.incomingRequests, key = { "request_${it.uid}" }) { request ->
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
                                    onClick = { viewModel.acceptFriendRequest(request.uid) },
                                ) {
                                    Text("Accept")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = { viewModel.denyFriendRequest(request.uid) },
                                ) {
                                    Text("Deny")
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            if (uiState.friends.isNotEmpty()) {
                item {
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                items(uiState.friends, key = { "friend_${it.uid}" }) { friend ->
                    FriendRow(
                        uid = friend.uid,
                        displayName = friend.displayName,
                        avatarId = friend.avatarId,
                        major = friend.major,
                        showOnlineStatus = friend.showOnlineStatus,
                        online = friend.online,
                        onlineStatus = friend.onlineStatus,
                        lastMessage = friend.lastMessage,
                        hasUnreadMessages = friend.hasUnreadMessages,
                        unreadCount = friend.unreadCount,
                        onMessageClick = { uid, displayName, avatarId ->
                            onMessageClick(uid, displayName, avatarId)
                        },
                        onViewProfileClick = onProfileClick,
                        onRemoveFriendClick = { uid ->
                            viewModel.removeFriend(uid)
                        },
                    )
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    text = "Find People",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.searchQuery.trim().length < 2 -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Type at least 2 letters to search")
                        }
                    }
                }

                uiState.users.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No users found")
                        }
                    }
                }

                else -> {
                    items(uiState.users, key = { "user_${it.id}" }) { user ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onProfileClick(user.id)
                            },
                            headlineContent = {
                                Text(user.displayName)
                            },
                            supportingContent = {
                                val subtitle = buildString {
                                    if (user.major.isNotBlank()) append(user.major)
                                    if (user.email.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(user.email)
                                    }
                                }

                                if (subtitle.isNotBlank()) {
                                    Text(subtitle)
                                }
                            },
                            trailingContent = {
                                val isFriend = uiState.friends.any { it.uid == user.id }
                                val isPending = user.id in uiState.outgoingRequestIds
                                val hasIncomingRequest =
                                    uiState.incomingRequests.any { it.uid == user.id }

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
                                            onClick = { viewModel.sendFriendRequest(user.id) },
                                        ) {
                                            Text("Add")
                                        }
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    uid: String,
    displayName: String,
    avatarId: String,
    major: String,
    showOnlineStatus: Boolean,
    online: Boolean,
    onlineStatus: OnlineStatus,
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onViewProfileClick(uid) },
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
                        contentScale = ContentScale.Crop
                    )

                    if (showOnlineStatus && online) {
                        val statusColor = when (onlineStatus) {
                            OnlineStatus.ONLINE -> Color(0xFF4CAF50)
                            OnlineStatus.AWAY -> Color(0xFFFFC107)
                            OnlineStatus.BUSY -> Color(0xFFF44336)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                        }
                    }
                }
            },
            headlineContent = {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column {
                    if (major.isNotBlank()) {
                        Text(
                            text = major,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (lastMessage.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasUnreadMessages) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3))
                                )
                            }
                            val fadeBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    if (hasUnreadMessages) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    Color.Transparent
                                ),
                                startX = 0f,
                                endX = 600f
                            )
                            Text(
                                text = lastMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    brush = fadeBrush,
                                    fontWeight = if (hasUnreadMessages) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$unreadCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { onMessageClick(uid, displayName, avatarId) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "Message",
                            tint = if (hasUnreadMessages) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("View Profile") },
                onClick = {
                    showMenu = false
                    onViewProfileClick(uid)
                },
            )

            DropdownMenuItem(
                text = { Text("Remove Friend") },
                onClick = {
                    showMenu = false
                    onRemoveFriendClick(uid)
                },
            )
        }
    }
}
