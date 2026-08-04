package com.sentinelle.app.service

import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.network.ListSummary

object ListSyncService {
    fun filterDefaultLists(lists: List<ListSummary>): List<ListSummary> = lists.filter { it.isEnabledByDefault == true }

    fun computeStaleIds(
        existingApiLists: List<PatternListEntity>,
        keptIds: Set<Long>,
    ): Set<Long> {
        val existingIds = existingApiLists.map { it.id }.toSet()
        return existingIds - keptIds
    }

    fun shouldSkip(
        existing: PatternListEntity?,
        incoming: ListSummary,
    ): Boolean = existing != null && existing.version == incoming.version
}
