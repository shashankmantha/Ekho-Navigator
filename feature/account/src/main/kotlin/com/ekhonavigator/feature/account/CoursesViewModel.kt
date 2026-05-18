package com.ekhonavigator.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.UserCourseRepository
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.normalizeCourseLabel
import com.ekhonavigator.core.model.CourseColorChoice
import com.ekhonavigator.core.model.UserCourse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Surfaces what the Add dialog needs to validate input live (cap, dup check). */
sealed interface AddCourseResult {
    data object Success : AddCourseResult
    data object EmptyCode : AddCourseResult
    data object Duplicate : AddCourseResult
    data object CapReached : AddCourseResult
}

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val userCourseRepository: UserCourseRepository,
) : ViewModel() {

    val courses: StateFlow<List<UserCourse>> = userCourseRepository.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastResult = MutableStateFlow<AddCourseResult?>(null)
    val lastResult: StateFlow<AddCourseResult?> = _lastResult.asStateFlow()

    fun consumeResult() {
        _lastResult.value = null
    }

    /**
     * Default color slot when the dialog opens — wraps so the Nth course
     * lands on slot `(N % 6)`. The user can pick any swatch before saving.
     */
    fun defaultSlotForNewCourse(): Int {
        val activeCount = courses.value.count { !it.archived }
        return activeCount % CoursePaletteSize
    }

    /** Returns whether the add succeeded; UI consumes via [lastResult] for the toast. */
    fun addCourse(rawCode: String, slot: Int) {
        val normalized = normalizeCourseLabel(rawCode)
        if (normalized.isNullOrBlank()) {
            _lastResult.value = AddCourseResult.EmptyCode
            return
        }
        val familyKey = CourseColorAssigner.familyKey(normalized)
        val active = courses.value.filterNot { it.archived }
        if (active.size >= MaxActiveCourses) {
            _lastResult.value = AddCourseResult.CapReached
            return
        }
        if (active.any { it.familyKey == familyKey }) {
            _lastResult.value = AddCourseResult.Duplicate
            return
        }
        viewModelScope.launch {
            userCourseRepository.upsert(
                UserCourse(
                    familyKey = familyKey,
                    code = normalized,
                    displayName = normalized,
                    colorChoice = CourseColorChoice.Palette(slot.coerceIn(0, CoursePaletteSize - 1)),
                    archived = false,
                )
            )
            _lastResult.value = AddCourseResult.Success
        }
    }

    /**
     * Updates the editable fields on an existing course. Doc-id stays put —
     * to "rename" a course code, the user archives + deletes + re-adds.
     */
    fun editCourse(familyKey: String, displayName: String, slot: Int) {
        val current = courses.value.firstOrNull { it.familyKey == familyKey } ?: return
        viewModelScope.launch {
            userCourseRepository.upsert(
                current.copy(
                    displayName = displayName.trim().ifBlank { current.code },
                    colorChoice = CourseColorChoice.Palette(slot.coerceIn(0, CoursePaletteSize - 1)),
                )
            )
        }
    }

    fun toggleArchive(familyKey: String) {
        val current = courses.value.firstOrNull { it.familyKey == familyKey } ?: return
        viewModelScope.launch {
            userCourseRepository.archive(familyKey, !current.archived)
        }
    }

    fun deleteCourse(familyKey: String) {
        viewModelScope.launch {
            userCourseRepository.delete(familyKey)
        }
    }

    companion object {
        const val MaxActiveCourses = 20
        const val CoursePaletteSize = 6
    }
}
