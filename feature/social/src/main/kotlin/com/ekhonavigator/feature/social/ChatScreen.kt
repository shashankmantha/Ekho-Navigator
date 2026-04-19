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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
private fun ChatAvatar(
    friendAvatarId: String,
    friendDisplayName: String,
    modifier: Modifier = Modifier,
) {
    val avatarRes = when (friendAvatarId) {
        "avatar_dolphin" -> com.ekhonavigator.core.designsystem.R.drawable.avatar_dolphin
        "avatar_whale" -> com.ekhonavigator.core.designsystem.R.drawable.avatar_whale
        "avatar_turtle" -> com.ekhonavigator.core.designsystem.R.drawable.avatar_turtle
        else -> com.ekhonavigator.core.designsystem.R.drawable.avatar_default
    }

    Image(
        painter = painterResource(id = avatarRes),
        contentDescription = "$friendDisplayName avatar",
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun LocationPreviewCard(
    location: com.ekhonavigator.core.model.SharedLocation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Sharing Location:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = location.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            // Optional Dismiss "X" Button
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    friendUserId: String,
    friendDisplayName: String,
    friendAvatarId: String,
    modifier: Modifier = Modifier,
    sharedLocation: com.ekhonavigator.core.model.SharedLocation? = null,
    onNavigateToMap: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val currentUserId = viewModel.getCurrentUserId()
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    LaunchedEffect(friendUserId) {
        viewModel.startConversation(
            friendUserId = friendUserId,
            friendDisplayName = friendDisplayName,
        )
        if (sharedLocation != null) {
            viewModel.stageSharedLocation(sharedLocation)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(imeBottom, uiState.messages.size) {
        if (imeBottom > 0 && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChatAvatar(
                friendAvatarId = friendAvatarId,
                friendDisplayName = friendDisplayName,
            )

            Text(
                text = friendDisplayName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val isMine = message.senderId == currentUserId
                        val statusText = when {
                            !isMine -> ""
                            friendUserId in message.readBy -> "Read"
                            else -> "Sent"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            },
                        ) {
                            Column(
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                            ) {
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
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        message.sharedLocation?.let { location ->
                                            LocationPreviewCard(
                                                location = location,
                                                modifier = Modifier.padding(bottom = 4.dp),
                                                onClick = {
                                                    viewModel.saveSharedLocationToMap(
                                                        location = location,
                                                        onSaved = onNavigateToMap
                                                    )
                                                }
                                            )
                                        }
                                        Text(
                                            text = message.text,
                                            color = if (isMine) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }

                                if (isMine) {
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
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.infoMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Surface(
                onClick = { viewModel.dismissInfoMessage() },
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 4.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.infoMessage ?: "",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    IconButton( // <-- ADDED IconButton
                        onClick = { viewModel.dismissInfoMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        uiState.pendingSharedLocation?.let { location ->
            LocationPreviewCard(
                location = location,
                modifier = Modifier.padding(horizontal = 8.dp),
                onDismiss = { viewModel.clearPendingSharedLocation() }
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.draftMessage,
                onValueChange = viewModel::onDraftMessageChange,
                modifier = Modifier.weight(1f),
                label = { Text("Message") },
                singleLine = true,
            )

            Button(
                onClick = {
                    viewModel.sendMessage(
                        friendUserId = friendUserId,
                        friendDisplayName = friendDisplayName,
                    )
                },
            ) {
                Text("Send")
            }
        }
    }
}
