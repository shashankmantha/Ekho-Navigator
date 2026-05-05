package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.designsystem.R as DesignR

@Composable
fun NewChatScreen(
    onDirectChatSelected: (FriendUser) -> Unit,
    onGroupDraftCreated: (String, List<FriendUser>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SocialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var friendSearchQuery by remember { mutableStateOf("") }
    val selectedFriendIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            viewModel.loadSocialData()
        }
    }

    val filteredFriends = uiState.friends.filter { friend ->
        val query = friendSearchQuery.trim()

        query.isBlank() ||
                friend.displayName.contains(query, ignoreCase = true) ||
                friend.major.contains(query, ignoreCase = true)
    }

    val selectedFriends = uiState.friends.filter { friend ->
        friend.uid in selectedFriendIds
    }

    val createButtonText = when (selectedFriendIds.size) {
        0 -> "Select a Friend"
        1 -> "Start Chat"
        else -> "Create Group Chat"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "New Chat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (selectedFriendIds.size >= 2) {
            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Group Title (Optional)")
                },
                singleLine = true,
            )
        }

        OutlinedTextField(
            value = friendSearchQuery,
            onValueChange = {
                friendSearchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search friends")
            },
            singleLine = true,
            trailingIcon = {
                if (friendSearchQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            friendSearchQuery = ""
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

        Text(
            text = "${selectedFriendIds.size} selected",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (filteredFriends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No friends found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    items = filteredFriends,
                    key = { friend ->
                        friend.uid
                    },
                ) { friend ->
                    val selected = friend.uid in selectedFriendIds

                    SelectableFriendRow(
                        friend = friend,
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedFriendIds.remove(friend.uid)
                            } else {
                                selectedFriendIds.add(friend.uid)
                            }
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }

        Button(
            onClick = {
                when (selectedFriends.size) {
                    1 -> {
                        onDirectChatSelected(selectedFriends.first())
                    }

                    else -> {
                        onGroupDraftCreated(
                            groupName,
                            selectedFriends,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedFriendIds.isNotEmpty() && !uiState.isLoading,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
            )

            Text(
                text = createButtonText,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SelectableFriendRow(
    friend: FriendUser,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        leadingContent = {
            FriendAvatar(
                avatarId = friend.avatarId,
            )
        },
        headlineContent = {
            Text(
                text = friend.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (friend.major.isNotBlank()) {
                Text(
                    text = friend.major,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = {
                    onClick()
                },
            )
        },
    )
}

@Composable
private fun FriendAvatar(
    avatarId: String,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}