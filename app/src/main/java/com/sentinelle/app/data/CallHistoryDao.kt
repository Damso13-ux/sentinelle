package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CallHistoryDao {
    @Insert
    fun insert(entry: CallHistoryEntity)

    @Query(
        "SELECT timestamp FROM call_history " +
            "WHERE phoneNumber = :phoneNumber AND timestamp >= :sinceTimestamp " +
            "ORDER BY timestamp DESC",
    )
    fun getTimestampsForNumberSince(
        phoneNumber: Long,
        sinceTimestamp: Long,
    ): List<Long>

    @Query("DELETE FROM call_history WHERE timestamp < :beforeTimestamp")
    fun deleteOlderThan(beforeTimestamp: Long)
}
