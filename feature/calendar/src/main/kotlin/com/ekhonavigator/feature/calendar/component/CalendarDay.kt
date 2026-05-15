package com.ekhonavigator.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.ekhonavigator.core.designsystem.theme.EkhoColors
import com.ekhonavigator.core.designsystem.theme.LocalAssignmentDecorator
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.isPast
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition

// Fits date number + MaxVisibleSlots pills at labelSmall sizing.
val DayCellHeight: Dp = 100.dp

// Last slot becomes "…" on overflow.
private const val MaxVisibleSlots = 5

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

    val cardinal = EkhoColors.current.cardinal
    val onEventPill = EkhoColors.current.onEventPill
    val calendarPillColor = cardinal
    val customPillColor = MaterialTheme.colorScheme.secondary
    val campusMutedPillColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val campusBookmarkedPillColor = MaterialTheme.colorScheme.tertiary

    val onCalendarPillColor = onEventPill
    val onCustomPillColor = onEventPill
    val onCampusMutedPillColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onCampusBookmarkedPillColor = onEventPill

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

        if (events.isNotEmpty()) {
            Spacer(Modifier.height(1.dp))

            val hasOverflow = events.size > MaxVisibleSlots
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
                    val isCompleted = decorator.isCompleted(event.id)
                    val isPastEvent = event.isPast()
                    // Knock dark-mode pills back so white text reads on top.
                    val baseAlpha = if (isSystemInDarkTheme()) 0.8f else 1f
                    val effectivePillBg = when {
                        isPendingInvite -> pillBg.copy(alpha = 0.35f)
                        isCompleted -> pillBg.copy(alpha = 0.4f)
                        isPastEvent -> pillBg.copy(alpha = 0.55f)
                        else -> pillBg.copy(alpha = baseAlpha)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(effectivePillBg)
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
                                textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            ),
                            color = when {
                                isPendingInvite -> pillText.copy(alpha = 0.75f)
                                isCompleted -> pillText.copy(alpha = 0.7f)
                                isPastEvent -> pillText.copy(alpha = 0.85f)
                                else -> pillText
                            },
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
    // Mirrors TimelineGrid — ASSIGNMENT identity beats source so personal
    // assignments read red instead of sage-green.
    event.type == EventType.ASSIGNMENT -> calendarPill to onCalendar

    event.source == EventSource.ICAL_FEED && event.isBookmarked ->
        campusBookmarkedPill to onCampusBookmarked

    event.source == EventSource.ICAL_FEED ->
        campusMutedPill to onCampusMuted

    event.source == EventSource.USER_CREATED || event.source == EventSource.SHARED ->
        customPill to onCustom
    else -> calendarPill to onCalendar
}
