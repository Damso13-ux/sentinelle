package com.sentinelle.app.service

import android.content.Context
import android.util.Log
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.data.PatternListItemEntity
import com.sentinelle.app.network.ListPatternInfo
import com.sentinelle.app.network.ListSummary
import com.sentinelle.app.network.NetworkError
import com.sentinelle.app.network.NetworkService
import com.sentinelle.app.util.PatternManager
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ListService {
    private const val TAG = "ListService"

    fun updateList(
        context: Context,
        listSummary: ListSummary,
        patterns: List<ListPatternInfo>,
    ) {
        val db = AppDatabase.getInstance(context)
        val itemDao = db.patternListItemDao()
        val listDao = db.patternListDao()
        val listId = listSummary.id
        val now = System.currentTimeMillis()

        val type =
            when (listSummary.type?.lowercase()) {
                "allow" -> PatternListEntity.TYPE_ALLOW
                else -> PatternListEntity.TYPE_BLOCK
            }

        val channel =
            when (listSummary.channel?.lowercase()) {
                "sms" -> PatternListEntity.CHANNEL_SMS
                else -> PatternListEntity.CHANNEL_PHONE
            }

        val newEntities =
            patterns.map { pattern ->
                PatternListItemEntity(
                    listId = listId,
                    name = pattern.name,
                    pattern = pattern.pattern,
                    dateAdded = now,
                )
            }

        db.runInTransaction {
            val existing = listDao.getById(listId)
            val isEnabled = existing?.isEnabled ?: (listSummary.isEnabledByDefault ?: false)

            val metadata =
                PatternListEntity(
                    id = listId,
                    name = listSummary.name,
                    description = listSummary.description,
                    license = listSummary.license,
                    isEnabled = isEnabled,
                    priority = listSummary.priority ?: 100,
                    version = listSummary.version,
                    count = patterns.sumOf { PatternService.calculateCoveredNumbers(it.pattern) },
                    channel = channel,
                    type = type,
                    source = PatternListEntity.SOURCE_API,
                    downloadUrl = listSummary.downloadUrl,
                    lastDownloaded = now,
                )
            listDao.upsert(metadata)

            itemDao.deleteByListId(listId)
            if (newEntities.isNotEmpty()) {
                itemDao.insertAll(newEntities)
            }
        }
        PatternManager.clearCache()
    }

    private fun upsertListMetadata(
        listDao: com.sentinelle.app.data.PatternListDao,
        listSummary: ListSummary,
        isEnabled: Boolean,
        count: Long,
        lastDownloaded: Long,
    ) {
        val type =
            when (listSummary.type?.lowercase()) {
                "allow" -> PatternListEntity.TYPE_ALLOW
                else -> PatternListEntity.TYPE_BLOCK
            }

        val channel =
            when (listSummary.channel?.lowercase()) {
                "sms" -> PatternListEntity.CHANNEL_SMS
                else -> PatternListEntity.CHANNEL_PHONE
            }

        listDao.upsert(
            PatternListEntity(
                id = listSummary.id,
                name = listSummary.name,
                description = listSummary.description,
                license = listSummary.license,
                isEnabled = isEnabled,
                priority = listSummary.priority ?: 100,
                version = listSummary.version,
                count = count,
                channel = channel,
                type = type,
                source = PatternListEntity.SOURCE_API,
                downloadUrl = listSummary.downloadUrl,
                lastDownloaded = lastDownloaded,
            ),
        )
    }

    suspend fun updateLists(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val countryCodes = PreferencesManager.getCountryCodes(context)
                val deviceId = PreferencesManager.getDeviceId(context)
                val apiKey = PreferencesManager.getApiKey(context)
                val networkService = NetworkService()

                val lists =
                    try {
                        networkService.fetchAvailableLists(countryCodes, deviceId, apiKey)
                    } catch (e: NetworkError) {
                        Log.e(TAG, "Failed to fetch available lists", e)
                        return@withContext false
                    }

                if (lists.isEmpty()) {
                    Log.w(TAG, "No lists available for country codes: $countryCodes")
                    return@withContext false
                }

                val db = AppDatabase.getInstance(context)
                val keptIds = lists.map { it.id }.toSet()
                val existingApiLists = db.patternListDao().getBySource(PatternListEntity.SOURCE_API)
                val existingById = existingApiLists.associateBy { it.id }

                var hadChanges = false
                val staleIds = ListSyncService.computeStaleIds(existingApiLists, keptIds)
                if (staleIds.isNotEmpty()) {
                    db.runInTransaction {
                        for (id in staleIds) {
                            Log.d(TAG, "Deleting stale API list id=$id (no longer available server-side)")
                            db.patternListDao().deleteById(id)
                        }
                    }
                    hadChanges = true
                }

                val listDao = db.patternListDao()
                var success = false
                for (list in lists) {
                    val existing = existingById[list.id]
                    val isEnabled = existing?.isEnabled ?: (list.isEnabledByDefault ?: false)

                    if (!isEnabled) {
                        if (existing == null || list.version != existing.version) {
                            upsertListMetadata(listDao, list, isEnabled, existing?.count ?: 0, existing?.lastDownloaded ?: 0)
                            hadChanges = true
                        }
                        continue
                    }

                    if (ListSyncService.shouldSkip(existing, list)) continue
                    try {
                        val patterns =
                            try {
                                networkService.downloadListPatterns(list.downloadUrl, deviceId, apiKey)
                            } catch (e: NetworkError) {
                                Log.e(TAG, "Failed to download list ${list.id} (${list.name})", e)
                                continue
                            }
                        if (patterns.isEmpty()) continue
                        updateList(context, list, patterns)
                        success = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process list ${list.id} (${list.name})", e)
                    }
                }

                if (success) {
                    PreferencesManager.setLastListUpdate(context, System.currentTimeMillis())
                }
                if (success || hadChanges) {
                    PatternManager.clearCache()
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download lists", e)
                false
            }
        }
    }

    suspend fun reinstallLists(context: Context): Boolean {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                db.patternListDao().deleteBySource(PatternListEntity.SOURCE_API)
                PatternManager.clearCache()
            } catch (_: Exception) {
            }
        }
        return updateLists(context)
    }

    suspend fun resetApp(context: Context): Boolean {
        Log.d(TAG, "resetApp: clearing all tables and preferences")
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                db.clearAllTables()
                db.seedUserLists()
                PreferencesManager.clear(context)
                PatternManager.clearCache()
            } catch (_: Exception) {
            }
        }
        return updateLists(context)
    }
}
