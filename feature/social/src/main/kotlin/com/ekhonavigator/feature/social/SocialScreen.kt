package com.ekhonavigator.feature.social

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SocialScreen(
    onProfileClick: (String) -> Unit,
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
                        major = friend.major,
                        showOnlineStatus = friend.showOnlineStatus,
                        online = friend.online,
                        onMessageClick = { uid ->
                            println("Message clicked for user: $uid")
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
    major: String,
    showOnlineStatus: Boolean,
    online: Boolean,
    onMessageClick: (String) -> Unit,
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
                    showMenu = true
                },
            leadingContent = {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    if (showOnlineStatus && online) {
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
                                    .background(Color(0xFF34C759))
                            )
                        }
                    }
                }
            },
            headlineContent = {
                Text(displayName)
            },
            supportingContent = {
                if (major.isNotBlank()) {
                    Text(major)
                }
            },
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Message") },
                onClick = {
                    showMenu = false
                    onMessageClick(uid)
                },
            )

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
