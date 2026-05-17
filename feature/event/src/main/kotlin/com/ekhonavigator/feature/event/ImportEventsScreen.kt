package com.ekhonavigator.feature.event

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.network.model.StagedImportedEvent
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ImportEventsScreen(
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ImportEventsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Some exporters mislabel mime type as application/octet-stream — accept
    // anything via OpenDocument and let the parser decide if the bytes parse.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.parseFile(uri)
    }

    LaunchedEffect(uiState) {
        if (uiState is ImportEventsUiState.Imported) onDone()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Import from .ics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Pick a calendar file exported from CI Records or any other system. Weekly schedules become class meetings; one-off entries become events.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                ImportEventsUiState.Idle -> {
                    PickerCallToAction(onPick = { picker.launch(arrayOf("*/*")) })
                }
                ImportEventsUiState.Parsing -> {
                    CenteredSpinner(label = "Reading file…")
                }
                is ImportEventsUiState.Empty -> {
                    EmptyState(message = state.message, onRetry = { picker.launch(arrayOf("*/*")) })
                }
                is ImportEventsUiState.Preview -> {
                    PreviewList(
                        state = state,
                        onToggle = viewModel::toggleExclusion,
                        onSelectAll = viewModel::includeAll,
                        onClearAll = viewModel::excludeAll,
                    )
                }
                is ImportEventsUiState.Imported -> {
                    CenteredSpinner(label = "Imported ${state.count} events.")
                }
            }
        }

        if (uiState is ImportEventsUiState.Preview) {
            val previewState = uiState as ImportEventsUiState.Preview
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = viewModel::reset, enabled = !previewState.isImporting) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = viewModel::commitImport,
                        enabled = previewState.selectedCount > 0 && !previewState.isImporting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text("Import ${previewState.selectedCount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCallToAction(onPick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Pick a .ics file to preview before anything gets added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPick, shape = RoundedCornerShape(12.dp)) {
            Text("Choose file")
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Need a schedule export?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Sign in to CI Records, open Schedule, then pick the download .ics dropdown and choose your term.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                // Custom Tabs handles the SSO redirect chain better than a raw
                // Intent — keeps the user in our app's task back-stack on return.
                runCatching {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, "http://go.csuci.edu/CIRecordsStudent".toUri())
                }
            },
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Open CI Records")
        }
    }
}

@Composable
private fun CenteredSpinner(label: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Text("Choose another file")
        }
    }
}

@Composable
private fun PreviewList(
    state: ImportEventsUiState.Preview,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.selectedCount} of ${state.staged.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = if (state.selectedCount == state.staged.size) onClearAll else onSelectAll) {
                Text(
                    text = if (state.selectedCount == state.staged.size) "Clear" else "Select all",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(state.staged, key = { it.sourceUid }) { staged ->
                StagedRow(
                    event = staged,
                    selected = staged.sourceUid !in state.excludedUids,
                    onToggle = { onToggle(staged.sourceUid) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun StagedRow(
    event: StagedImportedEvent,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
    val subtitle = buildString {
        append(event.startTime.atZone(zone).format(timeFormatter))
        if (event.recurrenceDays.isNotEmpty()) {
            append(" · weekly ")
            append(event.recurrenceDays.sorted().joinToString("") {
                it.getDisplayName(TextStyle.SHORT, Locale.US).take(2)
            })
        }
        if (!event.inferredCourseLabel.isNullOrBlank()) {
            append(" · ")
            append(event.inferredCourseLabel)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title.ifBlank { "(untitled)" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
