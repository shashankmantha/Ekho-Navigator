package com.ekhonavigator.feature.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect



@Composable


fun SocialScreen(
    onEventClick: (String) -> Unit,
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
                                    onClick = { viewModel.acceptFriendRequest(request.uid) }
                                ) {
                                    Text("Accept")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = { viewModel.denyFriendRequest(request.uid) }
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
                    ListItem(
                        headlineContent = {
                            Text(friend.displayName)
                        },
                        supportingContent = {
                            if (friend.major.isNotBlank()) {
                                Text(friend.major)
                            }
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
                                val hasIncomingRequest = uiState.incomingRequests.any { it.uid == user.id }

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
                                            onClick = { viewModel.sendFriendRequest(user.id) }
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