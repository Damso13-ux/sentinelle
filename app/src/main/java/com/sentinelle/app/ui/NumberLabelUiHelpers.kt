package com.sentinelle.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.sentinelle.app.data.NumberLabelEntity

val NUMBER_LABEL_CATEGORIES =
    listOf(
        NumberLabelEntity.CATEGORY_DEMARCHAGE,
        NumberLabelEntity.CATEGORY_ARNAQUE,
        NumberLabelEntity.CATEGORY_ROBOT,
        NumberLabelEntity.CATEGORY_LIVRAISON,
        NumberLabelEntity.CATEGORY_BANQUE_ASSURANCE,
        NumberLabelEntity.CATEGORY_AUTRE,
    )

fun categoryLabel(category: String): String = NumberLabelEntity.displayName(category)

fun categoryIcon(category: String): ImageVector =
    when (category) {
        NumberLabelEntity.CATEGORY_DEMARCHAGE -> Icons.Rounded.Campaign
        NumberLabelEntity.CATEGORY_ARNAQUE -> Icons.Rounded.ReportProblem
        NumberLabelEntity.CATEGORY_ROBOT -> Icons.Rounded.SmartToy
        NumberLabelEntity.CATEGORY_LIVRAISON -> Icons.Rounded.LocalShipping
        NumberLabelEntity.CATEGORY_BANQUE_ASSURANCE -> Icons.Rounded.AccountBalance
        else -> Icons.AutoMirrored.Rounded.Label
    }
