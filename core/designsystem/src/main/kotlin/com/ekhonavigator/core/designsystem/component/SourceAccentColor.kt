package com.ekhonavigator.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Returns the theme accent color for an event based on source type and bookmark state.
 *
 * - "CLASS_SCHEDULE" → primary (SchoolRed)
 * - "USER_CREATED" / "SHARED" → secondary (DolphinCyan)
 * - "ICAL_FEED" bookmarked → tertiary (CampusAmber) — pops to reward commitment
 * - "ICAL_FEED" not bookmarked → onSurfaceVariant (muted) — blends in, doesn't dominate
 *
 * This lives in designsystem so all feature modules can use it
 * without depending on core:model directly.
 */
@Composable
@ReadOnlyComposable
fun sourceAccentColor(sourceName: String, isBookmarked: Boolean = false): Color =
    when (sourceName) {
        "ICAL_FEED" -> if (isBookmarked) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        "USER_CREATED", "SHARED" -> MaterialTheme.colorScheme.secondary
        "CLASS_SCHEDULE" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
