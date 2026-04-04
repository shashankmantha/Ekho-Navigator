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
 * Light default theme color scheme
 */
@VisibleForTesting
val LightDefaultColorScheme = lightColorScheme(
    primary = SchoolRed,
    onPrimary = Color.White,
    primaryContainer = SchoolRedContainer,
    onPrimaryContainer = Color.White,
    secondary = DolphinCyan,
    onSecondary = DolphinCyanDark,
    secondaryContainer = DolphinCyanContainer,
    onSecondaryContainer = DolphinCyanDark,
    tertiary = CampusAmber,
    onTertiary = Color.White,
    tertiaryContainer = CampusAmberContainer,
    onTertiaryContainer = CampusAmberDark,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = ErrorRed,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    inverseSurface = DarkSurface,
    inverseOnSurface = LightSurfaceContainerLow,
    outline = GhostOutline.copy(alpha = 0.15f),
)

/**
 * Dark default theme color scheme
 */
@VisibleForTesting
val DarkDefaultColorScheme = darkColorScheme(
    primary = SchoolRedBright,
    onPrimary = Color.Black,
    primaryContainer = SchoolRedContainer,
    onPrimaryContainer = Color.White,
    secondary = DolphinCyan,
    onSecondary = DolphinCyanDark,
    secondaryContainer = DolphinCyanContainer,
    onSecondaryContainer = DolphinCyanDark,
    tertiary = CampusAmber,
    onTertiary = Color.White,
    tertiaryContainer = CampusAmberContainer,
    onTertiaryContainer = CampusAmberDark,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = ErrorRed,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    inverseSurface = LightSurface,
    inverseOnSurface = DarkSurfaceContainerLow,
    outline = GhostOutline.copy(alpha = 0.15f),
)

/**
 * Light Ekho Navigator theme color scheme
 */
@VisibleForTesting
val LightEkhoNavigatorColorScheme = lightColorScheme(
    primary = SchoolRed,
    onPrimary = Color.White,
    primaryContainer = SchoolRedContainer,
    onPrimaryContainer = Color.White,
    secondary = DolphinCyan,
    onSecondary = DolphinCyanDark,
    secondaryContainer = DolphinCyanContainer,
    onSecondaryContainer = DolphinCyanDark,
    tertiary = CampusAmber,
    onTertiary = Color.White,
    tertiaryContainer = CampusAmberContainer,
    onTertiaryContainer = CampusAmberDark,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = ErrorRed,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    inverseSurface = DarkSurface,
    inverseOnSurface = LightSurfaceContainerLow,
    outline = GhostOutline.copy(alpha = 0.15f),
)

/**
 * Dark Ekho Navigator theme color scheme
 */
@VisibleForTesting
val DarkEkhoNavigatorColorScheme = darkColorScheme(
    primary = SchoolRedBright,
    onPrimary = Color.Black,
    primaryContainer = SchoolRedContainer,
    onPrimaryContainer = Color.White,
    secondary = DolphinCyan,
    onSecondary = DolphinCyanDark,
    secondaryContainer = DolphinCyanContainer,
    onSecondaryContainer = DolphinCyanDark,
    tertiary = CampusAmber,
    onTertiary = Color.White,
    tertiaryContainer = CampusAmberContainer,
    onTertiaryContainer = CampusAmberDark,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = ErrorRed,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    inverseSurface = LightSurface,
    inverseOnSurface = DarkSurfaceContainerLow,
    outline = GhostOutline.copy(alpha = 0.15f),
)

/**
 * Light Ekho Navigator gradient colors
 */
val LightEkhoNavigatorGradientColors = GradientColors(container = LightSurfaceContainerLow)

/**
 * Dark Ekho Navigator gradient colors
 */
val DarkEkhoNavigatorGradientColors = GradientColors(container = DarkSurface)

/**
 * Light Ekho Navigator background theme
 */
val LightEkhoNavigatorBackgroundTheme = BackgroundTheme(color = LightSurface)

/**
 * Dark Ekho Navigator background theme
 */
val DarkEkhoNavigatorBackgroundTheme = BackgroundTheme(color = DarkSurface)

/**
 * Ekho Navigator theme.
 *
 * @param darkTheme Whether the theme should use a dark color scheme (follows system by default).
 * @param androidTheme Whether the theme should use the Android theme color scheme instead of the
 *        default theme.
 * @param disableDynamicTheming If `true`, disables the use of dynamic theming, even when it is
 *        supported. This parameter has no effect if [androidTheme] is `true`.
 */
@Composable
fun EkhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    androidTheme: Boolean = false,
    disableDynamicTheming: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Color scheme
    val colorScheme = when {
        androidTheme -> if (darkTheme) DarkEkhoNavigatorColorScheme else LightEkhoNavigatorColorScheme
        !disableDynamicTheming && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> if (darkTheme) DarkDefaultColorScheme else LightDefaultColorScheme
    }
    // Gradient colors
    val emptyGradientColors = GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    val defaultGradientColors = GradientColors(
        top = colorScheme.inverseOnSurface,
        bottom = colorScheme.primaryContainer,
        container = colorScheme.surface,
    )
    val gradientColors = when {
        androidTheme -> if (darkTheme) DarkEkhoNavigatorGradientColors else LightEkhoNavigatorGradientColors
        !disableDynamicTheming && supportsDynamicTheming() -> emptyGradientColors
        else -> defaultGradientColors
    }
    // Background theme
    val defaultBackgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 0.dp,
    )
    val backgroundTheme = when {
        androidTheme -> if (darkTheme) DarkEkhoNavigatorBackgroundTheme else LightEkhoNavigatorBackgroundTheme
        else -> defaultBackgroundTheme
    }
    val tintTheme = when {
        androidTheme -> TintTheme()
        !disableDynamicTheming && supportsDynamicTheming() -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }
    // Composition locals
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EkhoTypography,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
