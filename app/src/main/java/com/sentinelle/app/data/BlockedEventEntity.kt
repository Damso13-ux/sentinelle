package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_events",
    indices = [Index("timestamp"), Index("channel"), Index("phoneNumber")],
)
data class BlockedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: String,
    val phoneNumber: Long,
    val timestamp: Long,
    val reasonType: String,
    val reasonListId: Long?,
    val reasonPatternName: String?,
    val heuristicScore: Double?,
    val heuristicReason: String?,
) {
    companion object {
        const val REASON_PATTERN_LIST = "pattern_list"
        const val REASON_HEURISTIC = "heuristic"
        const val REASON_ANONYMOUS = "anonymous"
        const val REASON_ONLY_CONTACTS = "only_contacts"
    }
}
