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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Ekho Navigator light color scheme — derived from CSUCI garnet seed via Material 3
 * tonal palette mapping. Every slot fills explicitly so we control the surface
 * container ladder rather than letting M3 derive defaults that don't match our seed.
 */
@VisibleForTesting
val LightEkhoColorScheme = lightColorScheme(
    primary = GarnetTone40,
    onPrimary = NeutralTone100,
    primaryContainer = GarnetTone90,
    onPrimaryContainer = GarnetTone10,
    secondary = SageGreenTone40,
    onSecondary = NeutralTone100,
    secondaryContainer = SageGreenTone90,
    onSecondaryContainer = SageGreenTone10,
    tertiary = AmberTone40,
    onTertiary = NeutralTone100,
    tertiaryContainer = AmberTone90,
    onTertiaryContainer = AmberTone10,
    error = ErrorLight,
    onError = NeutralTone100,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = LightSurface,
    onBackground = NeutralTone20,
    surface = LightSurface,
    onSurface = NeutralTone20,
    surfaceVariant = NeutralVariantTone90,
    onSurfaceVariant = NeutralVariantTone30,
    surfaceTint = GarnetTone40,
    inverseSurface = DarkSurface,
    inverseOnSurface = NeutralTone90,
    inversePrimary = GarnetTone80,
    outline = NeutralVariantTone50,
    outlineVariant = NeutralVariantTone80,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

/**
 * Ekho Navigator dark color scheme — paired with light via M3 tonal inversion.
 * Primary uses tone 80 (light pink) for legibility on dark surface; container uses
 * tone 30 (dim brick) so the brand still reads as garnet-warm rather than pastel.
 */
@VisibleForTesting
val DarkEkhoColorScheme = darkColorScheme(
    primary = GarnetTone80,
    onPrimary = GarnetTone20,
    primaryContainer = GarnetTone30,
    onPrimaryContainer = GarnetTone90,
    secondary = SageGreenTone80,
    onSecondary = SageGreenTone20,
    secondaryContainer = SageGreenTone30,
    onSecondaryContainer = SageGreenTone90,
    tertiary = AmberTone80,
    onTertiary = AmberTone20,
    tertiaryContainer = AmberTone30,
    onTertiaryContainer = AmberTone90,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = DarkSurface,
    onBackground = NeutralTone90,
    surface = DarkSurface,
    onSurface = NeutralTone90,
    surfaceVariant = NeutralVariantTone30,
    onSurfaceVariant = NeutralVariantTone80,
    surfaceTint = GarnetTone80,
    inverseSurface = NeutralTone90,
    inverseOnSurface = NeutralTone20,
    inversePrimary = GarnetTone40,
    outline = NeutralVariantTone60,
    outlineVariant = NeutralVariantTone30,
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

    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
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
