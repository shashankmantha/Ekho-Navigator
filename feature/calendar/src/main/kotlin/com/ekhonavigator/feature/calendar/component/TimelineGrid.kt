package com.ekhonavigator.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Height of each hour row in the timeline. */
private val HourHeight: Dp = 60.dp

/** Width of the time label column on the left. */
private val TimeLabelWidth: Dp = 48.dp

/** Number of hours to display (full day). */
private const val HourCount = 24

/**
 * Reusable timeline grid for day and week views.
 *
 * @param columnCount 1 for day view, 7 for week view
 * @param maxVisibleOverlaps Max concurrent events shown side-by-side. 0 = unlimited.
 */
@Composable
fun TimelineGrid(
    columnCount: Int,
    columnDates: List<LocalDate>,
    events: List<CalendarEvent>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDayClick: ((Long) -> Unit)? = null,
    maxVisibleOverlaps: Int = 0,
    scrollToHour: Int = 7,
) {
    val zone = remember { ZoneId.systemDefault() }

    // Group events by date
    val eventsByDate = remember(events) {
        events.groupBy { it.startTime.atZone(zone).toLocalDate() }
    }

    // Lay out overlapping events per column
    val layoutsByDate = remember(eventsByDate, columnDates) {
        columnDates.associateWith { date ->
            layoutEventsForDay(eventsByDate[date].orEmpty(), zone)
        }
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Scroll to initial hour on first composition
    LaunchedEffect(Unit) {
        val offsetPx = with(density) { (HourHeight * scrollToHour).toPx().toInt() }
        scrollState.scrollTo(offsetPx)
    }

    val totalHeight = HourHeight * HourCount

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth - TimeLabelWidth
        val columnWidth = availableWidth / columnCount

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight),
            ) {
                // Hour grid lines and labels
                repeat(HourCount) { hour ->
                    val yOffset = HourHeight * hour

                    Text(
                        text = if (hour == 0) "" else DateTimeFormatter.ofPattern("h a")
                            .format(java.time.LocalTime.of(hour, 0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(TimeLabelWidth)
                            .offset(y = yOffset - 6.dp)
                            .padding(end = 8.dp),
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = yOffset)
                            .padding(start = TimeLabelWidth),
                    )
                }

                // Per-column rendering
                columnDates.forEachIndexed { colIndex, date ->
                    val xOffset = TimeLabelWidth + (columnWidth * colIndex)

                    val layouts = layoutsByDate[date].orEmpty()
                    val overflowSlots = mutableMapOf<Float, Int>()

                    layouts.forEach { layout ->
                        val event = layout.event
                        val startZoned = event.startTime.atZone(zone)
                        val endZoned = event.endTime.atZone(zone)

                        val startFraction = startZoned.hour + startZoned.minute / 60f
                        val endFraction = endZoned.hour + endZoned.minute / 60f
                        val durationFraction = (endFraction - startFraction).coerceAtLeast(0.25f)

                        val yOffset = HourHeight * startFraction
                        val blockHeight = HourHeight * durationFraction

                        // Overflow cap
                        if (maxVisibleOverlaps > 0 && layout.indexInGroup >= maxVisibleOverlaps) {
                            overflowSlots[startFraction] =
                                (overflowSlots[startFraction] ?: 0) + 1
                            return@forEach
                        }

                        val effectiveTotal = if (maxVisibleOverlaps > 0) {
                            layout.totalInGroup.coerceAtMost(maxVisibleOverlaps)
                        } else {
                            layout.totalInGroup
                        }

                        val blockWidth = columnWidth * (1f / effectiveTotal)
                        val blockXOffset = xOffset + blockWidth * layout.indexInGroup

                        TimelineEventBlock(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            modifier = Modifier
                                .offset(x = blockXOffset, y = yOffset)
                                .width(blockWidth - 1.dp)
                                .height(blockHeight - 1.dp)
                                .zIndex(1f),
                        )
                    }

                    // "+N" overflow badges
                    if (onDayClick != null) {
                        overflowSlots.forEach { (startFraction, count) ->
                            OverflowBadge(
                                count = count,
                                onClick = { onDayClick(date.toEpochDay()) },
                                modifier = Modifier
                                    .offset(
                                        x = xOffset + columnWidth - 24.dp,
                                        y = HourHeight * startFraction,
                                    )
                                    .zIndex(2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventBlock(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (bgColor, textColor) = when {
        event.source == EventSource.ICAL_FEED && event.isBookmarked ->
            colors.tertiaryContainer to colors.onTertiaryContainer

        event.source == EventSource.ICAL_FEED ->
            colors.surfaceContainerHighest to colors.onSurfaceVariant

        event.source == EventSource.USER_CREATED || event.source == EventSource.SHARED ->
            colors.secondaryContainer to colors.onSecondaryContainer

        else -> colors.primaryContainer to colors.onPrimaryContainer
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Column {
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OverflowBadge(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

// ══════════════════════════════════════════════════
// Overlap layout algorithm
// ══════════════════════════════════════════════════

internal data class EventLayout(
    val event: CalendarEvent,
    val indexInGroup: Int,
    val totalInGroup: Int,
)

/**
 * Assigns horizontal positions to events that overlap in time.
 * Events that don't overlap get the full column width.
 */
internal fun layoutEventsForDay(
    events: List<CalendarEvent>,
    zone: ZoneId,
): List<EventLayout> {
    if (events.isEmpty()) return emptyList()

    val sorted = events.sortedWith(
        compareBy<CalendarEvent> { it.startTime }
            .thenByDescending { it.endTime.epochSecond - it.startTime.epochSecond },
    )

    val result = mutableListOf<EventLayout>()
    val groups = mutableListOf<MutableList<CalendarEvent>>()

    for (event in sorted) {
        var placed = false
        for (group in groups) {
            val overlaps = group.any { existing ->
                event.startTime < existing.endTime && event.endTime > existing.startTime
            }
            if (overlaps) {
                group.add(event)
                placed = true
                break
            }
        }
        if (!placed) {
            groups.add(mutableListOf(event))
        }
    }

    for (group in groups) {
        val columns = mutableListOf<MutableList<CalendarEvent>>()
        for (event in group.sortedBy { it.startTime }) {
            var placedInColumn = false
            for (column in columns) {
                val lastInColumn = column.last()
                if (event.startTime >= lastInColumn.endTime) {
                    column.add(event)
                    placedInColumn = true
                    break
                }
            }
            if (!placedInColumn) {
                columns.add(mutableListOf(event))
            }
        }

        val totalColumns = columns.size
        for ((colIndex, column) in columns.withIndex()) {
            for (event in column) {
                result.add(EventLayout(event, colIndex, totalColumns))
            }
        }
    }

    return result
}
