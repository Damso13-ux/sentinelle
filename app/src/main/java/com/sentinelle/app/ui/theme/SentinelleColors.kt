package com.sentinelle.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status colours, alongside Material's ColorScheme rather than inside it.
 *
 * Material 3 has no success or warning role. The usual workaround is to
 * borrow `tertiary`, but that breaks as soon as an accent variant swaps the
 * brand colour: "protected" would turn teal or purple depending on the
 * user's theme, and a state colour that changes meaning isn't a state
 * colour. These stay fixed across every variant.
 *
 * Read through [SentinelleTheme.colors], not directly, so light/dark
 * resolution happens in one place.
 */
@Immutable
data class SentinelleColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LightSentinelleColors =
    SentinelleColors(
        success = successLight,
        onSuccess = onSuccessLight,
        successContainer = successContainerLight,
        onSuccessContainer = onSuccessContainerLight,
        warning = warningLight,
        onWarning = onWarningLight,
        warningContainer = warningContainerLight,
        onWarningContainer = onWarningContainerLight,
    )

val DarkSentinelleColors =
    SentinelleColors(
        success = successDark,
        onSuccess = onSuccessDark,
        successContainer = successContainerDark,
        onSuccessContainer = onSuccessContainerDark,
        warning = warningDark,
        onWarning = onWarningDark,
        warningContainer = warningContainerDark,
        onWarningContainer = onWarningContainerDark,
    )

// Defaults to light so a preview that forgets to wrap in AppTheme still
// renders something legible rather than throwing.
val LocalSentinelleColors = staticCompositionLocalOf { LightSentinelleColors }

/** Companion to MaterialTheme for the roles Material doesn't define. */
object SentinelleTheme {
    val colors: SentinelleColors
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalSentinelleColors.current
}
