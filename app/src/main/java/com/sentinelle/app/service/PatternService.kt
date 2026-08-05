package com.sentinelle.app.service

import android.content.Context
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.data.PatternListItemEntity

sealed class PatternValidation {
    data object Valid : PatternValidation()

    data class Invalid(
        val message: String,
    ) : PatternValidation()
}

data class OverlapInfo(
    val existingPattern: PatternListItemEntity,
    val type: OverlapType,
    val message: String,
)

enum class OverlapType {
    COVERED_BY_EXISTING,
    COVERS_EXISTING,
}

object PatternService {
    fun validatePattern(input: String): PatternValidation {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return PatternValidation.Invalid("La saisie ne peut pas être vide.")
        }

        if (trimmed.contains(" ")) {
            return PatternValidation.Invalid("La saisie ne doit pas contenir d'espaces.")
        }

        if (!trimmed.all { it.isDigit() || it == '#' || it == '+' }) {
            return PatternValidation.Invalid("La saisie ne peut contenir que des chiffres, '+' et '#'.")
        }

        if (!trimmed.startsWith("+")) {
            return PatternValidation.Invalid("La saisie doit commencer par '+' (format international).")
        }

        val body = trimmed.substring(1)
        val wildcardCount = body.count { it == '#' }
        val type = if (wildcardCount > 0) "Le préfixe" else "Le numéro"

        if (wildcardCount > 0) {
            val firstHash = body.indexOf('#')
            val afterHash = body.substring(firstHash)
            if (!afterHash.all { it == '#' }) {
                return PatternValidation.Invalid("Les jokers '#' doivent être placés uniquement en fin de numéro.")
            }
        }

        if (trimmed.length < 4) {
            return PatternValidation.Invalid("$type est trop court (minimum 4 caractères).")
        }

        if (wildcardCount > 15) {
            return PatternValidation.Invalid("Trop de jokers '#'. Maximum : 15 jokers dans un préfixe.")
        }

        return PatternValidation.Valid
    }

    fun detectDuplicate(
        pattern: String,
        context: Context,
    ): String? {
        val db = AppDatabase.getInstance(context)
        val normalized = normalizeInput(pattern)
        val allItems = db.patternListItemDao().getAllPatterns()
        val existing = allItems.find { it.pattern == normalized } ?: return null
        val source = db.patternListDao().getById(existing.listId)?.source
        val type = if (pattern.contains("#")) "préfixe" else "numéro"
        return if (source == PatternListEntity.SOURCE_API) {
            "Ce $type est déjà présent dans la liste de blocage."
        } else {
            "Ce $type existe déjà dans vos éléments personnalisés."
        }
    }

    fun detectOverlaps(
        pattern: String,
        context: Context,
    ): List<OverlapInfo> {
        val db = AppDatabase.getInstance(context)
        val allItems = db.patternListItemDao().getAllPatterns()
        val normalized = normalizeInput(pattern)
        val type = if (pattern.contains("#")) "préfixe" else "numéro"

        return allItems.mapNotNull { existing ->
            when {
                isPatternCoveredBy(normalized, existing.pattern) -> {
                    OverlapInfo(
                        existing,
                        OverlapType.COVERED_BY_EXISTING,
                        "Ce $type est déjà couvert par l'élément existant +${existing.pattern}.",
                    )
                }

                isPatternCoveredBy(existing.pattern, normalized) -> {
                    OverlapInfo(
                        existing,
                        OverlapType.COVERS_EXISTING,
                        "Ce $type englobe l'élément existant +${existing.pattern}.",
                    )
                }

                else -> {
                    null
                }
            }
        }
    }

    fun validateKeyword(input: String): PatternValidation {
        val trimmed = input.trim()

        if (trimmed.isEmpty()) {
            return PatternValidation.Invalid("Le mot-clé ne peut pas être vide.")
        }
        if (trimmed.length < 2) {
            return PatternValidation.Invalid("Le mot-clé est trop court (minimum 2 caractères).")
        }
        if (trimmed.length > 100) {
            return PatternValidation.Invalid("Le mot-clé est trop long (maximum 100 caractères).")
        }

        return PatternValidation.Valid
    }

    // Scoped to SMS-channel lists only — comparing a keyword against a
    // phone-number pattern's raw text would be a meaningless "duplicate".
    fun detectDuplicateKeyword(
        keyword: String,
        context: Context,
    ): String? {
        val db = AppDatabase.getInstance(context)
        val trimmed = keyword.trim()
        val smsListIds =
            (db.patternListDao().getBySource(PatternListEntity.SOURCE_USER) + db.patternListDao().getBySource(PatternListEntity.SOURCE_API))
                .filter { it.channel == PatternListEntity.CHANNEL_SMS }
                .map { it.id }
                .toSet()
        val existing =
            db
                .patternListItemDao()
                .getAllPatterns()
                .filter { it.listId in smsListIds }
                .find { it.pattern.equals(trimmed, ignoreCase = true) } ?: return null
        val source = db.patternListDao().getById(existing.listId)?.source
        return if (source == PatternListEntity.SOURCE_API) {
            "Ce mot-clé est déjà présent dans une liste publique."
        } else {
            "Ce mot-clé existe déjà dans vos éléments personnalisés."
        }
    }

    fun addUserKeyword(
        keyword: String,
        name: String,
        listId: Long,
        context: Context,
    ): Long {
        val db = AppDatabase.getInstance(context)
        val itemDao = db.patternListItemDao()
        val listDao = db.patternListDao()
        val entity =
            PatternListItemEntity(
                listId = listId,
                name = name,
                pattern = keyword.trim(),
                dateAdded = System.currentTimeMillis(),
            )
        var newId = 0L
        db.runInTransaction {
            newId = itemDao.insert(entity)
            listDao.addCount(listId, 1)
        }
        com.sentinelle.app.util.PatternManager
            .clearCache()
        return newId
    }

    fun addUserPattern(
        pattern: String,
        name: String,
        listId: Long,
        context: Context,
    ): Long {
        val db = AppDatabase.getInstance(context)
        val itemDao = db.patternListItemDao()
        val listDao = db.patternListDao()
        val normalized = normalizeInput(pattern)
        val covered = calculateCoveredNumbers(normalized)
        val entity =
            PatternListItemEntity(
                listId = listId,
                name = name,
                pattern = normalized,
                dateAdded = System.currentTimeMillis(),
            )
        var newId = 0L
        db.runInTransaction {
            newId = itemDao.insert(entity)
            listDao.addCount(listId, covered)
        }
        com.sentinelle.app.util.PatternManager
            .clearCache()
        return newId
    }

    fun deletePattern(
        id: Long,
        context: Context,
    ) {
        val db = AppDatabase.getInstance(context)
        val itemDao = db.patternListItemDao()
        val listDao = db.patternListDao()
        db.runInTransaction {
            val pattern = itemDao.getPatternById(id)
            if (pattern != null) {
                val covered = calculateCoveredNumbers(pattern.pattern)
                itemDao.deleteById(id)
                listDao.addCount(pattern.listId, -covered)
            }
        }
        com.sentinelle.app.util.PatternManager
            .clearCache()
    }

    fun getNumberRange(pattern: String): Pair<String, String> {
        val first = pattern.replace('#', '0')
        val last = pattern.replace('#', '9')
        return Pair("+$first", "+$last")
    }

    fun calculateCoveredNumbers(pattern: String): Long {
        val wildcardCount = pattern.count { it == '#' }
        var total = 1L
        repeat(wildcardCount) { total *= 10 }
        return total
    }

    private fun normalizeInput(input: String): String = if (input.startsWith("+")) input.substring(1) else input

    private fun isPatternCoveredBy(
        child: String,
        parent: String,
    ): Boolean {
        if (child.length != parent.length) return false
        for (i in child.indices) {
            val p = parent[i]
            val c = child[i]
            if (p == '#') continue
            if (c == '#') return false
            if (p != c) return false
        }
        return true
    }
}
