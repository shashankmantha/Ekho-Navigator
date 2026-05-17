package com.ekhonavigator.feature.event

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.designsystem.theme.normalizeCourseLabel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RecurrenceRule
import com.ekhonavigator.core.network.ICalImportSource
import com.ekhonavigator.core.network.model.StagedImportedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

sealed interface ImportEventsUiState {
    data object Idle : ImportEventsUiState
    data object Parsing : ImportEventsUiState
    data class Preview(
        val staged: List<StagedImportedEvent>,
        val excludedUids: Set<String> = emptySet(),
        val isImporting: Boolean = false,
    ) : ImportEventsUiState {
        val selectedCount: Int get() = staged.count { it.sourceUid !in excludedUids }
    }
    data class Empty(val message: String) : ImportEventsUiState
    data class Imported(val count: Int) : ImportEventsUiState
}

@HiltViewModel
class ImportEventsViewModel @Inject constructor(
    private val parser: ICalImportSource,
    @ApplicationContext private val context: Context,
    private val customEventRepository: CustomEventRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val contentResolver get() = context.contentResolver

    private val _uiState = MutableStateFlow<ImportEventsUiState>(ImportEventsUiState.Idle)
    val uiState: StateFlow<ImportEventsUiState> = _uiState.asStateFlow()

    fun parseFile(uri: Uri) {
        _uiState.value = ImportEventsUiState.Parsing
        viewModelScope.launch {
            val staged = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use(parser::parse) ?: emptyList()
                }.getOrDefault(emptyList())
            }
            _uiState.value = if (staged.isEmpty()) {
                ImportEventsUiState.Empty("Couldn't read events from that file. Make sure it's a valid .ics export.")
            } else {
                ImportEventsUiState.Preview(staged = staged.sortedBy { it.startTime })
            }
        }
    }

    fun toggleExclusion(uid: String) {
        _uiState.update { state ->
            if (state !is ImportEventsUiState.Preview) return@update state
            state.copy(
                excludedUids = if (uid in state.excludedUids) state.excludedUids - uid else state.excludedUids + uid,
            )
        }
    }

    fun excludeAll() = _uiState.update { state ->
        if (state !is ImportEventsUiState.Preview) state
        else state.copy(excludedUids = state.staged.map { it.sourceUid }.toSet())
    }

    fun includeAll() = _uiState.update { state ->
        if (state !is ImportEventsUiState.Preview) state
        else state.copy(excludedUids = emptySet())
    }

    fun commitImport() {
        val state = _uiState.value as? ImportEventsUiState.Preview ?: return
        val ownerUid = authRepository.getCurrentUserUid() ?: return
        val ownerName = authRepository.getCurrentUserDisplayName().orEmpty()
        val toImport = state.staged.filter { it.sourceUid !in state.excludedUids }
        if (toImport.isEmpty()) return

        _uiState.value = state.copy(isImporting = true)
        viewModelScope.launch {
            val events = toImport.map { it.toCalendarEvent(ownerUid, ownerName) }
            for (event in events) {
                runCatching { customEventRepository.createEvent(event, sharedWith = emptyMap()) }
            }
            _uiState.value = ImportEventsUiState.Imported(events.size)
        }
    }

    fun reset() {
        _uiState.value = ImportEventsUiState.Idle
    }
}

private fun StagedImportedEvent.toCalendarEvent(ownerUid: String, ownerName: String): CalendarEvent {
    val course = normalizeCourseLabel(inferredCourseLabel.orEmpty())
    // Local val — cross-module property can't smart-cast through the null check.
    val endDate = recurrenceEndDate
    val recurrence = if (recurrenceDays.isNotEmpty() && endDate != null) {
        RecurrenceRule(recurrenceDays, endDate)
    } else null
    // Match the Create form's inference rule: a labeled recurring event is a
    // class meeting; everything else stays a generic EVENT.
    val type = when {
        recurrence != null && !course.isNullOrBlank() -> EventType.CLASS_MEETING
        else -> EventType.EVENT
    }
    return CalendarEvent(
        id = "",
        title = title,
        description = description,
        location = location,
        startTime = startTime,
        endTime = endTime,
        categories = listOf(EventCategory.GENERAL),
        url = "",
        status = "CONFIRMED",
        isBookmarked = true,
        lastSyncedAt = Instant.now(),
        source = EventSource.USER_CREATED,
        ownerUid = ownerUid,
        ownerDisplayName = ownerName,
        type = type,
        courseLabel = course,
        recurrence = recurrence,
    )
}
