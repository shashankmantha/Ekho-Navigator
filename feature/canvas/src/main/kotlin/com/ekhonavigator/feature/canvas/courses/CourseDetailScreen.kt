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
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
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
                course = state.course,
                courseColor = courseColor,
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
    course: CanvasCourse,
    courseColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroSection(course = course, courseColor = courseColor)

        val courseHtmlUrl = course.htmlUrl
        if (!courseHtmlUrl.isNullOrBlank()) {
            OpenInCanvasButton(url = courseHtmlUrl)
        }

        // A2.2/A2.3/A2.4 sections land here: What-if calculator, Upcoming,
        // Recent submissions, Past assignments + grades, Announcements.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "More course detail coming soon — what-if calculator, upcoming work, past grades, announcements.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
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
) : ViewModel() {

    private val _courseId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun setCourseId(id: String) {
        _courseId.value = id
    }

    val uiState: StateFlow<CourseDetailUiState> = combine(
        _courseId,
        courseRepository.observeCourses(),
    ) { id, courses ->
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
        CourseDetailUiState.Loaded(course = course, paletteSlot = slots[id] ?: 0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourseDetailUiState.Loading,
    )
}

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data object NotFound : CourseDetailUiState
    data class Loaded(
        val course: CanvasCourse,
        val paletteSlot: Int,
    ) : CourseDetailUiState
}
