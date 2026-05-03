package com.ekhonavigator.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Ekho Navigator shape scale — Material 3 token sizes.
 *
 * Use these via `MaterialTheme.shapes.*` rather than ad-hoc `RoundedCornerShape(N.dp)`
 * literals so radii stay consistent across the app:
 *
 * - extraSmall (4dp) — timeline blocks, small inline indicators, monogram badges
 * - small      (8dp) — compact buttons, sub-chips
 * - medium     (12dp) — text fields, filter chips, list rows
 * - large      (16dp) — cards, sheets, primary surfaces
 * - extraLarge (28dp) — hero cards, FAB-adjacent surfaces, modal sheets
 *
 * Values intentionally avoid the in-between radii (6/10/14/18/20/24) that previously
 * proliferated screen-by-screen — those compress here onto the nearest token rung.
 */
val EkhoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
