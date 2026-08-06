package com.sentinelle.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.ui.NUMBER_LABEL_CATEGORIES
import com.sentinelle.app.ui.categoryIcon
import com.sentinelle.app.ui.categoryLabel
import com.sentinelle.app.ui.viewmodel.lookup.LookupResult
import com.sentinelle.app.ui.viewmodel.lookup.LookupViewModel
import com.sentinelle.app.ui.viewmodel.lookup.LookupViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LookupScreen(
    initialNumber: String? = null,
    onReportNumber: (String) -> Unit = {},
    onOpenMyLabels: () -> Unit = {},
    viewModel: LookupViewModel =
        viewModel(factory = LookupViewModelFactory(LocalContext.current)),
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrBlank()) {
            viewModel.updateQuery(initialNumber)
            viewModel.search()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Recherche", fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = onOpenMyLabels) {
                        Icon(Icons.Rounded.Bookmarks, contentDescription = "Mes labels")
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
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                label = { Text("Numéro de téléphone") },
                placeholder = { Text("+33612345678") },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            viewModel.search()
                        },
                    ),
                trailingIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        viewModel.search()
                    }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Rechercher")
                    }
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.notFound) {
                Text(
                    text = "Numéro invalide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            uiState.result?.let { result ->
                LookupResultCard(
                    result = result,
                    onReportNumber = { onReportNumber(result.displayNumber) },
                    onSaveLabel = viewModel::saveLabel,
                    onDeleteLabel = viewModel::deleteLabel,
                    onAllowNumber = viewModel::allowNumber,
                    onRemoveFromAllowList = viewModel::removeFromAllowList,
                )
            }
        }
    }
}

@Composable
private fun LookupResultCard(
    result: LookupResult,
    onReportNumber: () -> Unit,
    onSaveLabel: (String, String?) -> Unit,
    onDeleteLabel: () -> Unit,
    onAllowNumber: () -> Unit,
    onRemoveFromAllowList: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = result.displayNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            StatusBadges(result)

            if (result.blockedCount > 0) {
                Text(
                    text = "Bloqué ${result.blockedCount} fois par Sentinelle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LabelSection(
                label = result.label,
                onSaveLabel = onSaveLabel,
                onDeleteLabel = onDeleteLabel,
            )

            AllowListSection(
                isAllowed = result.allowListItemId != null,
                onAllowNumber = onAllowNumber,
                onRemoveFromAllowList = onRemoveFromAllowList,
            )

            // Reporting from a number you actually saw blocked, rather than
            // retyping one from memory into an empty form — this is why
            // "Signaler" no longer needs a tab of its own.
            Button(
                onClick = onReportNumber,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Signaler ce numéro")
            }
        }
    }
}

// The escape hatch for a false positive noticed after the fact — the
// "Ce n'est pas un spam" notification action is gone once the notification
// is dismissed, and before this the only recourse was retyping the number
// by hand in Listes.
@Composable
private fun AllowListSection(
    isAllowed: Boolean,
    onAllowNumber: () -> Unit,
    onRemoveFromAllowList: () -> Unit,
) {
    if (isAllowed) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Ce numéro est dans vos numéros autorisés — il ne sera jamais bloqué.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRemoveFromAllowList,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retirer des numéros autorisés")
            }
        }
    } else {
        Button(
            onClick = onAllowNumber,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ne jamais bloquer ce numéro")
        }
    }
}

// FlowRow, not Row: badge text is driven by list names, which can be long
// ("Préfixes France ARCEP de démarchage"). A Row can't wrap, so it squeezes
// whichever badge doesn't fit down to its minimum width — which for a pill
// containing text means one character per line, a tall vertical ribbon.
// FlowRow moves the badge to the next line instead.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBadges(result: LookupResult) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        result.patternMatch?.let { match ->
            val isBlocked = match.type == PatternListEntity.TYPE_BLOCK
            Badge(
                text = if (isBlocked) "Bloqué (${match.listName})" else "Autorisé (${match.listName})",
                icon = if (isBlocked) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
        }
        if (result.isArcepNpv) {
            Badge(
                text = "Démarchage officiel (ARCEP)",
                icon = Icons.Rounded.Shield,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun Badge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier =
            Modifier
                .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.height(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelSection(
    label: com.sentinelle.app.data.NumberLabelEntity?,
    onSaveLabel: (String, String?) -> Unit,
    onDeleteLabel: () -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var selectedCategory by remember(label) { mutableStateOf(label?.category ?: NUMBER_LABEL_CATEGORIES.first()) }
    var note by remember(label) { mutableStateOf(label?.note ?: "") }

    if (label != null && !isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(categoryIcon(label.category), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.height(18.dp))
                    Text(categoryLabel(label.category), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                if (!label.note.isNullOrBlank()) {
                    Text(label.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Modifier le label")
                }
                IconButton(onClick = onDeleteLabel) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Supprimer le label", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (label == null) "Ajouter un label" else "Modifier le label",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NUMBER_LABEL_CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(categoryLabel(category)) },
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optionnel)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSaveLabel(selectedCategory, note)
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
                if (label != null) {
                    Button(
                        onClick = { isEditing = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Text("Annuler", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
