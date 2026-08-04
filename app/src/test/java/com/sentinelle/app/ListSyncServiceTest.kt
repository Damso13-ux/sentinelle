package com.sentinelle.app

import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.network.ListSummary
import com.sentinelle.app.service.ListSyncService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListSyncServiceTest {
    private fun summary(
        id: Long,
        version: String = "v1",
        enabledByDefault: Boolean? = true,
    ) = ListSummary(
        id = id,
        name = "list-$id",
        description = null,
        license = null,
        isEnabledByDefault = enabledByDefault,
        priority = null,
        channel = null,
        type = null,
        downloadUrl = "/api/v2/lists/$id",
        version = version,
    )

    private fun entity(
        id: Long,
        version: String = "v1",
        isEnabled: Boolean = true,
    ) = PatternListEntity(
        id = id,
        name = "list-$id",
        description = null,
        license = null,
        isEnabled = isEnabled,
        priority = 100,
        version = version,
        count = 0,
        channel = PatternListEntity.CHANNEL_PHONE,
        type = PatternListEntity.TYPE_BLOCK,
        source = PatternListEntity.SOURCE_API,
        downloadUrl = "/api/v2/lists/$id",
        lastDownloaded = 0,
    )

    @Test
    fun filterDefaultListsKeepsOnlyEnabledByDefaultTrue() {
        val lists =
            listOf(
                summary(1, enabledByDefault = true),
                summary(2, enabledByDefault = false),
                summary(3, enabledByDefault = null),
            )

        val result = ListSyncService.filterDefaultLists(lists)

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun filterDefaultListsEmptyWhenNoneEnabledByDefault() {
        val lists = listOf(summary(1, enabledByDefault = false), summary(2, enabledByDefault = null))

        assertTrue(ListSyncService.filterDefaultLists(lists).isEmpty())
    }

    @Test
    fun computeStaleIdsReturnsLocalIdsMissingFromKept() {
        val existing = listOf(entity(1), entity(2), entity(3))
        val keptIds = setOf(2L, 4L)

        assertEquals(setOf(1L, 3L), ListSyncService.computeStaleIds(existing, keptIds))
    }

    @Test
    fun computeStaleIdsEmptyWhenAllLocalKept() {
        val existing = listOf(entity(1), entity(2))
        val keptIds = setOf(1L, 2L)

        assertTrue(ListSyncService.computeStaleIds(existing, keptIds).isEmpty())
    }

    @Test
    fun computeStaleIdsReturnsAllLocalWhenKeptEmpty() {
        val existing = listOf(entity(1), entity(2))
        val keptIds = emptySet<Long>()

        assertEquals(setOf(1L, 2L), ListSyncService.computeStaleIds(existing, keptIds))
    }

    @Test
    fun computeStaleIdsEmptyWhenNoLocalLists() {
        val keptIds = setOf(1L, 2L)

        assertTrue(ListSyncService.computeStaleIds(emptyList(), keptIds).isEmpty())
    }

    @Test
    fun shouldSkipTrueWhenExistingVersionMatches() {
        val existing = entity(1, version = "v2")
        val incoming = summary(1, version = "v2")

        assertTrue(ListSyncService.shouldSkip(existing, incoming))
    }

    @Test
    fun shouldSkipFalseWhenVersionDiffers() {
        val existing = entity(1, version = "v1")
        val incoming = summary(1, version = "v2")

        assertFalse(ListSyncService.shouldSkip(existing, incoming))
    }

    @Test
    fun shouldSkipFalseWhenListIsNew() {
        val incoming = summary(1, version = "v1")

        assertFalse(ListSyncService.shouldSkip(null, incoming))
    }

    @Test
    fun shouldSkipFalseWhenExistingVersionBlankAndIncomingBlank() {
        val existing = entity(1, version = "")
        val incoming = summary(1, version = "")

        assertTrue(ListSyncService.shouldSkip(existing, incoming))
    }
}
