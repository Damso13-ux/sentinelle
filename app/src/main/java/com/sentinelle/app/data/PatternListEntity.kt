package com.sentinelle.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_list")
data class PatternListEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String?,
    val license: String?,
    val isEnabled: Boolean,
    val priority: Int,
    val version: String,
    val count: Long,
    val channel: String,
    val type: String,
    val source: String,
    val downloadUrl: String,
    val lastDownloaded: Long,
) {
    fun displayName(): String =
        when (name) {
            "user allow" -> "Autorisé et identifié"
            "user block" -> "Bloqué"
            else -> name
        }

    companion object {
        const val SOURCE_USER = "user"
        const val SOURCE_API = "api"

        const val TYPE_ALLOW = "allow"
        const val TYPE_BLOCK = "block"

        const val CHANNEL_PHONE = "phone"
        const val CHANNEL_SMS = "sms"

        const val USER_ALLOW_LIST_ID = -1L
        const val USER_BLOCK_LIST_ID = -2L
    }
}
