package com.sentinelle.app.ui

import android.telephony.PhoneNumberUtils

/**
 * How a stored number is rendered on screen. Numbers are held as Long
 * (digits only, country code included) so they can be compared and indexed;
 * this is the single place that turns one back into something readable.
 *
 * Currently E.164 ("+33424040115"). Kept in one place so switching to
 * national grouping ("04 24 04 01 15") is a one-line change rather than a
 * hunt across screens — it used to be copy-pasted into DashboardScreen and
 * MyLabelsScreen, which meant two places to keep in sync.
 */
fun formatPhoneNumberForDisplay(number: Long): String =
    PhoneNumberUtils.formatNumberToE164(number.toString(), "FR") ?: number.toString()
