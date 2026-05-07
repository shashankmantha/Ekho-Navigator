package com.ekhonavigator.feature.canvas.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.PlannerItem
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * A first-class "My Courses" section for the Campus tab. Renders a 2-column grid
 * of course cards using the same family-key palette mapping as the calendar pills
 * and FilterSheet course chips, so a course's identity reads consistently across
 * every surface.
 *
 * Self-gating via the LoadedEmpty state — if no Canvas courses are cached, the
 * caller can decide what to render (typically nothing, since the surrounding
 * Campus tab is itself gated on `LocalCanvasConnected`).
 */
@Composable
fun MyCoursesGrid(
    onCourseClick: (courseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyCoursesGridViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    if (state !is MyCoursesGridUiState.Loaded || state.cards.isEmpty()) return

    val palette = coursePalette()
    // Manual 2-column grid via row-pairs — avoids the nested-vertical-scroll
    // crash that LazyVerticalGrid hits when embedded in DiscoverScreen's
    // verticalScroll Column. Course count per term is small (≤10), so the
    // perf hit of skipping LazyVerticalGrid is negligible.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCards.forEach { card ->
                    CourseGridCard(
                        course = card.course,
                        courseColor = palette[card.paletteSlot % palette.size],
                        lastActivity = card.lastActivity,
                        onClick = { onCourseClick(card.course.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad the last row with an empty weight slot when odd count so
                // the lone card doesn't stretch full-width.
                if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CourseGridCard(
    course: CanvasCourse,
    courseColor: Color,
    lastActivity: LastActivity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(courseColor),
                )
                if (course.currentGrade != null || course.currentScore != null) {
                    GradeChip(grade = course.currentGrade, score = course.currentScore)
                }
            }

            Text(
                text = CourseColorAssigner.familyKey(course.code),
                style = MaterialTheme.typography.labelMedium,
                color = courseColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = course.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val term = course.termName
            if (!term.isNullOrBlank()) {
                Text(
                    text = term,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            ActivitySnippet(lastActivity = lastActivity)
        }
    }
}

@Composable
private fun GradeChip(grade: String?, score: Double?) {
    val text = grade ?: score?.let { "${it.toInt()}%" } ?: return
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ActivitySnippet(lastActivity: LastActivity?) {
    val (text, alpha) = if (lastActivity == null) {
        "No recent activity" to 0.55f
    } else {
        "${lastActivity.verb} ${lastActivity.title} · ${lastActivity.relativeTime}" to 0.85f
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@HiltViewModel
class MyCoursesGridViewModel @Inject constructor(
    @Suppress("unused") savedStateHandle: SavedStateHandle,
    courseRepository: CanvasCourseRepository,
    plannerRepository: CanvasPlannerRepository,
) : ViewModel() {

    val uiState: StateFlow<MyCoursesGridUiState> = combine(
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
    ) { courses, planners ->
        if (courses.isEmpty()) {
            MyCoursesGridUiState.Loaded(emptyList())
        } else {
            val slots = CourseColorAssigner.assign(
                courses.map { CourseColorInput(id = it.id, code = it.code) },
            )
            val today = LocalDate.now()
            val activityByCourse = lastActivityByCourseId(planners, today)
            MyCoursesGridUiState.Loaded(
                cards = courses.map { course ->
                    CourseCardData(
                        course = course,
                        paletteSlot = slots[course.id] ?: 0,
                        lastActivity = activityByCourse[course.id],
                    )
                },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyCoursesGridUiState.Loading,
    )
}

sealed interface MyCoursesGridUiState {
    data object Loading : MyCoursesGridUiState
    data class Loaded(val cards: List<CourseCardData>) : MyCoursesGridUiState
}

data class CourseCardData(
    val course: CanvasCourse,
    val paletteSlot: Int,
    val lastActivity: LastActivity?,
)

data class LastActivity(
    val verb: String,
    val title: String,
    val relativeTime: String,
)

/**
 * Picks the most recent (by `plannableDate`) submitted/graded/excused planner
 * item per course within the last 30 days. Renders as e.g. "Graded Lab 3 · 2d ago".
 */
private fun lastActivityByCourseId(
    items: List<PlannerItem>,
    today: LocalDate,
    horizonDays: Long = 30L,
): Map<String, LastActivity> {
    val cutoff = today.minusDays(horizonDays).atStartOfDay(ZoneId.systemDefault()).toInstant()
    return items
        .asSequence()
        .filter { it.courseId != null }
        .filter { it.submission.submitted || it.submission.graded || it.submission.excused }
        .filter { it.plannableDate >= cutoff }
        .groupBy { it.courseId!! }
        .mapValues { (_, list) -> list.maxBy { it.plannableDate } }
        .mapValues { (_, item) -> item.toActivity(today) }
}

private fun PlannerItem.toActivity(today: LocalDate): LastActivity {
    val verb = when {
        submission.graded -> "Graded"
        submission.excused -> "Excused"
        else -> "Submitted"
    }
    val date = plannableDate.atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(date, today).coerceAtLeast(0)
    val relative = when {
        days == 0L -> "today"
        days == 1L -> "yesterday"
        days < 7L -> "${days}d ago"
        days < 30L -> "${days / 7}w ago"
        else -> "${days}d ago"
    }
    return LastActivity(verb = verb, title = title, relativeTime = relative)
}
