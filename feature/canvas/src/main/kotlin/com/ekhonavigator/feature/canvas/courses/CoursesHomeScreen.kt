package com.ekhonavigator.feature.canvas.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.data.canvas.CanvasAnnouncementRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
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

        if (uiState.announcements.isNotEmpty()) {
            RecentAnnouncementsSection(
                announcements = uiState.announcements,
                courseCodeById = uiState.courseCodeById,
                onMarkRead = viewModel::markAnnouncementRead,
            )
        }

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
                Text(
                    text = "${state.deadlineCount} ${if (state.deadlineCount == 1) "deadline" else "deadlines"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
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
private fun RecentAnnouncementsSection(
    announcements: List<CanvasAnnouncement>,
    courseCodeById: Map<String, String>,
    onMarkRead: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Recent announcements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        announcements.forEach { announcement ->
            RecentAnnouncementRow(
                announcement = announcement,
                courseLabel = courseCodeById[announcement.courseId]
                    ?.let(CourseColorAssigner::familyKey),
                onExpand = { if (announcement.isUnread) onMarkRead(announcement.id) },
            )
        }
    }
}

@Composable
private fun RecentAnnouncementRow(
    announcement: CanvasAnnouncement,
    courseLabel: String?,
    onExpand: () -> Unit,
) {
    var expanded by remember(announcement.id) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val zone = remember { ZoneId.systemDefault() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainerLow)
            .clickable {
                if (!expanded) onExpand()
                expanded = !expanded
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (announcement.isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
            if (!courseLabel.isNullOrBlank()) {
                Text(
                    text = courseLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = announcement.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (announcement.isUnread) FontWeight.SemiBold else FontWeight.Medium,
                color = colors.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        announcement.postedAt?.let { instant ->
            val dateText = remember(instant) {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US)
                instant.atZone(zone).format(formatter)
            }
            Text(
                text = listOfNotNull(announcement.authorName, dateText).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
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

    fun markAnnouncementRead(announcementId: String) {
        viewModelScope.launch {
            announcementRepository.markRead(announcementId)
        }
    }

    val uiState: StateFlow<CoursesHomeUiState> = combine(
        courseRepository.observeCourses(),
        plannerRepository.observeAllItems(),
        announcementRepository.observeAll(),
    ) { courses, planners, announcements ->
        CoursesHomeUiState(
            weekOutlook = computeWeekOutlook(planners),
            announcements = pickRecentAnnouncements(announcements),
            courseCodeById = courses.associate { it.id to it.code },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoursesHomeUiState(
            weekOutlook = WeekOutlookState.Empty,
            announcements = emptyList(),
            courseCodeById = emptyMap(),
        ),
    )
}

data class CoursesHomeUiState(
    val weekOutlook: WeekOutlookState,
    val announcements: List<CanvasAnnouncement>,
    /** Lookup so the announcement row can render the originating course's
     *  family-key label without spinning up another VM. */
    val courseCodeById: Map<String, String>,
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

    return WeekOutlookState.Available(
        deadlineCount = inWindow.size,
        totalPoints = totalPoints,
        activeDays = activeDays,
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

/** Top 3 announcements: prefer unread, fall through to most recent. */
internal fun pickRecentAnnouncements(
    announcements: List<CanvasAnnouncement>,
): List<CanvasAnnouncement> {
    val unread = announcements.filter { it.isUnread }.take(3)
    if (unread.size == 3) return unread
    val fillCount = 3 - unread.size
    val unreadIds = unread.mapTo(HashSet()) { it.id }
    val fillers = announcements
        .asSequence()
        .filter { it.id !in unreadIds }
        .take(fillCount)
        .toList()
    return unread + fillers
}

private val com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus.engagedForOutlook: Boolean
    get() = submitted || graded || excused
