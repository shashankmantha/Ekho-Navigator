package com.ekhonavigator.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.SignedIn) onSignIn()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val clientId = stringResource(R.string.default_web_client_id)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
            AccountUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            AccountUiState.SignedOut,
            is AccountUiState.Error -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "\uD83D\uDC2C",
                                        style = MaterialTheme.typography.headlineLarge,
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Welcome to Ekho Navigator",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Sign in to have full access to all features.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                Button(
                                    onClick = {
                                        viewModel.onGoogleSignInClick(context, clientId)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(
                                        text = "Sign in with Google",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "You can sign in later from your account settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                )

                                if (state is AccountUiState.Error) {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(200.dp))
                    }
                }
            }

            is AccountUiState.SignedIn -> {
                EditProfileScreen(
                    userEmail = state.email,
                    initialDisplayName = state.displayName,
                    initialMajor = state.major,
                    initialDescription = state.description,
                    initialLinks = state.links,
                    initialMajorVisible = state.majorVisible,
                    initialDescriptionVisible = state.descriptionVisible,
                    initialLinksVisible = state.linksVisible,
                    initialSearchable = state.searchable,
                    initialShowOnlineStatus = state.showOnlineStatus,
                    avatarId = state.avatarId,
                    onSaveClick = { displayName, major, description, links, majorVisible, descriptionVisible, linksVisible, searchable, showOnlineStatus, avatarId ->
                        val oldDisplayName = state.displayName
                        val oldMajor = state.major
                        val oldDescription = state.description
                        val oldLinks = state.links
                        val oldMajorVisible = state.majorVisible
                        val oldDescriptionVisible = state.descriptionVisible
                        val oldLinksVisible = state.linksVisible
                        val oldAvatarId = state.avatarId
                        val oldSearchable = state.searchable
                        val oldShowOnlineStatus = state.showOnlineStatus

                        viewModel.saveProfile(
                            displayName = displayName,
                            major = major,
                            description = description,
                            links = links,
                            majorVisible = majorVisible,
                            descriptionVisible = descriptionVisible,
                            linksVisible = linksVisible,
                            avatarId = avatarId,
                            searchable = searchable,
                            showOnlineStatus = showOnlineStatus,
                        )

                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Saved successfully",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long,
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.saveProfile(
                                    displayName = oldDisplayName,
                                    major = oldMajor,
                                    description = oldDescription,
                                    links = oldLinks,
                                    majorVisible = oldMajorVisible,
                                    descriptionVisible = oldDescriptionVisible,
                                    linksVisible = oldLinksVisible,
                                    avatarId = oldAvatarId,
                                    searchable = oldSearchable,
                                    showOnlineStatus = oldShowOnlineStatus,
                                )
                            }
                        }
                    },
                    onSignOutClick = {
                        viewModel.onSignOutClick()
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        )
    }
}
