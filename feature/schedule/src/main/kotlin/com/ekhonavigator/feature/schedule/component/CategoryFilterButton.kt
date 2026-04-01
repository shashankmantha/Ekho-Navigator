package com.ekhonavigator.feature.schedule.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.ekhonavigator.core.model.EventCategory

@Composable
fun CategoryFilterButton(
    selectedCategories: Set<EventCategory>,
    onToggleCategory: (EventCategory) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasActiveFilter = selectedCategories.isNotEmpty()
    val colors = MaterialTheme.colorScheme

    Box(modifier = modifier) {
        FilterChip(
            selected = hasActiveFilter,
            onClick = { expanded = !expanded },
            label = {
                Icon(
                    imageVector = EkhoIcons.Grid3x3,
                    contentDescription = "Filter categories",
                    modifier = Modifier.size(16.dp),
                    tint = if (hasActiveFilter) colors.primary else colors.onSurfaceVariant,
                )
            },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.surfaceContainerHigh,
                containerColor = colors.surfaceContainerHigh,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = hasActiveFilter,
                borderColor = Color.Transparent,
                selectedBorderColor = colors.primary.copy(alpha = 0.3f),
                borderWidth = 1.dp,
                selectedBorderWidth = 1.dp,
            ),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surfaceContainerHigh),
        ) {
            // "All" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "All Categories",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!hasActiveFilter) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onClearAll()
                    expanded = false
                },
                trailingIcon = {
                    if (!hasActiveFilter) {
                        Icon(
                            imageVector = EkhoIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.primary,
                        )
                    }
                },
            )

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 2.dp),
            )

            EventCategory.entries.forEach { category ->
                val isSelected = category in selectedCategories

                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        )
                    },
                    onClick = { onToggleCategory(category) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = EkhoIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}
