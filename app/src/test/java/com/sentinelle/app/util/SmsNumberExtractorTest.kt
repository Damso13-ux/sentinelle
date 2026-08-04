package com.sentinelle.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsNumberExtractorTest {
    private val extractor = SmsNumberExtractor

    @Test
    fun extractFromPeopleList_telUri() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:+33612345678"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromPeopleList_telUriWithFormatting() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:+33 6 12 34 56 78"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromPeopleList_telUriStripsNonDigits() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:(06) 12-34.56 78"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun extractFromPeopleList_shortNumberRejected() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:123"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromPeopleList_exactlyFourDigits() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:1234"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("1234", result)
    }

    @Test
    fun extractFromPeopleList_nonTelUriSkipped() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "mailto:test@example.com"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromPeopleList_nullUriSkipped() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = null))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromPeopleList_multiplePeople_firstTelWins() {
        val people =
            listOf(
                SmsNumberExtractor.PersonInfo(uri = "tel:+33612345678"),
                SmsNumberExtractor.PersonInfo(uri = "tel:+33698765432"),
            )
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromMessagingPerson_telUri() {
        val person = SmsNumberExtractor.PersonInfo(uri = "tel:0612345678")
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = person,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun peopleListTakesPriorityOverMessagingPerson() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:+33611111111"))
        val messagingPerson = SmsNumberExtractor.PersonInfo(uri = "tel:+33622222222")
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = messagingPerson,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33611111111", result)
    }

    @Test
    fun messagingPersonFallsBackWhenPeopleListEmpty() {
        val messagingPerson = SmsNumberExtractor.PersonInfo(uri = "tel:+33622222222")
        val result =
            extractor.extractSenderNumber(
                peopleList = emptyList(),
                messagingPerson = messagingPerson,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33622222222", result)
    }

    @Test
    fun messagingPersonSkippedWhenNonTelUri() {
        val messagingPerson = SmsNumberExtractor.PersonInfo(uri = "mailto:test@example.com")
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = messagingPerson,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromTitle_numericTitle() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "+33612345678",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromTitle_formattedNumber() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "06 12 34 56 78",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun extractFromTitle_numberWithDots() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "(06) 12-34.56 78",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun extractFromTitle_titleWithLetters_fallsBackToRegex() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "0612345678aub",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun extractFromTitle_tooShortRejected() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "123",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromTitle_exactlyFourDigits() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "1234",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("1234", result)
    }

    @Test
    fun peopleListTakesPriorityOverTitle() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:+33611111111"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = "+33622222222",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33611111111", result)
    }

    @Test
    fun extractFromText_formattedPhoneNumber() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "Nouveau SMS",
                text = "Message de +33 6 12 34 56 78",
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromText_internationalNumber() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "SMS",
                text = "+33612345678",
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromText_regexRequiresMinSevenDigits() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "SMS de 123456",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun extractFromText_regexMatchesSevenDigits() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "SMS de 1234567",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("1234567", result)
    }

    @Test
    fun allNullsReturnsNull() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals(null, result)
    }

    @Test
    fun titleTakesPriorityOverTextRegex() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "06 12 34 56 78",
                text = "Message de +33 9 87 65 43 21",
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun extractFromBigText() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "Message",
                text = "Coucou",
                bigText = "+33612345678 vous a envoyé un message",
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractFromCharSequenceText() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "Message",
                text = null,
                bigText = null,
                charSequenceText = "De +33612345678",
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun contactNameInTitle_fallsBackToRegex() {
        val result =
            extractor.extractSenderNumber(
                peopleList = null,
                messagingPerson = null,
                title = "Jean Dupont",
                text = "06 12 34 56 78: Salut ça va?",
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }

    @Test
    fun emptyPeopleList_fallsBackToMessagingPerson() {
        val person = SmsNumberExtractor.PersonInfo(uri = "tel:+33612345678")
        val result =
            extractor.extractSenderNumber(
                peopleList = emptyList(),
                messagingPerson = person,
                title = "+33698765432",
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun peopleListWithNonTelAndPasswordTel() {
        val people =
            listOf(
                SmsNumberExtractor.PersonInfo(uri = "mailto:test@example.com"),
                SmsNumberExtractor.PersonInfo(uri = "tel:+33612345678"),
            )
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractedNumberPreservesPlus() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:+33612345678"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("+33612345678", result)
    }

    @Test
    fun extractedNumberWithoutPlus() {
        val people = listOf(SmsNumberExtractor.PersonInfo(uri = "tel:0612345678"))
        val result =
            extractor.extractSenderNumber(
                peopleList = people,
                messagingPerson = null,
                title = null,
                text = null,
                bigText = null,
                charSequenceText = null,
            )
        assertEquals("0612345678", result)
    }
}
