package com.ekhonavigator.feature.event

import android.content.Intent
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Loads the event reactively from Room — navigates back if the event is deleted remotely. */
@Composable
fun EventScreen(
    eventId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(
        checkNotNull(
            LocalViewModelStoreOwner.current
        ) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }, null
    ),
) {
    LaunchedEffect(eventId) {
        viewModel.setEventId(eventId)
    }

    val event by viewModel.event.collectAsStateWithLifecycle()
    val attendees by viewModel.attendees.collectAsStateWithLifecycle()
    val currentUserRsvp by viewModel.currentUserRsvp.collectAsStateWithLifecycle()

    // Track whether we ever loaded an event — if it disappears after loading,
    // the event was deleted (by owner, by self, or by remote sync) and we navigate back.
    // Also handles navigating back to a deleted event from the nav stack.
    var hadEvent by remember { mutableStateOf(false) }
    if (event != null) hadEvent = true
    LaunchedEffect(event, hadEvent) {
        if (hadEvent && event == null) onBack()
    }
    // If event never loads (deleted before we got here), bail after a short delay
    LaunchedEffect(eventId) {
        kotlinx.coroutines.delay(500)
        if (!hadEvent) onBack()
    }

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
            canDelete = viewModel.canDelete,
            onDeleteClick = viewModel::deleteEvent,
            hasAttendees = viewModel.hasAttendees,
            canRsvp = viewModel.canRsvp,
            isOwner = viewModel.isOwner,
            currentUserRsvp = currentUserRsvp,
            attendees = attendees,
            onRsvp = viewModel::rsvp,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDetailContent(
    event: CalendarEvent,
    onBookmarkClick: () -> Unit,
    canDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    hasAttendees: Boolean = false,
    canRsvp: Boolean = false,
    isOwner: Boolean = false,
    currentUserRsvp: RsvpStatus? = null,
    attendees: List<EventAttendee> = emptyList(),
    onRsvp: (RsvpStatus) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = remember { ZoneId.systemDefault() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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

            // Share always first (leftmost action)
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

            // Rightmost position: bookmark (campus) or delete (owned custom)
            if (event.source == EventSource.ICAL_FEED) {
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (event.isBookmarked) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                        contentDescription = if (event.isBookmarked) "Remove bookmark" else "Bookmark",
                        tint = if (event.isBookmarked) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (canDelete) {
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = EkhoIcons.Close,
                        contentDescription = "Delete event",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (event.categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                event.categories.forEach { category ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        ),
                    )
                }
            }
        }

        HorizontalDivider()

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
                    text = "${startZoned.format(timeFormatter)} \u2013 ${
                        endZoned.format(
                            timeFormatter
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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

        if (hasAttendees) {
            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = EkhoIcons.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (isOwner) {
                        val count = attendees.size
                        if (count == 1) "You shared this with 1 person"
                        else "You shared this with $count people"
                    } else {
                        "Shared event"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (canRsvp) {
                Text(
                    text = "Your Response",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onRsvp(RsvpStatus.GOING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentUserRsvp == RsvpStatus.GOING) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (currentUserRsvp == RsvpStatus.GOING) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = EkhoIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Going")
                }

                Button(
                    onClick = { onRsvp(RsvpStatus.NOT_GOING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentUserRsvp == RsvpStatus.NOT_GOING) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (currentUserRsvp == RsvpStatus.NOT_GOING) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = EkhoIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Not Going")
                }
            }
            }

            if (attendees.isNotEmpty()) {
                Text(
                    text = "Attendees",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                attendees.forEach { attendee ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = EkhoIcons.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = attendee.displayName.ifBlank { attendee.userId.take(8) },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = when (attendee.rsvpStatus) {
                                RsvpStatus.GOING -> "Going"
                                RsvpStatus.NOT_GOING -> "Not Going"
                                RsvpStatus.PENDING -> "Pending"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (attendee.rsvpStatus) {
                                RsvpStatus.GOING -> MaterialTheme.colorScheme.primary
                                RsvpStatus.NOT_GOING -> MaterialTheme.colorScheme.error
                                RsvpStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        if (event.description.isNotBlank()) {
            HorizontalDivider()

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            HtmlDescription(
                html = event.description,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete \"${event.title}\"? This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Renders an HTML string using Android's [TextView] + [Html.fromHtml].
 * This handles `<p>`, `<br>`, `<b>`, `<a href>` tags and HTML entities
 * that the 25Live Publisher feed embeds in event descriptions.
 * Links are clickable via [LinkMovementMethod].
 */
@Composable
private fun HtmlDescription(
    html: String,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val textSizeSp = MaterialTheme.typography.bodyMedium.fontSize.value

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = textSizeSp
            }
        },
        update = { textView ->
            textView.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        },
    )
}
