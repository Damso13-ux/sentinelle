package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HeuristicShadowEventDao {
    @Insert
    fun insert(event: HeuristicShadowEventEntity)

    @Query("SELECT * FROM heuristic_shadow_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): List<HeuristicShadowEventEntity>

    @Query("DELETE FROM heuristic_shadow_events WHERE timestamp < :beforeTimestamp")
    fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM heuristic_shadow_events")
    fun deleteAll()
}
