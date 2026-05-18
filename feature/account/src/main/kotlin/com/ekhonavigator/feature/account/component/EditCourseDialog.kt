package com.ekhonavigator.feature.account.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.theme.coursePalette
import com.ekhonavigator.core.model.CourseColorChoice
import com.ekhonavigator.core.model.UserCourse

/**
 * Single-step edit dialog — display name and color are editable in place.
 * Delete is gated behind archive so a user can't accidentally drop a course
 * with live events tagged to it.
 */
@Composable
fun EditCourseDialog(
    course: UserCourse,
    onDismiss: () -> Unit,
    onSave: (displayName: String, slot: Int) -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var displayName by remember(course.familyKey) { mutableStateOf(course.displayName) }
    val initialSlot = (course.colorChoice as? CourseColorChoice.Palette)?.slot ?: 0
    var slot by remember(course.familyKey) { mutableStateOf(initialSlot) }
    val palette = coursePalette()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = course.code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.getOrNull(slot) ?: MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    palette.forEachIndexed { index, color ->
                        Swatch(
                            color = color,
                            selected = index == slot,
                            onClick = { slot = index },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onArchiveToggle,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(if (course.archived) "Restore" else "Archive")
                    }
                    if (course.archived) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(displayName, slot) },
                enabled = displayName.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ringColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, ringColor, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
    )
}
