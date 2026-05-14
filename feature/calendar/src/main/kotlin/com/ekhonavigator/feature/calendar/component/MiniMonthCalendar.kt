package com.ekhonavigator.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.theme.EkhoColors
import com.ekhonavigator.core.designsystem.theme.coursePalette
import com.ekhonavigator.core.model.EventSourceType
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Per-course palette when courseId is known; source-type fallback otherwise.
// Keeps dot identity aligned with the timeline pills.
sealed interface DayDot {
    data class Source(val type: EventSourceType) : DayDot
    data class CourseSlot(val slot: Int) : DayDot
}

@Composable
fun MiniMonthCalendar(
    selectedDate: LocalDate,
    dayDots: Map<LocalDate, Set<DayDot>>,
    onDayClick: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val startMonth = remember { YearMonth.now().minusMonths(12) }
    val endMonth = remember { YearMonth.now().plusMonths(12) }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.from(selectedDate),
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid,
    )

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collect { month -> onMonthChanged(month) }
    }

    LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        if (calendarState.firstVisibleMonth.yearMonth != targetMonth) {
            calendarState.animateScrollToMonth(targetMonth)
        }
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Header nested in monthHeader so its columns align with the date grid.
        HorizontalCalendar(
            state = calendarState,
            monthHeader = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            dayContent = { day ->
                MiniDayCell(
                    day = day,
                    isToday = day.date == today,
                    isSelected = day.date == selectedDate,
                    dots = dayDots[day.date].orEmpty(),
                    onClick = { onDayClick(day.date) },
                )
            },
        )

        MonthTabRow(
            currentMonth = calendarState.firstVisibleMonth.yearMonth,
            startMonth = startMonth,
            endMonth = endMonth,
            onMonthClick = { month ->
                scope.launch { calendarState.animateScrollToMonth(month) }
            },
        )
    }
}

@Composable
private fun dotColorFor(dot: DayDot): Color = when (dot) {
    is DayDot.Source -> when (dot.type) {
        EventSourceType.CUSTOM -> MaterialTheme.colorScheme.secondary
        EventSourceType.CAMPUS -> MaterialTheme.colorScheme.onSurfaceVariant
        EventSourceType.BOOKMARKED -> MaterialTheme.colorScheme.tertiary
        // Cardinal is LMS-identity, not brand chrome (design.md §5).
        EventSourceType.CANVAS -> EkhoColors.current.cardinal
    }
    is DayDot.CourseSlot -> {
        val palette = coursePalette()
        palette[dot.slot % palette.size]
    }
}

@Composable
private fun MiniDayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    dots: Set<DayDot>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate

    val textColor = when {
        isToday && isCurrentMonth -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onSurface
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val todayBg = MaterialTheme.colorScheme.onSurface
    val selectedBg = MaterialTheme.colorScheme.surfaceContainerHighest

    // Resolve here — dotColorFor is @Composable but drawBehind isn't. Cap at 4 for density.
    val dotColors = if (isCurrentMonth && dots.isNotEmpty()) {
        dots.take(4).map { dotColorFor(it) }
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isToday && isCurrentMonth -> Modifier.background(todayBg)
                        isSelected -> Modifier.background(selectedBg)
                        else -> Modifier
                    },
                ),
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 1.dp),
        ) {
            if (dotColors.isNotEmpty()) {
                dotColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(color) },
                    )
                }
            } else {
                // Spacer keeps empty-day row height equal to busy-day rows.
                Box(modifier = Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun MonthTabRow(
    currentMonth: YearMonth,
    startMonth: YearMonth,
    endMonth: YearMonth,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val months = remember(startMonth, endMonth) {
        generateSequence(startMonth) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(endMonth) }
            .toList()
    }
    val selectedIndex = remember(currentMonth, startMonth) {
        currentMonth.monthsUntil(startMonth)
    }

    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(months.indices),
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = {},
        indicator = {},
    ) {
        months.forEachIndexed { index, month ->
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = { onMonthClick(month) },
                text = {
                    Text(
                        text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
        }
    }
}

private fun YearMonth.monthsUntil(start: YearMonth): Int {
    return ((year - start.year) * 12) + (monthValue - start.monthValue)
}
