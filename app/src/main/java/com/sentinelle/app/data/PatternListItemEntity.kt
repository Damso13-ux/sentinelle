package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pattern_list_item",
    indices = [Index(value = ["pattern"]), Index(value = ["listId"])],
    foreignKeys = [
        ForeignKey(
            entity = PatternListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PatternListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val pattern: String,
    val dateAdded: Long,
)
