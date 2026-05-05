package com.ekhonavigator.feature.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.LocalCanvasConnected
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.isPast
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InvitesScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitesViewModel = hiltViewModel(),
) {
    val pending by viewModel.pendingInvites.collectAsStateWithLifecycle()
    val declined by viewModel.declinedInvites.collectAsStateWithLifecycle()
    val friendRequests by viewModel.friendRequests.collectAsStateWithLifecycle()
    val showPast by viewModel.showPast.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val courseCodeById by viewModel.courseCodeById.collectAsStateWithLifecycle()
    val canvasConnected = LocalCanvasConnected.current

    var showDeclined by rememberSaveable { mutableStateOf(false) }
    // Section collapse state — keyed per section so each remembers
    // independently across recomposition + process death.
    var friendRequestsExpanded by rememberSaveable { mutableStateOf(true) }
    var eventInvitesExpanded by rememberSaveable { mutableStateOf(true) }
    var announcementsExpanded by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "friend-requests-category") {
            CategoryHeader(
                icon = EkhoIcons.Person,
                title = "Friend Requests",
                subtitle = "People who want to connect",
                expanded = friendRequestsExpanded,
                onToggleExpanded = { friendRequestsExpanded = !friendRequestsExpanded },
            )
        }

        if (friendRequestsExpanded) {
            if (friendRequests.isEmpty()) {
                item(key = "friend-requests-empty") {
                    CategoryEmptyState(message = "No friend requests right now")
                }
            } else {
                item(key = "friend-requests-sub") {
                    SubsectionLabel(title = "Pending", count = friendRequests.size)
                }
                items(friendRequests, key = { "friend-req-${it.uid}" }) { request ->
                    FriendRequestRow(
                        request = request,
                        onAccept = { viewModel.acceptFriendRequest(request.uid) },
                        onDeny = { viewModel.denyFriendRequest(request.uid) },
                    )
                }
            }
        }

        item(key = "event-invites-category") {
            CategoryHeader(
                icon = EkhoIcons.Notifications,
                title = "Event Invites",
                subtitle = "Invitations from friends",
                expanded = eventInvitesExpanded,
                onToggleExpanded = { eventInvitesExpanded = !eventInvitesExpanded },
                trailing = {
                    PastToggle(
                        checked = showPast,
                        onCheckedChange = { viewModel.togglePast() },
                    )
                },
            )
        }

        if (eventInvitesExpanded) {
            if (pending.isEmpty() && declined.isEmpty()) {
                item(key = "event-invites-empty") {
                    CategoryEmptyState(
                        message = if (showPast) {
                            "No event invites — past or present"
                        } else {
                            "No event invites right now"
                        },
                    )
                }
            } else {
                renderEventInviteRows(
                    pending = pending,
                    declined = declined,
                    showDeclined = showDeclined,
                    onToggleShowDeclined = { showDeclined = !showDeclined },
                    onEventClick = onEventClick,
                    onRsvp = viewModel::rsvp,
                )
            }
        }

        if (canvasConnected) {
            item(key = "announcements-category") {
                CategoryHeader(
                    icon = EkhoIcons.Notifications,
                    title = "Announcements",
                    subtitle = "From your Canvas courses",
                    expanded = announcementsExpanded,
                    onToggleExpanded = { announcementsExpanded = !announcementsExpanded },
                )
            }
            if (announcementsExpanded) {
                if (announcements.isEmpty()) {
                    item(key = "announcements-empty") {
                        CategoryEmptyState(message = "No course announcements right now")
                    }
                } else {
                    items(announcements, key = { "announcement-${it.id}" }) { announcement ->
                        BellAnnouncementRow(
                            announcement = announcement,
                            courseLabel = courseCodeById[announcement.courseId]
                                ?.let(CourseColorAssigner::familyKey),
                            onClick = {
                                if (announcement.isUnread) viewModel.markAnnouncementRead(announcement.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pending invites rendered in the LazyColumn — extracted so the section's
 * expanded/collapsed branch in the screen body stays readable. Returns no
 * value; emits items into the surrounding LazyListScope.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.renderEventInviteRows(
    pending: List<CalendarEvent>,
    declined: List<CalendarEvent>,
    showDeclined: Boolean,
    onToggleShowDeclined: () -> Unit,
    onEventClick: (String) -> Unit,
    onRsvp: (eventId: String, status: RsvpStatus) -> Unit,
) {
    if (pending.isNotEmpty()) {
        item(key = "pending-sub") {
            SubsectionLabel(title = "Pending", count = pending.size)
        }
        items(pending, key = { "pending-${it.id}" }) { event ->
            InviteRow(
                event = event,
                onClick = { onEventClick(event.id) },
                primaryAction = {
                    Button(
                        onClick = { onRsvp(event.id, RsvpStatus.GOING) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(EkhoIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Going")
                    }
                },
                secondaryAction = {
                    OutlinedButton(
                        onClick = { onRsvp(event.id, RsvpStatus.NOT_GOING) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(EkhoIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Decline")
                    }
                },
            )
        }
    }

    if (declined.isNotEmpty()) {
        item(key = "declined-toggle") {
            DeclinedToggle(
                count = declined.size,
                expanded = showDeclined,
                onToggle = onToggleShowDeclined,
            )
        }
        if (showDeclined) {
            items(declined, key = { "declined-${it.id}" }) { event ->
                InviteRow(
                    event = event,
                    onClick = { onEventClick(event.id) },
                    primaryAction = {
                        Button(
                            onClick = { onRsvp(event.id, RsvpStatus.GOING) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(EkhoIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Undo decline")
                        }
                    },
                )
            }
        }
    }
}

/**
 * Now collapsible — chevron rotates 90° when expanded. Caller owns the state
 * via `rememberSaveable` keyed on the section id, so collapse survives both
 * recomposition and process-death. Trailing slot is hidden when collapsed
 * (the toggle was usually only meaningful with the section visible).
 */
@Composable
private fun CategoryHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (expanded && trailing != null) trailing()
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = EkhoIcons.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 30.dp, top = 2.dp),
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/**
 * Bell-screen row for a Canvas announcement. Mirrors the per-class detail
 * row's vocabulary (unread dot + course label + title + author·date caption)
 * but stays compact — no inline body expansion here. First click marks read.
 */
@Composable
private fun BellAnnouncementRow(
    announcement: CanvasAnnouncement,
    courseLabel: String?,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val zone = remember { ZoneId.systemDefault() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (announcement.isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
            if (!courseLabel.isNullOrBlank()) {
                Text(
                    text = courseLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = announcement.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (announcement.isUnread) FontWeight.SemiBold else FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        announcement.postedAt?.let { instant ->
            val dateText = remember(instant) {
                instant.atZone(zone).format(DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US))
            }
            Text(
                text = listOfNotNull(announcement.authorName, dateText).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PastBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "PAST",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PastToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Show past",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        // Default unchecked track sits on `surfaceContainerHighest` and disappears
        // against light surfaces — pin to outlineVariant so the affordance reads
        // as a toggle in both color schemes.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun CategoryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubsectionLabel(title: String, count: Int) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DeclinedToggle(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = EkhoIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                .rotate(if (expanded) 90f else 0f),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Declined ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FriendRequestRow(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = request.displayName.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.displayName.ifBlank { "Someone" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (request.major.isNotBlank()) {
                        Text(
                            text = request.major,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(EkhoIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(EkhoIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Deny")
                }
            }
        }
    }
}

@Composable
private fun InviteRow(
    event: CalendarEvent,
    onClick: () -> Unit,
    primaryAction: @Composable RowScope.() -> Unit,
    secondaryAction: (@Composable RowScope.() -> Unit)? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d  h:mm a") }
    val whenLabel = event.startTime.atZone(zone).format(dateFormatter)
    val isPast = event.isPast()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isPast) {
                    Spacer(Modifier.size(8.dp))
                    PastBadge()
                }
            }

            if (event.ownerDisplayName.isNotBlank()) {
                Text(
                    text = "Invited by ${event.ownerDisplayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Text(
                text = whenLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = EkhoIcons.Place,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                primaryAction()
                secondaryAction?.invoke(this)
            }
        }
    }
}
