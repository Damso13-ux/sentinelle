package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternListItemDao {
    @Query("SELECT * FROM pattern_list_item")
    fun getAllPatterns(): List<PatternListItemEntity>

    @Query("SELECT COUNT(*) FROM pattern_list_item")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT * FROM pattern_list_item WHERE id = :id")
    fun getPatternById(id: Long): PatternListItemEntity?

    @Query("SELECT * FROM pattern_list_item WHERE listId = :listId ORDER BY dateAdded DESC")
    fun getPatternsByListId(listId: Long): List<PatternListItemEntity>

    @Query("SELECT * FROM pattern_list_item WHERE listId = :listId ORDER BY dateAdded DESC LIMIT :limit OFFSET :offset")
    fun getPatternsByListIdPaged(
        listId: Long,
        limit: Int,
        offset: Int,
    ): List<PatternListItemEntity>

    @Query("SELECT COUNT(*) FROM pattern_list_item WHERE listId = :listId")
    fun getCountByListId(listId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(patterns: List<PatternListItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pattern: PatternListItemEntity): Long

    @Query("DELETE FROM pattern_list_item WHERE listId = :listId")
    fun deleteByListId(listId: Long)

    @Query("DELETE FROM pattern_list_item WHERE id = :id")
    fun deleteById(id: Long)
}
