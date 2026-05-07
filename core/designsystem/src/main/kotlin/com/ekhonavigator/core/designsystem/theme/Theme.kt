package com.ekhonavigator.core.designsystem.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/**
 * Ekho Navigator light color scheme — CSUCI Channel Islands rebrand.
 *
 * Role mapping (see design.md §2):
 *  - primary       = Clay         (chrome workhorse: FAB, active tab, primary CTAs)
 *  - secondary     = Sage         (personal events / "yours" affordances)
 *  - tertiary      = Horizon      (bookmarked / starred)
 *  - cardinal/shale live in [EkhoExtendedColors] — they're event-taxonomy
 *    tokens, not chrome, and don't fit M3 slots cleanly.
 */
@VisibleForTesting
val LightEkhoColorScheme = lightColorScheme(
    primary = ClayLight,
    onPrimary = OnFoundationLight,
    primaryContainer = ClaySoftLight,
    onPrimaryContainer = ClayDeepLight,
    secondary = SageLight,
    onSecondary = OnFoundationLight,
    secondaryContainer = SageSoftLight,
    onSecondaryContainer = Color(0xFF1F2D14),
    tertiary = HorizonLight,
    onTertiary = OnFoundationLight,
    tertiaryContainer = HorizonSoftLight,
    onTertiaryContainer = Color(0xFF3D1A02),
    error = ErrorLight,
    onError = OnFoundationLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = LightSurface,
    onBackground = OnSurfaceLight,
    surface = LightSurface,
    onSurface = OnSurfaceLight,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = OnSurfaceVarLight,
    surfaceTint = ClayLight,
    inverseSurface = DarkSurface,
    inverseOnSurface = OnSurfaceDark,
    inversePrimary = ClayDark,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE0D5C5),
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

/**
 * Ekho Navigator dark color scheme. Foreground/background flip in dark mode:
 * `onPrimary`/`onSecondary`/`onTertiary` resolve to the warm-dark surface
 * (`#1A1410`), NOT white. The lifted foundation hues are bright enough that
 * dark-on-color reads cleanly without vibrating. See design.md §2.
 */
@VisibleForTesting
val DarkEkhoColorScheme = darkColorScheme(
    primary = ClayDark,
    onPrimary = OnFoundationDark,
    primaryContainer = ClaySoftDark,
    onPrimaryContainer = ClayDeepDark,
    secondary = SageDark,
    onSecondary = OnFoundationDark,
    secondaryContainer = SageSoftDark,
    onSecondaryContainer = Color(0xFFD9E5CD),
    tertiary = HorizonDark,
    onTertiary = OnFoundationDark,
    tertiaryContainer = HorizonSoftDark,
    onTertiaryContainer = Color(0xFFFFE0CC),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = DarkSurface,
    onBackground = OnSurfaceDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = OnSurfaceVarDark,
    surfaceTint = ClayDark,
    inverseSurface = LightSurface,
    inverseOnSurface = OnSurfaceLight,
    inversePrimary = ClayLight,
    outline = OutlineDark,
    outlineVariant = Color(0xFF4D4035),
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

/** Light gradient companion — uses surfaceContainerLow so it reads as a tonal lift. */
val LightEkhoGradientColors = GradientColors(container = LightSurfaceContainerLow)

/** Dark gradient companion. */
val DarkEkhoGradientColors = GradientColors(container = DarkSurface)

val LightEkhoBackgroundTheme = BackgroundTheme(color = LightSurface)
val DarkEkhoBackgroundTheme = BackgroundTheme(color = DarkSurface)

/**
 * Ekho Navigator theme.
 *
 * @param darkTheme follow system by default
 * @param dynamicTheming enable Material You on Android 12+. Defaults to false until
 *        the cohesion-phase opt-in toggle (commit 6) ships in Settings.
 */
@Composable
fun EkhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicTheming: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicTheming && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkEkhoColorScheme
        else -> LightEkhoColorScheme
    }

    val gradientColors = when {
        dynamicTheming && supportsDynamicTheming() ->
            GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
        darkTheme -> DarkEkhoGradientColors
        else -> LightEkhoGradientColors
    }

    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 0.dp,
    )

    val tintTheme = when {
        dynamicTheming && supportsDynamicTheming() -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }

    val extendedColors = if (darkTheme) DarkEkhoExtendedColors else LightEkhoExtendedColors

    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
        LocalEkhoExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EkhoTypography,
            shapes = EkhoShapes,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
