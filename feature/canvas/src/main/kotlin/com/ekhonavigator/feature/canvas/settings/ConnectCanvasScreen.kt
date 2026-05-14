package com.ekhonavigator.feature.canvas.settings

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons

@Composable
fun ConnectCanvasScreen(
    onViewCoursesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ConnectCanvasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Canvas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            when (val state = uiState) {
                ConnectCanvasUiState.Loading -> LoadingPlaceholder()
                is ConnectCanvasUiState.NotConnected -> NotConnectedContent(
                    state = state,
                    onDomainChange = viewModel::setDomain,
                    onTokenChange = viewModel::setToken,
                    onConnectClick = viewModel::connect,
                )
                is ConnectCanvasUiState.Connected -> ConnectedContent(
                    state = state,
                    onViewCoursesClick = onViewCoursesClick,
                    onDisconnectClick = viewModel::disconnect,
                )
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotConnectedContent(
    state: ConnectCanvasUiState.NotConnected,
    onDomainChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onConnectClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Connect your Canvas account to see assignments, grades, and announcements alongside the rest of your campus life.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "Open Canvas Settings, scroll to Approved Integrations, and generate a New Access Token. Your token stays on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OpenCanvasSettingsButton()

        OutlinedTextField(
            value = state.domain,
            onValueChange = onDomainChange,
            label = { Text("Canvas domain") },
            placeholder = { Text("csuci.instructure.com") },
            singleLine = true,
            enabled = !state.isConnecting,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChange,
            label = { Text("Access token") },
            singleLine = true,
            enabled = !state.isConnecting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onConnectClick,
            enabled = !state.isConnecting && state.domain.isNotBlank() && state.token.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun ConnectedContent(
    state: ConnectCanvasUiState.Connected,
    onViewCoursesClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = EkhoIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Connected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.domain,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Button(
        onClick = onViewCoursesClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("View My Courses")
    }

    OutlinedButton(
        onClick = onDisconnectClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text("Disconnect Canvas")
    }
}

// CSUCI students sign into Canvas at the vanity URL, not the Instructure
// subdomain we hit for API calls. Custom Tabs reuses their existing web
// session, landing them on Approved Integrations already signed in.
private const val CANVAS_SETTINGS_URL = "https://cilearn.csuci.edu/profile/settings"

@Composable
private fun OpenCanvasSettingsButton() {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            // Guards against devices with no browser / Custom Tabs provider.
            runCatching {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, CANVAS_SETTINGS_URL.toUri())
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            imageVector = EkhoIcons.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Open Canvas Settings",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
