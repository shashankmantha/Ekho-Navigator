package com.ekhonavigator.feature.canvas.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyCoursesViewModel @Inject constructor(
    private val repository: CanvasCourseRepository,
    private val accountSource: CanvasAccountSource,
) : ViewModel() {

    private val _syncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyCoursesUiState> = combine(
        repository.observeCourses(),
        _syncing,
        _error,
    ) { courses, syncing, error ->
        if (accountSource.currentOrNull() == null) {
            MyCoursesUiState.Disconnected
        } else {
            MyCoursesUiState.Loaded(courses = courses, syncing = syncing, error = error)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyCoursesUiState.Loading,
    )

    init {
        refresh()
    }

    fun refresh() {
        if (accountSource.currentOrNull() == null) return
        viewModelScope.launch {
            _syncing.value = true
            _error.value = null
            repository.sync().onFailure { _error.value = it.message ?: "Couldn't sync courses." }
            _syncing.value = false
        }
    }
}
