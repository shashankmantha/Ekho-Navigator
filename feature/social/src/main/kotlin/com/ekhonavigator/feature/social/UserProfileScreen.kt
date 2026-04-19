package com.ekhonavigator.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
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
