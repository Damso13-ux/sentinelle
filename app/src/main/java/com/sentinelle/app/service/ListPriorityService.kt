package com.sentinelle.app.service

import com.sentinelle.app.data.PatternListEntity

object ListPriorityService {
    fun sortListsByPriority(lists: List<PatternListEntity>): List<PatternListEntity> =
        lists.sortedWith(
            compareBy<PatternListEntity> { it.priority }
                .thenBy { typeOrder(it.type) }
                .thenBy { it.displayName().lowercase() },
        )

    private fun typeOrder(type: String): Int =
        when (type) {
            PatternListEntity.TYPE_ALLOW -> 0
            else -> 1
        }
}
