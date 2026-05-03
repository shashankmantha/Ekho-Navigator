package com.ekhonavigator.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekhonavigator.core.designsystem.theme.LocalAssignmentDecorator
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition

/** Fixed height for each day cell. Fits date number + up to 5 event pills. */
val DayCellHeight: Dp = 100.dp

/** Max visible event slots per cell. The last slot becomes "..." if there's overflow. */
private const val MaxVisibleSlots = 5

/**
 * A single day cell in the month calendar grid.
 *
 * Fixed height. Shows the date number at top, then event title previews as
 * colored pills (source-type colors). If more events than fit, shows "..."
 * overflow indicator.
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

    val todayBg = MaterialTheme.colorScheme.onSurface
    val selectedBg = MaterialTheme.colorScheme.surfaceContainerHighest

    val textColor = when {
        isToday && isCurrentMonth -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onSurface
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Source-type pill colors from the theme
    val calendarPillColor = MaterialTheme.colorScheme.primary
    val customPillColor = MaterialTheme.colorScheme.secondary
    val campusMutedPillColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val campusBookmarkedPillColor = MaterialTheme.colorScheme.tertiary

    val onCalendarPillColor = MaterialTheme.colorScheme.onPrimary
    val onCustomPillColor = MaterialTheme.colorScheme.onSecondary
    val onCampusMutedPillColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onCampusBookmarkedPillColor = MaterialTheme.colorScheme.onTertiary

    val cellColor = MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(DayCellHeight)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cellColor)
            .drawBehind {
                if (isSelected && !(isToday && isCurrentMonth)) {
                    drawRoundRect(
                        color = selectedBg.copy(alpha = 0.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                }
            }
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        // Date number with today/selected highlight
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = if (isToday && isCurrentMonth) {
                Modifier
                    .drawBehind {
                        drawCircle(
                            color = todayBg,
                            radius = size.maxDimension / 2 + 2.dp.toPx(),
                        )
                    }
                    .padding(2.dp)
            } else {
                Modifier
            },
        )

        // Event preview pills — only for current month dates
        if (events.isNotEmpty()) {
            Spacer(Modifier.height(1.dp))

            val hasOverflow = events.size > MaxVisibleSlots
            // If overflow, the last slot becomes "..."
            val pillCount = if (hasOverflow) MaxVisibleSlots - 1 else events.size
            val visibleEvents = events.take(pillCount)

            val decorator = LocalAssignmentDecorator.current
            Column(verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
                visibleEvents.forEach { event ->
                    val courseColor = if (event.type == EventType.ASSIGNMENT) {
                        decorator.courseColorFor(event.id)
                    } else {
                        null
                    }
                    val (pillBg, pillText) = eventPillColors(
                        event = event,
                        calendarPill = courseColor ?: calendarPillColor,
                        customPill = customPillColor,
                        campusMutedPill = campusMutedPillColor,
                        campusBookmarkedPill = campusBookmarkedPillColor,
                        onCalendar = onCalendarPillColor,
                        onCustom = onCustomPillColor,
                        onCampusMuted = onCampusMutedPillColor,
                        onCampusBookmarked = onCampusBookmarkedPillColor,
                    )
                    val isPendingInvite = event.myRsvpStatus == RsvpStatus.PENDING
                    val pendingBorder = MaterialTheme.colorScheme.error

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPendingInvite) pillBg.copy(alpha = 0.35f) else pillBg,
                            )
                            .drawBehind {
                                if (isPendingInvite) {
                                    drawRoundRect(
                                        color = pendingBorder,
                                        cornerRadius = CornerRadius(2.dp.toPx()),
                                        style = Stroke(
                                            width = 1.2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(2.5.dp.toPx(), 1.5.dp.toPx()),
                                                0f,
                                            ),
                                        ),
                                    )
                                }
                            }
                            .padding(horizontal = 1.dp),
                    ) {
                        Text(
                            text = event.eventName.ifEmpty { event.title },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = if (isPendingInvite) pillText.copy(alpha = 0.75f) else pillText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (hasOverflow) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.5.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 1.dp),
                    )
                }
            }
        }
    }
}

/**
 * Returns (background, text) color pair for an event pill based on source type.
 * Campus events are muted by default, amber when bookmarked.
 * All colors come from MaterialTheme — no hardcoded hex.
 */
private fun eventPillColors(
    event: CalendarEvent,
    calendarPill: Color,
    customPill: Color,
    campusMutedPill: Color,
    campusBookmarkedPill: Color,
    onCalendar: Color,
    onCustom: Color,
    onCampusMuted: Color,
    onCampusBookmarked: Color,
): Pair<Color, Color> = when {
    event.source == EventSource.ICAL_FEED && event.isBookmarked ->
        campusBookmarkedPill to onCampusBookmarked

    event.source == EventSource.ICAL_FEED ->
        campusMutedPill to onCampusMuted

    event.source == EventSource.USER_CREATED || event.source == EventSource.SHARED ->
        customPill to onCustom
    // CLASS_SCHEDULE (future) falls through to calendar accent
    else -> calendarPill to onCalendar
}
