package com.sentinelle.app.ui

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// PhoneNumberUtils is an Android framework class, so the E.164 fallback
// path needs Robolectric even though the French path is pure Kotlin.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PhoneNumberUiHelpersTest {
    @Test
    fun frenchNumbersAreGroupedTheWayPeopleReadThem() {
        assertEquals("04 24 04 01 15", formatPhoneNumberForDisplay(33424040115L))
        assertEquals("06 12 34 56 78", formatPhoneNumberForDisplay(33612345678L))
        assertEquals("01 23 45 67 89", formatPhoneNumberForDisplay(33123456789L))
    }

    /**
     * The trunk 0 is dropped in international form. Leaving it off would
     * print "4 24 04 01 15", which is not a number anyone recognises.
     */
    @Test
    fun theLeadingZeroComesBack() {
        assertEquals("0", formatPhoneNumberForDisplay(33424040115L).take(1))
    }

    @Test
    fun nonFrenchNumbersFallBackRatherThanBeingMisgrouped() {
        // A UK number: right country code length, wrong national length for
        // the French grouping — must not be chunked as if it were French.
        val result = formatPhoneNumberForDisplay(447700900123L)
        assertEquals(false, result.startsWith("0"))
    }

    @Test
    fun aFrenchPrefixWithTooFewDigitsIsNotGrouped() {
        val result = formatPhoneNumberForDisplay(3342404L)
        assertEquals(false, result.contains(" "))
    }
}
