package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {
    @Insert
    fun insert(blockedCall: BlockedCallEntity)

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAll(): List<BlockedCallEntity>

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun getCountFlow(): Flow<Int>

    @Query("DELETE FROM blocked_calls")
    fun deleteAll()
}
