package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// "Shadow mode": when enabled, a call/SMS that the heuristic would have
// blocked is logged here instead of actually being blocked, so the user can
// review accuracy before trusting it for real. Local-only, purged like the
// rest of the heuristic history — see PreferencesManager.isHeuristicShadowModeEnabled.
@Entity(
    tableName = "heuristic_shadow_events",
    indices = [Index("timestamp")],
)
data class HeuristicShadowEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: String,
    val phoneNumber: Long,
    val timestamp: Long,
    val score: Double,
    val reason: String?,
)
