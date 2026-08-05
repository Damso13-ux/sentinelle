package com.sentinelle.app.spam

import android.content.Context
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.runBlocking
import java.util.Calendar

// Rule-based spam scorer — no ML model, no training data, no dataset.
// Signals are weighted and summed into a 0..1 score. Call duration is
// deliberately not a signal: Call.Details doesn't expose it at screening
// time, and reading it after the fact would need READ_CALL_LOG, which
// Sentinelle doesn't request.
//
// History window and sensitivity are Pro-gated tuning knobs (see
// HeuristicSettings) — free users always get the values below, which are
// exactly what this detector did before tuning existed.
object HeuristicSpamDetector : SpamDetector {
    @Deprecated(
        "Not the effective threshold when Pro tuning is active — use " +
            "PreferencesManager.getEffectiveHeuristicSettings(context).blockThreshold instead.",
        ReplaceWith("HeuristicSettings.DEFAULT_BLOCK_THRESHOLD"),
    )
    const val BLOCK_THRESHOLD = HeuristicSettings.DEFAULT_BLOCK_THRESHOLD

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    private const val FREQUENCY_SATURATION = 8.0
    private const val FREQUENCY_SIGNAL_THRESHOLD = 4
    private const val FREQUENCY_WEIGHT = 0.4

    private const val BURST_THRESHOLD = 3
    private const val BURST_WEIGHT = 0.3

    private const val MIN_SAMPLES_FOR_TIME_SIGNAL = 5
    private const val BUSINESS_HOURS_RATIO_THRESHOLD = 0.8
    private const val BUSINESS_HOURS_WEIGHT = 0.15

    private const val PATTERNED_NUMBER_WEIGHT = 0.15

    override fun scoreCall(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): SpamScore {
        val now = System.currentTimeMillis()
        val settings = runBlocking { PreferencesManager.getEffectiveHeuristicSettings(context) }
        val timestamps =
            AppDatabase
                .getInstance(context)
                .callHistoryDao()
                .getTimestampsForNumberSince(phoneNumber, now - settings.historyWindowDays * DAY_MILLIS)
        return scoreFromHistory(timestamps, now, phoneNumber, settings.sensitivity)
    }

    override fun scoreSms(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): SpamScore {
        val now = System.currentTimeMillis()
        val settings = runBlocking { PreferencesManager.getEffectiveHeuristicSettings(context) }
        val timestamps =
            AppDatabase
                .getInstance(context)
                .smsHistoryDao()
                .getTimestampsForNumberSince(phoneNumber, now - settings.historyWindowDays * DAY_MILLIS)
        return scoreFromHistory(timestamps, now, phoneNumber, settings.sensitivity)
    }

    // Pure scoring logic — no Android/Room dependency, so it's directly
    // unit-testable (see HeuristicSpamDetectorTest). sensitivity is a
    // Pro-gated multiplier (see HeuristicSettings) applied to the summed
    // score before clamping — 1.0 (the default) reproduces the original,
    // untunable behavior exactly.
    fun scoreFromHistory(
        timestamps: List<Long>,
        now: Long,
        phoneNumber: Long,
        sensitivity: Double = HeuristicSettings.DEFAULT_SENSITIVITY,
    ): SpamScore {
        var score = 0.0
        val signals = mutableListOf<String>()

        val frequencyRatio = (timestamps.size / FREQUENCY_SATURATION).coerceAtMost(1.0)
        if (timestamps.size >= FREQUENCY_SIGNAL_THRESHOLD) {
            score += frequencyRatio * FREQUENCY_WEIGHT
            signals += "${timestamps.size} appels en 7 jours"
        }

        val last24h = timestamps.count { now - it <= DAY_MILLIS }
        if (last24h >= BURST_THRESHOLD) {
            score += BURST_WEIGHT
            signals += "$last24h appels en 24h"
        }

        if (timestamps.size >= MIN_SAMPLES_FOR_TIME_SIGNAL) {
            val businessHoursRatio = timestamps.count { isBusinessHours(it) }.toDouble() / timestamps.size
            if (businessHoursRatio >= BUSINESS_HOURS_RATIO_THRESHOLD) {
                score += BUSINESS_HOURS_WEIGHT
                signals += "concentré en heures ouvrées"
            }
        }

        if (hasPatternedDigits(phoneNumber)) {
            score += PATTERNED_NUMBER_WEIGHT
            signals += "numéro à motif répétitif"
        }

        return SpamScore(
            score = (score * sensitivity).coerceIn(0.0, 1.0),
            reason = signals.firstOrNull(),
            signals = signals,
        )
    }

    private fun isBusinessHours(timestampMillis: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMillis
        val isWeekday = calendar.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return isWeekday && hour in 9..18
    }

    // Robocall/spoofed numbers often have a suspiciously "round" local
    // subscriber number — 4+ repeated or sequential digits.
    private fun hasPatternedDigits(phoneNumber: Long): Boolean {
        val digits = phoneNumber.toString().takeLast(6)
        if (digits.length < 4) return false

        val hasRepeatRun = Regex("(\\d)\\1{3,}").containsMatchIn(digits)
        val hasSequentialRun =
            (0..digits.length - 4).any { start ->
                val window = digits.substring(start, start + 4).map { it - '0' }
                val ascending = window.zipWithNext().all { (a, b) -> b - a == 1 }
                val descending = window.zipWithNext().all { (a, b) -> a - b == 1 }
                ascending || descending
            }
        return hasRepeatRun || hasSequentialRun
    }
}
