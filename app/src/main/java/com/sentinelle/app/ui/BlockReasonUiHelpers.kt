package com.sentinelle.app.ui

import com.sentinelle.app.data.BlockedEventEntity

fun formatBlockReason(event: BlockedEventEntity): String =
    when (event.reasonType) {
        BlockedEventEntity.REASON_PATTERN_LIST -> event.reasonPatternName ?: "Liste de blocage"
        BlockedEventEntity.REASON_HEURISTIC -> event.heuristicReason ?: "Comportement suspect"
        BlockedEventEntity.REASON_ONLY_CONTACTS -> "Non enregistré dans les contacts"
        BlockedEventEntity.REASON_ANONYMOUS -> "Numéro masqué"
        else -> "Raison inconnue"
    }
