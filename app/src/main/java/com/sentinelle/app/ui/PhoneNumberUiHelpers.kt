package com.sentinelle.app.ui

import android.telephony.PhoneNumberUtils

/**
 * How a stored number is rendered on screen. Numbers are held as Long
 * (digits only, country code included) so they can be compared and indexed;
 * this is the single place that turns one back into something readable.
 *
 * French numbers are shown the way people actually read them out —
 * "04 24 04 01 15", not the E.164 "+33424040115" this used to print.
 * E.164 is the right format to *store* and to send to an API; it's the
 * wrong one to put in front of someone trying to recognise a caller.
 *
 * Anything that isn't a French number falls back to E.164, which at least
 * stays unambiguous.
 */
fun formatPhoneNumberForDisplay(number: Long): String {
    val digits = number.toString()
    val national = digits.removePrefix(FRANCE_COUNTRY_CODE)
    if (!digits.startsWith(FRANCE_COUNTRY_CODE) || national.length != FRENCH_NATIONAL_DIGITS) {
        return PhoneNumberUtils.formatNumberToE164(digits, "FR") ?: digits
    }
    // The leading 0 is dropped when a French number is written in
    // international form; putting it back is what makes it recognisable.
    return "0$national".chunked(2).joinToString(" ")
}

private const val FRANCE_COUNTRY_CODE = "33"

// French numbers are 9 digits once the country code and the trunk 0 are off.
private const val FRENCH_NATIONAL_DIGITS = 9
