package com.sentinelle.app.ui.viewmodel.lookup

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinelle.app.arcep.ArcepNpvPrefixes
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.NumberLabelEntity
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.ListPriorityService
import com.sentinelle.app.service.PatternService
import com.sentinelle.app.util.PhoneNumberMatcher
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PatternMatchInfo(
    val type: String,
    val listName: String,
)

data class LookupResult(
    val phoneNumber: Long,
    val displayNumber: String,
    val patternMatch: PatternMatchInfo?,
    val isArcepNpv: Boolean,
    val blockedCount: Int,
    val label: NumberLabelEntity?,
    // Id of this number's entry in the personal allow list, or null if it
    // isn't allowed. Doubles as the "is allowed?" flag and as the handle
    // needed to remove it again.
    val allowListItemId: Long? = null,
)

data class LookupUiState(
    val query: String = "",
    val notFound: Boolean = false,
    val result: LookupResult? = null,
)

// 100% local: everything here reads from on-device tables (pattern lists,
// blocked_events, number_labels) plus the static ARCEP prefix list — no
// contact list is uploaded and no network call is made.
class LookupViewModel(
    private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LookupUiState())
    val uiState: StateFlow<LookupUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query, notFound = false)
    }

    fun search() {
        val raw = _uiState.value.query
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { buildResult(raw) }
            _uiState.value =
                if (result == null) {
                    _uiState.value.copy(notFound = true, result = null)
                } else {
                    _uiState.value.copy(notFound = false, result = result)
                }
        }
    }

    fun saveLabel(
        category: String,
        note: String?,
    ) {
        val phoneNumber = _uiState.value.result?.phoneNumber ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase
                    .getInstance(context)
                    .numberLabelDao()
                    .upsert(
                        NumberLabelEntity(
                            phoneNumber = phoneNumber,
                            category = category,
                            note = note?.takeIf { it.isNotBlank() },
                            dateAdded = System.currentTimeMillis(),
                        ),
                    )
            }
            search()
        }
    }

    fun deleteLabel() {
        val phoneNumber = _uiState.value.result?.phoneNumber ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).numberLabelDao().deleteByPhoneNumber(phoneNumber)
            }
            search()
        }
    }

    /**
     * Adds the number to the personal allow list, which is checked before
     * every block list and before the heuristic — so this takes effect on
     * the next call, without touching the block lists themselves.
     *
     * This is the false-positive escape hatch for a number spotted after
     * the fact: the "Ce n'est pas un spam" notification action only exists
     * while the notification is still there.
     */
    fun allowNumber() {
        val result = _uiState.value.result ?: return
        if (result.allowListItemId != null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PatternService.addUserPattern(
                    pattern = "+${result.phoneNumber}",
                    name = "Autorisé depuis la recherche",
                    listId = PatternListEntity.USER_ALLOW_LIST_ID,
                    context = context,
                )
            }
            search()
        }
    }

    fun removeFromAllowList() {
        val itemId = _uiState.value.result?.allowListItemId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PatternService.deletePattern(itemId, context)
            }
            search()
        }
    }

    private suspend fun buildResult(raw: String): LookupResult? {
        if (raw.isBlank()) return null
        val prefixes = PreferencesManager.getCountryPrefixes(context)
        val phoneNumber =
            PhoneNumberMatcher.normalizePhoneNumber(raw, prefixes).firstOrNull() ?: return null

        val db = AppDatabase.getInstance(context)
        val displayNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber.toString(), "FR") ?: phoneNumber.toString()
        val patternMatch = findPatternMatch(phoneNumber, prefixes, db)
        val isArcepNpv = ArcepNpvPrefixes.isNpvNumber(phoneNumber)
        val blockedCount = db.blockedEventDao().getCountForNumber(phoneNumber)
        val label = db.numberLabelDao().getByPhoneNumber(phoneNumber)
        // Match on the variants rather than the raw number: the allow-list
        // entry may have been stored in a different notation (national vs.
        // international) depending on where it was added from.
        val variants = PhoneNumberMatcher.generateVariants(phoneNumber, prefixes)
        val allowListItemId =
            db
                .patternListItemDao()
                .getPatternsByListId(PatternListEntity.USER_ALLOW_LIST_ID)
                .firstOrNull { item ->
                    variants.any { PhoneNumberMatcher.matchesPattern(it, item.pattern) }
                }?.id

        return LookupResult(
            phoneNumber = phoneNumber,
            displayNumber = displayNumber,
            patternMatch = patternMatch,
            isArcepNpv = isArcepNpv,
            blockedCount = blockedCount,
            label = label,
            allowListItemId = allowListItemId,
        )
    }

    private fun findPatternMatch(
        phoneNumber: Long,
        prefixes: Set<String>,
        db: AppDatabase,
    ): PatternMatchInfo? {
        val variants = PhoneNumberMatcher.generateVariants(phoneNumber, prefixes)
        val lists = ListPriorityService.sortListsByPriority(db.patternListDao().getEnabledLists())
        for (list in lists) {
            val patterns = db.patternListItemDao().getPatternsByListId(list.id)
            val matched =
                variants.firstNotNullOfOrNull { variant ->
                    patterns.firstOrNull { PhoneNumberMatcher.matchesPattern(variant, it.pattern) }
                }
            if (matched != null) {
                return PatternMatchInfo(type = list.type, listName = list.displayName())
            }
        }
        return null
    }
}
