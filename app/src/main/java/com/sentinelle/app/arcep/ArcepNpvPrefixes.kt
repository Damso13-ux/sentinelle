package com.sentinelle.app.arcep

// Since January 2023, ARCEP (the French telecom regulator) requires
// telemarketing / automated-call platforms to use one of these 12 reserved
// "NPV" (numéro polyvalent vérifié) prefixes, so consumers can recognize a
// commercial-prospecting call. This is a stable regulatory list, not
// something fetched over the network — no backend to maintain.
//
// https://en-contact.com/la-foire-aux-numeros-larcep-alloue-des-nouvelles-tranches-de-numeros-obligatoires-pour-le-demarchage-telephonique
object ArcepNpvPrefixes {
    private val NATIONAL_PREFIXES =
        setOf(
            "0162", "0163",
            "0270", "0271",
            "0377", "0378",
            "0424", "0425",
            "0568", "0569",
            "0948", "0949",
        )

    // Numbers in this app are stored normalized with the country prefix
    // (e.g. "33") instead of the leading "0" — see PhoneNumberMatcher.
    private val INTERNATIONAL_PREFIXES =
        NATIONAL_PREFIXES.map { "33" + it.removePrefix("0") }.toSet()

    fun isNpvNumber(phoneNumber: Long): Boolean {
        val value = phoneNumber.toString()
        return INTERNATIONAL_PREFIXES.any { value.startsWith(it) }
    }
}
