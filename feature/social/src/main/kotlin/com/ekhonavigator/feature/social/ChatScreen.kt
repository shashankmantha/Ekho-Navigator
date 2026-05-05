package com.ekhonavigator.feature.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ekhonavigator.core.designsystem.R as DesignR
import com.ekhonavigator.core.model.PresenceStatus
import com.ekhonavigator.core.model.SharedLocation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    friendUserId: String,
    friendDisplayName: String,
    friendAvatarId: String,
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    chatTitle: String = friendDisplayName,
    isGroup: Boolean = false,
    groupParticipantNames: Map<String, String> = emptyMap(),
    groupParticipantAvatarIds: Map<String, String> = emptyMap(),
    sharedLocation: SharedLocation? = null,
    initialFocusedMessageId: String? = null,
    onNavigateToMap: () -> Unit = {},
    onOpenChatOptions: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = viewModel.getCurrentUserId()

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    val optionsConversationId = conversationId ?: uiState.conversationId
    val liveConversation = uiState.conversation
    val isLiveGroup = liveConversation?.isGroup ?: isGroup

    val displayTitle = when {
        liveConversation?.title?.isNotBlank() == true -> {
            liveConversation.title
        }

        liveConversation?.isGroup == true -> {
            liveConversation.participantNames
                .filterKeys { participantId ->
                    participantId != currentUserId
                }
                .values
                .joinToString(", ")
                .ifBlank { "Group Chat" }
        }

        chatTitle.isNotBlank() -> {
            chatTitle
        }

        isGroup -> {
            groupParticipantNames
                .filterKeys { participantId ->
                    participantId != currentUserId
                }
                .values
                .joinToString(", ")
                .ifBlank { "Group Chat" }
        }

        friendDisplayName.isNotBlank() -> {
            friendDisplayName
        }

        else -> {
            "Chat"
        }
    }

    val groupAvatarIdsToShow = if (isLiveGroup) {
        val participantIds = liveConversation?.participantIds
            ?: groupParticipantNames.keys.toList()

        participantIds
            .filter { participantId ->
                participantId != currentUserId
            }
            .map { participantId ->
                groupParticipantAvatarIds[participantId].orEmpty()
            }
    } else {
        emptyList()
    }

    LaunchedEffect(
        conversationId,
        friendUserId,
        isGroup,
        groupParticipantNames,
        initialFocusedMessageId,
    ) {
        when {
            conversationId != null -> {
                viewModel.startExistingConversation(
                    conversationId = conversationId,
                    initialFocusedMessageId = initialFocusedMessageId,
                )
            }

            isGroup -> {
                viewModel.startPendingGroupConversation(
                    groupTitle = chatTitle,
                    participantNames = groupParticipantNames,
                )
            }

            friendUserId.isNotBlank() -> {
                viewModel.startConversation(
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )
            }
        }

        if (sharedLocation != null) {
            viewModel.stageSharedLocation(sharedLocation)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChatHeader(
            title = displayTitle,
            avatarId = friendAvatarId,
            presenceStatus = if (isLiveGroup) null else uiState.friendPresence,
            isGroup = isLiveGroup,
            groupAvatarIds = groupAvatarIdsToShow,
            canOpenOptions = optionsConversationId != null,
            onOpenOptionsClick = {
                optionsConversationId?.let { id ->
                    onOpenChatOptions(id)
                }
            },
        )

        ChatContent(
            uiState = uiState,
            currentUserId = currentUserId,
            friendUserId = friendUserId,
            isGroup = isLiveGroup,
            groupParticipantNames = groupParticipantNames,
            groupParticipantAvatarIds = groupParticipantAvatarIds,
            imeBottom = imeBottom,
            onNavigateToMap = onNavigateToMap,
            onFocusedMessageHandled = viewModel::clearFocusedMessage,
            viewModel = viewModel,
            modifier = Modifier.weight(1f),
        )

        InfoMessageCard(
            message = uiState.infoMessage,
            onDismiss = viewModel::dismissInfoMessage,
        )

        uiState.pendingSharedLocation?.let { location ->
            LocationPreviewCard(
                location = location,
                modifier = Modifier.padding(horizontal = 8.dp),
                onDismiss = viewModel::clearPendingSharedLocation,
            )
        }

        MessageInputRow(
            draftMessage = uiState.draftMessage,
            onDraftMessageChange = viewModel::onDraftMessageChange,
            onSendClick = {
                viewModel.sendMessage(
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )
            },
        )
    }
}

private suspend fun scrollToLatestMessageIfPossible(
    messageCount: Int,
    scrollToIndex: suspend (Int) -> Unit,
) {
    if (messageCount > 0) {
        scrollToIndex(messageCount - 1)
    }
}

@Composable
private fun ChatHeader(
    title: String,
    avatarId: String,
    presenceStatus: PresenceStatus?,
    isGroup: Boolean,
    groupAvatarIds: List<String>,
    canOpenOptions: Boolean,
    onOpenOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = if (canOpenOptions) {
                Modifier.clickable {
                    onOpenOptionsClick()
                }
            } else {
                Modifier
            },
            contentAlignment = Alignment.Center,
        ) {
            if (isGroup) {
                GroupAvatarStack(
                    avatarIds = groupAvatarIds,
                )
            } else {
                ChatAvatar(
                    avatarId = avatarId,
                    displayName = title,
                    presenceStatus = presenceStatus,
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatAvatar(
    avatarId: String,
    displayName: String,
    presenceStatus: PresenceStatus?,
    modifier: Modifier = Modifier,
) {
    val statusColor = presenceStatus?.state?.uppercase()?.let { state ->
        when (state) {
            "ONLINE" -> Color(0xFF4CAF50)
            "AWAY" -> Color(0xFFFFC107)
            "BUSY" -> Color(0xFFF44336)
            else -> null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Image(
            painter = painterResource(id = avatarResourceForId(avatarId)),
            contentDescription = "$displayName avatar",
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )

        if (statusColor != null) {
            PresenceDot(statusColor = statusColor)
        }
    }
}

@Composable
private fun GroupAvatarStack(
    avatarIds: List<String>,
    modifier: Modifier = Modifier,
) {
    val avatarsToShow = avatarIds
        .ifEmpty { listOf("") }
        .take(4)

    val totalWidth = when (avatarsToShow.size) {
        1 -> 72.dp
        2 -> 104.dp
        3 -> 132.dp
        else -> 160.dp
    }

    Box(
        modifier = modifier.size(width = totalWidth, height = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        avatarsToShow.forEachIndexed { index, avatarId ->
            val offsetX = ((index - (avatarsToShow.size - 1) / 2f) * 30).dp

            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = avatarResourceForId(avatarId)),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun avatarResourceForId(avatarId: String): Int {
    return when (avatarId) {
        "avatar_dolphin" -> DesignR.drawable.avatar_dolphin
        "avatar_whale" -> DesignR.drawable.avatar_whale
        "avatar_turtle" -> DesignR.drawable.avatar_turtle
        else -> DesignR.drawable.avatar_default
    }
}

@Composable
private fun PresenceDot(
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(18.dp)
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

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    currentUserId: String?,
    friendUserId: String,
    isGroup: Boolean,
    groupParticipantNames: Map<String, String>,
    groupParticipantAvatarIds: Map<String, String>,
    imeBottom: Int,
    onNavigateToMap: () -> Unit,
    onFocusedMessageHandled: () -> Unit,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            LoadingChatContent(modifier = modifier)
        }

        uiState.errorMessage != null -> {
            ErrorChatContent(
                message = uiState.errorMessage,
                modifier = modifier,
            )
        }

        else -> {
            MessageList(
                uiState = uiState,
                currentUserId = currentUserId,
                friendUserId = friendUserId,
                isGroup = isGroup,
                groupParticipantNames = groupParticipantNames,
                groupParticipantAvatarIds = groupParticipantAvatarIds,
                imeBottom = imeBottom,
                onNavigateToMap = onNavigateToMap,
                onFocusedMessageHandled = onFocusedMessageHandled,
                viewModel = viewModel,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LoadingChatContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorChatContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessageList(
    uiState: ChatUiState,
    currentUserId: String?,
    friendUserId: String,
    isGroup: Boolean,
    groupParticipantNames: Map<String, String>,
    groupParticipantAvatarIds: Map<String, String>,
    imeBottom: Int,
    onNavigateToMap: () -> Unit,
    onFocusedMessageHandled: () -> Unit,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val conversationKey = uiState.conversationId ?: "pending_chat"

    var hasHandledInitialScroll by remember(conversationKey) {
        mutableStateOf(false)
    }

    var previousMessageCount by remember(conversationKey) {
        mutableIntStateOf(0)
    }

    val participantNames = remember(
        uiState.conversation,
        groupParticipantNames,
    ) {
        uiState.conversation?.participantNames
            ?.takeIf { names ->
                names.isNotEmpty()
            }
            ?: groupParticipantNames
    }

    LaunchedEffect(
        conversationKey,
        uiState.messages.size,
        uiState.focusedMessageId,
    ) {
        val messages = uiState.messages

        if (messages.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val focusedMessageId = uiState.focusedMessageId

        if (focusedMessageId != null) {
            val targetIndex = messages.indexOfFirst { message ->
                message.id == focusedMessageId
            }

            if (targetIndex >= 0) {
                hasHandledInitialScroll = true
                previousMessageCount = messages.size

                listState.animateScrollToItem(targetIndex)

                onFocusedMessageHandled()
                return@LaunchedEffect
            }
        }

        if (!hasHandledInitialScroll) {
            hasHandledInitialScroll = true
            previousMessageCount = messages.size

            scrollToLatestMessageIfPossible(
                messageCount = messages.size,
                scrollToIndex = { index ->
                    listState.scrollToItem(index)
                },
            )

            return@LaunchedEffect
        }

        if (messages.size > previousMessageCount && focusedMessageId == null) {
            previousMessageCount = messages.size

            scrollToLatestMessageIfPossible(
                messageCount = messages.size,
                scrollToIndex = { index ->
                    listState.animateScrollToItem(index)
                },
            )
        } else {
            previousMessageCount = messages.size
        }
    }

    LaunchedEffect(imeBottom) {
        if (
            imeBottom > 0 &&
            uiState.messages.isNotEmpty() &&
            uiState.focusedMessageId == null &&
            !listState.isScrollInProgress
        ) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val lastMessageIndex = uiState.messages.lastIndex
            val isNearBottom = lastVisibleIndex == null ||
                    lastVisibleIndex >= lastMessageIndex - 1

            if (isNearBottom) {
                scrollToLatestMessageIfPossible(
                    messageCount = uiState.messages.size,
                    scrollToIndex = { index ->
                        listState.animateScrollToItem(index)
                    },
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(
            items = uiState.messages,
            key = { it.id },
        ) { message ->
            val isMine = message.senderId == currentUserId

            val statusText = getMessageStatusText(
                isMine = isMine,
                isGroup = isGroup,
                currentUserId = currentUserId,
                friendUserId = friendUserId,
                readBy = message.readBy,
                participantNames = participantNames,
            )

            val shouldShowSenderInfo = isGroup && !isMine

            val senderDisplayName = participantNames[message.senderId]
                ?: message.senderName.ifBlank {
                    "Unknown"
                }

            val senderAvatarId = groupParticipantAvatarIds[message.senderId].orEmpty()

            MessageBubble(
                text = message.text,
                sharedLocation = message.sharedLocation,
                isMine = isMine,
                showSenderInfo = shouldShowSenderInfo,
                senderDisplayName = senderDisplayName,
                senderAvatarId = senderAvatarId,
                statusText = statusText,
                onLocationClick = {
                    message.sharedLocation?.let { location ->
                        viewModel.saveSharedLocationToMap(
                            location = location,
                            onSaved = onNavigateToMap,
                        )
                    }
                },
            )
        }
    }
}

private fun getMessageStatusText(
    isMine: Boolean,
    isGroup: Boolean,
    currentUserId: String?,
    friendUserId: String,
    readBy: List<String>,
    participantNames: Map<String, String>,
): String {
    if (!isMine) {
        return ""
    }

    if (isGroup) {
        val otherParticipantIds = participantNames.keys
            .filter { participantId ->
                participantId != currentUserId
            }

        val readByOtherIds = readBy
            .filter { participantId ->
                participantId != currentUserId
            }
            .distinct()

        val wasReadByEveryone = otherParticipantIds.isNotEmpty() &&
                otherParticipantIds.all { participantId ->
                    participantId in readByOtherIds
                }

        if (wasReadByEveryone) {
            return "Read by all"
        }

        val readByOtherNames = readByOtherIds
            .mapNotNull { participantId ->
                participantNames[participantId]
            }
            .distinct()

        return if (readByOtherNames.isEmpty()) {
            "Sent"
        } else {
            "Read by ${readByOtherNames.joinToString(", ")}"
        }
    }

    return if (friendUserId in readBy) {
        "Read"
    } else {
        "Sent"
    }
}

@Composable
private fun MessageBubble(
    text: String,
    sharedLocation: SharedLocation?,
    isMine: Boolean,
    showSenderInfo: Boolean,
    senderDisplayName: String,
    senderAvatarId: String,
    statusText: String,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
    ) {
        Column(
            horizontalAlignment = if (isMine) {
                Alignment.End
            } else {
                Alignment.Start
            },
        ) {
            if (showSenderInfo) {
                Row(
                    modifier = Modifier.padding(
                        start = 4.dp,
                        bottom = 4.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Image(
                        painter = painterResource(id = avatarResourceForId(senderAvatarId)),
                        contentDescription = "$senderDisplayName avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )

                    Text(
                        text = senderDisplayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isMine) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sharedLocation?.let { location ->
                        LocationPreviewCard(
                            location = location,
                            modifier = Modifier.padding(bottom = 4.dp),
                            onClick = onLocationClick,
                        )
                    }

                    if (text.isNotBlank()) {
                        Text(
                            text = text,
                            color = if (isMine) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            if (isMine && statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationPreviewCard(
    location: SharedLocation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Column {
                    Text(
                        text = "Sharing Location:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )

                    Text(
                        text = location.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoMessageCard(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(horizontal = 8.dp),
    ) {
        Surface(
            onClick = onDismiss,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputRow(
    draftMessage: String,
    onDraftMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draftMessage,
            onValueChange = onDraftMessageChange,
            modifier = Modifier.weight(1f),
            label = {
                Text("Message")
            },
            singleLine = true,
        )

        Button(
            onClick = onSendClick,
        ) {
            Text("Send")
        }
    }
}