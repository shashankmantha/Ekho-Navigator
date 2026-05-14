package com.ekhonavigator.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Ekho Navigator color palette — CSUCI Channel Islands rebrand.
 *
 * Replaces the prior garnet/salmon system. Foundation roles map to brand colors
 * by what they *do* in the app (Material 3 role mapping), not by brand prominence:
 *  - Clay = chrome (FAB, active tab, primary CTAs)
 *  - Cardinal = Canvas event identity ONLY
 *  - Sage = personal/user-created events
 *  - Horizon = bookmarked / starred
 *  - Shale = generic campus events with no other tag
 *
 * No pure white, no pure black — surfaces are warm in both modes (`#FAF6F1` /
 * `#1A1410`). Foreground/background flip in dark mode: text/icon on a foundation
 * color is `surface` (`#1A1410`), not `#FFFFFF`. See design.md §2.
 */

// ── Channel Clay — chrome workhorse (FAB, active tab, primary CTAs, top-bar accents).
// Same family used everywhere a soft active-state pill or button needs to read as brand.
internal val ClayLight = Color(0xFFB0573A) // 14°
internal val ClayDark = Color(0xFFD9846A)  // 14°, lifted to ~tone 70
// Soft clay tonal echo for primaryContainer (FilledTonalButton et al.). The design
// prefers direct Clay; this exists only so M3 components that key off
// primaryContainer don't render as off-brand defaults.
internal val ClaySoftLight = Color(0xFFF0D2C5)
internal val ClaySoftDark = Color(0xFF4A2D1F)
// Deep clay tone for `onPrimaryContainer` text — readable on the soft echo.
internal val ClayDeepLight = Color(0xFF2D0F05)
internal val ClayDeepDark = Color(0xFFFFE3D6)

// ── Cardinal — RESERVED for Canvas LMS event identity. Do not reuse for chrome.
// Salience is earned by being the visual signal that an event came from Canvas.
internal val CardinalLight = Color(0xFFC44060) // 345°
internal val CardinalDark = Color(0xFFE5708C)
internal val CardinalSoftLight = Color(0xFFF8D9DF)
internal val CardinalSoftDark = Color(0xFF4A1A22)

// ── Sage — personal / user-created events.
internal val SageLight = Color(0xFF7B9268) // 95°
internal val SageDark = Color(0xFFA8BD96)
internal val SageSoftLight = Color(0xFFDCE5D2)
internal val SageSoftDark = Color(0xFF2D3A22)

// ── Horizon — bookmarked / starred.
internal val HorizonLight = Color(0xFFE37B26) // 25°
internal val HorizonDark = Color(0xFFF4A763)
internal val HorizonSoftLight = Color(0xFFFAE0CC)
internal val HorizonSoftDark = Color(0xFF4A2A12)

// ── Shale — generic campus events with no other role. Inverted in dark mode
// (was dark neutral light → warm light gray dark).
internal val ShaleLight = Color(0xFF3A3A3C)
internal val ShaleDark = Color(0xFF9B958C)

// ── Warm surface ladder. NO pure white, NO pure black.
internal val LightSurface = Color(0xFFFAF6F1)
internal val LightSurfaceContainerLowest = Color(0xFFFCFAF6) // warmer than surface
internal val LightSurfaceContainerLow = Color(0xFFF4EFE7)
internal val LightSurfaceContainer = Color(0xFFEFE9DF)
internal val LightSurfaceContainerHigh = Color(0xFFE9E2D6)
internal val LightSurfaceContainerHighest = Color(0xFFE3DBCD)

internal val DarkSurface = Color(0xFF1A1410)
internal val DarkSurfaceContainerLowest = Color(0xFF14100C)
internal val DarkSurfaceContainerLow = Color(0xFF221C16)
internal val DarkSurfaceContainer = Color(0xFF2A2218)
internal val DarkSurfaceContainerHigh = Color(0xFF322A1F)
internal val DarkSurfaceContainerHighest = Color(0xFF3A3225)

// ── Outlines ──
internal val OutlineLight = Color(0xFFCBC1B4)
internal val OutlineSubtleLight = Color(0x142B2522) // rgba(43,37,34,0.08)
internal val OutlineDark = Color(0xFF3A3028)
internal val OutlineSubtleDark = Color(0x1AF4EFE7) // rgba(244,239,231,0.10)

// ── Text ──
internal val OnSurfaceLight = Color(0xFF2B2522)
internal val OnSurfaceVarLight = Color(0xFF5A524C)
internal val OnSurfaceDimLight = Color(0xFF8C8378)
internal val OnSurfaceDark = Color(0xFFF4EFE7)
internal val OnSurfaceVarDark = Color(0xFFB8AFA3)
internal val OnSurfaceDimDark = Color(0xFF7E756B)

// ── Foreground-on-foundation. The "flip": light mode uses warm-white on Clay etc.;
// dark mode uses warm-dark surface on the lifted foundation colors.
internal val OnFoundationLight = Color(0xFFFFFFFF)
internal val OnFoundationDark = DarkSurface

// Event-pill foreground stays light in both themes. Pills get alpha-faded for
// past/completed/pending — in dark mode that blends bg toward DarkSurface, and
// the standard dark-on-bright `onFoundation` token would collide with the
// darkened pill. Keeping pill text light side-steps the blend entirely.
internal val OnEventPillLight = Color(0xFFFFFFFF)
internal val OnEventPillDark = Color(0xFFF5F0E8)

// ── Error (standard M3 — distinct from brand red) ──
internal val ErrorLight = Color(0xFFBA1A1A)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorDark = Color(0xFF690005)
internal val OnErrorContainerLight = Color(0xFF410002)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)
