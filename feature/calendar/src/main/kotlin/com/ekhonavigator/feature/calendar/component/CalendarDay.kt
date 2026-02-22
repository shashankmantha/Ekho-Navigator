package com.ekhonavigator.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.model.CalendarEvent
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition

/**
 * A single day cell in the calendar grid.
 *
 * Uses drawBehind instead of clip(CircleShape) + background() for the
 * selection highlight. drawBehind draws directly on the canvas without
 * allocating a clipping layer — much cheaper when you have 42 cells.
 */
@Composable
fun DayContent(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !isCurrentMonth -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Capture colors outside drawBehind so the lambda doesn't read
    // CompositionLocals during draw (which would force recomposition).
    val selectedColor = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .drawBehind {
                if (isSelected) {
                    drawCircle(color = selectedColor)
                } else if (isToday && isCurrentMonth) {
                    drawCircle(color = todayColor)
                }
            }
            .clickable(enabled = isCurrentMonth) { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )

        // Category indicator dots — distinct colors, max 3 to keep it tidy
        if (events.isNotEmpty() && isCurrentMonth) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                events
                    .map { it.primaryCategory }
                    .distinct()
                    .take(3)
                    .forEach { category ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(category.color), CircleShape),
                        )
                    }
            }
        }
    }
}
