package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    friendUserId: String,
    friendDisplayName: String,
    friendAvatarId: String,
    modifier: Modifier = Modifier,
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
                                    Text(
                                        text = message.text,
                                        color = if (isMine) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
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