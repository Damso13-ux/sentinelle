package com.sentinelle.app.service

import android.content.Context
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.BlockedEventEntity
import com.sentinelle.app.util.BlockSource
import com.sentinelle.app.widget.SentinelleWidgetProvider

// Shared by CallScreeningService and SmsNotificationListener so both channels
// log into the same blocked_events table with a consistent reason mapping.
object BlockEventLogger {
    fun log(
        context: Context,
        channel: String,
        phoneNumber: Long,
        source: BlockSource?,
    ) {
        val (reasonType, reasonListId, reasonPatternName, heuristicScore, heuristicReason) =
            when (source) {
                is BlockSource.PatternMatch ->
                    Reason(BlockedEventEntity.REASON_PATTERN_LIST, source.listId, source.patternName, null, null)

                is BlockSource.Heuristic ->
                    Reason(BlockedEventEntity.REASON_HEURISTIC, null, null, source.score, source.reason)

                is BlockSource.OnlyContacts ->
                    Reason(BlockedEventEntity.REASON_ONLY_CONTACTS, null, null, null, null)

                BlockSource.Anonymous, null ->
                    Reason(BlockedEventEntity.REASON_ANONYMOUS, null, null, null, null)
            }

        AppDatabase
            .getInstance(context)
            .blockedEventDao()
            .insert(
                BlockedEventEntity(
                    channel = channel,
                    phoneNumber = phoneNumber,
                    timestamp = System.currentTimeMillis(),
                    reasonType = reasonType,
                    reasonListId = reasonListId,
                    reasonPatternName = reasonPatternName,
                    heuristicScore = heuristicScore,
                    heuristicReason = heuristicReason,
                ),
            )

        SentinelleWidgetProvider.requestUpdate(context)
    }

    private data class Reason(
        val type: String,
        val listId: Long?,
        val patternName: String?,
        val heuristicScore: Double?,
        val heuristicReason: String?,
    )
}
