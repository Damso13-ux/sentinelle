package com.sentinelle.app.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.data.PatternListItemEntity
import com.sentinelle.app.service.PatternService
import com.sentinelle.app.ui.formatVersionDate
import com.sentinelle.app.ui.getChannelIcon
import com.sentinelle.app.ui.getChannelLabel
import com.sentinelle.app.ui.getTypeColor
import com.sentinelle.app.ui.getTypeIcon
import com.sentinelle.app.ui.getTypeLabel
import com.sentinelle.app.util.PatternManager
import java.text.NumberFormat
import java.util.Locale

private const val PAGE_SIZE = 500
private const val LOAD_MORE_THRESHOLD = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternListSheet(
    listId: Long,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
    var showAddSheet by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var detailPatternId by remember { mutableStateOf<Long?>(null) }

    val db = AppDatabase.getInstance(context)
    val list =
        remember(refreshKey) {
            db.patternListDao().getById(listId)
        }

    if (list == null) {
        onDismiss()
        return
    }

    val isUser = list.source == PatternListEntity.SOURCE_USER
    val lazyListState = rememberLazyListState()

    var patterns by remember { mutableStateOf<List<PatternListItemEntity>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }

    LaunchedEffect(listId, refreshKey) {
        patterns = db.patternListItemDao().getPatternsByListIdPaged(listId, PAGE_SIZE, 0)
        totalCount = db.patternListItemDao().getCountByListId(listId)
        hasMore = patterns.size < totalCount
        lazyListState.scrollToItem(0)
    }

    LaunchedEffect(lazyListState, hasMore) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.collect { lastVisibleIndex ->
            if (hasMore && lastVisibleIndex >= 0 && lastVisibleIndex >= patterns.size - LOAD_MORE_THRESHOLD) {
                val more = db.patternListItemDao().getPatternsByListIdPaged(listId, PAGE_SIZE, patterns.size)
                patterns = patterns + more
                hasMore = patterns.size < totalCount
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        contentWindowInsets = { WindowInsets.systemBars },
    ) {
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            item(key = "title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = list.displayName(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    if (isUser) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Ajouter un élément",
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isUser) {
                item(key = "list_header") {
                    ListDetailHeader(
                        list = list,
                        patternCount = totalCount,
                        numberFormat = numberFormat,
                        onToggle = { enabled ->
                            db.patternListDao().setEnabled(listId, enabled)
                            PatternManager.clearCache()
                            refreshKey++
                        },
                    )
                }
            } else {
                item(key = "user_header") {
                    val descriptionText =
                        when (list.type) {
                            PatternListEntity.TYPE_ALLOW -> {
                                "Ajoutez des préfixes ou des numéros à autoriser. Ces numéros ne seront jamais bloqués et seront identifiés par une notification."
                            }

                            else -> {
                                "Ajoutez des préfixes ou des numéros à bloquer. Ces numéros s'ajoutent à ceux des listes publiques."
                            }
                        }

                    Column {
                        if (!list.description.isNullOrBlank() || !list.license.isNullOrBlank()) {
                            Column {
                                if (!list.description.isNullOrBlank()) {
                                    Text(
                                        text = list.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!list.license.isNullOrBlank()) {
                                    if (!list.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = "Licence : ${list.license}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(16.dp),
                                    ).padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (list.isEnabled) "Liste activée" else "Liste désactivée",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Active ou désactive l'application de cette liste.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = list.isEnabled,
                                    onCheckedChange = { enabled ->
                                        db.patternListDao().setEnabled(listId, enabled)
                                        PatternManager.clearCache()
                                        refreshKey++
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = descriptionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            items(patterns, key = { "${it.id}" }) { pattern ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { detailPatternId = pattern.id },
                ) {
                    PatternRow(
                        pattern = pattern,
                        type = list.type,
                        numberFormat = numberFormat,
                    )
                }
            }

            if (hasMore) {
                item(key = "loading_more") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }

            if (patterns.isEmpty() && !hasMore) {
                item(key = "empty") {
                    Text(
                        text = "Aucun élément dans cette liste.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddSheet) {
        AddElementSheet(
            listId = listId,
            onDismiss = { showAddSheet = false },
            onPatternAdded = { refreshKey++ },
        )
    }

    detailPatternId?.let { patternId ->
        PatternDetailSheet(
            patternId = patternId,
            onDismiss = { detailPatternId = null },
        )
    }
}

@Composable
private fun PatternRow(
    pattern: PatternListItemEntity,
    type: String,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val isPhone = pattern.pattern.firstOrNull()?.isDigit() == true
    val isPrefix = pattern.pattern.contains('#')
    val coveredNumbers = if (isPhone && isPrefix) PatternService.calculateCoveredNumbers(pattern.pattern) else null
    val countLabel =
        when {
            isPhone && isPrefix -> "numéros"
            isPhone -> "numéro"
            else -> "élément"
        }
    val actionIcon = getTypeIcon(type)
    val actionColor = getTypeColor(type)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = actionIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = actionColor,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pattern.name.ifEmpty { if (isPhone) "+${pattern.pattern}" else pattern.pattern },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (coveredNumbers != null && coveredNumbers > 1) {
                    Text(
                        text = "${numberFormat.format(coveredNumbers)} $countLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (isPhone) "+${pattern.pattern}" else pattern.pattern,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListDetailHeader(
    list: PatternListEntity,
    patternCount: Int,
    numberFormat: NumberFormat,
    onToggle: (Boolean) -> Unit,
) {
    val typeIcon = getTypeIcon(list.type)
    val typeColor = getTypeColor(list.type)
    val typeLabel = getTypeLabel(list.type, list.channel)
    val channelIcon = getChannelIcon(list.channel)
    val channelLabel = getChannelLabel(list.channel)
    val versionDate = formatVersionDate(list.version)

    Column {
        if (!list.description.isNullOrBlank() || !list.license.isNullOrBlank()) {
            Column {
                if (!list.description.isNullOrBlank()) {
                    Text(
                        text = list.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!list.license.isNullOrBlank()) {
                    if (!list.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Licence : ${list.license}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (list.isEnabled) "Liste activée" else "Liste désactivée",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Active ou désactive l'application de cette liste.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = list.isEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(icon = typeIcon, label = "Type", value = typeLabel, iconTint = typeColor)
            DetailRow(icon = channelIcon, label = "Canal", value = channelLabel)
            DetailRow(icon = Icons.Rounded.CalendarMonth, label = "Version", value = versionDate)
            DetailRow(
                icon = Icons.Rounded.Numbers,
                label = "Éléments",
                value = "${numberFormat.format(patternCount)} élément${if (patternCount > 1) "s" else ""}",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = Color.Black,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
