package com.sentinelle.app.util

/**
 * Utility for phone number normalization and pattern matching.
 * Used by CallScreeningService to determine if a call should be blocked.
 * All phone numbers are handled as Long internally.
 */
object PhoneNumberMatcher {
    /**
     * Normalize a raw phone number string and generate variants as Longs.
     * If the number already has an international prefix (+ or 00), returns it as-is.
     * Otherwise, prepends each prefix to generate all possible international variants.
     */
    fun normalizePhoneNumber(
        phoneNumber: String,
        prefixes: Set<String>,
    ): List<Long> {
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleaned.isEmpty()) return emptyList()
        val hasInternationalPrefix = cleaned.startsWith("+") || cleaned.startsWith("00")
        val normalized =
            when {
                cleaned.startsWith("+") -> cleaned.substring(1)
                else -> cleaned.trimStart('0')
            }

        if (hasInternationalPrefix) {
            return listOfNotNull(normalized.toLongOrNull())
        }
        if (prefixes.any { normalized.startsWith(it) }) {
            return listOfNotNull(normalized.toLongOrNull())
        }
        return prefixes.mapNotNull { prefix ->
            (prefix + normalized).toLongOrNull()
        }
    }

    /**
     * Generate variants of an already-normalized Long phone number for each country prefix.
     * If the number starts with a known prefix, replace it with each alternative prefix.
     * Otherwise, prepend each prefix.
     */
    fun generateVariants(
        phoneNumber: Long,
        prefixes: Set<String>,
    ): List<Long> {
        val phoneStr = phoneNumber.toString()

        val matchedPrefix =
            prefixes
                .sortedByDescending { it.length }
                .firstOrNull { phoneStr.startsWith(it) }

        if (matchedPrefix != null) {
            val withoutPrefix = phoneStr.removePrefix(matchedPrefix)
            return prefixes.mapNotNull { prefix ->
                (prefix + withoutPrefix).toLongOrNull()
            }
        }

        return prefixes.mapNotNull { prefix ->
            (prefix + phoneStr).toLongOrNull()
        }
    }

    /**
     * Check if a phone number matches a pattern with wildcards (#)
     * Both numbers must have the same length after normalization
     */
    fun matchesPattern(
        phoneNumber: Long,
        pattern: String,
    ): Boolean {
        val phoneStr = phoneNumber.toString()
        if (phoneStr.length != pattern.length) return false

        for (i in pattern.indices) {
            when (pattern[i]) {
                '#' -> {
                    if (!phoneStr[i].isDigit()) return false
                }

                else -> {
                    if (phoneStr[i] != pattern[i]) return false
                }
            }
        }
        return true
    }

    /**
     * Check if any normalized variant of a phone number matches any blocked pattern.
     */
    fun matchesAnyPattern(
        normalizedVariants: List<Long>,
        blockedPatterns: List<BlockedPattern>,
    ): Boolean =
        normalizedVariants.any { normalized ->
            blockedPatterns.any { pattern ->
                matchesPattern(normalized, pattern.pattern)
            }
        }

    /**
     * Check if a phone number (as Long) matches any blocked pattern,
     * generating variants for each country prefix.
     */
    fun shouldBlock(
        phoneNumber: Long,
        prefixes: Set<String>,
        blockedPatterns: List<BlockedPattern>,
    ): Boolean = matchesAnyPattern(generateVariants(phoneNumber, prefixes), blockedPatterns)

    /**
     * Normalize a raw phone number string and check if it matches any blocked pattern.
     */
    fun shouldBlock(
        phoneNumber: String,
        prefixes: Set<String>,
        blockedPatterns: List<BlockedPattern>,
    ): Boolean = matchesAnyPattern(normalizePhoneNumber(phoneNumber, prefixes), blockedPatterns)
}
