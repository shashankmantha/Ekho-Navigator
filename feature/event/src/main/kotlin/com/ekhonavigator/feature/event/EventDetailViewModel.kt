package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the single-event detail screen.
 *
 * The event ID comes from the navigation key (EventNavKey.id). Because
 * Navigation3's SavedStateHandle population is still evolving, we use an
 * explicit [setEventId] call from the Screen composable rather than
 * reading from SavedStateHandle. This keeps things reliable regardless
 * of Navigation3 internals.
 *
 * The [event] StateFlow uses flatMapLatest: once the ID is set, it
 * subscribes to Room's Flow for that specific event. If the event gets
 * updated (e.g. bookmark toggled, re-synced), the UI reflects it
 * automatically — no manual refresh needed.
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    private val _eventId = MutableStateFlow("")

    val event: StateFlow<CalendarEvent?> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> repository.observeEventById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Called by the Screen composable with the ID from EventNavKey.
     * Only needs to be set once — subsequent calls with the same ID are no-ops
     * since MutableStateFlow deduplicates equal values.
     */
    fun setEventId(id: String) {
        _eventId.value = id
    }

    fun toggleBookmark() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                repository.toggleBookmark(id)
            }
        }
    }
}
