package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _eventId = MutableStateFlow("")

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val event: StateFlow<CalendarEvent?> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> repository.observeEventById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether the current user owns this event and can delete it. */
    val canDelete: Boolean
        get() {
            val event = event.value ?: return false
            val uid = authRepository.getCurrentUserUid() ?: return false
            return event.source == EventSource.USER_CREATED && event.ownerUid == uid
        }

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

    fun deleteEvent() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.deleteEvent(id)
                _navigateBack.emit(Unit)
            }
        }
    }
}
