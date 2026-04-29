package com.ekhonavigator.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.icon.EkhoIcons

data class FriendPickerEntry(
    val uid: String,
    val displayName: String,
    val subtitle: String = "",
    val initiallySelected: Boolean = false,
    val locked: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendPickerSheet(
    friends: List<FriendPickerEntry>,
    onConfirm: (selected: Set<String>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Share with friends",
    actionLabel: String = "Share",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initial = remember(friends) {
        friends.filter { it.initiallySelected }.map { it.uid }.toSet()
    }
    var selected by rememberSaveable(initial) { mutableStateOf(initial) }
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(friends, query) {
        if (query.isBlank()) friends
        else friends.filter { it.displayName.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search friends") },
                leadingIcon = {
                    Icon(
                        imageVector = EkhoIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            if (filtered.isEmpty()) {
                EmptyFriendList(query)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.uid }) { friend ->
                        FriendRow(
                            friend = friend,
                            checked = friend.uid in selected,
                            onToggle = {
                                if (friend.locked) return@FriendRow
                                selected = if (friend.uid in selected) {
                                    selected - friend.uid
                                } else {
                                    selected + friend.uid
                                }
                            },
                        )
                    }
                }
            }

            // Enabled whenever the selection differs from initial — including the
            // edit-mode "remove everyone" path (initial = {A}, selected = ∅).
            Button(
                onClick = { onConfirm(selected) },
                enabled = selected != initial,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (selected.size > initial.size) {
                        "$actionLabel with ${selected.size - initial.size}"
                    } else {
                        actionLabel
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: FriendPickerEntry,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val containerColor = when {
        friend.locked -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        checked -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(enabled = !friend.locked, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(displayName = friend.displayName)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (friend.subtitle.isNotBlank()) {
                Text(
                    text = friend.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        SelectionPip(checked = checked, locked = friend.locked)
    }
}

@Composable
private fun Avatar(displayName: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.take(1).uppercase().ifBlank { "?" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SelectionPip(checked: Boolean, locked: Boolean) {
    val (background, content) = when {
        locked -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        checked -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (checked || locked) {
            Icon(
                imageVector = EkhoIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = content,
            )
        }
    }
}

@Composable
private fun EmptyFriendList(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (query.isBlank()) "No friends to share with" else "No matches for \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
