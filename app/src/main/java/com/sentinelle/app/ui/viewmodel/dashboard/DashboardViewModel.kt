package com.sentinelle.app.ui.viewmodel.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.BlockedEventEntity
import com.sentinelle.app.data.DayCount
import com.sentinelle.app.data.HeuristicShadowEventEntity
import com.sentinelle.app.data.NumberLabelEntity
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.ui.formatBlockReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class TimeRange(
    val label: String,
    val days: Long?,
) {
    WEEK("Semaine", 7L),
    MONTH("Mois", 30L),
    ALL("Tout", null),
}

data class TopBlockedNumberDisplay(
    val phoneNumber: Long,
    val count: Int,
    val label: String?,
)

data class DashboardUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val totalBlocked: Int = 0,
    val blockedCalls: Int = 0,
    val blockedSms: Int = 0,
    val dailyTrend: List<DayCount> = emptyList(),
    val topBlockedNumbers: List<TopBlockedNumberDisplay> = emptyList(),
    val recentEvents: List<BlockedEventEntity> = emptyList(),
    val shadowEvents: List<HeuristicShadowEventEntity> = emptyList(),
)

class DashboardViewModel(
    private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        loadData()
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).blockedEventDao().deleteAll()
            }
            loadData()
        }
    }

    private fun loadData() {
        val range = _uiState.value.selectedRange
        viewModelScope.launch {
            val newState =
                withContext(Dispatchers.IO) {
                    val since = rangeStart(range)
                    val db = AppDatabase.getInstance(context)
                    val eventDao = db.blockedEventDao()
                    val labelDao = db.numberLabelDao()

                    val total = eventDao.getCountSince(since)
                    val byChannel =
                        eventDao.getCountByChannelSince(since).associate { it.channel to it.count }
                    val trend = eventDao.getCountByDaySince(since)
                    // Recent activity is a feed, not a range-scoped stat — always
                    // shows the latest events regardless of the selected range.
                    val recent = eventDao.getRecent(limit = 15)
                    val shadow = db.heuristicShadowEventDao().getRecent(limit = 15)
                    val top =
                        eventDao.getTopBlockedNumbersSince(since, limit = 5).map { entry ->
                            TopBlockedNumberDisplay(
                                phoneNumber = entry.phoneNumber,
                                count = entry.count,
                                label =
                                    labelDao
                                        .getByPhoneNumber(entry.phoneNumber)
                                        ?.let { NumberLabelEntity.displayName(it.category) },
                            )
                        }

                    DashboardUiState(
                        selectedRange = range,
                        totalBlocked = total,
                        blockedCalls = byChannel[PatternListEntity.CHANNEL_PHONE] ?: 0,
                        blockedSms = byChannel[PatternListEntity.CHANNEL_SMS] ?: 0,
                        dailyTrend = trend,
                        topBlockedNumbers = top,
                        recentEvents = recent,
                        shadowEvents = shadow,
                    )
                }
            _uiState.value = newState
        }
    }

    /**
     * Builds a CSV of every blocked event ever recorded (not just the
     * selected range or the 15-event "recent" feed) — the whole point of
     * exporting is to get more than what fits on screen. Caller writes the
     * result wherever the user picked via Storage Access Framework; this
     * class never touches a Uri or a file, just the data. Pro-gated at the
     * call site (DashboardScreen), not here.
     */
    suspend fun buildCsvExport(): String =
        withContext(Dispatchers.IO) {
            val events = AppDatabase.getInstance(context).blockedEventDao().getAll()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE)

            buildString {
                appendLine("Date,Canal,Numero,Raison,Score heuristique")
                events.forEach { event ->
                    val date = dateFormat.format(Date(event.timestamp))
                    val channel = if (event.channel == PatternListEntity.CHANNEL_SMS) "SMS" else "Appel"
                    val number = if (event.phoneNumber == 0L) "Masque" else event.phoneNumber.toString()
                    val reason = formatBlockReason(event).replace("\"", "\"\"")
                    val score = event.heuristicScore?.let { "%.2f".format(Locale.FRANCE, it) } ?: ""
                    appendLine("$date,$channel,$number,\"$reason\",$score")
                }
            }
        }

    private fun rangeStart(range: TimeRange): Long {
        val days = range.days ?: return 0L
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days)
    }
}
