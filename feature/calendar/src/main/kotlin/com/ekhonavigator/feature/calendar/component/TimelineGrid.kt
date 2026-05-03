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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import java.time.Duration
import java.time.Instant
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
 * Floor for an event pill's rendered height. Canvas assignments are due-time-driven
 * (start == end), so without a floor they collapse to a 1-px sliver with no readable
 * text. 28dp leaves room for one line of labelSmall + the 2dp vertical padding.
 */
private val MinBlockHeight: Dp = 28.dp

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
                        val durationFraction = (endFraction - startFraction).coerceAtLeast(0f)

                        val blockHeight = (HourHeight * durationFraction).coerceAtLeast(MinBlockHeight)
                        // Anchor late-day pills (11:59 PM "due" assignments) so their bottom
                        // edge sits flush with the grid bottom rather than spilling off-screen
                        // and getting clipped to a sliver.
                        val rawY = HourHeight * startFraction
                        val maxY = (totalHeight - blockHeight).coerceAtLeast(0.dp)
                        val yOffset = rawY.coerceAtMost(maxY)

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
    // ASSIGNMENT type wins first (matches EkhoEventRow's toRowState logic).
    // Primary garnet is the default; per-course palette (C5.5) will override
    // for course-tagged assignments. Slate blue stays available as one option
    // in the per-course rotation rather than the source-wide default.
    val (bgColor, textColor) = when {
        event.type == EventType.ASSIGNMENT -> colors.primary to colors.onPrimary

        event.source == EventSource.ICAL_FEED && event.isBookmarked ->
            colors.tertiary to colors.onTertiary

        event.source == EventSource.ICAL_FEED ->
            colors.surfaceContainerHighest to colors.onSurfaceVariant

        event.source == EventSource.USER_CREATED || event.source == EventSource.SHARED ->
            colors.secondary to colors.onSecondary

        else -> colors.primary to colors.onPrimary
    }

    val isPendingInvite = event.myRsvpStatus == RsvpStatus.PENDING
    val pendingBorder = colors.error
    val effectiveBg = if (isPendingInvite) bgColor.copy(alpha = 0.35f) else bgColor
    val effectiveText = if (isPendingInvite) textColor.copy(alpha = 0.75f) else textColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(effectiveBg)
            .drawBehind {
                if (isPendingInvite) {
                    drawRoundRect(
                        color = pendingBorder,
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(5.dp.toPx(), 3.dp.toPx()),
                                0f,
                            ),
                        ),
                    )
                }
            }
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Column {
            Text(
                text = event.eventName.ifEmpty { event.title },
                style = MaterialTheme.typography.labelSmall,
                color = effectiveText,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = effectiveText.copy(alpha = 0.7f),
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

/** Floor for an event's effective end time in layout — keeps degenerate ranges from escaping overlap detection. */
private val MinLayoutDuration: Duration = Duration.ofMinutes(15)

private val CalendarEvent.layoutEndTime: Instant
    get() {
        val floor = startTime.plus(MinLayoutDuration)
        return if (endTime.isAfter(floor)) endTime else floor
    }

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
            .thenByDescending { it.layoutEndTime.epochSecond - it.startTime.epochSecond },
    )

    val result = mutableListOf<EventLayout>()
    val groups = mutableListOf<MutableList<CalendarEvent>>()

    for (event in sorted) {
        var placed = false
        for (group in groups) {
            val overlaps = group.any { existing ->
                event.startTime < existing.layoutEndTime && event.layoutEndTime > existing.startTime
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
                if (event.startTime >= lastInColumn.layoutEndTime) {
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
