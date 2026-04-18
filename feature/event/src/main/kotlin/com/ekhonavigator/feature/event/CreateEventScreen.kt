package com.ekhonavigator.feature.event

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoMonogramBadge
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    initialEpochDay: Long? = null,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialEpochDay) {
        if (initialEpochDay != null) {
            viewModel.setDate(LocalDate.ofEpochDay(initialEpochDay))
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::setTitle,
            label = { RequiredLabel("Title") },
            placeholder = { Text("Study session, club meeting...") },
            singleLine = true,
            isError = uiState.titleError,
            supportingText = if (uiState.titleError) {
                { Text("Required") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        PickerField(
            value = uiState.date?.format(dateFormatter) ?: "",
            label = "Date",
            placeholder = "Tap to pick a date",
            onClick = { showDatePicker = true },
            isRequired = true,
            isError = uiState.dateError,
            errorText = "Required",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerField(
                value = uiState.startTime?.format(timeFormatter) ?: "",
                label = "Start",
                placeholder = "Start time",
                onClick = { showStartTimePicker = true },
                modifier = Modifier.weight(1f),
                isRequired = true,
                isError = uiState.startTimeError,
                errorText = "Required",
            )
            PickerField(
                value = uiState.endTime?.format(timeFormatter) ?: "",
                label = "End",
                placeholder = "End time",
                onClick = { showEndTimePicker = true },
                modifier = Modifier.weight(1f),
                isRequired = true,
                isError = uiState.endTimeError,
                errorText = "Required",
            )
        }

        OutlinedTextField(
            value = uiState.location,
            onValueChange = viewModel::setLocation,
            label = { Text("Location") },
            placeholder = { Text("Room, building, or address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::setDescription,
            label = { Text("Description") },
            placeholder = { Text("What's this event about?") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EventCategory.entries.forEach { category ->
                val isSelected = category == uiState.category

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    leadingIcon = {
                        EkhoMonogramBadge(
                            monogram = category.monogram,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        if (uiState.friends.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Share with Friends",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.friends.forEach { friend ->
                    val isSelected = friend.uid in uiState.selectedFriendUids

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleFriend(friend.uid) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = EkhoIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = EkhoIcons.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        label = {
                            Text(friend.displayName, style = MaterialTheme.typography.labelSmall)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = 0.3f
                            ),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (uiState.isSaving) "Saving..." else "Create Event")
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.setDate(date)
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setStartTime(LocalTime.of(hour, minute))
                showStartTimePicker = false
            },
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setEndTime(LocalTime.of(hour, minute))
                showEndTimePicker = false
            },
        )
    }
}

@Composable
private fun PickerField(
    value: String,
    label: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = {
                if (isRequired) RequiredLabel(label) else Text(label)
            },
            placeholder = { Text(placeholder) },
            readOnly = true,
            singleLine = true,
            enabled = false,
            isError = isError,
            supportingText = if (isError && errorText != null) {
                { Text(errorText) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun RequiredLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = errorColor)) {
                append(" *")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { TimePicker(state = state) },
    )
}
