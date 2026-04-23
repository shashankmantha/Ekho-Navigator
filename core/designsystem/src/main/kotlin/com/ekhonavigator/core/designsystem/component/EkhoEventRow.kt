package com.ekhonavigator.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class EkhoEventRowState { NONE, BOOKMARKED, PERSONAL }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EkhoEventRow(
    title: String,
    startTime: Instant,
    endTime: Instant,
    zone: ZoneId,
    location: String,
    monograms: List<String>,
    state: EkhoEventRowState,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPending: Boolean = false,
    organization: String = "",
) {
    val accent = when (state) {
        EkhoEventRowState.NONE -> MaterialTheme.colorScheme.outlineVariant
        EkhoEventRowState.BOOKMARKED -> MaterialTheme.colorScheme.tertiary
        EkhoEventRowState.PERSONAL -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TimeColumn(
            startTime = startTime,
            endTime = endTime,
            zone = zone,
            accent = accent,
            tintStartWithAccent = state != EkhoEventRowState.NONE,
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (organization.isNotBlank()) {
                OrganizationByline(organization)
                Spacer(Modifier.height(2.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = EkhoIcons.Place,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val hasChips = monograms.isNotEmpty() || isPending
            if (hasChips) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    monograms.take(5).forEach { EkhoMonogramBadge(monogram = it) }
                    if (isPending) PendingChip()
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        BookmarkIndicator(state = state, onBookmarkClick = onBookmarkClick)
    }
}

@Composable
private fun TimeColumn(
    startTime: Instant,
    endTime: Instant,
    zone: ZoneId,
    accent: Color,
    tintStartWithAccent: Boolean,
) {
    val hourFormatter = remember { DateTimeFormatter.ofPattern("h:mm") }
    val amPmFormatter = remember { DateTimeFormatter.ofPattern("a") }
    val start = startTime.atZone(zone)
    val end = endTime.atZone(zone)

    Row(modifier = Modifier.width(72.dp)) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = start.format(hourFormatter),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                color = if (tintStartWithAccent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = start.format(amPmFormatter),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.height(3.dp))
            HorizontalDivider(
                modifier = Modifier.width(10.dp),
                color = accent,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = end.format(hourFormatter),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = end.format(amPmFormatter),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(accent),
        )
    }
}

@Composable
private fun PendingChip() {
    val error = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = error,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 2.dp.toPx()),
                            0f,
                        ),
                    ),
                )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "PENDING",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.08.em,
            ),
            color = error,
        )
    }
}

@Composable
private fun OrganizationByline(organization: String) {
    Text(
        text = organization,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BookmarkIndicator(
    state: EkhoEventRowState,
    onBookmarkClick: () -> Unit,
) {
    when (state) {
        EkhoEventRowState.NONE -> {
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = EkhoIcons.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        EkhoEventRowState.BOOKMARKED -> {
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = EkhoIcons.Bookmark,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        EkhoEventRowState.PERSONAL -> {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = EkhoIcons.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
