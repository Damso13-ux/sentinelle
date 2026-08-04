package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_history",
    indices = [Index("phoneNumber"), Index("timestamp")],
)
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: Long,
    val timestamp: Long,
    val wasBlocked: Boolean,
)
