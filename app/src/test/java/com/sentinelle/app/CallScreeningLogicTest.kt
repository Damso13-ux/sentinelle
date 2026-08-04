package com.sentinelle.app

import com.sentinelle.app.util.BlockedPattern
import com.sentinelle.app.util.PhoneNumberMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallScreeningLogicTest {
    /**
     * Test pattern matching logic
     */
    @Test
    fun testPatternMatching() {
        assertTrue(PhoneNumberMatcher.matchesPattern(33162123456L, "33162######"))
        assertFalse(PhoneNumberMatcher.matchesPattern(33163123456L, "33162######"))

        assertFalse(PhoneNumberMatcher.matchesPattern(3316212345L, "33162######"))
        assertFalse(PhoneNumberMatcher.matchesPattern(331621234567L, "33162######"))

        assertFalse(PhoneNumberMatcher.matchesPattern(33162L, "33162######"))

        assertTrue(PhoneNumberMatcher.matchesPattern(3394751234L, "339475####"))
        assertFalse(PhoneNumberMatcher.matchesPattern(3394761234L, "339475####"))
    }

    /**
     * Test number normalization with single prefix
     */
    @Test
    fun testNormalizeWithSinglePrefix() {
        val prefixes = setOf("33")

        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("+33612345678", prefixes))
        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("0033612345678", prefixes))
        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("0612345678", prefixes))
        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("612345678", prefixes))
        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("33612345678", prefixes))
        assertEquals(listOf(33162123456L), PhoneNumberMatcher.normalizePhoneNumber("33 1 62 12 34 56", prefixes))
        assertEquals(listOf(33162123456L), PhoneNumberMatcher.normalizePhoneNumber("33-162-123-456", prefixes))
        assertEquals(listOf(33162123456L), PhoneNumberMatcher.normalizePhoneNumber("(33) 162 123 456", prefixes))
        assertEquals(listOf(33162123456L), PhoneNumberMatcher.normalizePhoneNumber("00 33 162 123 456", prefixes))
        assertEquals(listOf(33162123456L), PhoneNumberMatcher.normalizePhoneNumber("000033162123456", prefixes))
    }

    /**
     * Test number normalization with multiple prefixes (multi-SIM)
     */
    @Test
    fun testNormalizeWithMultiplePrefixes() {
        val prefixes = setOf("33", "32")

        assertEquals(listOf(33612345678L), PhoneNumberMatcher.normalizePhoneNumber("+33612345678", prefixes))
        assertEquals(listOf(32412345678L), PhoneNumberMatcher.normalizePhoneNumber("+32412345678", prefixes))

        assertEquals(listOf(32612345678L), PhoneNumberMatcher.normalizePhoneNumber("0032612345678", prefixes))
        assertEquals(listOf(32612345678L), PhoneNumberMatcher.normalizePhoneNumber("32612345678", prefixes))

        assertEquals(listOf(49612345678L), PhoneNumberMatcher.normalizePhoneNumber("+49612345678", prefixes))
        assertEquals(listOf(49612345678L), PhoneNumberMatcher.normalizePhoneNumber("0049612345678", prefixes))

        val variants = PhoneNumberMatcher.normalizePhoneNumber("0612345678", prefixes)
        assertEquals(2, variants.size)
        assertTrue(variants.contains(33612345678L))
        assertTrue(variants.contains(32612345678L))
    }

    /**
     * Test should block logic with common blocked patterns
     */
    @Test
    fun testShouldBlockNumber() {
        val blockedPatterns =
            listOf(
                BlockedPattern("Préfixe démarchage ARCEP", "33162######"),
            )
        val prefixes = setOf("33")

        assertTrue("Should block 33162123456", PhoneNumberMatcher.shouldBlock(33162123456L, prefixes, blockedPatterns))
        assertTrue("Should block national number 0162123456", PhoneNumberMatcher.shouldBlock("0162123456", prefixes, blockedPatterns))
        assertFalse("Should not block 33161123456", PhoneNumberMatcher.shouldBlock(33161123456L, prefixes, blockedPatterns))
        assertFalse("Should not block 0161123456", PhoneNumberMatcher.shouldBlock("0161123456", prefixes, blockedPatterns))
    }
}
