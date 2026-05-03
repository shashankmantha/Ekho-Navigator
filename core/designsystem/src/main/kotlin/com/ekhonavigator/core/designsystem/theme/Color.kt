package com.ekhonavigator.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Ekho Navigator color palette — softened CSUCI garnet, single accent family.
 *
 * Replaces the prior "Kinetic Monolith" three-color setup (red + neon cyan + amber)
 * which over-saturated and clashed. The new palette derives from one seed (CSUCI
 * garnet ~#8B2D3A) and lets Material 3's tonal-palette pattern do the heavy lifting:
 * primary for brand chrome and key CTAs, surface tonal layers for depth without
 * shadow, secondary/tertiary as muted harmonics rather than competing accents.
 */

// ── Brand: CSUCI Garnet (primary tonal palette) ────────────
internal val GarnetTone10 = Color(0xFF3F0010)
internal val GarnetTone20 = Color(0xFF5F1124)
internal val GarnetTone30 = Color(0xFF7A1F30)
internal val GarnetTone40 = Color(0xFF8B2D3A) // brand seed
internal val GarnetTone80 = Color(0xFFFFB3B9)
internal val GarnetTone90 = Color(0xFFFFD9DC)
internal val GarnetTone95 = Color(0xFFFFEDED)

// ── Secondary: Sage Green — calendar-side palette saturation (GCal-leaning).
// Distinct hue presence so CUSTOM chips read as "green" at a glance, not
// "muted warm." Tone40 sits between gray-green and Christmas-green. ──
internal val SageGreenTone10 = Color(0xFF002111)
internal val SageGreenTone20 = Color(0xFF003820)
internal val SageGreenTone30 = Color(0xFF1E5236)
internal val SageGreenTone40 = Color(0xFF3F8C5F)
internal val SageGreenTone80 = Color(0xFFA5D5B6)
internal val SageGreenTone90 = Color(0xFFC1ECCF)

// ── Source-distinction: Slate Blue — boosted chroma so CANVAS chips read
// as a real blue (GCal Peacock-leaning) rather than warm-leaning gray.
// Public so feature modules can reference until EkhoSourceColors wrapper. ──
val SlateBlueTone10 = Color(0xFF001B3C)
val SlateBlueTone20 = Color(0xFF003063)
val SlateBlueTone30 = Color(0xFF0A4A8E)
val SlateBlueTone40 = Color(0xFF3B7FBF)
val SlateBlueTone80 = Color(0xFFA8C8EE)
val SlateBlueTone90 = Color(0xFFCFE0F8)

// ── Tertiary: Amber — warmer/saturated golden, away from the prior muddy
// brown. Reads as "starred / important" the way GCal Banana does. ──
internal val AmberTone10 = Color(0xFF2A1B00)
internal val AmberTone20 = Color(0xFF4A3300)
internal val AmberTone30 = Color(0xFF7A5810)
internal val AmberTone40 = Color(0xFFC68729)
internal val AmberTone80 = Color(0xFFF0CB85)
internal val AmberTone90 = Color(0xFFF8E2B5)

// ── Neutral (faintly warm-tinted greys for surfaces / text) ────────
// Warmth dialed way back from the original "salmon" levels — R bias is now
// 1-3 points over G/B (was 8-10), reading as off-white rather than pink.
internal val NeutralTone0 = Color(0xFF000000)
internal val NeutralTone10 = Color(0xFF1A1112)
internal val NeutralTone20 = Color(0xFF221919)
internal val NeutralTone30 = Color(0xFF382E2E)
internal val NeutralTone90 = Color(0xFFE5E1E0)
internal val NeutralTone95 = Color(0xFFF2EFEE)
internal val NeutralTone98 = Color(0xFFFBFAF9)
internal val NeutralTone100 = Color(0xFFFFFFFF)

// ── Neutral Variant (subtly warm greys for surface variants / outlines) ──
internal val NeutralVariantTone30 = Color(0xFF524345)
internal val NeutralVariantTone50 = Color(0xFF847374)
internal val NeutralVariantTone60 = Color(0xFF9F8C8D)
internal val NeutralVariantTone80 = Color(0xFFC9C5C4)
internal val NeutralVariantTone90 = Color(0xFFE5E2E1)

// ── Light surface ladder (M3 surfaceContainer* tonal stacking) ──
internal val LightSurface = NeutralTone98
internal val LightSurfaceContainerLowest = NeutralTone100
internal val LightSurfaceContainerLow = Color(0xFFF7F6F5)
internal val LightSurfaceContainer = Color(0xFFF2F1F0)
internal val LightSurfaceContainerHigh = Color(0xFFEDECEB)
internal val LightSurfaceContainerHighest = Color(0xFFE8E7E6)

// ── Dark surface ladder ────────────────────────────────────
internal val DarkSurface = NeutralTone10
internal val DarkSurfaceContainerLowest = Color(0xFF140C0D)
internal val DarkSurfaceContainerLow = NeutralTone20
internal val DarkSurfaceContainer = Color(0xFF261D1D)
internal val DarkSurfaceContainerHigh = Color(0xFF312727)
internal val DarkSurfaceContainerHighest = Color(0xFF3C3232)

// ── Error (standard M3 — stops looking like brand red) ─────
internal val ErrorLight = Color(0xFFBA1A1A)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorDark = Color(0xFF690005)
internal val OnErrorContainerLight = Color(0xFF410002)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

