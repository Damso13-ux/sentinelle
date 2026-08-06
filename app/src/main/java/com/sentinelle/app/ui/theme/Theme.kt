package com.sentinelle.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

@Composable
fun AppTheme(
    // Follows the system now. The app used to hard-code dark and never touch
    // its own light scheme, which is a poor fit for a general-audience app —
    // most people are on light or "follow system".
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: wallpaper-derived colour would override the accent, and
    // with it the distinction between brand colour and status colour that the
    // whole palette is built on.
    dynamicColor: Boolean = false,
    themeVariant: ThemeVariant = ThemeVariant.INDIGO,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> withAccent(darkScheme, themeVariant, dark = true)

            else -> withAccent(lightScheme, themeVariant, dark = false)
        }

    CompositionLocalProvider(
        LocalSentinelleColors provides if (darkTheme) DarkSentinelleColors else LightSentinelleColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

// tertiary tracks primary: the palette uses one accent, and leaving tertiary
// on the base indigo would leak the default colour into the other variants.
private fun withAccent(
    base: ColorScheme,
    variant: ThemeVariant,
    dark: Boolean,
): ColorScheme =
    when (variant) {
        ThemeVariant.INDIGO -> base

        ThemeVariant.OCEAN ->
            if (dark) {
                base.copy(
                    primary = oceanPrimaryDark,
                    onPrimary = oceanOnPrimaryDark,
                    primaryContainer = oceanPrimaryContainerDark,
                    onPrimaryContainer = oceanOnPrimaryContainerDark,
                    tertiary = oceanPrimaryDark,
                    onTertiary = oceanOnPrimaryDark,
                    tertiaryContainer = oceanPrimaryContainerDark,
                    onTertiaryContainer = oceanOnPrimaryContainerDark,
                    inversePrimary = oceanPrimaryLight,
                )
            } else {
                base.copy(
                    primary = oceanPrimaryLight,
                    onPrimary = oceanOnPrimaryLight,
                    primaryContainer = oceanPrimaryContainerLight,
                    onPrimaryContainer = oceanOnPrimaryContainerLight,
                    tertiary = oceanPrimaryLight,
                    onTertiary = oceanOnPrimaryLight,
                    tertiaryContainer = oceanPrimaryContainerLight,
                    onTertiaryContainer = oceanOnPrimaryContainerLight,
                    inversePrimary = oceanPrimaryDark,
                )
            }

        ThemeVariant.PRUNE ->
            if (dark) {
                base.copy(
                    primary = prunePrimaryDark,
                    onPrimary = pruneOnPrimaryDark,
                    primaryContainer = prunePrimaryContainerDark,
                    onPrimaryContainer = pruneOnPrimaryContainerDark,
                    tertiary = prunePrimaryDark,
                    onTertiary = pruneOnPrimaryDark,
                    tertiaryContainer = prunePrimaryContainerDark,
                    onTertiaryContainer = pruneOnPrimaryContainerDark,
                    inversePrimary = prunePrimaryLight,
                )
            } else {
                base.copy(
                    primary = prunePrimaryLight,
                    onPrimary = pruneOnPrimaryLight,
                    primaryContainer = prunePrimaryContainerLight,
                    onPrimaryContainer = pruneOnPrimaryContainerLight,
                    tertiary = prunePrimaryLight,
                    onTertiary = pruneOnPrimaryLight,
                    tertiaryContainer = prunePrimaryContainerLight,
                    onTertiaryContainer = pruneOnPrimaryContainerLight,
                    inversePrimary = prunePrimaryDark,
                )
            }
    }
