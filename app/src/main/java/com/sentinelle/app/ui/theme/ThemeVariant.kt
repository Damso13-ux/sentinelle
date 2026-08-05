package com.sentinelle.app.ui.theme

// Pro-gated accent variants layered on top of the "Garde" dark theme.
// Only the accent-driven roles (primary/tertiary and their containers)
// change between variants — background, surface, and outline stay the
// Garde neutrals so every variant still reads as the same app. Garde
// itself went through a proper Material Theme Builder pass for contrast;
// Corail and Violet are hand-picked to sit at a similar lightness/chroma
// so white-on-primary text still reads cleanly, but haven't had that same
// rigorous contrast-ratio check — worth a real design pass later.
enum class ThemeVariant(
    val storageKey: String,
    val displayName: String,
) {
    GARDE("garde", "Garde"),
    CORAIL("corail", "Corail"),
    VIOLET("violet", "Violet"),
    ;

    companion object {
        fun fromStorageKey(key: String?): ThemeVariant = entries.firstOrNull { it.storageKey == key } ?: GARDE
    }
}
