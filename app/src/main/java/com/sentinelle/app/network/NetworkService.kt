package com.sentinelle.app.network

import com.sentinelle.app.config.Config
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

class NetworkService(
    private val baseUrl: String = Config.API_BASE_URL,
    private val apiHost: String = Config.API_HOST,
) {
    private val gson = Gson()

    companion object {
        private const val TIMEOUT_SECONDS = 10
        private const val RESOURCE_TIMEOUT_SECONDS = 30
    }

    suspend fun reportPhoneNumber(
        phoneNumber: String,
        isGood: Boolean,
        deviceId: String,
        countryCodes: String,
        apiKey: String?,
    ) {
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/reports")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)

                if (!apiKey.isNullOrBlank()) {
                    connection.setRequestProperty("X-API-Key", apiKey)
                } else {
                    val firstCountryCode = countryCodes.split(",").firstOrNull()?.trim() ?: countryCodes
                    connection.setRequestProperty("X-Country-Code", firstCountryCode)
                }

                connection.doOutput = true
                connection.connectTimeout = TIMEOUT_SECONDS * 1000
                connection.readTimeout = RESOURCE_TIMEOUT_SECONDS * 1000

                val requestData =
                    ReportRequest(
                        phone = phoneNumber,
                        isGood = isGood,
                    )

                val jsonData = gson.toJson(requestData)
                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonData.toByteArray(StandardCharsets.UTF_8))
                }

                when (val responseCode = connection.responseCode) {
                    in 200..299 -> {
                        return@withContext
                    }

                    429 -> {
                        extractErrorMessage(connection)
                        throw NetworkError.TooManyRequests()
                    }

                    422 -> {
                        throw parseValidationError(connection)
                    }

                    401 -> {
                        throw NetworkError.Unauthorized()
                    }

                    in 400..499, in 500..599 -> {
                        val errorMessage = extractErrorMessage(connection)
                        throw NetworkError.ServerError(responseCode, errorMessage)
                    }

                    else -> {
                        throw NetworkError.Unknown()
                    }
                }
            } catch (_: SocketTimeoutException) {
                throw NetworkError.Timeout()
            } catch (_: IOException) {
                throw NetworkError.NetworkUnavailable()
            } catch (e: NetworkError) {
                throw e
            } catch (_: Exception) {
                throw NetworkError.Unknown()
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun healthCheck(
        deviceId: String,
        apiKey: String,
    ) {
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/health-check")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)
                connection.setRequestProperty("X-API-Key", apiKey)
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT_SECONDS * 1000
                connection.readTimeout = RESOURCE_TIMEOUT_SECONDS * 1000

                connection.outputStream.use { outputStream ->
                    outputStream.write("{}".toByteArray(StandardCharsets.UTF_8))
                }

                when (connection.responseCode) {
                    204 -> {
                        return@withContext
                    }

                    401 -> {
                        throw NetworkError.Unauthorized()
                    }

                    422 -> {
                        throw parseValidationError(connection)
                    }

                    else -> {
                        val errorMessage = extractErrorMessage(connection)
                        throw NetworkError.ServerError(connection.responseCode, errorMessage)
                    }
                }
            } catch (_: SocketTimeoutException) {
                throw NetworkError.Timeout()
            } catch (_: IOException) {
                throw NetworkError.NetworkUnavailable()
            } catch (e: NetworkError) {
                throw e
            } catch (_: Exception) {
                throw NetworkError.Unknown()
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun fetchAvailableLists(
        countryCodes: String,
        deviceId: String?,
        apiKey: String?,
    ): List<ListSummary> =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/lists")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                if (!apiKey.isNullOrBlank() && !deviceId.isNullOrBlank()) {
                    connection.setRequestProperty("X-Device-Id", deviceId)
                    connection.setRequestProperty("X-API-Key", apiKey)
                } else {
                    connection.setRequestProperty("X-Country-Code", countryCodes)
                }

                connection.connectTimeout = RESOURCE_TIMEOUT_SECONDS * 1000
                connection.readTimeout = RESOURCE_TIMEOUT_SECONDS * 1000

                when (val responseCode = connection.responseCode) {
                    in 200..299 -> {
                        val json = connection.inputStream.bufferedReader().use { it.readText() }
                        val type = object : TypeToken<List<ListSummary>>() {}.type
                        gson.fromJson(json, type) ?: emptyList()
                    }

                    429 -> {
                        extractErrorMessage(connection)
                        throw NetworkError.TooManyRequests()
                    }

                    422 -> {
                        throw parseValidationError(connection)
                    }

                    401 -> {
                        throw NetworkError.Unauthorized()
                    }

                    else -> {
                        val errorMessage = extractErrorMessage(connection)
                        throw NetworkError.ServerError(responseCode, errorMessage)
                    }
                }
            } catch (_: SocketTimeoutException) {
                throw NetworkError.Timeout()
            } catch (_: IOException) {
                throw NetworkError.NetworkUnavailable()
            } catch (e: NetworkError) {
                throw e
            } catch (_: Exception) {
                throw NetworkError.Unknown()
            } finally {
                connection.disconnect()
            }
        }

    suspend fun downloadListPatterns(
        listUrl: String,
        deviceId: String?,
        apiKey: String?,
    ): List<ListPatternInfo> =
        withContext(Dispatchers.IO) {
            val url =
                if (listUrl.startsWith("http")) {
                    URL(listUrl)
                } else {
                    URL("$apiHost$listUrl")
                }
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/x-ndjson")

                if (!apiKey.isNullOrBlank() && !deviceId.isNullOrBlank() && listUrl.startsWith("/api")) {
                    connection.setRequestProperty("X-Device-Id", deviceId)
                    connection.setRequestProperty("X-API-Key", apiKey)
                }

                connection.connectTimeout = RESOURCE_TIMEOUT_SECONDS * 1000
                connection.readTimeout = RESOURCE_TIMEOUT_SECONDS * 1000

                when (val responseCode = connection.responseCode) {
                    in 200..299 -> {
                        connection.inputStream.bufferedReader().use { reader ->
                            reader
                                .lineSequence()
                                .filter { it.isNotBlank() }
                                .mapNotNull { line ->
                                    try {
                                        gson.fromJson(line, ListPatternInfo::class.java)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }.toList()
                        }
                    }

                    429 -> {
                        extractErrorMessage(connection)
                        throw NetworkError.TooManyRequests()
                    }

                    422 -> {
                        throw parseValidationError(connection)
                    }

                    401 -> {
                        throw NetworkError.Unauthorized()
                    }

                    else -> {
                        val errorMessage = extractErrorMessage(connection)
                        throw NetworkError.ServerError(responseCode, errorMessage)
                    }
                }
            } catch (_: SocketTimeoutException) {
                throw NetworkError.Timeout()
            } catch (_: IOException) {
                throw NetworkError.NetworkUnavailable()
            } catch (e: NetworkError) {
                throw e
            } catch (_: Exception) {
                throw NetworkError.Unknown()
            } finally {
                connection.disconnect()
            }
        }

    private fun parseValidationError(connection: HttpURLConnection): NetworkError.ValidationError {
        val errorText =
            try {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) {
                ""
            }

        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
            val rawMap: Map<String, Any> = gson.fromJson(errorText, type)
            val errorsMap = mutableMapOf<String, List<String>>()

            val errorsField = rawMap["errors"]
            if (errorsField is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val fieldErrors = errorsField as Map<String, List<String>>
                errorsMap.putAll(fieldErrors)
            }

            NetworkError.ValidationError(errorsMap)
        } catch (_: Exception) {
            NetworkError.ValidationError(emptyMap())
        }
    }

    private fun extractErrorMessage(connection: HttpURLConnection): String? =
        try {
            val errorStream = connection.errorStream
            if (errorStream != null) {
                val errorText = errorStream.bufferedReader().use { it.readText() }

                try {
                    val errorResponse = gson.fromJson(errorText, ErrorResponse::class.java)
                    errorResponse.message
                        ?: errorResponse.error
                        ?: errorResponse.detail
                        ?: errorResponse.errors?.firstOrNull()?.detail
                        ?: errorResponse.errors?.firstOrNull()?.title
                } catch (_: JsonSyntaxException) {
                    errorText.ifBlank { null }
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
}
