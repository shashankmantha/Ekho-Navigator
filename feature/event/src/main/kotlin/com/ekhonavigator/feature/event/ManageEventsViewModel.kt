package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// Mass delete is only safe for user-owned rows — iCal feed events would just
// re-sync and bridged Canvas rows are managed by the planner repo.
enum class ManageEventBucket(val displayName: String) {
    EVENTS("Events"),
    ASSIGNMENTS("Assignments"),
    RECURRING("Recurring"),
}

enum class ManageDateRange(val displayName: String) {
    UPCOMING("Upcoming"),
    PAST("Past"),
    ALL("All time"),
}

data class ManageEventsUiState(
    val activeBuckets: Set<ManageEventBucket> = ManageEventBucket.entries.toSet(),
    val dateRange: ManageDateRange = ManageDateRange.ALL,
    val selectedIds: Set<String> = emptySet(),
    val isDeleting: Boolean = false,
)

@HiltViewModel
class ManageEventsViewModel @Inject constructor(
    calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageEventsUiState())
    val uiState: StateFlow<ManageEventsUiState> = _uiState.asStateFlow()

    // Observes the full user-owned event set and re-filters whenever state changes.
    // observeEvents() returns seed rows (not expanded instances) — exactly what
    // we want for delete: one row per series.
    val filteredEvents: StateFlow<List<CalendarEvent>> =
        combine(calendarRepository.observeEvents(), _uiState) { events, state ->
            val now = Instant.now()
            events.asSequence()
                .filter { it.source == EventSource.USER_CREATED || it.source == EventSource.SHARED }
                .filter { it.matchesBucket(state.activeBuckets) }
                .filter { it.matchesDateRange(state.dateRange, now) }
                .sortedByDescending { it.startTime }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleBucket(bucket: ManageEventBucket) {
        _uiState.update { state ->
            val next = if (bucket in state.activeBuckets) state.activeBuckets - bucket else state.activeBuckets + bucket
            // Selection survives filter changes only for ids still in view; the
            // screen reconciles the rest at collect time.
            state.copy(activeBuckets = next)
        }
    }

    fun setDateRange(range: ManageDateRange) = _uiState.update { it.copy(dateRange = range) }

    fun toggleSelection(id: String) = _uiState.update { state ->
        state.copy(
            selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id,
        )
    }

    fun selectAllVisible() = _uiState.update {
        it.copy(selectedIds = filteredEvents.value.map { event -> event.id }.toSet())
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            for (id in ids) {
                runCatching { customEventRepository.deleteEvent(id) }
            }
            _uiState.update { it.copy(selectedIds = emptySet(), isDeleting = false) }
        }
    }

    // Deletes every row matching the current filter — used by the "Delete all"
    // confirmation. Counts come from the snapshotted filteredEvents.
    fun deleteAllFiltered() {
        val all = filteredEvents.value
        if (all.isEmpty()) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            for (event in all) {
                runCatching { customEventRepository.deleteEvent(event.id) }
            }
            _uiState.update { it.copy(selectedIds = emptySet(), isDeleting = false) }
        }
    }
}

private fun CalendarEvent.matchesBucket(buckets: Set<ManageEventBucket>): Boolean {
    if (buckets.isEmpty()) return false
    val bucket = when {
        recurrence != null -> ManageEventBucket.RECURRING
        type == EventType.ASSIGNMENT -> ManageEventBucket.ASSIGNMENTS
        else -> ManageEventBucket.EVENTS
    }
    return bucket in buckets
}

private fun CalendarEvent.matchesDateRange(range: ManageDateRange, now: Instant): Boolean = when (range) {
    ManageDateRange.UPCOMING -> endTime >= now
    ManageDateRange.PAST -> endTime < now
    ManageDateRange.ALL -> true
}

