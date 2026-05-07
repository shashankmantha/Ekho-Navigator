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

/**
 * The Courses tab "home base" — week outlook + recent announcements + the
 * existing My Courses grid stacked into a single scrolling column.
 *
 * Replaces the previous bare `MyCoursesGrid`-only layout in DiscoverScreen.
 * Lives in feature/canvas because every section pulls from Canvas-side data;
 * DiscoverScreen now just calls this composable for the COURSES tab.
 */
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

        // Announcements moved to the bell screen (InvitesScreen) — they were
        // duplicating content here and the bell wasn't pulling its weight.
        // Background sync from this VM still fires so the bell badge stays
        // fresh whenever the user lands on Courses.

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

/**
 * Tiny color-dot row keyed off the same family-key palette the My Courses
 * grid + calendar pills use. One dot per course that contributes a deadline
 * this week — color-couples the outlook visually to the course set below.
 */
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

// ────────────────────────────────────────────────────────────────────────────
// ViewModel + state
// ────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CoursesHomeViewModel @Inject constructor(
    private val courseRepository: CanvasCourseRepository,
    private val plannerRepository: CanvasPlannerRepository,
    private val announcementRepository: CanvasAnnouncementRepository,
) : ViewModel() {

    init {
        // Background fan-out: announcement sync is otherwise per-course-detail
        // open, which means the Campus tab home would render whatever was in
        // cache from the LAST course the user opened — usually stale. Fire one
        // sync per active course on first VM creation; subsequent course-list
        // emissions don't re-fan-out (the dedupe set already saw them).
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
        /** Sorted distinct days the deadlines fall on. UI renders these as
         *  short day-of-week labels ("Mon · Tue · Fri") so the math behind
         *  `deadlineCount` is verifiable at a glance — fixes the "wait, where
         *  did that 4th day come from" debugging trap. */
        val activeDays: List<DayOfWeek>,
        /** Course palette slots for every course that contributed at least one
         *  deadline this week. Render as small color dots so the outlook visually
         *  ties back to the My Courses grid below — at a glance you see which
         *  courses are loading you up. Sorted by paletteSlot for a stable
         *  left-to-right order. */
        val coursePaletteSlots: List<Int>,
    ) : WeekOutlookState
}

/**
 * Counts deadlines + sums points for items due in the current Mon→Sun window
 * (local zone). Excludes:
 *  - non-assignment/quiz plannables (announcements, calendar events,
 *    planner notes, discussions, wiki pages) — students think of those as
 *    "things to read", not deadlines, and the calendar grid only renders
 *    assignment + quiz pills anyway, so their inclusion makes the outlook
 *    count diverge visibly from what the user sees on the calendar.
 *  - items the user has already engaged with (forward-looking only)
 *  - dupes by id (Canvas occasionally returns the same plannable twice
 *    across paginated planner responses)
 */
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

    // Build palette-slot list for the contributing courses, using the same
    // family-key assigner the calendar pills + My Courses grid use so the dots
    // here always match the colors below. Computed against the FULL course
    // list so a course's slot is stable regardless of which weeks it appears in.
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

/** Three-letter weekday label for the week-outlook caption. Locale.US so the
 *  abbreviation stays stable regardless of the device's display language —
 *  matches the rest of the app's date formatting. */
private fun dayShortLabel(day: DayOfWeek): String =
    day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)


private val com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus.engagedForOutlook: Boolean
    get() = submitted || graded || excused
