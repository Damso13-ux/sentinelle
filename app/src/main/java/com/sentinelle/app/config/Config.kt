package com.sentinelle.app.config

object Config {
    // Uses the upstream Saracroche list-sync API until Sentinelle has its own backend.
    const val API_HOST = "https://app.saracroche.org"
    const val API_BASE_URL = "$API_HOST/api/v2"

    const val BACKGROUND_UPDATE_INTERVAL_HOURS = 24L
    const val ORGANIZATION_DEVICE_HEALTH_CHECK_INTERVAL_HOURS = 12L
    const val LIST_UPDATE_INTERVAL_HOURS = 24L

    // Local-only call history used for heuristic spam scoring is purged
    // beyond this window, so it never grows unbounded on-device.
    const val CALL_HISTORY_RETENTION_DAYS = 30L
    const val CALL_HISTORY_CLEANUP_INTERVAL_HOURS = 24L
}
