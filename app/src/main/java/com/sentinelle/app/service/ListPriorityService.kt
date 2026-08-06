package com.sentinelle.app.service

import com.sentinelle.app.data.PatternListEntity

object ListPriorityService {
    /**
     * Evaluation order for pattern lists — PatternManager takes the first
     * match and stops, so this decides which list wins for a number that
     * appears in several.
     *
     * The user's own lists come first, ahead of anything downloaded. That
     * makes "Ne jamais bloquer ce numéro" hold no matter what the server
     * sends: `priority` comes from the API as a nullable Int with no floor,
     * so sorting on it first meant a negative value on a downloaded block
     * list would have outranked the user's explicit allow entry. It happens
     * not to today (user lists are seeded at 0, API lists default to 100),
     * but that's a property of the current server data, not of this code.
     *
     * Within each of those two groups: declared priority, then allow before
     * block, then name for a stable order.
     */
    fun sortListsByPriority(lists: List<PatternListEntity>): List<PatternListEntity> =
        lists.sortedWith(
            compareBy<PatternListEntity> { sourceOrder(it.source) }
                .thenBy { it.priority }
                .thenBy { typeOrder(it.type) }
                .thenBy { it.displayName().lowercase() },
        )

    private fun sourceOrder(source: String): Int =
        when (source) {
            PatternListEntity.SOURCE_USER -> 0
            else -> 1
        }

    private fun typeOrder(type: String): Int =
        when (type) {
            PatternListEntity.TYPE_ALLOW -> 0
            else -> 1
        }
}
