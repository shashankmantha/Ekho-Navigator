package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand colors that don't fit cleanly into Material 3's role slots — `cardinal`
 * (Canvas LMS event identity) and `shale` (generic campus events). They're event-
 * taxonomy tokens, not chrome, so claiming `primary*`/`secondary*`/`tertiary*` for
 * them would either dilute the role meaning (Cardinal isn't "secondary brand") or
 * displace tokens that already have semantic homes (Sage owns secondary, Horizon
 * owns tertiary).
 *
 * Soft variants exist for low-emphasis surfaces (Canvas pill background, etc.).
 */
@Stable
data class EkhoExtendedColors(
    val cardinal: Color = Color.Unspecified,
    val cardinalSoft: Color = Color.Unspecified,
    val onCardinalSoft: Color = Color.Unspecified,
    val shale: Color = Color.Unspecified,
    /**
     * Foreground color for use ON foundation colors (Clay, Cardinal, Sage, Horizon).
     * In light mode this is `#FFFFFF`; in dark mode it's the warm-dark `surface`
     * (`#1A1410`). Matches the foreground/background flip from design.md §2.
     */
    val onFoundation: Color = Color.Unspecified,
)

internal val LightEkhoExtendedColors = EkhoExtendedColors(
    cardinal = CardinalLight,
    cardinalSoft = CardinalSoftLight,
    onCardinalSoft = Color(0xFF40000F),
    shale = ShaleLight,
    onFoundation = OnFoundationLight,
)

internal val DarkEkhoExtendedColors = EkhoExtendedColors(
    cardinal = CardinalDark,
    cardinalSoft = CardinalSoftDark,
    onCardinalSoft = Color(0xFFFFD9DF),
    shale = ShaleDark,
    onFoundation = OnFoundationDark,
)

val LocalEkhoExtendedColors = staticCompositionLocalOf { LightEkhoExtendedColors }

/** Convenience accessor — `EkhoColors.current.cardinal` reads inside any composable
 *  the way `MaterialTheme.colorScheme.primary` does. */
object EkhoColors {
    val current: EkhoExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEkhoExtendedColors.current
}
