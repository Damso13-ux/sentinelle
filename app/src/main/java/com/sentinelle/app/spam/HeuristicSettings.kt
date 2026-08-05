package com.sentinelle.app.spam

// User-tunable parameters for HeuristicSpamDetector. Exposed as a Pro
// feature (see PreferencesManager.getEffectiveHeuristicSettings) — everyone
// else gets these exact defaults, which match the detector's original
// fixed behavior before tuning existed.
data class HeuristicSettings(
    val historyWindowDays: Int = DEFAULT_HISTORY_WINDOW_DAYS,
    val blockThreshold: Double = DEFAULT_BLOCK_THRESHOLD,
    val sensitivity: Double = DEFAULT_SENSITIVITY,
) {
    companion object {
        const val DEFAULT_HISTORY_WINDOW_DAYS = 7
        const val DEFAULT_BLOCK_THRESHOLD = 0.75
        const val DEFAULT_SENSITIVITY = 1.0

        // CallHistoryCleanupWorker already retains 30 days of call/SMS
        // history regardless of Pro status (Config.CALL_HISTORY_RETENTION_DAYS)
        // — the free detector just never looks past day 7 of it. Capping the
        // slider here means Pro never needs data that isn't already on-device.
        const val MIN_HISTORY_WINDOW_DAYS = 3
        const val MAX_HISTORY_WINDOW_DAYS = 30

        const val MIN_BLOCK_THRESHOLD = 0.5
        const val MAX_BLOCK_THRESHOLD = 0.95

        const val MIN_SENSITIVITY = 0.5
        const val MAX_SENSITIVITY = 1.5
    }
}
