package com.sentinelle.app.util

import android.content.Context
import android.util.Log
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.HeuristicShadowEventEntity
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.ListPriorityService
import com.sentinelle.app.spam.SpamDetectorProvider
import com.sentinelle.app.spam.SpamScore
import kotlinx.coroutines.runBlocking

data class BlockedPattern(
    val name: String,
    val pattern: String,
)

// Carries *why* a call/SMS was blocked, so it can be logged with a real
// reason in blocked_events instead of a generic flag. See BlockEventLogger.
sealed class BlockSource {
    data class PatternMatch(
        val listId: Long,
        val patternName: String,
    ) : BlockSource()

    data class Heuristic(
        val score: Double,
        val reason: String?,
    ) : BlockSource()

    data object OnlyContacts : BlockSource()

    data object Anonymous : BlockSource()
}

sealed class CallAction {
    data class Allow(
        val pattern: BlockedPattern,
    ) : CallAction()

    data class Block(
        val source: BlockSource,
    ) : CallAction()

    data object None : CallAction()
}

sealed class SmsAction {
    data class Hide(
        val source: BlockSource,
    ) : SmsAction()

    data object Keep : SmsAction()
}

object PatternManager {
    private const val TAG = "PatternManager"

    private data class CachedList(
        val listId: Long,
        val type: String,
        val channel: String,
        val patterns: List<BlockedPattern>,
    )

    @Volatile
    private var cachedLists: List<CachedList>? = null

    @Synchronized
    private fun loadCache(context: Context): List<CachedList> {
        cachedLists?.let { return it }
        return try {
            val db = AppDatabase.getInstance(context)
            val itemDao = db.patternListItemDao()
            val cached =
                db
                    .patternListDao()
                    .getEnabledLists()
                    .let { ListPriorityService.sortListsByPriority(it) }
                    .map { list ->
                        CachedList(
                            listId = list.id,
                            type = list.type,
                            channel = list.channel,
                            patterns =
                                itemDao.getPatternsByListId(list.id).map {
                                    BlockedPattern(name = it.name, pattern = it.pattern)
                                },
                        )
                    }
            cachedLists = cached
            cached
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pattern lists cache", e)
            emptyList()
        }
    }

    @Synchronized
    fun clearCache() {
        cachedLists = null
    }

    private fun findListMatch(
        phoneNumber: Long,
        prefixes: Set<String>,
        lists: List<CachedList>,
    ): Pair<CachedList, BlockedPattern>? {
        val variants = PhoneNumberMatcher.generateVariants(phoneNumber, prefixes)
        for (list in lists) {
            val matched =
                variants.firstNotNullOfOrNull { variant ->
                    list.patterns.firstOrNull { pattern ->
                        PhoneNumberMatcher.matchesPattern(variant, pattern.pattern)
                    }
                }
            if (matched != null) return list to matched
        }
        return null
    }

    // SMS-channel lists hold keywords, not number patterns (see the "Mots-clés
    // SMS" label in ListUiHelpers) — matched as a simple case-insensitive
    // substring against the message body. Works even without a resolvable
    // sender number, unlike findListMatch.
    private fun findKeywordMatch(
        messageText: String,
        lists: List<CachedList>,
    ): Pair<CachedList, BlockedPattern>? {
        for (list in lists) {
            val matched =
                list.patterns.firstOrNull { pattern ->
                    pattern.pattern.isNotBlank() && messageText.contains(pattern.pattern, ignoreCase = true)
                }
            if (matched != null) return list to matched
        }
        return null
    }

    fun evaluateCall(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): CallAction {
        val lists = loadCache(context).filter { it.channel == PatternListEntity.CHANNEL_PHONE }
        val match = findListMatch(phoneNumber, prefixes, lists)
        if (match != null) {
            val (list, pattern) = match
            return when (list.type) {
                PatternListEntity.TYPE_ALLOW -> CallAction.Allow(pattern)
                else -> CallAction.Block(BlockSource.PatternMatch(list.listId, pattern.name))
            }
        }

        // No pattern list match — fall back to the local heuristic scorer, but
        // only if the user opted in to call-history tracking (off by default;
        // see CallSettingsSheet). Without it, behavior is unchanged from today.
        val historyTrackingEnabled =
            try {
                runBlocking { PreferencesManager.isCallHistoryTrackingEnabled(context) }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading call history tracking preference", e)
                false
            }
        if (historyTrackingEnabled) {
            val score = SpamDetectorProvider.get().scoreCall(phoneNumber, prefixes, context)
            val blockThreshold =
                runBlocking { PreferencesManager.getEffectiveHeuristicSettings(context) }.blockThreshold
            if (score.score >= blockThreshold) {
                if (isShadowModeEnabled(context)) {
                    logShadowEvent(context, PatternListEntity.CHANNEL_PHONE, phoneNumber, score)
                    return CallAction.None
                }
                return CallAction.Block(BlockSource.Heuristic(score.score, score.reason))
            }
        }

        return CallAction.None
    }

    fun evaluateSms(
        phoneNumber: Long?,
        messageText: String?,
        prefixes: Set<String>,
        context: Context,
    ): SmsAction {
        val allLists = loadCache(context)

        // Phone-number lists also apply to SMS: a sender already on a call
        // block/allow list is treated the same way for texts.
        if (phoneNumber != null) {
            val phoneLists = allLists.filter { it.channel == PatternListEntity.CHANNEL_PHONE }
            val match = findListMatch(phoneNumber, prefixes, phoneLists)
            if (match != null) {
                val (list, pattern) = match
                return if (list.type == PatternListEntity.TYPE_BLOCK) {
                    SmsAction.Hide(BlockSource.PatternMatch(list.listId, pattern.name))
                } else {
                    SmsAction.Keep
                }
            }
        }

        // SMS keyword lists: content-based, works even for alphanumeric
        // senders ("FreeMobile", short codes...) that have no phone number.
        if (!messageText.isNullOrBlank()) {
            val smsLists = allLists.filter { it.channel == PatternListEntity.CHANNEL_SMS }
            val keywordMatch = findKeywordMatch(messageText, smsLists)
            if (keywordMatch != null) {
                val (list, pattern) = keywordMatch
                return if (list.type == PatternListEntity.TYPE_BLOCK) {
                    SmsAction.Hide(BlockSource.PatternMatch(list.listId, pattern.name))
                } else {
                    SmsAction.Keep
                }
            }
        }

        // No list match — same opt-in heuristic fallback as evaluateCall.
        // Requires a phone number since scoring keys on call/SMS frequency
        // history per number.
        if (phoneNumber != null) {
            val historyTrackingEnabled =
                try {
                    runBlocking { PreferencesManager.isCallHistoryTrackingEnabled(context) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading call history tracking preference", e)
                    false
                }
            if (historyTrackingEnabled) {
                val score = SpamDetectorProvider.get().scoreSms(phoneNumber, prefixes, context)
                val blockThreshold =
                    runBlocking { PreferencesManager.getEffectiveHeuristicSettings(context) }.blockThreshold
                if (score.score >= blockThreshold) {
                    if (isShadowModeEnabled(context)) {
                        logShadowEvent(context, PatternListEntity.CHANNEL_SMS, phoneNumber, score)
                        return SmsAction.Keep
                    }
                    return SmsAction.Hide(BlockSource.Heuristic(score.score, score.reason))
                }
            }
        }

        return SmsAction.Keep
    }

    private fun isShadowModeEnabled(context: Context): Boolean =
        try {
            runBlocking { PreferencesManager.isHeuristicShadowModeEnabled(context) }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading heuristic shadow mode preference", e)
            false
        }

    private fun logShadowEvent(
        context: Context,
        channel: String,
        phoneNumber: Long,
        score: SpamScore,
    ) {
        try {
            AppDatabase
                .getInstance(context)
                .heuristicShadowEventDao()
                .insert(
                    HeuristicShadowEventEntity(
                        channel = channel,
                        phoneNumber = phoneNumber,
                        timestamp = System.currentTimeMillis(),
                        score = score.score,
                        reason = score.reason,
                    ),
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error logging heuristic shadow event", e)
        }
    }
}
