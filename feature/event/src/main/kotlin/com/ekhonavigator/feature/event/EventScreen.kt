package com.ekhonavigator.feature.event

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ekhonavigator.core.designsystem.component.EkhoMonogramBadge
import com.ekhonavigator.core.designsystem.component.FriendPickerEntry
import com.ekhonavigator.core.designsystem.component.FriendPickerSheet
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.prettifyAllCaps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Loads the event reactively from Room — navigates back if the event is deleted remotely. */
@Composable
fun EventScreen(
    eventId: String,
    onBack: () -> Unit = {},
    onLocationClick: (placeId: String) -> Unit = {},
    onEditClick: (eventId: String) -> Unit = {},
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
    val effectivePlaceId by viewModel.effectivePlaceId.collectAsStateWithLifecycle()
    val customLocationOffer by viewModel.customLocationOffer.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val shareSheetVisible by viewModel.shareSheetVisible.collectAsStateWithLifecycle()

    // The VM emits the new (or deduped existing) marker placeId after save; route it
    // through the same nav callback the campus path uses so the camera-pan animation runs.
    LaunchedEffect(viewModel) {
        viewModel.navigateToMarker.collect { placeId ->
            onLocationClick(placeId)
        }
    }

    var showSaveMarkerPrompt by remember { mutableStateOf(false) }

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
            effectivePlaceId = effectivePlaceId,
            customLocationOfferAvailable = customLocationOffer != null,
            onBookmarkClick = viewModel::toggleBookmark,
            canDelete = viewModel.canDelete,
            onDeleteClick = viewModel::deleteEvent,
            canEdit = viewModel.canEdit,
            onEditClick = { onEditClick(eventId) },
            canShare = viewModel.canShare,
            onShareClick = viewModel::openShareSheet,
            hasAttendees = viewModel.hasAttendees,
            canRsvp = viewModel.canRsvp,
            isOwner = viewModel.isOwner,
            currentUserRsvp = currentUserRsvp,
            attendees = attendees,
            onRsvp = viewModel::rsvp,
            onLocationClick = onLocationClick,
            onLocationSaveOfferClick = { showSaveMarkerPrompt = true },
            modifier = modifier,
        )

        customLocationOffer?.let { offer ->
            if (showSaveMarkerPrompt) {
                AlertDialog(
                    onDismissRequest = { showSaveMarkerPrompt = false },
                    title = { Text("Save to your map?") },
                    text = {
                        Text(
                            "\u201C${offer.title}\u201D was pinned to a marker by the event creator. " +
                                "Save it to your map so you can navigate there.",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showSaveMarkerPrompt = false
                            viewModel.saveCustomLocationToMyMarkers()
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveMarkerPrompt = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }

        if (shareSheetVisible) {
            val attendeeUids = remember(attendees) { attendees.map { it.userId }.toSet() }
            val entries = remember(friends, attendeeUids) {
                friends.map { friend ->
                    FriendPickerEntry(
                        uid = friend.uid,
                        displayName = friend.displayName,
                        subtitle = friend.major,
                        initiallySelected = friend.uid in attendeeUids,
                        locked = friend.uid in attendeeUids,
                    )
                }
            }
            FriendPickerSheet(
                friends = entries,
                onDismiss = viewModel::dismissShareSheet,
                onConfirm = viewModel::shareWith,
                title = "Invite friends",
                actionLabel = "Send invites",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventDetailContent(
    event: CalendarEvent,
    effectivePlaceId: String?,
    customLocationOfferAvailable: Boolean = false,
    onBookmarkClick: () -> Unit,
    canDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    canEdit: Boolean = false,
    onEditClick: () -> Unit = {},
    canShare: Boolean = false,
    onShareClick: () -> Unit = {},
    hasAttendees: Boolean = false,
    canRsvp: Boolean = false,
    isOwner: Boolean = false,
    currentUserRsvp: RsvpStatus? = null,
    attendees: List<EventAttendee> = emptyList(),
    onRsvp: (RsvpStatus) -> Unit = {},
    onLocationClick: (placeId: String) -> Unit = {},
    onLocationSaveOfferClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val cleanedDescription = remember(event.description) {
        stripTrumbaHeaderLines(event.description)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ── Top action chrome ───────────────────────────
        ActionRow(
            isBookmarked = event.isBookmarked,
            showBookmark = event.source == EventSource.ICAL_FEED,
            onBookmarkClick = onBookmarkClick,
            canShare = canShare,
            onShareClick = onShareClick,
            canEdit = canEdit,
            onEditClick = onEditClick,
            canDelete = canDelete,
            onDeleteClick = { showDeleteConfirmation = true },
        )

        // ── State ribbon (only when event has a state) ─
        StateRibbon(
            source = event.source,
            isBookmarked = event.isBookmarked,
        )

        // ── Date eyebrow ───────────────────────────────
        DateEyebrow(
            startTime = event.startTime,
            zone = zone,
            tagCount = event.categories.size,
        )

        Spacer(Modifier.height(14.dp))

        // ── Title ──────────────────────────────────────
        Text(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                letterSpacing = (-0.02).em,
                lineHeight = 34.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (event.eventType.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            EventTypeBadge(event.eventType)
        }

        // ── Meta rail ──────────────────────────────────
        Spacer(Modifier.height(18.dp))
        ThinDivider()
        Spacer(Modifier.height(10.dp))

        MetaTextRow(label = "WHEN", value = formatTimeRange(event.startTime, event.endTime, zone), monoValue = true)
        if (event.organization.isNotBlank()) {
            MetaTextRow(label = "HOSTED BY", value = event.organization.prettifyAllCaps())
        }
        if (event.location.isNotBlank()) {
            // Three states: existing place id resolves → nav directly; offer available
            // (recipient or owner-with-deleted-marker) → prompt to save first; otherwise
            // no click and the row stays in its un-linked styling.
            val locationClick: (() -> Unit)? = when {
                effectivePlaceId != null -> { -> onLocationClick(effectivePlaceId) }
                customLocationOfferAvailable -> onLocationSaveOfferClick
                else -> null
            }
            MetaTextRow(
                label = "WHERE",
                value = event.location,
                onClick = locationClick,
            )
        }
        if (event.status != "CONFIRMED") {
            MetaTextRow(
                label = "STATUS",
                value = event.status.lowercase().replaceFirstChar { it.uppercase() },
            )
        }

        // ── Attendees / RSVP ───────────────────────────
        if (hasAttendees) {
            Spacer(Modifier.height(14.dp))
            ThinDivider()
            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = EkhoIcons.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
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
                Spacer(Modifier.height(12.dp))
                SectionEyebrow("YOUR RESPONSE")
                Spacer(Modifier.height(8.dp))

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
                Spacer(Modifier.height(12.dp))
                SectionEyebrow("ATTENDEES")
                Spacer(Modifier.height(6.dp))

                attendees.forEach { attendee ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
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

        // ── About / Description ────────────────────────
        if (cleanedDescription.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            ThinDivider()
            Spacer(Modifier.height(14.dp))

            SectionEyebrow("ABOUT")
            Spacer(Modifier.height(8.dp))

            HtmlDescription(
                html = cleanedDescription,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Meta rail ──────────────────────────────────
        Spacer(Modifier.height(18.dp))
        ThinDivider()
        Spacer(Modifier.height(10.dp))

        if (event.categories.isNotEmpty()) {
            MetaChipsRow(label = "TAGGED", categories = event.categories)
        }

        Spacer(modifier = Modifier.height(24.dp))
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

// ── Helpers ────────────────────────────────────────────

@Composable
private fun ActionRow(
    isBookmarked: Boolean,
    showBookmark: Boolean,
    onBookmarkClick: () -> Unit,
    canShare: Boolean,
    onShareClick: () -> Unit,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        if (showBookmark) {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (isBookmarked) EkhoIcons.Bookmark else EkhoIcons.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                    tint = if (isBookmarked) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (canShare) {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = EkhoIcons.Share,
                    contentDescription = "Share event",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (canEdit) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = EkhoIcons.Edit,
                    contentDescription = "Edit event",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (canDelete) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = EkhoIcons.Close,
                    contentDescription = "Delete event",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StateRibbon(
    source: EventSource,
    isBookmarked: Boolean,
) {
    val (label, color) = when {
        source == EventSource.ICAL_FEED && isBookmarked ->
            "SAVED TO MY SCHEDULE" to MaterialTheme.colorScheme.tertiary
        source == EventSource.USER_CREATED ->
            "YOUR EVENT" to MaterialTheme.colorScheme.secondary
        source == EventSource.SHARED ->
            "SHARED INVITE" to MaterialTheme.colorScheme.secondary
        else -> return
    }

    Row(
        modifier = Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.14.em,
            ),
            color = color,
        )
    }
}

@Composable
private fun DateEyebrow(
    startTime: Instant,
    zone: ZoneId,
    tagCount: Int,
) {
    val date = startTime.atZone(zone)
    val today = remember { LocalDate.now(zone) }
    val tomorrow = remember { today.plusDays(1) }
    val eventDate = date.toLocalDate()

    val dayLabel = when (eventDate) {
        today -> "TODAY"
        tomorrow -> "TOMORROW"
        else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase(Locale.US)
    }
    val suffix = " · " + date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        .uppercase(Locale.US)
    val isImportant = eventDate == today || eventDate == tomorrow
    val brandRed = MaterialTheme.colorScheme.primary

    val dateText = buildAnnotatedString {
        if (isImportant) {
            withStyle(SpanStyle(color = brandRed)) { append(dayLabel) }
        } else {
            append(dayLabel)
        }
        append(suffix)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.14.em,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (tagCount == 1) "1 TAG" else "$tagCount TAGS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.14.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun EventTypeBadge(eventType: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = eventType.prettifyAllCaps(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.04.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

private val MetaLabelWidth = 76.dp

@Composable
private fun MetaTextRow(
    label: String,
    value: String,
    monoValue: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .padding(vertical = 5.dp)
    val valueColor = if (onClick != null) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top,
    ) {
        MetaLabel(label, Modifier.width(MetaLabelWidth).padding(top = 2.dp))
        Text(
            text = value,
            style = if (monoValue) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.02).em,
                )
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = valueColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaChipsRow(
    label: String,
    categories: List<EventCategory>,
) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MetaLabel(label, Modifier.width(MetaLabelWidth).padding(top = 6.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { category ->
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    leadingIcon = {
                        EkhoMonogramBadge(
                            monogram = category.monogram,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
}

@Composable
private fun MetaLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun SectionEyebrow(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

private fun formatTimeRange(startTime: Instant, endTime: Instant, zone: ZoneId): String {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    return "${startTime.atZone(zone).format(timeFormatter)} \u2013 " +
            endTime.atZone(zone).format(timeFormatter)
}

private val trumbaHeaderPattern = Regex(
    "^\\s*(Event Name|Organization|Organizer|Categories)\\s*:.*$",
    RegexOption.IGNORE_CASE,
)

/**
 * Trumba pastes structured metadata ("Event Name: …", "Organization: …",
 * "Organizer: …", "Categories: …") as the first lines of DESCRIPTION. We now
 * surface those fields at the top of the detail screen, so stripping the
 * leading matching lines avoids showing them twice.
 */
private fun stripTrumbaHeaderLines(description: String): String {
    if (description.isBlank()) return description
    val normalized = description.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    val lines = normalized.split("\n")
    val kept = lines.dropWhile { trumbaHeaderPattern.matches(it) }
    return kept.joinToString("\n").trim()
}

/**
 * Renders an HTML string using Android's [TextView] + [Html.fromHtml]. Handles
 * `<p>`, `<br>`, `<b>`, `<a href>` and HTML entities the feed embeds. Links
 * are tappable via [LinkMovementMethod].
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
