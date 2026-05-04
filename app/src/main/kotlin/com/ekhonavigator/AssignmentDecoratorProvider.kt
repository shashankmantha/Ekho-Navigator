package com.ekhonavigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.designsystem.theme.AssignmentDecorator
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.LocalAssignmentDecorator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Provides the app-root [AssignmentDecorator] into composition.
 *
 * Combines the cached Canvas course list with all cached planner items into a single
 * lookup table — keeps every render site (calendar, day, mini-month, event row) free
 * of Canvas knowledge. When Canvas isn't connected both flows emit empty and the
 * decorator collapses to [AssignmentDecorator.Empty].
 */
@Composable
fun AssignmentDecoratorProvider(
    viewModel: AssignmentDecoratorViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val decorator by viewModel.decorator.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalAssignmentDecorator provides decorator) {
        content()
    }
}

@HiltViewModel
class AssignmentDecoratorViewModel @Inject constructor(
    @Suppress("unused") savedStateHandle: SavedStateHandle,
    courseRepository: CanvasCourseRepository,
    plannerItemDao: CanvasPlannerItemDao,
) : ViewModel() {

    val decorator: StateFlow<AssignmentDecorator> = combine(
        courseRepository.observeCourses(),
        plannerItemDao.observeAll(),
    ) { courses, plannerItems ->
        if (courses.isEmpty() && plannerItems.isEmpty()) {
            return@combine AssignmentDecorator.Empty
        }

        val courseInputs = courses.map { CourseColorInput(id = it.id, code = it.code) }
        val courseSlots = CourseColorAssigner.assign(courseInputs)
        val familyKeyToSlot = CourseColorAssigner.familySlots(courseInputs)

        val itemsWithCourse = plannerItems.mapNotNull { item ->
            val courseId = item.courseId ?: return@mapNotNull null
            item to courseId
        }

        val courseColorSlotByEventId = itemsWithCourse
            .mapNotNull { (item, courseId) ->
                val slot = courseSlots[courseId] ?: return@mapNotNull null
                item.id to slot
            }
            .toMap()

        val courseIdByEventId = itemsWithCourse.associate { (item, courseId) -> item.id to courseId }

        val completedEventIds = plannerItems
            .filter { it.submitted || it.graded || it.excused }
            .map { it.id }
            .toSet()

        AssignmentDecorator(
            courseColorSlotByEventId = courseColorSlotByEventId,
            completedEventIds = completedEventIds,
            courseIdByEventId = courseIdByEventId,
            familyKeyToSlot = familyKeyToSlot,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssignmentDecorator.Empty,
    )
}
