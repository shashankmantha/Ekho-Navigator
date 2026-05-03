package com.ekhonavigator.feature.event.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.component.EkhoMonogramBadge
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType

/**
 * Maps a [EventSourceType] to its (accent, onAccent) color pair from the theme.
 */
internal fun sourceTypeThemeColors(
    type: EventSourceType,
    colors: androidx.compose.material3.ColorScheme,
): Pair<Color, Color> = when (type) {
    EventSourceType.SCHEDULE -> colors.primary to colors.onPrimary
    EventSourceType.CUSTOM -> colors.secondary to colors.onSecondary
    EventSourceType.CAMPUS -> colors.onSurfaceVariant to colors.surface
    EventSourceType.BOOKMARKED -> colors.tertiary to colors.onTertiary
    EventSourceType.CANVAS -> colors.primary to colors.onPrimary
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheetContent(
    activeSourceTypes: Set<EventSourceType>,
    selectedCategories: Set<EventCategory>,
    onToggleSourceType: (EventSourceType) -> Unit,
    onToggleCategory: (EventCategory) -> Unit,
    onClearCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Sources",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // SCHEDULE excluded until class calendar import is implemented
            EventSourceType.entries
                .filter { it != EventSourceType.SCHEDULE }
                .forEach { type ->
                    val isActive = type in activeSourceTypes
                    val (accentColor, _) = sourceTypeThemeColors(type, colors)

                    FilterChip(
                        selected = isActive,
                        onClick = { onToggleSourceType(type) },
                        leadingIcon = if (isActive && type != EventSourceType.BOOKMARKED) {
                            {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .drawBehind { drawCircle(accentColor) },
                                )
                            }
                        } else {
                            null
                        },
                        label = {
                            if (type == EventSourceType.BOOKMARKED) {
                                Icon(
                                    imageVector = EkhoIcons.Bookmark,
                                    contentDescription = "Bookmarked",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isActive) accentColor else colors.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                        modifier = Modifier
                            .then(
                                if (type != EventSourceType.BOOKMARKED) Modifier.weight(1f)
                                else Modifier,
                            )
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.12f),
                            selectedLabelColor = accentColor,
                            containerColor = colors.surfaceContainerHigh,
                            labelColor = colors.onSurfaceVariant,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isActive,
                            borderColor = Color.Transparent,
                            selectedBorderColor = accentColor.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                        ),
                    )
                }
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.3f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )

            if (selectedCategories.isNotEmpty()) {
                FilterChip(
                    selected = false,
                    onClick = onClearCategories,
                    label = {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = colors.primary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.Transparent,
                    ),
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EventCategory.entries.forEach { category ->
                val isSelected = category in selectedCategories

                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCategory(category) },
                    label = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        EkhoMonogramBadge(
                            monogram = category.monogram,
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = if (isSelected) {
                                colors.onPrimaryContainer
                            } else {
                                colors.onSurfaceVariant
                            },
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                        selectedLeadingIconColor = colors.onPrimaryContainer,
                        containerColor = colors.surfaceContainerHigh,
                        labelColor = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
