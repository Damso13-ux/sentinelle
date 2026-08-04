package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternListDao {
    @Query("SELECT * FROM pattern_list WHERE id = :id")
    fun getById(id: Long): PatternListEntity?

    @Query("SELECT * FROM pattern_list WHERE source = :source ORDER BY name ASC")
    fun getBySource(source: String): List<PatternListEntity>

    @Query("SELECT * FROM pattern_list WHERE source = :source ORDER BY name ASC")
    fun getBySourceFlow(source: String): Flow<List<PatternListEntity>>

    @Query("SELECT * FROM pattern_list WHERE isEnabled = 1")
    fun getEnabledLists(): List<PatternListEntity>

    @Query("SELECT COALESCE(SUM(count), 0) FROM pattern_list")
    fun getTotalCoveredFlow(): Flow<Long>

    @Query("UPDATE pattern_list SET count = count + :delta WHERE id = :listId")
    fun addCount(
        listId: Long,
        delta: Long,
    )

    @Query("UPDATE pattern_list SET isEnabled = :enabled WHERE id = :id")
    fun setEnabled(
        id: Long,
        enabled: Boolean,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(metadata: PatternListEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfAbsent(list: PatternListEntity)

    @Query("DELETE FROM pattern_list WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM pattern_list WHERE source = :source")
    fun deleteBySource(source: String)
}
