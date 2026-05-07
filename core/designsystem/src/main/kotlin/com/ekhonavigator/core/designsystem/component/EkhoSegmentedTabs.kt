package com.ekhonavigator.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared in-screen tab strip used by Calendar (Day/Week/Month), Campus
 * (Discover/Courses), and Social (Friends/Chats). Centralizes the look so
 * "selected vs unselected" reads at a glance — `primaryContainer` fill on
 * the active tab, transparent on inactive, weight differential on the label.
 */
@Composable
fun <T> EkhoSegmentedTabs(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelect(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = items.size,
                ),
                icon = {},
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    },
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                label = {
                    Text(
                        text = labelOf(item),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}
