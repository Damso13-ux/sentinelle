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
        val result = HeuristicSpamDetector.scoreFromHistory(emptyList(), now = 0L, phoneNumber = 33612345678L)
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
}
