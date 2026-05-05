package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ekhonavigator.core.data.social.ChatConversation
import com.ekhonavigator.core.data.social.ChatMessage
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.designsystem.R as DesignR

data class ChatOptionsFocusTarget(
    val conversationId: String,
    val messageId: String,
    val chatTitle: String,
    val isGroup: Boolean,
    val friendUserId: String = "",
    val friendDisplayName: String = "",
    val friendAvatarId: String = "",
    val groupParticipantNames: Map<String, String> = emptyMap(),
    val groupParticipantAvatarIds: Map<String, String> = emptyMap(),
)

@Composable
fun ChatOptionsScreen(
    conversationId: String,
    onBack: () -> Unit,
    onParticipantClick: (String) -> Unit,
    onLeaveConversation: () -> Unit,
    onFocusedMessageRequested: (ChatOptionsFocusTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatOptionsViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val socialUiState by socialViewModel.uiState.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.start(conversationId)
    }

    LaunchedEffect(socialUiState.isSignedIn) {
        if (socialUiState.isSignedIn) {
            socialViewModel.loadSocialData()
        }
    }

    LaunchedEffect(uiState.hasLeftConversation) {
        if (uiState.hasLeftConversation) {
            onLeaveConversation()
        }
    }

    val conversation = uiState.conversation
    val currentUserId = viewModel.getCurrentUserId()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            conversation == null -> {
                Text(
                    text = uiState.errorMessage ?: "Conversation unavailable",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                ChatOptionsContent(
                    conversation = conversation,
                    currentUserId = currentUserId,
                    isMuted = uiState.isMuted,
                    searchQuery = uiState.searchQuery,
                    searchResults = uiState.searchResults,
                    errorMessage = uiState.errorMessage,
                    infoMessage = uiState.infoMessage,
                    friends = socialUiState.friends,
                    onParticipantClick = onParticipantClick,
                    onMuteChange = viewModel::setMuted,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchResultClick = { messageId ->
                        val focusTarget = buildChatOptionsFocusTarget(
                            conversationId = conversationId,
                            messageId = messageId,
                            conversation = conversation,
                            currentUserId = currentUserId,
                            friends = socialUiState.friends,
                        )

                        viewModel.focusMessage(
                            messageId = messageId,
                            onFocused = {
                                onFocusedMessageRequested(focusTarget)
                            },
                        )
                    },
                    onRenameClick = {
                        showRenameDialog = true
                    },
                    onInviteClick = {
                        showInviteDialog = true
                    },
                    onLeaveClick = {
                        showLeaveDialog = true
                    },
                    onDismissMessages = viewModel::dismissMessages,
                )
            }
        }
    }

    if (showRenameDialog && conversation != null) {
        RenameGroupDialog(
            currentTitle = conversation.title,
            onDismiss = {
                showRenameDialog = false
            },
            onSave = { newTitle ->
                viewModel.renameGroup(newTitle)
                showRenameDialog = false
            },
        )
    }

    if (showInviteDialog && conversation != null) {
        InviteParticipantsDialog(
            friends = socialUiState.friends,
            currentParticipantIds = conversation.participantIds,
            onDismiss = {
                showInviteDialog = false
            },
            onInvite = { selectedFriends ->
                viewModel.addParticipants(
                    selectedFriends.associate { friend ->
                        friend.uid to friend.displayName
                    },
                )
                showInviteDialog = false
            },
        )
    }

    if (showLeaveDialog) {
        LeaveGroupDialog(
            onDismiss = {
                showLeaveDialog = false
            },
            onConfirm = {
                viewModel.leaveGroup()
                showLeaveDialog = false
            },
        )
    }
}

@Composable
private fun ChatOptionsContent(
    conversation: ChatConversation,
    currentUserId: String?,
    isMuted: Boolean,
    searchQuery: String,
    searchResults: List<ChatMessage>,
    errorMessage: String?,
    infoMessage: String?,
    friends: List<FriendUser>,
    onParticipantClick: (String) -> Unit,
    onMuteChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchResultClick: (String) -> Unit,
    onRenameClick: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onDismissMessages: () -> Unit,
) {
    val title = buildConversationTitle(
        conversation = conversation,
        currentUserId = currentUserId,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Chat Options",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (errorMessage != null || infoMessage != null) {
            item {
                MessageCard(
                    message = errorMessage ?: infoMessage.orEmpty(),
                    isError = errorMessage != null,
                    onDismiss = onDismissMessages,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Mute conversation",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = if (isMuted) {
                            "You will not get notifications for this chat."
                        } else {
                            "You will get notifications for this chat."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isMuted,
                    onCheckedChange = onMuteChange,
                )
            }
        }

        item {
            HorizontalDivider()
        }

        if (conversation.isGroup) {
            item {
                SectionTitle("Group")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onRenameClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            text = "Rename",
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onInviteClick,
                        modifier = Modifier.weight(1f),
                        enabled = friends.any { friend ->
                            friend.uid !in conversation.participantIds
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            text = "Invite",
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
            }
        }

        item {
            SectionTitle("Participants")
        }

        items(
            items = conversation.participantIds,
            key = { participantId ->
                participantId
            },
        ) { participantId ->
            val participantName = conversation.participantNames[participantId]
                ?: "Unknown User"

            val friend = friends.firstOrNull { friend ->
                friend.uid == participantId
            }

            ParticipantRow(
                displayName = participantName,
                major = friend?.major.orEmpty(),
                avatarId = friend?.avatarId.orEmpty(),
                isCurrentUser = participantId == currentUserId,
                onClick = {
                    onParticipantClick(participantId)
                },
            )
        }

        item {
            HorizontalDivider()
        }

        item {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                label = "Search this chat",
            )
        }

        if (searchQuery.isNotBlank()) {
            if (searchResults.isEmpty()) {
                item {
                    Text(
                        text = "No messages found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(
                    items = searchResults,
                    key = { message ->
                        message.id
                    },
                ) { message ->
                    SearchResultRow(
                        message = message,
                        onClick = {
                            onSearchResultClick(message.id)
                        },
                    )
                }
            }
        }

        if (conversation.isGroup) {
            item {
                HorizontalDivider()
            }

            item {
                Button(
                    onClick = onLeaveClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Leave Group Chat")
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    label: String,
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
                Text(label)
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
private fun SectionTitle(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onDismiss()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )

        IconButton(
            onClick = onDismiss,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    displayName: String,
    major: String,
    avatarId: String,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        leadingContent = {
            UserAvatar(avatarId = avatarId)
        },
        headlineContent = {
            Text(
                text = if (isCurrentUser) {
                    "$displayName (You)"
                } else {
                    displayName
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = major.ifBlank { "Major unavailable" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun SearchResultRow(
    message: ChatMessage,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        headlineContent = {
            Text(
                text = message.senderName.ifBlank { "Unknown" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = message.text.ifBlank {
                    message.sharedLocation?.title ?: "Shared a location"
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun RenameGroupDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember {
        mutableStateOf(currentTitle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Group")
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                label = {
                    Text("Group name")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun InviteParticipantsDialog(
    friends: List<FriendUser>,
    currentParticipantIds: List<String>,
    onDismiss: () -> Unit,
    onInvite: (List<FriendUser>) -> Unit,
) {
    val selectedFriendIds = remember {
        mutableStateListOf<String>()
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val inviteableFriends = friends.filter { friend ->
        friend.uid !in currentParticipantIds
    }

    val filteredFriends = inviteableFriends.filter { friend ->
        val query = searchQuery.trim()

        query.isBlank() ||
                friend.displayName.contains(query, ignoreCase = true) ||
                friend.major.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Invite Friends")
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                    },
                    label = "Search friends",
                )

                when {
                    inviteableFriends.isEmpty() -> {
                        Text("No friends available to invite.")
                    }

                    filteredFriends.isEmpty() -> {
                        Text("No friends found.")
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(
                                items = filteredFriends,
                                key = { friend ->
                                    friend.uid
                                },
                            ) { friend ->
                                val selected = friend.uid in selectedFriendIds

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selected) {
                                                selectedFriendIds.remove(friend.uid)
                                            } else {
                                                selectedFriendIds.add(friend.uid)
                                            }
                                        },
                                    leadingContent = {
                                        UserAvatar(avatarId = friend.avatarId)
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
                                                if (selected) {
                                                    selectedFriendIds.remove(friend.uid)
                                                } else {
                                                    selectedFriendIds.add(friend.uid)
                                                }
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedFriendIds.isNotEmpty(),
                onClick = {
                    val selectedFriends = inviteableFriends.filter { friend ->
                        friend.uid in selectedFriendIds
                    }

                    onInvite(selectedFriends)
                },
            ) {
                Text("Invite")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun LeaveGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Leave Group Chat?")
        },
        text = {
            Text("You will no longer receive messages or notifications from this group.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text("Leave")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun UserAvatar(
    avatarId: String,
) {
    Image(
        painter = painterResource(id = avatarResourceForId(avatarId)),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

private fun avatarResourceForId(
    avatarId: String,
): Int {
    return when (avatarId) {
        "avatar_dolphin" -> DesignR.drawable.avatar_dolphin
        "avatar_whale" -> DesignR.drawable.avatar_whale
        "avatar_turtle" -> DesignR.drawable.avatar_turtle
        else -> DesignR.drawable.avatar_default
    }
}

private fun buildChatOptionsFocusTarget(
    conversationId: String,
    messageId: String,
    conversation: ChatConversation,
    currentUserId: String?,
    friends: List<FriendUser>,
): ChatOptionsFocusTarget {
    val chatTitle = buildConversationTitle(
        conversation = conversation,
        currentUserId = currentUserId,
    )

    if (conversation.isGroup) {
        return ChatOptionsFocusTarget(
            conversationId = conversationId,
            messageId = messageId,
            chatTitle = chatTitle,
            isGroup = true,
            groupParticipantNames = conversation.participantNames,
            groupParticipantAvatarIds = friends
                .filter { friend ->
                    friend.uid in conversation.participantIds
                }
                .associate { friend ->
                    friend.uid to friend.avatarId
                },
        )
    }

    val friendUserId = conversation.participantIds
        .firstOrNull { participantId ->
            participantId != currentUserId
        }
        .orEmpty()

    val friend = friends.firstOrNull { friend ->
        friend.uid == friendUserId
    }

    val friendDisplayName = conversation.participantNames[friendUserId]
        ?: friend?.displayName
        ?: chatTitle

    return ChatOptionsFocusTarget(
        conversationId = conversationId,
        messageId = messageId,
        chatTitle = chatTitle,
        isGroup = false,
        friendUserId = friendUserId,
        friendDisplayName = friendDisplayName,
        friendAvatarId = friend?.avatarId.orEmpty(),
    )
}

private fun buildConversationTitle(
    conversation: ChatConversation,
    currentUserId: String?,
): String {
    if (conversation.title.isNotBlank()) {
        return conversation.title
    }

    if (conversation.isGroup) {
        return conversation.participantNames
            .filterKeys { participantId ->
                participantId != currentUserId
            }
            .values
            .filter { name ->
                name.isNotBlank()
            }
            .joinToString(", ")
            .ifBlank {
                "Group Chat"
            }
    }

    return conversation.participantNames
        .filterKeys { participantId ->
            participantId != currentUserId
        }
        .values
        .firstOrNull()
        ?.ifBlank { null }
        ?: "Chat"
}