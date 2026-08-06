package com.sentinelle.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentinelle.app.ui.NUMBER_LABEL_CATEGORIES
import com.sentinelle.app.ui.categoryLabel
import com.sentinelle.app.ui.viewmodel.report.ReportUiState
import com.sentinelle.app.ui.viewmodel.report.ReportViewModel
import com.sentinelle.app.ui.viewmodel.report.ReportViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview
@Composable
fun ReportScreen(
    // Pre-filled when arriving from a number that was actually blocked,
    // rather than making the user retype it from memory.
    initialNumber: String? = null,
    viewModel: ReportViewModel =
        viewModel(
            factory =
                ReportViewModelFactory(
                    LocalContext.current,
                ),
        ),
) {
    val scrollState = rememberScrollState()
    LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialNumber) {
        if (!initialNumber.isNullOrBlank()) {
            viewModel.updatePhoneNumber(initialNumber)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Signaler",
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
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Main report card
                ReportCard(
                    uiState = uiState,
                    onPhoneNumberChange = viewModel::updatePhoneNumber,
                    onIsGoodChange = viewModel::setIsGood,
                    onCategoryChange = viewModel::setCategory,
                    onSubmit = viewModel::submitPhoneNumber,
                    focusManager = focusManager,
                )
            }
        }
    }

    // Alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAlert,
            title = { Text(uiState.alertType.title) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                Button(
                    onClick = viewModel::dismissAlert,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCard(
    uiState: ReportUiState,
    onPhoneNumberChange: (String) -> Unit,
    onIsGoodChange: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Signaler un numéro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // Phone number input
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Numéro de téléphone") },
                placeholder = { Text("+33612345678") },
                enabled = !uiState.isLoading,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Report type choice buttons
            ReportChoiceButton(
                title = "Spam",
                description = "Signaler comme indésirable.",
                icon = Icons.Rounded.Warning,
                isSelected = !uiState.isGood,
                color = MaterialTheme.colorScheme.error,
                onClick = { onIsGoodChange(false) },
            )

            ReportChoiceButton(
                title = "Légitime",
                description = "Marquer comme légitime.",
                icon = Icons.Rounded.CheckCircle,
                isSelected = uiState.isGood,
                color = MaterialTheme.colorScheme.tertiary,
                onClick = { onIsGoodChange(true) },
            )

            if (!uiState.isGood) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Catégorie (enregistrée uniquement sur cet appareil)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NUMBER_LABEL_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = uiState.category == category,
                                onClick = { onCategoryChange(category) },
                                label = { Text(categoryLabel(category)) },
                            )
                        }
                    }
                }
            }

            // Submit button
            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading && uiState.phoneNumber.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoyer", fontWeight = FontWeight.Bold)
                }
            }

            // Footer description
            Text(
                text = "Signaler un numéro contribue à améliorer les listes de blocage et à rendre l'application plus efficace.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportChoiceButton(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        color.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.background
                    },
            ),
        border =
            BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color =
                    if (isSelected) {
                        color
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
