package com.sentinelle.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.ui.theme.SentinelleTheme
import java.text.DateFormat
import java.util.Locale

fun getTypeIcon(type: String): ImageVector =
    when (type) {
        PatternListEntity.TYPE_BLOCK -> Icons.Rounded.Block
        PatternListEntity.TYPE_ALLOW -> Icons.Rounded.Info
        else -> Icons.Rounded.Block
    }

/**
 * Read from the theme rather than hard-coded: these used to be the old
 * Garde teal and a fixed red, which meant they didn't move with the light
 * scheme at all and stayed tuned for a dark background.
 *
 * "Allow" is the success green — a list that lets a number through is the
 * same idea as being protected — and "block" is the error red.
 */
@Composable
@ReadOnlyComposable
fun getTypeColor(type: String): Color =
    when (type) {
        PatternListEntity.TYPE_ALLOW -> SentinelleTheme.colors.success
        else -> MaterialTheme.colorScheme.error
    }

fun getTypeLabel(
    type: String,
    channel: String,
): String =
    when (type) {
        PatternListEntity.TYPE_BLOCK -> {
            when (channel.lowercase()) {
                "sms" -> "Mots-clés SMS bloqués"
                else -> "Numéros et préfixes bloqués"
            }
        }

        PatternListEntity.TYPE_ALLOW -> {
            when (channel.lowercase()) {
                "sms" -> "Mots-clés SMS autorisés"
                else -> "Numéros et préfixes identifiés ou autorisés"
            }
        }

        else -> {
            "Liste"
        }
    }

fun getTypeActionLabel(type: String): String =
    when (type) {
        PatternListEntity.TYPE_BLOCK -> "Bloquer"
        PatternListEntity.TYPE_ALLOW -> "Autoriser"
        else -> "Bloquer"
    }

fun getChannelIcon(channel: String): ImageVector =
    when (channel.lowercase()) {
        "sms" -> Icons.Rounded.Sms
        else -> Icons.Rounded.Phone
    }

fun getChannelLabel(channel: String): String =
    when (channel.lowercase()) {
        "sms" -> "SMS"
        else -> "Téléphone"
    }

fun formatVersionDate(version: String): String =
    try {
        val instant = java.time.Instant.parse(version)
        val display = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
        display.format(java.util.Date.from(instant))
    } catch (_: Exception) {
        version
    }
