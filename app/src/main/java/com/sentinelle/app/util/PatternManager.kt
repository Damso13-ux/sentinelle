package com.sentinelle.app.util

import android.content.Context
import android.util.Log
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.ListPriorityService
import com.sentinelle.app.spam.HeuristicSpamDetector
import com.sentinelle.app.spam.SpamDetectorProvider
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
            if (score.score >= HeuristicSpamDetector.BLOCK_THRESHOLD) {
                return CallAction.Block(BlockSource.Heuristic(score.score, score.reason))
            }
        }

        return CallAction.None
    }

    fun evaluateSms(
        phoneNumber: Long,
        prefixes: Set<String>,
        context: Context,
    ): SmsAction {
        val lists =
            loadCache(context).filter {
                it.channel == PatternListEntity.CHANNEL_PHONE || it.channel == PatternListEntity.CHANNEL_SMS
            }
        val match = findListMatch(phoneNumber, prefixes, lists)
        if (match != null) {
            val (list, pattern) = match
            return if (list.type == PatternListEntity.TYPE_BLOCK) {
                SmsAction.Hide(BlockSource.PatternMatch(list.listId, pattern.name))
            } else {
                SmsAction.Keep
            }
        }

        // No pattern list match — same opt-in heuristic fallback as evaluateCall.
        val historyTrackingEnabled =
            try {
                runBlocking { PreferencesManager.isCallHistoryTrackingEnabled(context) }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading call history tracking preference", e)
                false
            }
        if (historyTrackingEnabled) {
            val score = SpamDetectorProvider.get().scoreSms(phoneNumber, prefixes, context)
            if (score.score >= HeuristicSpamDetector.BLOCK_THRESHOLD) {
                return SmsAction.Hide(BlockSource.Heuristic(score.score, score.reason))
            }
        }

        return SmsAction.Keep
    }
}
