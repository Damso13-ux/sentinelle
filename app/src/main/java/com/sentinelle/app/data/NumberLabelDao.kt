package com.sentinelle.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NumberLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(label: NumberLabelEntity)

    @Query("SELECT * FROM number_labels WHERE phoneNumber = :phoneNumber LIMIT 1")
    fun getByPhoneNumber(phoneNumber: Long): NumberLabelEntity?

    @Query("SELECT * FROM number_labels WHERE phoneNumber = :phoneNumber LIMIT 1")
    fun getByPhoneNumberFlow(phoneNumber: Long): Flow<NumberLabelEntity?>

    @Query("SELECT * FROM number_labels ORDER BY dateAdded DESC")
    fun getAll(): List<NumberLabelEntity>

    @Query("DELETE FROM number_labels WHERE phoneNumber = :phoneNumber")
    fun deleteByPhoneNumber(phoneNumber: Long)
}
