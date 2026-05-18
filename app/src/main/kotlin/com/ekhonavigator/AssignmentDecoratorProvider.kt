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
import com.ekhonavigator.core.data.repository.UserCourseRepository
import com.ekhonavigator.core.designsystem.theme.AssignmentDecorator
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.LocalAssignmentDecorator
import com.ekhonavigator.core.model.CourseColorChoice
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
    userCourseRepository: UserCourseRepository,
) : ViewModel() {

    val decorator: StateFlow<AssignmentDecorator> = combine(
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
        // Folded into the same completed set as Canvas — strikethrough stays source-agnostic.
        calendarRepository.observeEvents(),
        userCourseRepository.observeCourses(),
    ) { courses, plannerItems, allEvents, userCourses ->
        if (courses.isEmpty() && plannerItems.isEmpty() &&
            allEvents.none { it.isCompleted } && userCourses.isEmpty()
        ) {
            return@combine AssignmentDecorator.Empty
        }

        val courseInputs = courses.map { CourseColorInput(id = it.id, code = it.code) }
        val canvasFamilySlots = CourseColorAssigner.familySlots(courseInputs)
        // Sort-by-code so lab + lecture siblings pick the same deterministic id;
        // groupBy + first prefers the alphabetically earliest section (lecture
        // before lab — "Sec 001" beats "Sec 01L").
        val familyKeyToCourseId = courseInputs
            .sortedBy { it.code }
            .groupBy { CourseColorAssigner.familyKey(it.code) }
            .mapValues { (_, group) -> group.first().id }

        // User-course store is the source of truth; Canvas sort-position is
        // the fallback for family-keys we haven't enrolled in the store yet.
        val activeUserCourses = userCourses.filterNot { it.archived }
        val userCourseSlots = activeUserCourses
            .mapNotNull { (it.colorChoice as? CourseColorChoice.Palette)?.let { p -> it.familyKey to p.slot } }
            .toMap()
        val familyKeyToCustomHex = activeUserCourses
            .mapNotNull { (it.colorChoice as? CourseColorChoice.Custom)?.let { c -> it.familyKey to c.hex } }
            .toMap()
        val familyKeyToSlot = canvasFamilySlots + userCourseSlots

        val itemsWithCourse = plannerItems.mapNotNull { item ->
            val courseId = item.courseId ?: return@mapNotNull null
            item to courseId
        }

        // Per-event slot: prefer the user-course choice (matched by family-key
        // of the Canvas course code), fall back to the Canvas sort-position slot.
        val canvasCourseFamilyKey = courses.associate { it.id to CourseColorAssigner.familyKey(it.code) }
        val courseColorSlotByEventId = itemsWithCourse
            .mapNotNull { (item, courseId) ->
                val familyKey = canvasCourseFamilyKey[courseId] ?: return@mapNotNull null
                val slot = familyKeyToSlot[familyKey] ?: return@mapNotNull null
                item.id to slot
            }
            .toMap()
        val courseColorHexByEventId = itemsWithCourse
            .mapNotNull { (item, courseId) ->
                val familyKey = canvasCourseFamilyKey[courseId] ?: return@mapNotNull null
                val hex = familyKeyToCustomHex[familyKey] ?: return@mapNotNull null
                item.id to hex
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
            familyKeyToCustomHex = familyKeyToCustomHex,
            courseColorHexByEventId = courseColorHexByEventId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssignmentDecorator.Empty,
    )
}
