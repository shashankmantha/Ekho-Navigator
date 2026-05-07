package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ekhonavigator.core.designsystem.R
import com.ekhonavigator.core.model.OnlineStatus

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Friend?") },
            text = { Text("Are you sure? We won't tell the other person, but in order to message them again you will have to send them a new friend request.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.removeFriend(userId) {
                            onBack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        uiState.user != null -> {
            val user = uiState.user!!

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isFriend) {
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                            Text("Remove Friend", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Image(
                        painter = painterResource(id = avatarIdToRes(user.avatarId)),
                        contentDescription = "Profile avatar",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                    )

                    if (user.showOnlineStatus && uiState.isOnline) {
                        val statusColor = when (uiState.onlineStatus) {
                            OnlineStatus.ONLINE -> Color(0xFF4CAF50)
                            OnlineStatus.AWAY -> Color(0xFFFFC107)
                            OnlineStatus.BUSY -> Color(0xFFF44336)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(4.dp)
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (user.majorVisible && user.major.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Major",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = user.major,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                if (user.descriptionVisible && user.description.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = user.description,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                if (user.linksVisible && user.links.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Links",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = user.links,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                if (
                    !user.majorVisible &&
                    !user.descriptionVisible &&
                    !user.linksVisible
                ) {
                    HorizontalDivider()
                    Text(
                        text = "This user has no visible profile details.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("User not found")
            }
        }
    }
}

private fun avatarIdToRes(avatarId: String): Int {
    return when (avatarId) {
        "avatar_dolphin" -> R.drawable.avatar_dolphin
        "avatar_whale" -> R.drawable.avatar_whale
        "avatar_turtle" -> R.drawable.avatar_turtle
        else -> R.drawable.avatar_default
    }
}
