package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SmsHistoryDao {
    @Insert
    fun insert(entry: SmsHistoryEntity)

    @Query(
        "SELECT timestamp FROM sms_history " +
            "WHERE phoneNumber = :phoneNumber AND timestamp >= :sinceTimestamp " +
            "ORDER BY timestamp DESC",
    )
    fun getTimestampsForNumberSince(
        phoneNumber: Long,
        sinceTimestamp: Long,
    ): List<Long>

    @Query("DELETE FROM sms_history WHERE timestamp < :beforeTimestamp")
    fun deleteOlderThan(beforeTimestamp: Long)
}
