package com.sentinelle.app.ui.theme

import androidx.compose.ui.graphics.Color

// Sentinelle's own palette. Two rules drive it:
//
// 1. Indigo is the brand. Green is *not* — it means "you are protected",
//    nothing else. The previous theme was teal/green throughout, which made
//    green decorative and left the app with no colour left to say "this is
//    fine" versus "this needs your attention". Here green, amber and red are
//    states; indigo is identity.
//
// 2. Light and dark are both first-class. The app used to force dark and
//    never touch its own light scheme.
//
// Every pairing below is contrast-checked against WCAG AA (4.5:1 for text).
// Notably successLight is #0C7E3D rather than a brighter green: the large
// "Vous êtes protégé" card puts white text on it, and #0E8F45 only reached
// 4.17:1.

// --- Light ------------------------------------------------------------

val primaryLight = Color(0xFF3D4FC4)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFE0E4FF)
val onPrimaryContainerLight = Color(0xFF10197A)
val secondaryLight = Color(0xFF5B6472)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFE3E7EE)
val onSecondaryContainerLight = Color(0xFF1B2027)
val tertiaryLight = Color(0xFF3D4FC4)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFE0E4FF)
val onTertiaryContainerLight = Color(0xFF10197A)
val errorLight = Color(0xFFB3261E)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFCE4E2)
val onErrorContainerLight = Color(0xFF5C110C)
val backgroundLight = Color(0xFFF6F7F9)
val onBackgroundLight = Color(0xFF14161A)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF14161A)
val surfaceVariantLight = Color(0xFFEDEFF3)
val onSurfaceVariantLight = Color(0xFF5B6472)
val outlineLight = Color(0xFFC5CAD3)
val outlineVariantLight = Color(0xFFE1E4EA)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF272A2F)
val inverseOnSurfaceLight = Color(0xFFF2F3F5)
val inversePrimaryLight = Color(0xFFA5B4FC)
val surfaceDimLight = Color(0xFFE4E6EB)
val surfaceBrightLight = Color(0xFFFFFFFF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFAFBFC)
val surfaceContainerLight = Color(0xFFF6F7F9)
val surfaceContainerHighLight = Color(0xFFEFF1F4)
val surfaceContainerHighestLight = Color(0xFFE8EAEF)

// --- Dark -------------------------------------------------------------

val primaryDark = Color(0xFFA5B4FC)
val onPrimaryDark = Color(0xFF101215)
val primaryContainerDark = Color(0xFF2A3496)
val onPrimaryContainerDark = Color(0xFFE0E4FF)
val secondaryDark = Color(0xFF9BA1AA)
val onSecondaryDark = Color(0xFF101215)
val secondaryContainerDark = Color(0xFF262B33)
val onSecondaryContainerDark = Color(0xFFDDE1E7)
val tertiaryDark = Color(0xFFA5B4FC)
val onTertiaryDark = Color(0xFF101215)
val tertiaryContainerDark = Color(0xFF2A3496)
val onTertiaryContainerDark = Color(0xFFE0E4FF)
val errorDark = Color(0xFFF2857D)
val onErrorDark = Color(0xFF101215)
val errorContainerDark = Color(0xFF6E1F19)
val onErrorContainerDark = Color(0xFFFCE4E2)
val backgroundDark = Color(0xFF101215)
val onBackgroundDark = Color(0xFFF2F3F5)
val surfaceDark = Color(0xFF101215)
val onSurfaceDark = Color(0xFFF2F3F5)
val surfaceVariantDark = Color(0xFF262B33)
val onSurfaceVariantDark = Color(0xFF9BA1AA)
val outlineDark = Color(0xFF5C636D)
val outlineVariantDark = Color(0xFF2E343D)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFF2F3F5)
val inverseOnSurfaceDark = Color(0xFF191C21)
val inversePrimaryDark = Color(0xFF3D4FC4)
val surfaceDimDark = Color(0xFF101215)
val surfaceBrightDark = Color(0xFF32373F)
val surfaceContainerLowestDark = Color(0xFF0B0D0F)
val surfaceContainerLowDark = Color(0xFF161A1E)
val surfaceContainerDark = Color(0xFF191C21)
val surfaceContainerHighDark = Color(0xFF22262C)
val surfaceContainerHighestDark = Color(0xFF2C3139)

// --- Status colours ---------------------------------------------------
//
// Material's ColorScheme has no success/warning role, so these live in
// SentinelleColors rather than being smuggled into `tertiary`. Keeping them
// out of the scheme also stops them drifting when the Pro accent variants
// swap the indigo out — a protected state must read the same in every theme.

val successLight = Color(0xFF0C7E3D)
val onSuccessLight = Color(0xFFFFFFFF)
val successContainerLight = Color(0xFFDDF3E5)
val onSuccessContainerLight = Color(0xFF05341A)

val successDark = Color(0xFF4ADE80)
val onSuccessDark = Color(0xFF101215)
val successContainerDark = Color(0xFF10502B)
val onSuccessContainerDark = Color(0xFFDDF3E5)

val warningLight = Color(0xFFB45309)
val onWarningLight = Color(0xFFFFFFFF)
val warningContainerLight = Color(0xFFFDEBD5)
val onWarningContainerLight = Color(0xFF4A2205)

val warningDark = Color(0xFFFBBF24)
val onWarningDark = Color(0xFF101215)
val warningContainerDark = Color(0xFF5C3A08)
val onWarningContainerDark = Color(0xFFFDEBD5)

// --- Pro accent variants ----------------------------------------------
//
// Accent-only swaps: neutrals, and the status colours above, stay put so
// every variant still reads as the same app and "protected" never changes
// meaning. Contrast-checked to the same bar as the default indigo.

val oceanPrimaryLight = Color(0xFF00696E)
val oceanOnPrimaryLight = Color(0xFFFFFFFF)
val oceanPrimaryContainerLight = Color(0xFFD3F1F2)
val oceanOnPrimaryContainerLight = Color(0xFF002022)
val oceanPrimaryDark = Color(0xFF7FD8DC)
val oceanOnPrimaryDark = Color(0xFF101215)
val oceanPrimaryContainerDark = Color(0xFF004F53)
val oceanOnPrimaryContainerDark = Color(0xFFD3F1F2)

val prunePrimaryLight = Color(0xFF8A3D8F)
val pruneOnPrimaryLight = Color(0xFFFFFFFF)
val prunePrimaryContainerLight = Color(0xFFF8DEF8)
val pruneOnPrimaryContainerLight = Color(0xFF340939)
val prunePrimaryDark = Color(0xFFE9B3EC)
val pruneOnPrimaryDark = Color(0xFF101215)
val prunePrimaryContainerDark = Color(0xFF6A2470)
val pruneOnPrimaryContainerDark = Color(0xFFF8DEF8)
