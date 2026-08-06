package com.sentinelle.app.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Exercises HeuristicSpamDetector.scoreFromHistory, the pure (no Android/Room)
// half of the scorer — the DB-backed half (scoreCall) is a thin wrapper around
// this, following the same style as PhoneNumberMatcher/ListPriorityService tests.
class HeuristicSpamDetectorTest {
    private val dayMillis = 24 * 60 * 60 * 1000L

    @Test
    fun noHistoryScoresZero() {
        // 33612345678 is deliberately avoided here: its last 6 digits ("345678")
        // trigger the sequential-digits pattern signal on their own, which would
        // make this assertion wrong regardless of call history.
        val result = HeuristicSpamDetector.scoreFromHistory(emptyList(), now = 0L, phoneNumber = 33698427L)
        assertEquals(0.0, result.score, 0.0001)
        assertTrue(result.signals.isEmpty())
    }

    @Test
    fun fewCallsBelowThresholdAddNoFrequencySignal() {
        val now = 10 * dayMillis
        val timestamps = listOf(now - dayMillis, now - 2 * dayMillis)
        val result = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33612345678L)
        assertTrue(result.signals.none { it.contains("appels en 7 jours") })
    }

    @Test
    fun manyCallsInWindowRaiseScoreAndAddFrequencySignal() {
        val now = 10 * dayMillis
        val timestamps = (1..8).map { now - it * (dayMillis / 10) }
        val result = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33612345678L)
        assertTrue(result.score > 0.0)
        assertTrue(result.signals.any { it.contains("appels en 7 jours") })
    }

    @Test
    fun burstInLast24hAddsSignal() {
        val now = 10 * dayMillis
        val timestamps = listOf(now - 1_000L, now - 2_000L, now - 3_000L)
        val result = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33612345678L)
        assertTrue(result.signals.any { it.contains("appels en 24h") })
    }

    @Test
    fun patternedNumberAddsSignal() {
        // Repeated-digit "local" number.
        val result = HeuristicSpamDetector.scoreFromHistory(emptyList(), now = 0L, phoneNumber = 33199999L)
        assertTrue(result.signals.any { it.contains("motif répétitif") })
    }

    @Test
    fun sequentialDigitsAddPatternedSignal() {
        val result = HeuristicSpamDetector.scoreFromHistory(emptyList(), now = 0L, phoneNumber = 33112345L)
        assertTrue(result.signals.any { it.contains("motif répétitif") })
    }

    @Test
    fun ordinaryNumberWithNoHistoryHasNoPatternSignal() {
        val result = HeuristicSpamDetector.scoreFromHistory(emptyList(), now = 0L, phoneNumber = 33698427L)
        assertTrue(result.signals.none { it.contains("motif répétitif") })
    }

    @Test
    fun scoreIsClampedToOne() {
        val now = 10 * dayMillis
        // Saturate every signal at once: high frequency, burst, business hours, patterned number.
        val timestamps = (1..30).map { now - it * 1_000L }
        val result = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33199999L)
        assertTrue(result.score <= 1.0)
    }

    @Test
    fun reasonIsFirstSignalWhenPresent() {
        val now = 10 * dayMillis
        val timestamps = listOf(now - 1_000L, now - 2_000L, now - 3_000L)
        val result = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33612345678L)
        assertEquals(result.signals.firstOrNull(), result.reason)
    }

    // --- Pro-gated tuning (sensitivity / history window) ------------------

    @Test
    fun defaultSensitivityLeavesScoreUnchanged() {
        val now = 10 * dayMillis
        val timestamps = (1..6).map { now - it * (dayMillis / 10) }
        val explicitDefault =
            HeuristicSpamDetector.scoreFromHistory(
                timestamps,
                now,
                phoneNumber = 33698427L,
                sensitivity = HeuristicSettings.DEFAULT_SENSITIVITY,
            )
        val implicitDefault = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33698427L)
        assertEquals(implicitDefault.score, explicitDefault.score, 0.0001)
    }

    @Test
    fun higherSensitivityRaisesScoreLowerSensitivityDropsIt() {
        val now = 10 * dayMillis
        // 6 calls: frequency signal fires but the raw score stays well under
        // 1.0, so the multiplier is visible in both directions without
        // hitting the clamp.
        val timestamps = (1..6).map { now - it * (dayMillis / 10) }
        val baseline = HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33698427L)
        val moreSensitive =
            HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33698427L, sensitivity = 1.5)
        val lessSensitive =
            HeuristicSpamDetector.scoreFromHistory(timestamps, now, phoneNumber = 33698427L, sensitivity = 0.5)

        assertTrue(moreSensitive.score > baseline.score)
        assertTrue(lessSensitive.score < baseline.score)
        assertEquals(baseline.score * 1.5, moreSensitive.score, 0.0001)
        assertEquals(baseline.score * 0.5, lessSensitive.score, 0.0001)
    }

    @Test
    fun sensitivityCannotPushScoreAboveOne() {
        val now = 10 * dayMillis
        val timestamps = (1..30).map { now - it * 1_000L }
        val result =
            HeuristicSpamDetector.scoreFromHistory(
                timestamps,
                now,
                phoneNumber = 33199999L,
                sensitivity = HeuristicSettings.MAX_SENSITIVITY,
            )
        assertTrue(result.score <= 1.0)
    }

    @Test
    fun sensitivityDoesNotInventOrRemoveSignals() {
        // The multiplier applies to the summed score only — which signals
        // fired is a separate decision and must not shift with sensitivity.
        val now = 10 * dayMillis
        val timestamps = (1..6).map { now - it * (dayMillis / 10) }
        val low = HeuristicSpamDetector.scoreFromHistory(timestamps, now, 33698427L, sensitivity = 0.5)
        val high = HeuristicSpamDetector.scoreFromHistory(timestamps, now, 33698427L, sensitivity = 1.5)
        assertEquals(low.signals, high.signals)
    }

    @Test
    fun frequencySignalReportsTheActualWindowNotAHardcodedSeven() {
        val now = 40 * dayMillis
        val timestamps = (1..6).map { now - it * dayMillis }
        val result =
            HeuristicSpamDetector.scoreFromHistory(
                timestamps,
                now,
                phoneNumber = 33698427L,
                historyWindowDays = 30,
            )
        assertTrue(result.signals.any { it.contains("6 appels en 30 jours") })
        assertTrue(result.signals.none { it.contains("7 jours") })
    }
}
