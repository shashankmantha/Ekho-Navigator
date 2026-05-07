package com.ekhonavigator.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.icon.EkhoIcons

/**
 * Collapsible section header + body for multi-select chip groups.
 *
 * Header: title + count badge (when any selected) + expand/collapse chevron.
 * Body: caller-provided content (typically a [FlowRow] of [androidx.compose.material3.FilterChip]s).
 *
 * Used by both FilterSheet (categories, courses) and CreateEvent (categories) so
 * the multi-select pattern looks identical across surfaces.
 *
 * Expansion state is `rememberSaveable` so it survives recomposition and
 * configuration change — but it's local to the call site, not hoisted, because
 * "is this section open" is UI-only state with no business meaning.
 *
 * @param initiallyExpanded — false by default to keep sheets compact at first paint.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollapsibleMultiSelectSection(
    title: String,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    headerTrailingContent: @Composable (() -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                if (selectedCount > 0) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "$selectedCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                headerTrailingContent?.invoke()
                Icon(
                    imageVector = if (expanded) EkhoIcons.ExpandLess else EkhoIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                body()
            }
        }
    }
}
