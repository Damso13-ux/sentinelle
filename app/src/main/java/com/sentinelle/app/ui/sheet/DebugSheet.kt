package com.sentinelle.app.ui.sheet

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sentinelle.app.BuildConfig
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.util.PatternManager
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.worker.ListUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSheet(onDismiss: () -> Unit) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var deviceId by remember { mutableStateOf("") }
    var countryCodes by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var proUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceId = PreferencesManager.getDeviceId(context)
        countryCodes = PreferencesManager.getCountryCodes(context)
        apiKey = PreferencesManager.getApiKey(context)
        proUnlocked = PreferencesManager.isProUnlocked(context)
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
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.Red,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Debug",
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
                        ).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "UUID Device",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Country Codes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = countryCodes.ifBlank { "FR (default)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (apiKey != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Managed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (apiKey != null) "Yes" else "No",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DebugButton(
                title = "Force update",
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    coroutineScope.launch {
                        val request = OneTimeWorkRequestBuilder<ListUpdateWorker>().build()
                        val wm = WorkManager.getInstance(context)
                        val workId = request.id
                        wm.enqueueUniqueWork(
                            ListUpdateWorker.WORK_NAME_LAUNCH,
                            ExistingWorkPolicy.REPLACE,
                            request,
                        )
                        Toast.makeText(context, "Mise à jour lancée.", Toast.LENGTH_SHORT).show()
                        wm.getWorkInfoByIdFlow(workId).collect { info ->
                            val state = info?.state ?: return@collect
                            val message =
                                when (state) {
                                    WorkInfo.State.SUCCEEDED -> "✅ Liste mise à jour."
                                    WorkInfo.State.FAILED -> "❌ Échec mise à jour."
                                    else -> return@collect
                                }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebugButton(
                title = "Clear database",
                backgroundColor = Color.Red,
                contentColor = Color.White,
                onClick = {
                    coroutineScope.launch {
                        clearDatabase(context)
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebugButton(
                title = "Reset preferences",
                backgroundColor = Color.Red,
                contentColor = Color.White,
                onClick = {
                    coroutineScope.launch {
                        resetPreferences(context)
                    }
                },
            )

            // Debug-only: simulates the Pro entitlement locally so gated UI
            // can be built and tested before a real "sentinelle_pro"
            // product exists in Play Console (merchant profile pending
            // verification as of this writing). Gated on BuildConfig.DEBUG
            // specifically — not just on DebugSheet being reachable — since
            // this sheet opens in release builds too via the triple-tap on
            // the Settings footer. Never present in a release build.
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))

                DebugButton(
                    title = if (proUnlocked) "Pro (debug) : activé — désactiver" else "Pro (debug) : désactivé — activer",
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    onClick = {
                        coroutineScope.launch {
                            val newValue = !proUnlocked
                            PreferencesManager.setProUnlocked(context, newValue)
                            proUnlocked = newValue
                            Toast
                                .makeText(
                                    context,
                                    if (newValue) "✅ Pro simulé activé (debug uniquement)." else "Pro simulé désactivé.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DebugButton(
    title: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
            ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private suspend fun clearDatabase(context: android.content.Context) {
    withContext(Dispatchers.IO) {
        try {
            AppDatabase.getInstance(context).clearAllTables()
            PatternManager.clearCache()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ Database cleared.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Clear failed: ${e.message}.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun resetPreferences(context: android.content.Context) {
    withContext(Dispatchers.IO) {
        try {
            PreferencesManager.clear(context)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ Preferences reset.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Reset failed: ${e.message}.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
