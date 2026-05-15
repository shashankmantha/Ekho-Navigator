package com.ekhonavigator.feature.calendar.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import kotlinx.coroutines.launch
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
import com.ekhonavigator.core.designsystem.theme.EkhoColors
import com.ekhonavigator.core.designsystem.theme.LocalAssignmentDecorator
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.isPast
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HourHeight: Dp = 60.dp

// Clears the two-FAB stack at the bottom of the scroll viewport.
private val ScrollBottomPadding: Dp = 144.dp

private val TimeLabelWidth: Dp = 48.dp

private const val HourCount = 24

// ASSIGNMENT pills are due-time-only (start == end) — a small floor would bury
// them as slivers. Full-hour floor keeps them legible; generic events keep 28dp.
private val MinBlockHeight: Dp = 28.dp
private val MinAssignmentBlockHeight: Dp = HourHeight

// columnCount: 1 = day, 7 = week. maxVisibleOverlaps: 0 = unlimited.
@Composable
fun TimelineGrid(
    columnCount: Int,
    columnDates: List<LocalDate>,
    events: List<CalendarEvent>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDayClick: ((Long) -> Unit)? = null,
    maxVisibleOverlaps: Int = 0,
    snapToTodayTrigger: Int = 0,
) {
    val zone = remember { ZoneId.systemDefault() }

    val eventsByDate = remember(events) {
        events.groupBy { it.startTime.atZone(zone).toLocalDate() }
    }

    val layoutsByDate = remember(eventsByDate, columnDates) {
        columnDates.associateWith { date ->
            layoutEventsForDay(eventsByDate[date].orEmpty(), zone)
        }
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 1-min tick — grid is 60dp/hour, finer is just battery churn.
    val nowInstant by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(60_000L)
        }
    }
    val nowZoned = nowInstant.atZone(zone)
    val today = nowZoned.toLocalDate()
    val nowHourFraction = nowZoned.hour + nowZoned.minute / 60f

    val totalHeight = HourHeight * HourCount

    // Use rendered top/bottom — ASSIGNMENT pills bottom-anchor, raw start skews hint detection.
    val (earliestEventHour, latestEventHour) = remember(layoutsByDate) {
        val bounds = layoutsByDate.values.flatten().map { layout ->
            val startZoned = layout.event.startTime.atZone(zone)
            val endZoned = layout.event.endTime.atZone(zone)
            val startHour = startZoned.hour + startZoned.minute / 60f
            val endHour = endZoned.hour + endZoned.minute / 60f
            if (layout.event.type == EventType.ASSIGNMENT) {
                (startHour - 1f).coerceAtLeast(0f) to startHour
            } else {
                startHour to endHour
            }
        }
        if (bounds.isEmpty()) {
            null to null
        } else {
            bounds.minOf { it.first } to bounds.maxOf { it.second }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth - TimeLabelWidth
        val columnWidth = availableWidth / columnCount
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val hourHeightPx = with(density) { HourHeight.toPx() }

        LaunchedEffect(snapToTodayTrigger) {
            val isTodayVisible = columnDates.contains(today)

            // 7AM fallback for non-today swipes — beats whatever hour was last scrolled.
            val targetHourFraction = if (isTodayVisible || snapToTodayTrigger > 0) {
                nowZoned.hour + nowZoned.minute / 60f
            } else {
                7f
            }

            val targetCenterPx = targetHourFraction * hourHeightPx
            val targetScrollPx = (targetCenterPx - (viewportHeightPx / 2)).toInt()

            // Compute maxScroll explicitly — scrollState.maxValue is 0 pre-layout.
            val maxScrollPx = (hourHeightPx * HourCount) + with(density) { ScrollBottomPadding.toPx() } - viewportHeightPx
            val finalTarget = targetScrollPx.coerceIn(0, maxScrollPx.toInt())

            if (snapToTodayTrigger > 0) {
                scrollState.animateScrollTo(finalTarget)
            } else {
                scrollState.scrollTo(finalTarget)
            }
        }

        // Keys MUST include earliest/latest — derivedStateOf otherwise captures stale nulls.
        val showAboveHint by remember(earliestEventHour, hourHeightPx) {
            derivedStateOf {
                val earliest = earliestEventHour ?: return@derivedStateOf false
                val viewportTopHour = scrollState.value / hourHeightPx
                earliest < viewportTopHour
            }
        }
        val showBelowHint by remember(latestEventHour, viewportHeightPx, hourHeightPx) {
            derivedStateOf {
                val latest = latestEventHour ?: return@derivedStateOf false
                val viewportBottomHour = (scrollState.value + viewportHeightPx) / hourHeightPx
                latest > viewportBottomHour
            }
        }

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

                        val isAssignment = event.type == EventType.ASSIGNMENT
                        val minHeight = if (isAssignment) MinAssignmentBlockHeight else MinBlockHeight
                        val blockHeight = (HourHeight * durationFraction).coerceAtLeast(minHeight)
                        // Anchor ASSIGNMENT bottom at due time — top-anchor reads as "starts at due."
                        val rawY = if (isAssignment) {
                            (HourHeight * startFraction - blockHeight).coerceAtLeast(0.dp)
                        } else {
                            HourHeight * startFraction
                        }
                        val maxY = (totalHeight - blockHeight).coerceAtLeast(0.dp)
                        val yOffset = rawY.coerceAtMost(maxY)

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

                // Scopes the now-line to today's column only (not all seven in week view).
                val todayColIndex = columnDates.indexOf(today)
                if (todayColIndex >= 0) {
                    val nowXOffset = TimeLabelWidth + (columnWidth * todayColIndex)
                    val nowYOffset = HourHeight * nowHourFraction
                    val nowColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .offset(x = nowXOffset - 4.dp, y = nowYOffset - 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(nowColor) }
                            .zIndex(3f),
                    )
                    HorizontalDivider(
                        thickness = 1.5.dp,
                        color = nowColor,
                        modifier = Modifier
                            .width(columnWidth)
                            .offset(x = nowXOffset, y = nowYOffset - 0.75.dp)
                            .zIndex(3f),
                    )
                }
            }
            // Bottom spacer so late-day pills clear the FAB stack.
            Spacer(Modifier.height(ScrollBottomPadding))
        }

        // Fixed above the scroller; appear only when off-viewport events exist. Tap scrolls in.
        AnimatedVisibility(
            visible = showAboveHint,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .zIndex(3f),
        ) {
            OffscreenHintChip(
                label = "Earlier events",
                arrowUp = true,
                onClick = {
                    val target = earliestEventHour ?: return@OffscreenHintChip
                    coroutineScope.launch {
                        // Land ~1hr below top edge for breathing room.
                        val targetPx = ((target - 1f) * hourHeightPx).toInt()
                        scrollState.animateScrollTo(
                            targetPx.coerceIn(0, scrollState.maxValue),
                        )
                    }
                },
            )
        }
        AnimatedVisibility(
            visible = showBelowHint,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .zIndex(3f),
        ) {
            OffscreenHintChip(
                label = "Later events",
                arrowUp = false,
                onClick = {
                    val target = latestEventHour ?: return@OffscreenHintChip
                    coroutineScope.launch {
                        val scrollBottomPaddingPx = with(density) { ScrollBottomPadding.toPx() }
                        // Subtract FAB padding — raw viewport bottom would hide pill behind FABs.
                        val targetBottomPx = (target + 1f) * hourHeightPx
                        val targetTopPx = (targetBottomPx - (viewportHeightPx - scrollBottomPaddingPx)).toInt()
                        scrollState.animateScrollTo(
                            targetTopPx.coerceIn(0, scrollState.maxValue),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun OffscreenHintChip(
    label: String,
    arrowUp: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = if (arrowUp) EkhoIcons.ExpandLess else EkhoIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
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
    val onEventPill = EkhoColors.current.onEventPill
    val cardinal = EkhoColors.current.cardinal
    // ASSIGNMENT wins before source branches (mirrors EkhoEventRow + month grid).
    val courseColor = if (event.type == EventType.ASSIGNMENT) {
        LocalAssignmentDecorator.current.courseColorFor(event.id)
    } else {
        null
    }
    val (bgColor, textColor) = when {
        event.type == EventType.ASSIGNMENT ->
            (courseColor ?: cardinal) to onEventPill

        event.source == EventSource.ICAL_FEED && event.isBookmarked ->
            colors.tertiary to onEventPill

        event.source == EventSource.ICAL_FEED ->
            colors.surfaceContainerHighest to colors.onSurfaceVariant

        event.source == EventSource.USER_CREATED || event.source == EventSource.SHARED ->
            colors.secondary to onEventPill

        else -> colors.primary to onEventPill
    }

    val isPendingInvite = event.myRsvpStatus == RsvpStatus.PENDING
    val pendingBorder = colors.error
    // Alpha priority: pending > completed > past. Past+completed → completed.
    val isCompletedPill = LocalAssignmentDecorator.current.isCompleted(event.id)
    val isPastEvent = event.isPast()
    // Dark-mode palette runs bright — knock current pills back so white text
    // reads on top. Past/completed/pending tier down from there.
    val baseAlpha = if (isSystemInDarkTheme()) 0.8f else 1f
    val effectiveBg = when {
        isPendingInvite -> bgColor.copy(alpha = 0.35f)
        isCompletedPill -> bgColor.copy(alpha = 0.4f)
        isPastEvent -> bgColor.copy(alpha = 0.55f)
        else -> bgColor.copy(alpha = baseAlpha)
    }
    val effectiveText = when {
        isPendingInvite -> textColor.copy(alpha = 0.75f)
        isCompletedPill -> textColor.copy(alpha = 0.75f)
        isPastEvent -> textColor.copy(alpha = 0.85f)
        else -> textColor
    }

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
        // effectiveText is already dimmed for completed — don't double-dim here.
        Column {
            Text(
                text = event.eventName.ifEmpty { event.title },
                style = MaterialTheme.typography.labelSmall.copy(
                    textDecoration = if (isCompletedPill) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                ),
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

internal data class EventLayout(
    val event: CalendarEvent,
    val indexInGroup: Int,
    val totalInGroup: Int,
)

// Floor so zero-duration events still register as overlapping when stacked.
private val MinLayoutDuration: Duration = Duration.ofMinutes(15)

private val CalendarEvent.layoutEndTime: Instant
    get() {
        val floor = startTime.plus(MinLayoutDuration)
        return if (endTime.isAfter(floor)) endTime else floor
    }

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
