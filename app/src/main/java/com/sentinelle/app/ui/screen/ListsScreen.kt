package com.sentinelle.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.ListPriorityService
import com.sentinelle.app.ui.formatVersionDate
import com.sentinelle.app.ui.getTypeColor
import com.sentinelle.app.ui.getTypeIcon
import com.sentinelle.app.ui.getTypeLabel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
    var openListId by remember { mutableStateOf<Long?>(null) }

    val db = AppDatabase.getInstance(context)
    val userLists by db
        .patternListDao()
        .getBySourceFlow(PatternListEntity.SOURCE_USER)
        .collectAsState(initial = null)
    val apiLists by db
        .patternListDao()
        .getBySourceFlow(PatternListEntity.SOURCE_API)
        .collectAsState(initial = null)

    val sortedUserLists =
        remember(userLists) {
            userLists?.let { ListPriorityService.sortListsByPriority(it) }
        }
    val sortedApiLists =
        remember(apiLists) {
            apiLists?.let { ListPriorityService.sortListsByPriority(it) }
        }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Listes",
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
        ) {
            ListSection(
                title = "Listes personnelle",
                description =
                    "Ajoutez vos propres numéros ou préfixes pour qu'ils soient autorisés, " +
                        "identifiés ou bloqués.",
                lists = sortedUserLists,
                emptyMessage = "Chargement des listes personnelles...",
                numberFormat = numberFormat,
                onClick = { openListId = it },
            )

            ListSection(
                title = "Listes publiques",
                description =
                    "Les listes publiques de Sentinelle sont triées par priorité " +
                        "d'exécution. Vous pouvez les activer ou les désactiver.",
                lists = sortedApiLists,
                emptyMessage = "Les listes publiques sont téléchargées automatiquement. Veuillez patienter.",
                numberFormat = numberFormat,
                onClick = { openListId = it },
            )
        }
    }

    openListId?.let { listId ->
        com.sentinelle.app.ui.sheet.PatternListSheet(
            listId = listId,
            onDismiss = { openListId = null },
        )
    }
}

@Composable
private fun ListSection(
    title: String,
    description: String,
    lists: List<PatternListEntity>?,
    emptyMessage: String,
    numberFormat: NumberFormat,
    onClick: (Long) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp),
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        if (lists.isNullOrEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Column {
                lists.forEach { list ->
                    ListRow(
                        list = list,
                        numberFormat = numberFormat,
                        onClick = { onClick(list.id) },
                        isEnabled = list.isEnabled,
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ListRow(
    list: PatternListEntity,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
    isEnabled: Boolean,
) {
    val typeIcon = getTypeIcon(list.type)
    val typeLabel = getTypeLabel(list.type, list.channel)
    val noun = if (list.channel == PatternListEntity.CHANNEL_SMS) "élément" else "numéro"
    val countLabel = "${numberFormat.format(list.count)} $noun${if (list.count > 1) "s" else ""}"

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
                .alpha(if (isEnabled) 1f else 0.38f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = list.displayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            IconLabel(typeIcon, typeLabel, tint = getTypeColor(list.type))

            Spacer(modifier = Modifier.height(4.dp))

            IconLabel(Icons.Rounded.Numbers, countLabel)

            if (list.source == PatternListEntity.SOURCE_API && list.version.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                IconLabel(Icons.Rounded.CalendarMonth, "Mise à jour le ${formatVersionDate(list.version)}")
            }
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun IconLabel(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
