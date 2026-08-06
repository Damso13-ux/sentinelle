package com.sentinelle.app.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Guards the Pro tuning defaults/ranges. getEffectiveHeuristicSettings
// itself isn't covered here — it needs a Context/DataStore, so it would
// require Robolectric or an instrumented test, neither of which this
// project currently uses.
class HeuristicSettingsTest {
    @Test
    fun defaultsSitInsideTheSliderRanges() {
        // The settings sliders (CallSettingsSheet) are built from these
        // MIN/MAX constants. A default outside its own range would render
        // a slider whose initial position is clamped somewhere the user
        // never chose — and silently rewrite their tuning on first drag.
        assertTrue(
            HeuristicSettings.DEFAULT_HISTORY_WINDOW_DAYS in
                HeuristicSettings.MIN_HISTORY_WINDOW_DAYS..HeuristicSettings.MAX_HISTORY_WINDOW_DAYS,
        )
        assertTrue(HeuristicSettings.DEFAULT_BLOCK_THRESHOLD >= HeuristicSettings.MIN_BLOCK_THRESHOLD)
        assertTrue(HeuristicSettings.DEFAULT_BLOCK_THRESHOLD <= HeuristicSettings.MAX_BLOCK_THRESHOLD)
        assertTrue(HeuristicSettings.DEFAULT_SENSITIVITY >= HeuristicSettings.MIN_SENSITIVITY)
        assertTrue(HeuristicSettings.DEFAULT_SENSITIVITY <= HeuristicSettings.MAX_SENSITIVITY)
    }

    @Test
    fun noArgConstructorMatchesTheDeclaredDefaults() {
        // What a free (non-Pro) user always gets — getEffectiveHeuristicSettings
        // returns exactly this when Pro isn't unlocked.
        val settings = HeuristicSettings()
        assertEquals(HeuristicSettings.DEFAULT_HISTORY_WINDOW_DAYS, settings.historyWindowDays)
        assertEquals(HeuristicSettings.DEFAULT_BLOCK_THRESHOLD, settings.blockThreshold, 0.0001)
        assertEquals(HeuristicSettings.DEFAULT_SENSITIVITY, settings.sensitivity, 0.0001)
    }

    @Test
    fun defaultSensitivityIsNeutral() {
        // 1.0 must stay the identity multiplier, otherwise free users would
        // silently get a different score than the detector's documented
        // pre-tuning behavior.
        assertEquals(1.0, HeuristicSettings.DEFAULT_SENSITIVITY, 0.0001)
    }

    @Test
    fun historyWindowMaxStaysWithinWhatIsActuallyRetained() {
        // CallHistoryCleanupWorker purges beyond Config.CALL_HISTORY_RETENTION_DAYS
        // (30) regardless of Pro. Allowing a longer window would promise
        // analysis over data that no longer exists on-device.
        assertTrue(HeuristicSettings.MAX_HISTORY_WINDOW_DAYS <= 30)
    }
}
