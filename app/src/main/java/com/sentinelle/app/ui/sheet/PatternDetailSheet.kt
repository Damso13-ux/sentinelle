package com.sentinelle.app.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.data.PatternListItemEntity
import com.sentinelle.app.service.PatternService
import com.sentinelle.app.ui.getTypeActionLabel
import com.sentinelle.app.ui.getTypeColor
import com.sentinelle.app.ui.getTypeIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private sealed interface PatternDetailState {
    data object Loading : PatternDetailState

    data object NotFound : PatternDetailState

    data class Ready(
        val item: PatternListItemEntity,
        val list: PatternListEntity,
    ) : PatternDetailState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternDetailSheet(
    patternId: Long,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Read off the main thread. "Loading" and "not found" must stay
    // distinct states: this sheet dismisses itself when the row is gone,
    // and a plain nullable would make it dismiss on every open, before the
    // query had a chance to return.
    val state by produceState<PatternDetailState>(PatternDetailState.Loading, patternId) {
        value =
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(context)
                val item = db.patternListItemDao().getPatternById(patternId)
                val list = item?.let { db.patternListDao().getById(it.listId) }
                if (item != null && list != null) {
                    PatternDetailState.Ready(item, list)
                } else {
                    PatternDetailState.NotFound
                }
            }
    }

    if (state is PatternDetailState.NotFound) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val ready = state as? PatternDetailState.Ready ?: return
    val pattern = ready.item
    val parentList = ready.list

    // Driven by the parent list's channel rather than sniffing the pattern
    // text — a keyword like "100% gratuit" starts with a digit too.
    val isPhone = parentList.channel != PatternListEntity.CHANNEL_SMS
    val isPrefix = isPhone && pattern.pattern.contains('#')
    val (rangeFirst, rangeLast) = PatternService.getNumberRange(pattern.pattern)
    val coveredNumbers = PatternService.calculateCoveredNumbers(pattern.pattern)
    val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
    val dateFormat =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.FRANCE)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        contentWindowInsets = { WindowInsets.systemBars },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = pattern.name.ifEmpty { if (isPhone) "+${pattern.pattern}" else pattern.pattern },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                        ).padding(horizontal = 16.dp),
            ) {
                DetailRow(
                    icon = Icons.Rounded.Phone,
                    label =
                        when {
                            isPhone && isPrefix -> "Préfixe"
                            isPhone -> "Numéro"
                            else -> "Élément"
                        },
                    value = if (isPhone) "+${pattern.pattern}" else pattern.pattern,
                )

                if (isPhone && isPrefix) {
                    DetailRow(
                        icon = Icons.Rounded.Numbers,
                        label = "Plage de numéros",
                        value = "$rangeFirst → $rangeLast",
                    )

                    DetailRow(
                        icon = Icons.Rounded.Tag,
                        label = "Numéros couverts",
                        value = numberFormat.format(coveredNumbers),
                    )
                }

                DetailRow(
                    icon = getTypeIcon(parentList.type),
                    label = "Action",
                    value = getTypeActionLabel(parentList.type),
                    iconTint = getTypeColor(parentList.type),
                )

                DetailRow(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Date d'ajout",
                    value = dateFormat.format(Date(pattern.dateAdded)),
                )
            }

            if (parentList.source == PatternListEntity.SOURCE_USER) {
                Spacer(modifier = Modifier.height(24.dp))

                val deleteLabel =
                    when {
                        isPhone && isPrefix -> "Supprimer ce préfixe"
                        isPhone -> "Supprimer ce numéro"
                        else -> "Supprimer cet élément"
                    }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(deleteLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        val dialogTitle =
            when {
                isPhone && isPrefix -> "Supprimer le préfixe ?"
                isPhone -> "Supprimer le numéro ?"
                else -> "Supprimer l'élément ?"
            }
        val dialogMessage =
            when {
                isPhone && isPrefix -> "Le préfixe +${pattern.pattern} sera supprimé définitivement."
                isPhone -> "Le numéro +${pattern.pattern} sera supprimé définitivement."
                else -> "L'élément ${pattern.pattern} sera supprimé définitivement."
            }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        PatternService.deletePattern(pattern.id, context)
                        showDeleteDialog = false
                        onDismiss()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
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
