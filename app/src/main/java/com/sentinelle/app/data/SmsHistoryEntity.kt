package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Mirrors CallHistoryEntity: local-only, feeds HeuristicSpamDetector.scoreSms.
// Only written when the user opts in via isCallHistoryTrackingEnabled, purged
// by CallHistoryCleanupWorker after Config.CALL_HISTORY_RETENTION_DAYS.
@Entity(
    tableName = "sms_history",
    indices = [Index("phoneNumber"), Index("timestamp")],
)
data class SmsHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: Long,
    val timestamp: Long,
    val wasBlocked: Boolean,
)
