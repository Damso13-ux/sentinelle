package com.sentinelle.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sentinelle.app.R

val bodyFontFamily =
    FontFamily(
        Font(R.font.atkinson_hyperlegible_next_vf_variable),
    )

// Default Material 3 typography values
val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge =
            baseline.displayLarge.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        displayMedium =
            baseline.displayMedium.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        displaySmall =
            baseline.displaySmall.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.ExtraBold,
            ),
        headlineLarge =
            baseline.headlineLarge.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        headlineMedium =
            baseline.headlineMedium.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        headlineSmall =
            baseline.headlineSmall.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        titleLarge =
            baseline.titleLarge.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        titleMedium =
            baseline.titleMedium.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        titleSmall =
            baseline.titleSmall.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            baseline.bodyLarge.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        bodyMedium =
            baseline.bodyMedium.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Normal,
            ),
        bodySmall =
            baseline.bodySmall.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            baseline.labelLarge.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        labelMedium =
            baseline.labelMedium.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        labelSmall =
            baseline.labelSmall.copy(
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Medium,
            ),
    )
