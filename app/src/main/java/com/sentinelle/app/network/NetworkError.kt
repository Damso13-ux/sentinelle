package com.sentinelle.app.network

sealed class NetworkError(
    message: String,
) : Exception(message) {
    class InvalidURL : NetworkError("URL invalide.")

    class NoData : NetworkError("Aucune donnée reçue du serveur.")

    class DecodingError : NetworkError("Erreur lors du traitement des données.")

    data class ServerError(
        val code: Int,
        val serverMessage: String?,
    ) : NetworkError(
            serverMessage ?: "Erreur serveur ($code). Veuillez réessayer plus tard.",
        )

    class NetworkUnavailable : NetworkError("Connexion réseau indisponible. Vérifiez votre connexion Internet.")

    class Timeout : NetworkError("Délai d'attente dépassé. Veuillez réessayer.")

    class TooManyRequests : NetworkError("Trop de requêtes effectuées. Veuillez patienter quelques minutes avant de réessayer.")

    class Unauthorized : NetworkError("Clé API invalide ou accès non autorisé.")

    data class ValidationError(
        val errors: Map<String, List<String>>,
    ) : NetworkError(
            errors.entries
                .firstOrNull()
                ?.value
                ?.firstOrNull()
                ?: "Données invalides. Veuillez vérifier votre saisie.",
        )

    class Unknown : NetworkError("Une erreur inattendue s'est produite.")

    val userMessage: String
        get() = message ?: "Une erreur inattendue s'est produite."
}
