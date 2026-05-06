package com.ekhonavigator.feature.event

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.component.EkhoMonogramBadge
import com.ekhonavigator.core.designsystem.component.EkhoSegmentedTabs
import com.ekhonavigator.core.designsystem.component.FriendPickerEntry
import com.ekhonavigator.core.designsystem.component.FriendPickerSheet
import com.ekhonavigator.core.designsystem.component.LocationAutocompleteField
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    initialEpochDay: Long? = null,
    eventId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationSuggestions by viewModel.locationSuggestions.collectAsStateWithLifecycle()
    val courseSuggestions by viewModel.courseSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) {
        if (eventId != null) viewModel.setEventId(eventId)
    }

    // initialEpochDay is a create-mode shortcut from "tap a calendar day to create"; it would
    // overwrite the loaded event's date in edit mode, so suppress it when editing.
    LaunchedEffect(initialEpochDay, eventId) {
        if (eventId == null && initialEpochDay != null) {
            viewModel.setDate(LocalDate.ofEpochDay(initialEpochDay))
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showFriendPicker by remember { mutableStateOf(false) }

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

        TypeSelector(
            selected = uiState.type,
            onSelected = viewModel::setType,
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

        if (uiState.type == EventType.ASSIGNMENT) {
            // Single "Due" picker — assignments are a moment, not a span. Internally
            // the save path collapses endTime = startTime so the entity stays valid
            // and the existing bottom-anchored ASSIGNMENT pill renderer still works.
            PickerField(
                value = uiState.startTime?.format(timeFormatter) ?: "",
                label = "Due",
                placeholder = "Due time",
                onClick = { showStartTimePicker = true },
                isRequired = true,
                isError = uiState.startTimeError,
                errorText = "Required",
            )
        } else {
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
                    isError = uiState.endTimeError || uiState.endBeforeStart,
                    errorText = when {
                        uiState.endTimeError -> "Required"
                        uiState.endBeforeStart -> "End must be after start"
                        else -> null
                    },
                )
            }
        }

        if (uiState.startsInPast) {
            PastDateWarning()
        }

        LocationAutocompleteField(
            value = uiState.location,
            onValueChange = viewModel::setLocationText,
            onSuggestionSelected = viewModel::selectLocationSuggestion,
            onCustomLocation = viewModel::useCustomLocationText,
            suggestions = locationSuggestions,
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

        if (uiState.friends.isNotEmpty()) {
            ShareWithFriendsButton(
                selectedCount = uiState.selectedFriendUids.size,
                onClick = { showFriendPicker = true },
            )
        }

        // Categories don't make sense for assignments — General has no contextual
        // meaning on a homework or test. Skipped entirely; contextual category
        // sets per type (test/quiz/etc) is parked for next sprint.
        if (uiState.type != EventType.ASSIGNMENT) {
            CategoryDropdown(
                selected = uiState.category,
                onSelected = viewModel::setCategory,
            )
        }

        CourseField(
            value = uiState.courseLabel,
            suggestions = courseSuggestions,
            onValueChange = viewModel::setCourseLabel,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                when {
                    uiState.isSaving -> "Saving..."
                    uiState.editingEventId != null -> "Save Changes"
                    else -> "Create Event"
                },
            )
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

    if (showFriendPicker) {
        val entries = remember(uiState.friends, uiState.selectedFriendUids) {
            uiState.friends.map { friend ->
                FriendPickerEntry(
                    uid = friend.uid,
                    displayName = friend.displayName,
                    subtitle = friend.major,
                    initiallySelected = friend.uid in uiState.selectedFriendUids,
                )
            }
        }
        FriendPickerSheet(
            friends = entries,
            onDismiss = { showFriendPicker = false },
            onConfirm = { newSelection ->
                viewModel.setSelectedFriends(newSelection)
                showFriendPicker = false
            },
            actionLabel = "Add",
        )
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
private fun ShareWithFriendsButton(
    selectedCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = EkhoIcons.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Share with friends",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (selectedCount == 0) {
                        "Optional — invite friends to RSVP"
                    } else if (selectedCount == 1) {
                        "1 friend selected"
                    } else {
                        "$selectedCount friends selected"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = EkhoIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: EventCategory,
    onSelected: (EventCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = {
                EkhoMonogramBadge(
                    monogram = selected.monogram,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            EventCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    leadingIcon = {
                        EkhoMonogramBadge(
                            monogram = category.monogram,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TypeSelector(
    selected: EventType,
    onSelected: (EventType) -> Unit,
) {
    // Limited to the two types the create form supports — EVENT and ASSIGNMENT.
    // Other EventType values (CANVAS imports, etc.) come from sync, never from
    // user creation, so they're intentionally not surfaced as selectable options.
    val options = listOf(
        EventType.EVENT to "Event",
        EventType.ASSIGNMENT to "Assignment",
    )
    EkhoSegmentedTabs(
        items = options,
        selected = options.first { it.first == selected },
        onSelect = { (type, _) -> onSelected(type) },
        labelOf = { it.second },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseField(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Filter suggestions by case-insensitive substring on the typed value, so
    // "comp" matches "COMP-262", "COMP-360", etc. Hide menu when no matches
    // (still accepts free-text — the OutlinedTextField is always editable).
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions
        else suggestions.filter { it.contains(value, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text("Course (optional)") },
            placeholder = { Text("e.g. COMP-262") },
            singleLine = true,
            trailingIcon = if (suggestions.isNotEmpty()) {
                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            } else {
                null
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                filteredSuggestions.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(code) },
                        onClick = {
                            onValueChange(code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PastDateWarning() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = EkhoIcons.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "This event is in the past — save anyway?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
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
