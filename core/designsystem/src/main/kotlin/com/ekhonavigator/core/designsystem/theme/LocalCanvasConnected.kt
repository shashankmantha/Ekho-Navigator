package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * `true` when the current user has an active Canvas connection (signed in,
 * institution stored, PAT present). Provided at the app root by a
 * Canvas-connection-aware Composable.
 *
 * Default `false` so any surface that reads this CompositionLocal outside the
 * provider (tests, previews, screens shown before the provider runs) safely
 * hides Canvas-only chrome rather than showing dead/broken affordances.
 */
val LocalCanvasConnected = compositionLocalOf { false }
