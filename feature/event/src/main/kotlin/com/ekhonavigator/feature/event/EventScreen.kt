package com.ekhonavigator.feature.event

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Single event detail screen. Receives an event ID from navigation,
 * loads the event reactively from Room, and displays full details.
 */
@Composable
fun EventScreen(
    eventId: String,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    // Tell the ViewModel which event to observe
    LaunchedEffect(eventId) {
        viewModel.setEventId(eventId)
    }

    val event by viewModel.event.collectAsStateWithLifecycle()

    if (event == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        EventDetailContent(
            event = event!!,
            onBookmarkClick = viewModel::toggleBookmark,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDetailContent(
    event: CalendarEvent,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Title + action buttons ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (event.isBookmarked) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                    contentDescription = if (event.isBookmarked) "Remove bookmark" else "Bookmark",
                    tint = if (event.isBookmarked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            IconButton(onClick = {
                val shareText = buildString {
                    append(event.title)
                    append("\n")
                    val startZoned = event.startTime.atZone(zone)
                    append(startZoned.format(dateFormatter))
                    append(" at ")
                    append(startZoned.format(timeFormatter))
                    if (event.location.isNotBlank()) {
                        append("\n")
                        append(event.location)
                    }
                    if (event.url.isNotBlank()) {
                        append("\n")
                        append(event.url)
                    }
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share event"))
            }) {
                Icon(
                    imageVector = EkhoIcons.Share,
                    contentDescription = "Share",
                )
            }
        }

        // ---- Category chips ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            event.categories.forEach { category ->
                AssistChip(
                    onClick = { },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(category.color)),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(category.color).copy(alpha = 0.1f),
                        labelColor = Color(category.color),
                    ),
                )
            }
        }

        HorizontalDivider()

        // ---- Date & Time ----
        val startZoned = event.startTime.atZone(zone)
        val endZoned = event.endTime.atZone(zone)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = EkhoIcons.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = startZoned.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${startZoned.format(timeFormatter)} \u2013 ${endZoned.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- Location ----
        if (event.location.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = EkhoIcons.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // ---- Status (only shown if not the default CONFIRMED) ----
        if (event.status != "CONFIRMED") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = event.status.first().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Text(
                    text = event.status.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // ---- Description ----
        if (event.description.isNotBlank()) {
            HorizontalDivider()

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
