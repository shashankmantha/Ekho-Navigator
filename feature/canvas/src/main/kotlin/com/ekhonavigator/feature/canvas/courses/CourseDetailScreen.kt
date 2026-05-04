package com.ekhonavigator.feature.canvas.courses

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.designsystem.component.EkhoEventRow
import com.ekhonavigator.core.designsystem.component.EkhoEventRowState
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.coursePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Per-class detail screen. **No Scaffold / TopAppBar** — the app-level nav
 * scaffold provides the contextual back arrow for any non-top-level destination
 * (mirrors the pattern in EventScreen, every other detail screen). Same reason
 * we don't manage system bars here — `EkhoNavigatorApp` handles edge-to-edge
 * insets globally.
 */
@Composable
fun CourseDetailScreen(
    courseId: String,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CourseDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(courseId) { viewModel.setCourseId(courseId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        CourseDetailUiState.Loading -> CenteredSpinner(modifier = modifier)
        CourseDetailUiState.NotFound -> NotFoundState(modifier = modifier)
        is CourseDetailUiState.Loaded -> {
            // Resolve the slot index against the active palette here — the
            // ViewModel can't call the @Composable coursePalette() hook,
            // and the palette lists are `internal` to the design system,
            // so the only place the resolution can live is at the render
            // boundary.
            val palette = coursePalette()
            val courseColor = palette[state.paletteSlot % palette.size]
            LoadedContent(
                state = state,
                courseColor = courseColor,
                onEventClick = onEventClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CenteredSpinner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotFoundState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Course not found in your active courses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadedContent(
    state: CourseDetailUiState.Loaded,
    courseColor: Color,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val course = state.course
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HeroSection(course = course, courseColor = courseColor)

        val courseHtmlUrl = course.htmlUrl
        if (!courseHtmlUrl.isNullOrBlank()) {
            OpenInCanvasButton(url = courseHtmlUrl)
        }

        WhatIfSection(state = state.whatIf)

        if (state.upcoming.isNotEmpty()) {
            PlannerItemSection(
                title = "Upcoming",
                items = state.upcoming,
                onItemClick = onEventClick,
            )
        }

        if (state.recentSubmissions.isNotEmpty()) {
            PlannerItemSection(
                title = "Recent submissions",
                items = state.recentSubmissions,
                onItemClick = onEventClick,
            )
        }

        // A2.3/A2.4: Past assignments + grades + Announcements land below.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Past assignments + announcements coming next.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PlannerItemSection(
    title: String,
    items: List<PlannerItem>,
    onItemClick: (eventId: String) -> Unit,
) {
    val zone = remember { java.time.ZoneId.systemDefault() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(title)
        items.forEach { item ->
            // Reuse EkhoEventRow so the row gets the same family-key palette,
            // strikethrough wiring (LocalAssignmentDecorator), and pending-invite
            // border behavior every other row in the app uses. The `eventId`
            // matches the bridged calendar_events.uid so the decorator's
            // courseColorFor() / isCompleted() lookups resolve.
            val instant = item.dueAt ?: item.plannableDate
            EkhoEventRow(
                title = item.title,
                startTime = instant,
                endTime = instant,
                zone = zone,
                location = "",
                monograms = emptyList(),
                state = EkhoEventRowState.ASSIGNMENT,
                onClick = { onItemClick(item.id) },
                onBookmarkClick = { /* assignments aren't bookmarkable */ },
                eventId = item.id,
            )
        }
    }
}

@Composable
private fun WhatIfSection(state: WhatIfState) {
    if (state == WhatIfState.Unavailable) {
        // No grade or no points to project from — skip the section entirely
        // rather than render an empty stub. Most common cause: instructor
        // hasn't released grades yet, so currentScore on the course is null.
        return
    }
    val loaded = state as WhatIfState.Available
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("What-if calculator")
        Text(
            text = "Slide to assume an average score on the ${formatPoints(loaded.remainingPoints)} pts " +
                "you have remaining — projected final updates live.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Estimated from your current ${"%.1f".format(loaded.currentPercent)}% — " +
                "Canvas weighting can shift the actual result.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Slider lives in the screen so we can keep WhatIfState pure-data;
        // the slider's onValueChange computes the projection on the fly.
        WhatIfSlider(state = loaded)
    }
}

@Composable
private fun WhatIfSlider(state: WhatIfState.Available) {
    var sliderValue by remember(state.assumedRemainingPercent) {
        mutableFloatStateOf(state.assumedRemainingPercent)
    }
    androidx.compose.material3.Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        valueRange = 0f..100f,
        steps = 19,  // 5-pt increments — fine enough for projection, coarse enough to feel intentional
    )
    val projection = projectFinal(
        earnedPoints = state.earnedPoints,
        gradedPoints = state.gradedPoints,
        remainingPoints = state.remainingPoints,
        assumedRemainingPercent = sliderValue,
    )
    Text(
        text = "Projected final: ${"%.1f".format(projection)}%",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

private fun formatPoints(points: Double): String =
    if (points % 1.0 == 0.0) points.toInt().toString() else "%.1f".format(points)

private fun projectFinal(
    earnedPoints: Double,
    gradedPoints: Double,
    remainingPoints: Double,
    assumedRemainingPercent: Float,
): Double {
    val totalPoints = gradedPoints + remainingPoints
    if (totalPoints == 0.0) return 0.0
    val projectedRemaining = remainingPoints * (assumedRemainingPercent / 100.0)
    return ((earnedPoints + projectedRemaining) / totalPoints) * 100.0
}

@Composable
private fun HeroSection(course: CanvasCourse, courseColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(courseColor),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = CourseColorAssigner.familyKey(course.code),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = courseColor,
                )
            }
            if (course.currentGrade != null || course.currentScore != null) {
                GradePill(grade = course.currentGrade, score = course.currentScore)
            }
        }

        Text(
            text = course.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        val term = course.termName
        if (!term.isNullOrBlank()) {
            Text(
                text = term,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GradePill(grade: String?, score: Double?) {
    val text = grade ?: score?.let { "${it.toInt()}%" } ?: return
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun OpenInCanvasButton(url: String) {
    val isLaunchable = url.startsWith("http://", ignoreCase = true) ||
        url.startsWith("https://", ignoreCase = true)
    if (!isLaunchable) return

    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            // runCatching defends against odd device configurations (no browser,
            // no Custom Tabs provider). Same pattern as EventScreen's button.
            runCatching {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, url.toUri())
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            imageVector = EkhoIcons.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Open in Canvas",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    @Suppress("unused") savedStateHandle: SavedStateHandle,
    private val courseRepository: CanvasCourseRepository,
    private val plannerRepository: CanvasPlannerRepository,
) : ViewModel() {

    private val _courseId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun setCourseId(id: String) {
        _courseId.value = id
    }

    val uiState: StateFlow<CourseDetailUiState> = combine(
        _courseId,
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
    ) { id, courses, planners ->
        if (id == null) return@combine CourseDetailUiState.Loading
        val course = courses.firstOrNull { it.id == id }
            ?: return@combine CourseDetailUiState.NotFound
        // Compute palette slot against the FULL active-course set so the color
        // matches every other surface (calendar pills, My Courses grid, filter
        // chips). Slot index is what travels — actual Color resolution happens
        // in the Composable layer where coursePalette() can be called.
        val slots = CourseColorAssigner.assign(
            courses.map { CourseColorInput(id = it.id, code = it.code) },
        )
        val courseItems = planners.filter { it.courseId == id }
        CourseDetailUiState.Loaded(
            course = course,
            paletteSlot = slots[id] ?: 0,
            upcoming = pickUpcoming(courseItems),
            recentSubmissions = pickRecentSubmissions(courseItems),
            whatIf = computeWhatIf(course = course, items = courseItems),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourseDetailUiState.Loading,
    )
}

/** Future, not-yet-engaged work for this course. Top 5 by ascending due date —
 *  if the user has more than that we trust they'll go to the calendar for the
 *  full picture; the per-class screen optimizes for "what's next" at a glance. */
private fun pickUpcoming(items: List<PlannerItem>, now: java.time.Instant = java.time.Instant.now()): List<PlannerItem> =
    items
        .asSequence()
        .filter {
            val anchor = it.dueAt ?: it.plannableDate
            anchor.isAfter(now) && !it.submission.engaged
        }
        .sortedBy { it.dueAt ?: it.plannableDate }
        .take(5)
        .toList()

/** Anything submitted, graded, or excused — sorted most-recent first. Same
 *  top-5 cap as upcoming; the future "Past assignments + grades" section in
 *  A2.3 will surface the full graded history with per-item scores. */
private fun pickRecentSubmissions(items: List<PlannerItem>): List<PlannerItem> =
    items
        .filter { it.submission.engaged }
        .sortedByDescending { it.dueAt ?: it.plannableDate }
        .take(5)

/** True when the user has acted on (or been excused from) the assignment —
 *  the submission row should land in "Recent submissions", not "Upcoming". */
private val PlannerSubmissionStatus.engaged: Boolean
    get() = submitted || graded || excused

/**
 * Builds the what-if calculator's underlying numbers from cached planner items.
 *
 * Uses an approximation because the planner endpoint we read from doesn't
 * carry per-item submission scores — only boolean status flags. So we back-derive
 * earned points from `course.currentScore` × points-of-graded-items. A2.3's
 * assignments endpoint will bring real per-item scores; revisit then.
 *
 * Returns Unavailable when there's no current grade (instructor hasn't released
 * any) OR no points possible so far (zero graded items) OR no remaining points
 * (term is over) — projection has no signal in any of those cases.
 */
private fun computeWhatIf(course: CanvasCourse, items: List<PlannerItem>): WhatIfState {
    val currentPercent = course.currentScore ?: return WhatIfState.Unavailable
    val gradedPoints = items
        .filter { it.submission.graded || it.submission.excused }
        .sumOf { it.pointsPossible ?: 0.0 }
    val remainingPoints = items
        .filter { !it.submission.graded && !it.submission.excused }
        .sumOf { it.pointsPossible ?: 0.0 }
    if (gradedPoints == 0.0 || remainingPoints == 0.0) return WhatIfState.Unavailable

    val earnedPoints = (currentPercent / 100.0) * gradedPoints
    return WhatIfState.Available(
        currentPercent = currentPercent,
        earnedPoints = earnedPoints,
        gradedPoints = gradedPoints,
        remainingPoints = remainingPoints,
        assumedRemainingPercent = currentPercent.toFloat(),
    )
}

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data object NotFound : CourseDetailUiState
    data class Loaded(
        val course: CanvasCourse,
        val paletteSlot: Int,
        val upcoming: List<PlannerItem>,
        val recentSubmissions: List<PlannerItem>,
        val whatIf: WhatIfState,
    ) : CourseDetailUiState
}

sealed interface WhatIfState {
    data object Unavailable : WhatIfState
    data class Available(
        val currentPercent: Double,
        val earnedPoints: Double,
        val gradedPoints: Double,
        val remainingPoints: Double,
        /** Slider seed — defaults to "user maintains current grade." */
        val assumedRemainingPercent: Float,
    ) : WhatIfState
}
