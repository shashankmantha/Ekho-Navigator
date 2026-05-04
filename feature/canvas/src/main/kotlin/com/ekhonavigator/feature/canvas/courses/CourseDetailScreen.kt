package com.ekhonavigator.feature.canvas.courses

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasAssignmentGroup
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus
import com.ekhonavigator.core.data.canvas.CanvasAssignmentRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.designsystem.component.EkhoEventRow
import com.ekhonavigator.core.designsystem.component.EkhoEventRowState
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.coursePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        HeroSection(
            course = course,
            courseColor = courseColor,
            showGradePill = state.gradeSummary !is GradeSummaryState.Available,
        )

        val courseHtmlUrl = course.htmlUrl
        if (!courseHtmlUrl.isNullOrBlank()) {
            OpenInCanvasButton(url = courseHtmlUrl)
        }

        if (state.gradeSummary is GradeSummaryState.Available) {
            GradeSummarySection(state = state.gradeSummary)
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

        if (state.pastAssignments.isNotEmpty()) {
            PastAssignmentsSection(
                assignments = state.pastAssignments,
                onAssignmentClick = onEventClick,
            )
        }
    }
}

@Composable
private fun PastAssignmentsSection(
    assignments: List<CanvasAssignment>,
    onAssignmentClick: (eventId: String) -> Unit,
) {
    val zone = remember { java.time.ZoneId.systemDefault() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Past assignments")
        assignments.forEach { assignment ->
            // Tap routes to EventScreen via the planner-bridged calendar event
            // uid (`assignment_<id>`). EventScreen already handles "row not in
            // Room anymore" by auto-navigating back, so out-of-window items
            // gracefully bail rather than rendering an empty detail.
            val plannerBridgedUid = "assignment_${assignment.id}"
            PastAssignmentRow(
                assignment = assignment,
                zone = zone,
                onClick = { onAssignmentClick(plannerBridgedUid) },
            )
        }
    }
}

@Composable
private fun PastAssignmentRow(
    assignment: CanvasAssignment,
    zone: java.time.ZoneId,
    onClick: () -> Unit,
) {
    // Strikethrough engaged assignments (graded/submitted/excused) the same
    // way EkhoEventRow + LocalAssignmentDecorator do for in-window planner
    // rows — this section is outside the decorator's index, so we apply the
    // style locally using the assignment's own submission state.
    val struck = assignment.submission.engaged
    val titleStyle = MaterialTheme.typography.bodyMedium.copy(
        textDecoration = if (struck) {
            androidx.compose.ui.text.style.TextDecoration.LineThrough
        } else {
            androidx.compose.ui.text.style.TextDecoration.None
        },
        color = if (struck) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = assignment.name,
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val due = assignment.dueAt
            if (due != null) {
                val dueText = remember(due) {
                    val local = due.atZone(zone)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US)
                    "Due ${local.format(formatter)}"
                }
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AssignmentGradePill(assignment = assignment)
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
private fun GradeSummarySection(state: GradeSummaryState.Available) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Grade summary")
        // Headline: weighted total + caption with the source-of-truth + count.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${"%.1f".format(state.weightedPercent)}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val caption = if (state.usesWeights) {
                "weighted · ${state.gradedAssignmentCount} graded"
            } else {
                "by points · ${state.gradedAssignmentCount} graded"
            }
            Text(
                text = caption,
                modifier = Modifier.padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Per-group rows. Sorted by Canvas position (already DAO-sorted), with
        // the synthetic "Other" bucket trailing thanks to position=Int.MAX_VALUE.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.groups.forEach { group ->
                GradeGroupRow(group = group)
            }
        }
    }
}

@Composable
private fun GradeGroupRow(group: GroupBreakdown) {
    val percent = group.percent
    val barFraction = (percent ?: 0.0).coerceIn(0.0, 100.0).toFloat() / 100f
    // Tier the tint by performance so the row's color reads the same way the
    // hero pill does — green-ish for ≥90, neutral for 70-89, red for <70.
    // Greyed out entirely when the group has no graded items yet.
    val colors = MaterialTheme.colorScheme
    val barColor = when {
        percent == null -> colors.surfaceContainerHighest
        percent >= 90.0 -> colors.secondary
        percent >= 70.0 -> colors.tertiary
        else -> colors.error
    }
    val titleColor = if (percent == null) colors.onSurfaceVariant else colors.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val w = group.weight
                if (w != null && w > 0.0) {
                    WeightBadge(weight = w)
                }
            }
            Text(
                text = percent?.let { "${"%.1f".format(it)}%" } ?: "—",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (percent == null) colors.onSurfaceVariant else barColor,
            )
        }
        // Slim performance bar — clipped to a rounded rect so the tint reads
        // as a chip rather than a raw progress indicator.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surfaceContainerHighest),
        ) {
            if (barFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = barFraction)
                        .height(6.dp)
                        .background(barColor),
                )
            }
        }
    }
}

@Composable
private fun WeightBadge(weight: Double) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "${formatPoints(weight)}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun HeroSection(
    course: CanvasCourse,
    courseColor: Color,
    showGradePill: Boolean,
) {
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
            // Hide the hero pill when the GradeSummarySection has its own
            // (richer) total — the section's headline number replaces it.
            if (showGradePill && (course.currentGrade != null || course.currentScore != null)) {
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

/**
 * Per-assignment grade pill. Mirrors the course-level GradePill visual so the
 * vocabulary stays consistent. Falls back through three render modes:
 *  1. "score / pointsPossible" — most informative; both numbers Canvas-known
 *  2. "Excused" / "Missing" — terminal states with no numeric grade
 *  3. status-only badge ("Submitted" / "Late") — Canvas knows the workflow
 *     state but no score has been entered yet
 *  4. nothing — assignment is past-due but un-engaged; the row's just listed
 *     for the "what was assigned this term" recap
 */
@Composable
private fun AssignmentGradePill(assignment: CanvasAssignment) {
    val sub = assignment.submission
    val colors = MaterialTheme.colorScheme
    val score = sub.score
    val points = assignment.pointsPossible

    val (label, container, content) = when {
        sub.excused -> Triple("Excused", colors.surfaceContainerHigh, colors.onSurfaceVariant)
        score != null && points != null && points > 0.0 ->
            Triple(
                "${formatPoints(score)}/${formatPoints(points)}",
                colors.secondaryContainer,
                colors.onSecondaryContainer,
            )
        sub.graded && sub.grade != null ->
            Triple(sub.grade!!, colors.secondaryContainer, colors.onSecondaryContainer)
        sub.missing -> Triple("Missing", colors.errorContainer, colors.onErrorContainer)
        sub.late -> Triple("Late", colors.tertiaryContainer, colors.onTertiaryContainer)
        sub.submitted -> Triple("Submitted", colors.surfaceContainerHigh, colors.onSurfaceVariant)
        else -> return
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
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
    private val assignmentRepository: CanvasAssignmentRepository,
) : ViewModel() {

    private val _courseId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** Per-course assignment sync runs lazily here — the planner sync that
     *  AuthLifecycleObserver fires at sign-in covers calendar surfaces, but the
     *  assignments endpoint is the only source of per-item grades, and we
     *  don't want to N×-fan-out at app launch (one call per course). Triggered
     *  every time a new courseId arrives; `distinctUntilChanged` keeps repeat
     *  Composable recompositions from re-firing. NoCanvasAccountException is
     *  expected for signed-out users — runCatching swallows it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val assignments: StateFlow<List<CanvasAssignment>> = _courseId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            viewModelScope.launch {
                runCatching { assignmentRepository.sync(id) }
            }
            assignmentRepository.observeForCourse(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Same lazy-sync model as [assignments]: the sync trigger lives over there
     *  so we don't fire it twice. This flow just reads the joined view the
     *  repository composes (groups + their assignments), which the
     *  GradeSummarySection turns into a weighted breakdown. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val assignmentGroups: StateFlow<List<CanvasAssignmentGroup>> = _courseId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id -> assignmentRepository.observeGroupsForCourse(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setCourseId(id: String) {
        _courseId.value = id
    }

    val uiState: StateFlow<CourseDetailUiState> = combine(
        _courseId,
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
        assignments,
        assignmentGroups,
    ) { id, courses, planners, assigns, groups ->
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
            pastAssignments = pickPastAssignments(assigns),
            gradeSummary = computeGradeSummary(course = course, groups = groups),
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
 *  top-5 cap as upcoming. The full graded history with per-item scores lives
 *  in pickPastAssignments (assignments endpoint, not planner). */
private fun pickRecentSubmissions(items: List<PlannerItem>): List<PlannerItem> =
    items
        .filter { it.submission.engaged }
        .sortedByDescending { it.dueAt ?: it.plannableDate }
        .take(5)

/**
 * Full graded-history view for the course. Shows assignments where the user
 * has either acted on it (submitted/graded/excused) OR the due date is in the
 * past — the second clause catches missing assignments + ungraded busywork the
 * student should still see in their term recap.
 *
 * Sorted most-recent first; assignments with no due date sink to the bottom
 * (matches DAO sort, repeated here so the in-memory list-flow path stays
 * consistent if ordering ever moves off the DAO).
 */
private fun pickPastAssignments(
    assignments: List<CanvasAssignment>,
    now: java.time.Instant = java.time.Instant.now(),
): List<CanvasAssignment> =
    assignments
        .filter { a ->
            val due = a.dueAt
            a.submission.engaged || (due != null && !due.isAfter(now))
        }
        .sortedWith(
            compareByDescending<CanvasAssignment> { it.dueAt != null }
                .thenByDescending { it.dueAt },
        )

/** True when the user has acted on (or been excused from) the assignment —
 *  the submission row should land in "Recent submissions", not "Upcoming". */
private val PlannerSubmissionStatus.engaged: Boolean
    get() = submitted || graded || excused

/**
 * Builds the weighted grade-summary breakdown from the cached assignment_groups
 * tree. Two-phase compute:
 *
 *  1. Per-group: Σ score / Σ pointsPossible across the group's graded,
 *     non-excused assignments.
 *  2. Course total: weighted average across groups that have at least one
 *     graded item, renormalized so the un-started buckets don't drag the
 *     average to zero. If no group exposes a weight, fall through to a
 *     pure points-total fallback.
 *
 * Returns Unavailable when nothing's been graded yet — the UI falls back to
 * the hero's `course.currentScore` pill in that case.
 */
private fun computeGradeSummary(
    course: CanvasCourse,
    groups: List<CanvasAssignmentGroup>,
): GradeSummaryState {
    if (groups.isEmpty()) return GradeSummaryState.Unavailable

    val breakdowns = groups.map { g ->
        val gradedAssignments = g.assignments.filter {
            it.submission.graded && !it.submission.excused && it.pointsPossible != null
        }
        val earned = gradedAssignments.sumOf { it.submission.score ?: 0.0 }
        val possible = gradedAssignments.sumOf { it.pointsPossible ?: 0.0 }
        val pct = if (possible > 0.0) (earned / possible) * 100.0 else null
        GroupBreakdown(
            name = g.name,
            weight = g.weight,
            earnedPoints = earned,
            possiblePoints = possible,
            percent = pct,
        )
    }

    val gradedCount = groups.sumOf { g ->
        g.assignments.count { it.submission.graded && !it.submission.excused }
    }
    if (gradedCount == 0) return GradeSummaryState.Unavailable

    val weightedActive = breakdowns.filter {
        val w = it.weight
        w != null && w > 0.0 && it.percent != null
    }
    val totalActiveWeight = weightedActive.sumOf { it.weight!! }

    val (weightedPct, usesWeights) = if (totalActiveWeight > 0.0) {
        val total = weightedActive.sumOf { (it.percent!! * it.weight!!) / totalActiveWeight }
        total to true
    } else {
        // Points-only fallback: course doesn't expose weights, or no weighted
        // group has graded items yet. Still useful to surface the raw percent.
        val totalPossible = breakdowns.sumOf { it.possiblePoints }
        if (totalPossible <= 0.0) {
            // Last-ditch fall-through to the course-level Canvas number when
            // we can't compute anything ourselves but Canvas can.
            val fromCourse = course.currentScore ?: return GradeSummaryState.Unavailable
            fromCourse to false
        } else {
            (breakdowns.sumOf { it.earnedPoints } / totalPossible) * 100.0 to false
        }
    }

    return GradeSummaryState.Available(
        weightedPercent = weightedPct,
        gradedAssignmentCount = gradedCount,
        usesWeights = usesWeights,
        groups = breakdowns,
    )
}

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
        val pastAssignments: List<CanvasAssignment>,
        val gradeSummary: GradeSummaryState,
        val whatIf: WhatIfState,
    ) : CourseDetailUiState
}

sealed interface GradeSummaryState {
    /** Course has no graded items yet, or assignment_groups sync hasn't
     *  populated. The hero falls back to `course.currentScore` (the existing
     *  Canvas-aggregated number) in this state so the user still sees
     *  *something* if Canvas knows a grade we can't yet break down. */
    data object Unavailable : GradeSummaryState
    data class Available(
        /** Renormalized weighted total — computed only across groups that
         *  have at least one graded assignment, so an early-term grade isn't
         *  crushed by 0% on un-started buckets. */
        val weightedPercent: Double,
        val gradedAssignmentCount: Int,
        /** Whether the renormalized total uses Canvas-supplied weights or
         *  fell through to the points-only fallback (course doesn't use
         *  weighted grading). Drives the caption in the UI. */
        val usesWeights: Boolean,
        val groups: List<GroupBreakdown>,
    ) : GradeSummaryState
}

data class GroupBreakdown(
    val name: String,
    /** Null when Canvas didn't surface a weight (un-weighted course) — the
     *  per-group bar still renders but no weight badge appears. */
    val weight: Double?,
    val earnedPoints: Double,
    val possiblePoints: Double,
    /** Null when no graded assignments in this group yet — the row renders
     *  greyed with an em-dash so the structure stays visible. */
    val percent: Double?,
)

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
