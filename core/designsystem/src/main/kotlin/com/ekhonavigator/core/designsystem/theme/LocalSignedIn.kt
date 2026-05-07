package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * `true` when a Firebase user is currently signed in. Provided at the app root
 * by a Hilt-backed Composable that observes `AuthRepository.userFlow()`.
 *
 * Default `false` so any surface that reads this CompositionLocal outside the
 * provider (tests, previews, screens shown before the provider runs) safely
 * disables sign-in-required affordances rather than letting them no-op or
 * crash on a NotSignedInException.
 */
val LocalSignedIn = compositionLocalOf { false }
