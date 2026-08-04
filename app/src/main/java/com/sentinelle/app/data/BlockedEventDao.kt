package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DayCount(
    val day: String,
    val count: Int,
)

data class ChannelCount(
    val channel: String,
    val count: Int,
)

data class TopBlockedNumber(
    val phoneNumber: Long,
    val count: Int,
)

@Dao
interface BlockedEventDao {
    @Insert
    fun insert(event: BlockedEventEntity)

    @Query("SELECT * FROM blocked_events ORDER BY timestamp DESC")
    fun getAll(): List<BlockedEventEntity>

    @Query("SELECT COUNT(*) FROM blocked_events")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_events WHERE channel = :channel")
    fun getCountByChannelFlow(channel: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_events WHERE phoneNumber = :phoneNumber")
    fun getCountForNumber(phoneNumber: Long): Int

    @Query("SELECT COUNT(*) FROM blocked_events WHERE timestamp >= :sinceTimestamp")
    fun getCountSince(sinceTimestamp: Long): Int

    @Query(
        "SELECT date(timestamp / 1000, 'unixepoch') AS day, COUNT(*) AS count " +
            "FROM blocked_events WHERE timestamp >= :sinceTimestamp " +
            "GROUP BY day ORDER BY day ASC",
    )
    fun getCountByDaySince(sinceTimestamp: Long): List<DayCount>

    @Query(
        "SELECT channel, COUNT(*) AS count FROM blocked_events " +
            "WHERE timestamp >= :sinceTimestamp GROUP BY channel",
    )
    fun getCountByChannelSince(sinceTimestamp: Long): List<ChannelCount>

    @Query(
        "SELECT phoneNumber, COUNT(*) AS count FROM blocked_events " +
            "WHERE timestamp >= :sinceTimestamp AND phoneNumber != 0 " +
            "GROUP BY phoneNumber ORDER BY count DESC LIMIT :limit",
    )
    fun getTopBlockedNumbersSince(
        sinceTimestamp: Long,
        limit: Int,
    ): List<TopBlockedNumber>

    @Query("DELETE FROM blocked_events")
    fun deleteAll()
}
