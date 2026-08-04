package com.sentinelle.app.util

object SmsNumberExtractor {
    data class PersonInfo(
        val uri: String?,
    )

    fun extractSenderNumber(
        peopleList: List<PersonInfo>?,
        messagingPerson: PersonInfo?,
        title: String?,
        text: String?,
        bigText: String?,
        charSequenceText: CharSequence?,
    ): String? {
        peopleList?.forEach { person ->
            person.uri?.let { uri ->
                if (uri.startsWith("tel:")) {
                    val number = uri.substring(4).replace(Regex("[^0-9+]"), "")
                    if (number.length >= 4) return number
                }
            }
        }

        messagingPerson?.uri?.let { uri ->
            if (uri.startsWith("tel:")) {
                val number = uri.substring(4).replace(Regex("[^0-9+]"), "")
                if (number.length >= 4) return number
            }
        }

        title?.let { t ->
            val cleaned = t.replace(Regex("[^0-9+]"), "")
            if (cleaned.length >= 4 && t.matches(Regex("^[+0-9\\s().-]{4,}$"))) {
                return cleaned
            }
        }

        val allText =
            buildString {
                title?.let { append(it).append(" ") }
                text?.let { append(it).append(" ") }
                bigText?.let { append(it).append(" ") }
                charSequenceText?.let { append(it).append(" ") }
            }

        val phoneRegex = Regex("\\+?[0-9]{1,4}(?:[\\s().-]?[0-9]{1,4})+")
        phoneRegex.find(allText)?.value?.let { match ->
            val cleaned = match.replace(Regex("[^0-9+]"), "")
            if (cleaned.length >= 7) return cleaned
        }

        return null
    }
}
