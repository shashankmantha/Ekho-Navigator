package com.ekhonavigator.feature.canvas.courses

import com.ekhonavigator.core.canvas.model.CanvasCourse

sealed interface MyCoursesUiState {

    data object Loading : MyCoursesUiState

    data object Disconnected : MyCoursesUiState

    data class Loaded(
        val courses: List<CanvasCourse>,
        val syncing: Boolean = false,
        val error: String? = null,
    ) : MyCoursesUiState
}
