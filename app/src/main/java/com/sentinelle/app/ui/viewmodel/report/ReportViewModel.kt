package com.sentinelle.app.ui.viewmodel.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.NumberLabelEntity
import com.sentinelle.app.network.NetworkError
import com.sentinelle.app.network.NetworkService
import com.sentinelle.app.ui.NUMBER_LABEL_CATEGORIES
import com.sentinelle.app.util.PhoneNumberMatcher
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportUiState(
    val phoneNumber: String = "",
    val isGood: Boolean = false,
    val category: String = NUMBER_LABEL_CATEGORIES.first(),
    val isLoading: Boolean = false,
    val showAlert: Boolean = false,
    val alertMessage: String = "",
    val alertType: AlertType = AlertType.INFO,
)

enum class AlertType(
    val title: String,
) {
    SUCCESS("Succès"),
    ERROR("Erreur"),
    INFO("Information"),
}

class ReportViewModel(
    private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val networkService = NetworkService()

    fun updatePhoneNumber(number: String) {
        val formattedNumber = formatPhoneNumber(number)
        _uiState.value = _uiState.value.copy(phoneNumber = formattedNumber)
    }

    fun setIsGood(value: Boolean) {
        _uiState.value = _uiState.value.copy(isGood = value)
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun submitPhoneNumber() {
        if (!validatePhoneNumber()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val deviceId = PreferencesManager.getDeviceId(context)
                val countryCodes = PreferencesManager.getCountryCodes(context)
                val apiKey = PreferencesManager.getApiKey(context)
                networkService.reportPhoneNumber(_uiState.value.phoneNumber, _uiState.value.isGood, deviceId, countryCodes, apiKey)

                // A "spam" report also saves a local label on-device, so the
                // number immediately shows up in Lookup/Mes labels — this
                // never leaves the device beyond the anonymous report above.
                if (!_uiState.value.isGood) {
                    saveLocalLabel(_uiState.value.phoneNumber, _uiState.value.category)
                }

                handleSuccess()
            } catch (e: NetworkError) {
                handleNetworkError(e)
            } catch (e: Exception) {
                handleError()
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun saveLocalLabel(
        rawPhoneNumber: String,
        category: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val prefixes = PreferencesManager.getCountryPrefixes(context)
                val phoneNumber =
                    PhoneNumberMatcher.normalizePhoneNumber(rawPhoneNumber, prefixes).firstOrNull()
                        ?: return@withContext
                AppDatabase
                    .getInstance(context)
                    .numberLabelDao()
                    .upsert(
                        NumberLabelEntity(
                            phoneNumber = phoneNumber,
                            category = category,
                            note = null,
                            dateAdded = System.currentTimeMillis(),
                        ),
                    )
            } catch (e: Exception) {
                // Best-effort: the report itself already succeeded, so a
                // failure to save the local label is not surfaced to the user.
            }
        }
    }

    fun dismissAlert() {
        _uiState.value = _uiState.value.copy(showAlert = false)
    }

    private fun validatePhoneNumber(): Boolean {
        val trimmedNumber = _uiState.value.phoneNumber.trim()

        when {
            trimmedNumber.isEmpty() -> {
                showError("Veuillez saisir un numéro de téléphone.")
                return false
            }
        }

        // Clean: keep + at the beginning, digits only
        val cleaned = trimmedNumber.replace(Regex("[^0-9+]"), "")

        // Validate: must contain at least 2 digits and at most 15 digits
        val digitsOnly = cleaned.replace("+", "")
        if (digitsOnly.length < 2) {
            showError("Le numéro doit contenir au moins 2 chiffres.")
            return false
        }
        if (digitsOnly.length > 15) {
            showError("Le numéro doit contenir au maximum 15 chiffres.")
            return false
        }

        // Update with the cleaned version
        _uiState.value = _uiState.value.copy(phoneNumber = cleaned)

        return true
    }

    private fun handleSuccess() {
        _uiState.value =
            _uiState.value.copy(
                phoneNumber = "",
                alertType = AlertType.SUCCESS,
                alertMessage = "Numéro signalé avec succès ! Merci de votre contribution 😊",
                showAlert = true,
            )
    }

    private fun handleNetworkError(error: NetworkError) {
        _uiState.value =
            _uiState.value.copy(
                alertType = AlertType.ERROR,
                alertMessage = error.userMessage,
                showAlert = true,
            )
    }

    private fun handleError() {
        _uiState.value =
            _uiState.value.copy(
                alertType = AlertType.ERROR,
                alertMessage = "Une erreur inattendue s'est produite. Veuillez réessayer.",
                showAlert = true,
            )
    }

    private fun showError(message: String) {
        _uiState.value =
            _uiState.value.copy(
                alertType = AlertType.ERROR,
                alertMessage = message,
                showAlert = true,
            )
    }

    private fun formatPhoneNumber(input: String): String = input.replace(" ", "").filter { it.isDigit() || it == '+' }
}
