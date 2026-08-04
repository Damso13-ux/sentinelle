package com.sentinelle.app

import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.ListPriorityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListPriorityServiceTest {
    private fun list(
        id: Long,
        name: String = "list-$id",
        priority: Int = 100,
        type: String = PatternListEntity.TYPE_BLOCK,
        source: String = PatternListEntity.SOURCE_API,
        channel: String = PatternListEntity.CHANNEL_PHONE,
    ) = PatternListEntity(
        id = id,
        name = name,
        description = null,
        license = null,
        isEnabled = true,
        priority = priority,
        version = "",
        count = 0,
        channel = channel,
        type = type,
        source = source,
        downloadUrl = "",
        lastDownloaded = 0,
    )

    @Test
    fun sortsByPriorityAscending() {
        val lists =
            listOf(
                list(1, priority = 50),
                list(2, priority = 200),
                list(3, priority = 100),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(listOf(1L, 3L, 2L), sorted.map { it.id })
    }

    @Test
    fun userAllowBeatsApiBlock() {
        val lists =
            listOf(
                list(1, priority = 100, type = PatternListEntity.TYPE_BLOCK, source = PatternListEntity.SOURCE_API),
                list(
                    PatternListEntity.USER_ALLOW_LIST_ID,
                    name = "user allow",
                    priority = 0,
                    type = PatternListEntity.TYPE_ALLOW,
                    source = PatternListEntity.SOURCE_USER,
                ),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(PatternListEntity.USER_ALLOW_LIST_ID, sorted.first().id)
    }

    @Test
    fun userListsPreserveAllowBlockOrder() {
        val lists =
            listOf(
                list(PatternListEntity.USER_BLOCK_LIST_ID, name = "user block", priority = 0, type = PatternListEntity.TYPE_BLOCK, source = PatternListEntity.SOURCE_USER),
                list(PatternListEntity.USER_ALLOW_LIST_ID, name = "user allow", priority = 0, type = PatternListEntity.TYPE_ALLOW, source = PatternListEntity.SOURCE_USER),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(
            listOf(PatternListEntity.USER_ALLOW_LIST_ID, PatternListEntity.USER_BLOCK_LIST_ID),
            sorted.map { it.id },
        )
    }

    @Test
    fun tiesBrokenByNameCaseInsensitive() {
        val lists =
            listOf(
                list(1, name = "Banana", priority = 100),
                list(2, name = "apple", priority = 100),
                list(3, name = "Cherry", priority = 100),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun equalPriorityTieBreakAllowBeforeBlock() {
        val lists =
            listOf(
                list(1, priority = 100, type = PatternListEntity.TYPE_BLOCK),
                list(2, priority = 100, type = PatternListEntity.TYPE_ALLOW),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun allowBeatsBlockAtEqualPriorityRegardlessOfName() {
        val lists =
            listOf(
                list(1, name = "AAA block", priority = 100, type = PatternListEntity.TYPE_BLOCK),
                list(2, name = "zzz allow", priority = 100, type = PatternListEntity.TYPE_ALLOW),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test
    fun equalPriorityAndNameKeepsStableOrder() {
        val lists =
            listOf(
                list(1, name = "same", priority = 100),
                list(2, name = "same", priority = 100),
            )

        val sorted = ListPriorityService.sortListsByPriority(lists)

        assertEquals(listOf(1L, 2L), sorted.map { it.id })
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertTrue(ListPriorityService.sortListsByPriority(emptyList()).isEmpty())
    }
}
