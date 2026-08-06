package com.sentinelle.app.ui.theme

/**
 * Accent variants layered over the shared neutrals.
 *
 * Only the primary/tertiary roles change between variants. Backgrounds,
 * surfaces, outlines and — importantly — the status colours in
 * [SentinelleColors] stay identical, so every variant reads as the same app
 * and "protected" is always the same green.
 *
 * All three are contrast-checked to WCAG AA in both light and dark, unlike
 * the previous Corail/Violet pair which were hand-picked by eye.
 */
enum class ThemeVariant(
    val storageKey: String,
    val displayName: String,
) {
    INDIGO("indigo", "Indigo"),
    OCEAN("ocean", "Océan"),
    PRUNE("prune", "Prune"),
    ;

    companion object {
        /**
         * Unknown keys fall back to the default. That covers the old
         * "garde"/"corail"/"violet" values still sitting in DataStore from
         * before the redesign — they resolve to Indigo rather than needing a
         * migration.
         */
        fun fromStorageKey(key: String?): ThemeVariant = entries.firstOrNull { it.storageKey == key } ?: INDIGO
    }
}
