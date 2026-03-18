package com.ekhonavigator.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    /** When true (default), show every event. When false, only bookmarked. */
    private val _showAll = MutableStateFlow(true)
    val showAll: StateFlow<Boolean> = _showAll.asStateFlow()

    /** All events, optionally filtered to bookmarked-only. */
    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _showAll,
    ) { allEvents, showAll ->
        if (showAll) allEvents else allEvents.filter { it.isBookmarked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleShowAll() {
        _showAll.value = !_showAll.value
    }
}
