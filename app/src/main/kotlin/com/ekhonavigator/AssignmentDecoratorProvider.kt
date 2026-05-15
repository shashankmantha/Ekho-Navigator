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
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
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

// Keeps render sites (calendar, day, mini-month, event row) free of Canvas knowledge.
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
    plannerRepository: CanvasPlannerRepository,
    calendarRepository: CalendarRepository,
) : ViewModel() {

    val decorator: StateFlow<AssignmentDecorator> = combine(
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
        // Folded into the same completed set as Canvas — strikethrough stays source-agnostic.
        calendarRepository.observeEvents(),
    ) { courses, plannerItems, allEvents ->
        if (courses.isEmpty() && plannerItems.isEmpty() && allEvents.none { it.isCompleted }) {
            return@combine AssignmentDecorator.Empty
        }

        val courseInputs = courses.map { CourseColorInput(id = it.id, code = it.code) }
        val courseSlots = CourseColorAssigner.assign(courseInputs)
        val familyKeyToSlot = CourseColorAssigner.familySlots(courseInputs)
        // Sort-by-code so lab + lecture siblings pick the same deterministic id;
        // groupBy + first prefers the alphabetically earliest section (lecture
        // before lab — "Sec 001" beats "Sec 01L").
        val familyKeyToCourseId = courseInputs
            .sortedBy { it.code }
            .groupBy { CourseColorAssigner.familyKey(it.code) }
            .mapValues { (_, group) -> group.first().id }

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

        val canvasCompletedIds = plannerItems
            .filter { it.submission.submitted || it.submission.graded || it.submission.excused }
            .map { it.id }
        val personalCompletedIds = allEvents
            .filter { it.isCompleted }
            .map { it.id }
        val completedEventIds = (canvasCompletedIds + personalCompletedIds).toSet()

        AssignmentDecorator(
            courseColorSlotByEventId = courseColorSlotByEventId,
            completedEventIds = completedEventIds,
            courseIdByEventId = courseIdByEventId,
            familyKeyToSlot = familyKeyToSlot,
            familyKeyToCourseId = familyKeyToCourseId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssignmentDecorator.Empty,
    )
}
