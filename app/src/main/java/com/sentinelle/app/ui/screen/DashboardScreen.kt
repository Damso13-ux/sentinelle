package com.sentinelle.app.ui.screen

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentinelle.app.data.BlockedEventEntity
import com.sentinelle.app.data.DayCount
import com.sentinelle.app.data.HeuristicShadowEventEntity
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.ui.formatBlockReason
import com.sentinelle.app.ui.viewmodel.dashboard.DashboardViewModel
import java.text.DateFormat
import java.util.Locale
import com.sentinelle.app.ui.viewmodel.dashboard.DashboardViewModelFactory
import com.sentinelle.app.ui.viewmodel.dashboard.TimeRange
import com.sentinelle.app.ui.viewmodel.dashboard.TopBlockedNumberDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel =
        viewModel(
            factory = DashboardViewModelFactory(LocalContext.current),
        ),
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Statistiques", fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Effacer l'historique")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RangeSelector(selected = uiState.selectedRange, onSelect = viewModel::setRange)
            SummaryRow(total = uiState.totalBlocked, calls = uiState.blockedCalls, sms = uiState.blockedSms)
            TrendCard(dailyTrend = uiState.dailyTrend)
            TopNumbersCard(topBlockedNumbers = uiState.topBlockedNumbers)
            RecentEventsCard(events = uiState.recentEvents)
            if (uiState.shadowEvents.isNotEmpty()) {
                ShadowEventsCard(events = uiState.shadowEvents)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Effacer l'historique ?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Tous les événements bloqués enregistrés seront supprimés définitivement.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Effacer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDialog = false },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                ) {
                    Text("Annuler", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun RangeSelector(
    selected: TimeRange,
    onSelect: (TimeRange) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            )
        }
    }
}

@Composable
private fun SummaryRow(
    total: Int,
    calls: Int,
    sms: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.PhoneDisabled, label = "Total bloqué", value = total)
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Phone, label = "Appels", value = calls)
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Sms, label = "SMS", value = sms)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.height(20.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendCard(dailyTrend: List<DayCount>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tendance quotidienne",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (dailyTrend.isEmpty()) {
                Text(
                    text = "Aucun blocage sur cette période.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                BarChart(dailyTrend)
            }
        }
    }
}

@Composable
private fun BarChart(dailyTrend: List<DayCount>) {
    val barColor = MaterialTheme.colorScheme.tertiary
    val maxCount = (dailyTrend.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val barCount = dailyTrend.size
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1).coerceAtLeast(0)) / barCount
        dailyTrend.forEachIndexed { index, day ->
            val barHeight = size.height * (day.count.toFloat() / maxCount.toFloat())
            drawRect(
                color = barColor,
                topLeft =
                    androidx.compose.ui.geometry.Offset(
                        x = index * (barWidth + gap),
                        y = size.height - barHeight,
                    ),
                size = androidx.compose.ui.geometry.Size(width = barWidth, height = barHeight.coerceAtLeast(2f)),
            )
        }
    }
}

@Composable
private fun TopNumbersCard(topBlockedNumbers: List<TopBlockedNumberDisplay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Numéros les plus bloqués",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (topBlockedNumbers.isEmpty()) {
                Text(
                    text = "Aucun numéro récurrent sur cette période.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                topBlockedNumbers.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = formatPhoneNumber(entry.phoneNumber),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (entry.label != null) {
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = "${entry.count}×",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEventsCard(events: List<BlockedEventEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Derniers blocages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (events.isEmpty()) {
                Text(
                    text = "Aucun blocage récent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEach { event -> RecentEventRow(event) }
            }
        }
    }
}

@Composable
private fun RecentEventRow(event: BlockedEventEntity) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.FRANCE) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (event.channel == PatternListEntity.CHANNEL_SMS) Icons.Rounded.Sms else Icons.Rounded.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (event.phoneNumber == 0L) "Numéro masqué" else formatPhoneNumber(event.phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatBlockReason(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = dateFormat.format(java.util.Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShadowEventsCard(events: List<HeuristicShadowEventEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Simulation (mode silencieux)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Ce que l'heuristique aurait bloqué si le mode silencieux était désactivé.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            events.forEach { event -> ShadowEventRow(event) }
        }
    }
}

@Composable
private fun ShadowEventRow(event: HeuristicShadowEventEntity) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.FRANCE) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (event.channel == PatternListEntity.CHANNEL_SMS) Icons.Rounded.Sms else Icons.Rounded.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatPhoneNumber(event.phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = event.reason ?: "Score ${(event.score * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = dateFormat.format(java.util.Date(event.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPhoneNumber(number: Long): String =
    PhoneNumberUtils.formatNumberToE164(number.toString(), "FR") ?: number.toString()
