package com.sentinelle.app.spam

import android.content.Context

data class SpamScore(
    val score: Double,
    val reason: String?,
    val signals: List<String> = emptyList(),
)

// Interface behind which a future on-device ML model (e.g. TFLite) can be
// swapped in without touching PatternManager or the call/SMS screening
// services — only SpamDetectorProvider needs to change.
interface SpamDetector {
    fun scoreCall(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): SpamScore

    fun scoreSms(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): SpamScore
}

object SpamDetectorProvider {
    fun get(): SpamDetector = HeuristicSpamDetector
}
