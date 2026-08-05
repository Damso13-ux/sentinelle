package com.sentinelle.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    // Sentinelle is designed around its dark "Garde" look, so it defaults to
    // dark regardless of the system setting — see MainActivity.
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+, off by default to keep the
    // "Garde" brand palette consistent across devices.
    dynamicColor: Boolean = false,
    // Pro-gated accent swap — see ThemeVariant. Only takes effect when
    // dynamicColor is off, since dynamic color already overrides the accent
    // from the device wallpaper.
    themeVariant: ThemeVariant = ThemeVariant.GARDE,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> withAccent(darkScheme, themeVariant)

            else -> lightScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun withAccent(
    base: androidx.compose.material3.ColorScheme,
    variant: ThemeVariant,
) = when (variant) {
    ThemeVariant.GARDE -> base

    ThemeVariant.CORAIL ->
        base.copy(
            primary = primaryDarkCorail,
            onPrimary = onPrimaryDarkCorail,
            primaryContainer = primaryContainerDarkCorail,
            onPrimaryContainer = onPrimaryContainerDarkCorail,
            tertiary = tertiaryDarkCorail,
            onTertiary = onTertiaryDarkCorail,
            tertiaryContainer = tertiaryContainerDarkCorail,
            onTertiaryContainer = onTertiaryContainerDarkCorail,
            inversePrimary = inversePrimaryDarkCorail,
        )

    ThemeVariant.VIOLET ->
        base.copy(
            primary = primaryDarkViolet,
            onPrimary = onPrimaryDarkViolet,
            primaryContainer = primaryContainerDarkViolet,
            onPrimaryContainer = onPrimaryContainerDarkViolet,
            tertiary = tertiaryDarkViolet,
            onTertiary = onTertiaryDarkViolet,
            tertiaryContainer = tertiaryContainerDarkViolet,
            onTertiaryContainer = onTertiaryContainerDarkViolet,
            inversePrimary = inversePrimaryDarkViolet,
        )
}
