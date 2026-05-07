package com.ekhonavigator.feature.canvas.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.CourseColorInput
import com.ekhonavigator.core.designsystem.theme.coursePalette

@Composable
fun MyCoursesScreen(
    onConnectClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MyCoursesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "My Courses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            when (val state = uiState) {
                MyCoursesUiState.Loading -> CenteredSpinner()
                MyCoursesUiState.Disconnected -> DisconnectedState(onConnectClick)
                is MyCoursesUiState.Loaded -> LoadedContent(state, onRetry = viewModel::refresh)
            }
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DisconnectedState(onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connect Canvas to see your courses.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConnectClick) {
            Text("Connect Canvas")
        }
    }
}

@Composable
private fun LoadedContent(
    state: MyCoursesUiState.Loaded,
    onRetry: () -> Unit,
) {
    if (state.syncing && state.courses.isEmpty()) {
        CenteredSpinner()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.syncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.courses.isEmpty() && !state.syncing) {
            EmptyCourses(onRetry)
        } else {
            // Compute palette slot per course once at this level so every row
            // shares the same family-key assignment as the calendar / filter
            // sheet do — no Canvas knowledge leaks into the row composable.
            val palette = coursePalette()
            val courseSlots = remember(state.courses) {
                CourseColorAssigner.assign(
                    state.courses.map { CourseColorInput(id = it.id, code = it.code) },
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.courses, key = CanvasCourse::id) { course ->
                    val slot = courseSlots[course.id] ?: 0
                    CourseCard(course, palette[slot % palette.size])
                }
            }
        }
    }
}

@Composable
private fun EmptyCourses(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No active courses returned by Canvas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun CourseCard(course: CanvasCourse, courseColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CourseColorBlock(color = courseColor)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = courseColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val termName = course.termName
                if (termName != null) {
                    Text(
                        text = termName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (course.currentGrade != null || course.currentScore != null) {
                Spacer(Modifier.width(12.dp))
                GradePill(grade = course.currentGrade, score = course.currentScore)
            }
        }
    }
}

@Composable
private fun CourseColorBlock(color: Color) {
    // Solid color "tile" — same family-key→palette mapping the calendar pills,
    // assignment dots, and FilterSheet course chips use, so a course's identity
    // stays consistent across every surface. Sized down from the old 64dp image
    // box to give the title + grade pill more horizontal room.
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
    )
}

@Composable
private fun GradePill(grade: String?, score: Double?) {
    val text = grade ?: score?.let { "${it.toInt()}%" } ?: return
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
