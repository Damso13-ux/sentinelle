package com.sentinelle.app.ui.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.PatternService
import com.sentinelle.app.service.PatternValidation
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddElementSheet(
    listId: Long,
    channel: String,
    onDismiss: () -> Unit,
    onPatternAdded: () -> Unit,
) {
    val isKeyword = channel == PatternListEntity.CHANNEL_SMS
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
    var patternInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasWildcards = !isKeyword && patternInput.contains("#")
    val formatValidation =
        if (isKeyword) PatternService.validateKeyword(patternInput) else PatternService.validatePattern(patternInput)
    val showRange = hasWildcards && formatValidation is PatternValidation.Valid

    fun validateAndCheck() {
        val validation = if (isKeyword) PatternService.validateKeyword(patternInput) else PatternService.validatePattern(patternInput)
        if (validation is PatternValidation.Invalid) {
            errorMessage = validation.message
            return
        }

        if (isKeyword) {
            errorMessage = PatternService.detectDuplicateKeyword(patternInput, context)
            return
        }

        val duplicate = PatternService.detectDuplicate(patternInput, context)
        if (duplicate != null) {
            errorMessage = duplicate
            return
        }

        val overlaps = PatternService.detectOverlaps(patternInput, context)
        if (overlaps.isNotEmpty()) {
            errorMessage = overlaps.first().message
            return
        }

        errorMessage = null
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nouvel élément",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isKeyword) "Mot-clé" else "Numéro ou préfixe",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = patternInput,
                onValueChange = {
                    patternInput = it
                    errorMessage = null
                },
                placeholder = { Text(if (isKeyword) "gagné, cliquez ici, urgent..." else "+33612345678 ou +33612345####") },
                keyboardOptions =
                    if (isKeyword) {
                        KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    } else {
                        KeyboardOptions(keyboardType = KeyboardType.Uri)
                    },
                singleLine = true,
                isError = errorMessage != null,
                supportingText =
                    if (errorMessage != null) {
                        { Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else if (isKeyword) {
                        {
                            Text(
                                text =
                                    "Le SMS sera masqué s'il contient ce mot ou cette phrase " +
                                        "(insensible à la casse). Exemple : « gagné ».",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        {
                            Text(
                                text =
                                    "Format international requis. " +
                                        "Exemple : +33612345678 (numéro) ou +33612345#### (préfixe).",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
            )

            if (showRange) {
                val normalized = patternInput.removePrefix("+")
                val (first, last) = PatternService.getNumberRange(normalized)
                val count = PatternService.calculateCoveredNumbers(normalized)
                Text(
                    text = "Plage: $first → $last (${numberFormat.format(count)} numéros).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nom",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = { Text(if (isKeyword) "Spam loterie" else "Spam Marketing") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                supportingText = {
                    Text(
                        text = "Un nom pour identifier cet élément, par exemple « Spam Marketing ».",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    validateAndCheck()
                    if (errorMessage == null) {
                        if (isKeyword) {
                            PatternService.addUserKeyword(
                                patternInput,
                                nameInput,
                                listId,
                                context,
                            )
                        } else {
                            PatternService.addUserPattern(
                                patternInput,
                                nameInput,
                                listId,
                                context,
                            )
                        }
                        onPatternAdded()
                        onDismiss()
                    }
                },
                enabled = patternInput.isNotEmpty() && nameInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
