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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.theme.coursePalette

private enum class AddCourseStep { Code, Color }

/**
 * Two-step modal — text in step 1, palette swatch in step 2 with [initialSlot]
 * pre-selected so a user who just wants the default can tap Save and be done.
 */
@Composable
fun AddCourseDialog(
    initialSlot: Int,
    onDismiss: () -> Unit,
    onSubmit: (code: String, slot: Int) -> Unit,
) {
    var step by remember { mutableStateOf(AddCourseStep.Code) }
    var code by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf(initialSlot) }
    val palette = coursePalette()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    AddCourseStep.Code -> "Add course"
                    AddCourseStep.Color -> "Pick a color"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            when (step) {
                AddCourseStep.Code -> CodeStep(value = code, onValueChange = { code = it })
                AddCourseStep.Color -> ColorStep(
                    palette = palette,
                    selectedSlot = slot,
                    courseCode = code,
                    onSlotSelected = { slot = it },
                )
            }
        },
        confirmButton = {
            when (step) {
                AddCourseStep.Code -> TextButton(
                    onClick = { step = AddCourseStep.Color },
                    enabled = code.isNotBlank(),
                ) { Text("Next") }
                AddCourseStep.Color -> TextButton(
                    onClick = { onSubmit(code, slot) },
                ) { Text("Save") }
            }
        },
        dismissButton = {
            if (step == AddCourseStep.Color) {
                TextButton(onClick = { step = AddCourseStep.Code }) { Text("Back") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun CodeStep(value: String, onValueChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("e.g. COMP-262") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Code becomes the family-key — COMP-262 covers all your sections.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorStep(
    palette: List<Color>,
    selectedSlot: Int,
    courseCode: String,
    onSlotSelected: (Int) -> Unit,
) {
    Column {
        Text(
            text = courseCode,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.getOrNull(selectedSlot) ?: MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            palette.forEachIndexed { index, color ->
                Swatch(
                    color = color,
                    selected = index == selectedSlot,
                    onClick = { onSlotSelected(index) },
                )
            }
        }
    }
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
