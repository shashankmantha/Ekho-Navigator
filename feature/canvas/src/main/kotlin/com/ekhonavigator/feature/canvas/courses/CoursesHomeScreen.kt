package com.ekhonavigator.feature.canvas.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.data.canvas.CanvasAnnouncementRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.coursePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@Composable
fun CoursesHomeContent(
    onCourseClick: (courseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CoursesHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        WeekOutlookCard(state = uiState.weekOutlook)

        Text(
            text = "My Courses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        MyCoursesGrid(onCourseClick = onCourseClick)
    }
}

@Composable
private fun WeekOutlookCard(state: WeekOutlookState) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "This week",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        when (state) {
            WeekOutlookState.Empty -> {
                Text(
                    text = "Nothing due this week — enjoy it.",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
            }
            is WeekOutlookState.Available -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${state.deadlineCount} ${if (state.deadlineCount == 1) "deadline" else "deadlines"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    if (state.coursePaletteSlots.isNotEmpty()) {
                        CourseColorStrip(slots = state.coursePaletteSlots)
                    }
                }
                val daysLabel = state.activeDays.joinToString(" · ") { dayShortLabel(it) }
                val pointsCaption = if (state.totalPoints > 0.0) {
                    val pointsText = if (state.totalPoints % 1.0 == 0.0) {
                        state.totalPoints.toInt().toString()
                    } else {
                        "%.1f".format(state.totalPoints)
                    }
                    "$pointsText points · $daysLabel"
                } else {
                    daysLabel
                }
                Text(
                    text = pointsCaption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CourseColorStrip(slots: List<Int>) {
    val palette = coursePalette()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        slots.forEach { slot ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette[slot % palette.size]),
            )
        }
    }
}

@HiltViewModel
class CoursesHomeViewModel @Inject constructor(
    private val courseRepository: CanvasCourseRepository,
    private val plannerRepository: CanvasPlannerRepository,
    private val announcementRepository: CanvasAnnouncementRepository,
) : ViewModel() {

    init {
        // Fan out announcement sync once per course on first VM creation —
        // otherwise the Courses home shows whatever the last opened course left
        // in cache. Dedupe set keeps later emissions from re-syncing.
        viewModelScope.launch {
            val seen = mutableSetOf<String>()
            courseRepository.observeCourses().collect { courses ->
                val newOnes = courses.filter { it.id !in seen }
                if (newOnes.isEmpty()) return@collect
                seen += newOnes.map { it.id }
                newOnes.forEach { course ->
                    launch { runCatching { announcementRepository.sync(course.id) } }
                }
            }
        }
    }

    val uiState: StateFlow<CoursesHomeUiState> = combine(
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
    ) { courses, planners ->
        CoursesHomeUiState(
            weekOutlook = computeWeekOutlook(planners, courses),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoursesHomeUiState(
            weekOutlook = WeekOutlookState.Empty,
        ),
    )
}

data class CoursesHomeUiState(
    val weekOutlook: WeekOutlookState,
)

sealed interface WeekOutlookState {
    data object Empty : WeekOutlookState
    data class Available(
        val deadlineCount: Int,
        val totalPoints: Double,
        // Sorted distinct days the deadlines fall on — UI renders as "Mon · Tue · Fri"
        // so the deadlineCount math is verifiable at a glance.
        val activeDays: List<DayOfWeek>,
        // Course palette slots for courses with at least one deadline this week,
        // sorted for stable left-to-right rendering.
        val coursePaletteSlots: List<Int>,
    ) : WeekOutlookState
}

// Excludes non-assignment/quiz plannables so the count matches what the
// calendar grid actually renders. Filters submitted/graded/excused items, and
// dedupes by id since paginated planner responses sometimes repeat.
internal fun computeWeekOutlook(
    items: List<PlannerItem>,
    courses: List<com.ekhonavigator.core.canvas.model.CanvasCourse>,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
): WeekOutlookState {
    val today = now.atZone(zone).toLocalDate()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusWeeks(1) // exclusive
    val startInstant = weekStart.atStartOfDay(zone).toInstant()
    val endInstant = weekEnd.atStartOfDay(zone).toInstant()

    val inWindow = items
        .asSequence()
        .filter { it.kind in DEADLINE_KINDS }
        .filter { !it.submission.engagedForOutlook }
        .filter { item ->
            val anchor = item.dueAt ?: item.plannableDate
            !anchor.isBefore(startInstant) && anchor.isBefore(endInstant)
        }
        .distinctBy { it.id }
        .toList()
    if (inWindow.isEmpty()) return WeekOutlookState.Empty

    val totalPoints = inWindow.sumOf { it.pointsPossible ?: 0.0 }
    val activeDays = inWindow
        .map { (it.dueAt ?: it.plannableDate).atZone(zone).dayOfWeek }
        .toSortedSet()
        .toList()

    // Assign against the FULL course list so a slot stays stable week to week.
    val slotByCourseId = CourseColorAssigner.assign(
        courses.map { CourseColorInput(id = it.id, code = it.code) },
    )
    val courseIdsWithDeadlines = inWindow.mapNotNull { it.courseId }.toSet()
    val coursePaletteSlots = courseIdsWithDeadlines
        .mapNotNull { slotByCourseId[it] }
        .toSortedSet()
        .toList()

    return WeekOutlookState.Available(
        deadlineCount = inWindow.size,
        totalPoints = totalPoints,
        activeDays = activeDays,
        coursePaletteSlots = coursePaletteSlots,
    )
}

private val DEADLINE_KINDS = setOf(
    com.ekhonavigator.core.canvas.model.PlannerKind.ASSIGNMENT,
    com.ekhonavigator.core.canvas.model.PlannerKind.QUIZ,
)

// Locale.US so the abbreviation stays stable across device languages.
private fun dayShortLabel(day: DayOfWeek): String =
    day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)


private val com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus.engagedForOutlook: Boolean
    get() = submitted || graded || excused
