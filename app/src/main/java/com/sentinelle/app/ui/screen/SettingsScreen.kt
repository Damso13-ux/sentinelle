package com.sentinelle.app.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.android.billingclient.api.ProductDetails
import com.sentinelle.app.BuildConfig
import com.sentinelle.app.billing.BillingManager
import com.sentinelle.app.service.ListService
import com.sentinelle.app.ui.sheet.DebugSheet
import com.sentinelle.app.ui.theme.ThemeVariant
import com.sentinelle.app.util.PermissionUtils
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.widget.SentinelleWidgetProvider
import com.sentinelle.app.worker.ListUpdateWorker
import kotlinx.coroutines.launch

sealed class SettingsItem {
    data class Action(
        val title: String,
        val subtitle: String? = null,
        val icon: ImageVector,
        val onClick: () -> Unit,
    ) : SettingsItem()

    data class Switch(
        val title: String,
        val subtitle: String?,
        val icon: ImageVector,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val enabled: Boolean = true,
    ) : SettingsItem()
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
) {
    Column {
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier = Modifier.padding(16.dp),
        )

        items.forEach { item ->
            when (item) {
                is SettingsItem.Action -> {
                    SettingsActionItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        onClick = item.onClick,
                    )
                }

                is SettingsItem.Switch -> {
                    SettingsSwitchItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        checked = item.checked,
                        onCheckedChange = item.onCheckedChange,
                        enabled = item.enabled,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (enabled) 1f else 0.38f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SettingsScreen(
    onOpenFilters: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    onResetApp: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val coroutineScope = rememberCoroutineScope()

    var showReinstallDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDebugSheet by remember { mutableStateOf(false) }
    var bisouTapCount by remember { mutableIntStateOf(0) }

    val proUnlocked by PreferencesManager.getProUnlockedFlow(context).collectAsState(initial = false)
    val selectedThemeVariant by
        PreferencesManager.getStoredThemeVariantFlow(context).collectAsState(initial = ThemeVariant.INDIGO)
    val billingManager = remember { BillingManager(context) }
    var proProductDetails by remember { mutableStateOf<ProductDetails?>(null) }
    // Debug-only shortcut: 3 taps on the Pro row flips the local debug
    // unlock, same effect as the toggle in DebugSheet but without leaving
    // this screen. Each of the first two taps still behaves like a normal
    // tap (attempts the real purchase flow / shows "already unlocked") —
    // only the 3rd one is intercepted. Same BuildConfig.DEBUG gate as
    // DebugSheet's own toggle: never available in a release build.
    var proDebugTapCount by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        // Re-checks Play's purchase records on every visit to this screen,
        // not just after a purchase — a refund or a restore on a new
        // device needs to be reflected here too. Also fetches the price so
        // the purchase item can show it instead of a bare "Débloquer" —
        // stays null (price-less fallback label) until the product exists
        // in Play Console.
        billingManager.startConnection {
            billingManager.queryProDetails { proProductDetails = it }
        }
        onDispose { billingManager.endConnection() }
    }
    val proPrice = proProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Réglages",
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
                    .verticalScroll(scrollState),
        ) {
            // Took over the two tabs that left the bottom bar. "Mes filtres"
            // rather than "Listes": what the user manages is which numbers
            // get through, not the data structure holding them.
            SettingsSection(
                title = "Mes données",
                items =
                    listOf(
                        SettingsItem.Action(
                            title = "Mes filtres",
                            subtitle = "Numéros et mots-clés que vous avez autorisés ou bloqués.",
                            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            onClick = onOpenFilters,
                        ),
                        SettingsItem.Action(
                            title = "Signaler un numéro",
                            subtitle = "Aider à améliorer les listes partagées.",
                            icon = Icons.Rounded.Campaign,
                            onClick = onOpenReport,
                        ),
                    ),
            )

            // Configuration Section
            SettingsSection(
                title = "Configuration",
                items =
                    listOf(
                        SettingsItem.Action(
                            title = "Activer ou désactiver Sentinelle comme application par défaut pour le blocage d'appels",
                            icon = Icons.Rounded.Settings,
                            onClick = { PermissionUtils.openCallScreeningSettings(context) },
                        ),
                        SettingsItem.Action(
                            title = "Activer ou désactiver l'accès aux notifications pour le masquage des SMS",
                            icon = Icons.Rounded.Notifications,
                            onClick = { PermissionUtils.openNotificationListenerSettings(context) },
                        ),
                        SettingsItem.Action(
                            title = "Ajouter le widget à l'écran d'accueil",
                            icon = Icons.Rounded.Widgets,
                            onClick = {
                                val pinned = SentinelleWidgetProvider.requestPin(context)
                                if (!pinned) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Ajoute-le manuellement : appui long sur l'écran d'accueil → Widgets → Sentinelle",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                            },
                        ),
                        SettingsItem.Action(
                            title = "Réinstaller les listes de blocage",
                            icon = Icons.Rounded.Refresh,
                            onClick = { showReinstallDialog = true },
                        ),
                        SettingsItem.Action(
                            title = "Réinitialiser l'application",
                            icon = Icons.Rounded.DeleteForever,
                            onClick = { showResetDialog = true },
                        ),
                    ),
            )

            // Sentinelle Pro Section
            SettingsSection(
                title = "Sentinelle Pro",
                items =
                    listOf(
                        SettingsItem.Action(
                            title =
                                when {
                                    proUnlocked -> "Sentinelle Pro actif"
                                    proPrice != null -> "Débloquer Sentinelle Pro — $proPrice"
                                    else -> "Débloquer Sentinelle Pro"
                                },
                            subtitle =
                                if (proUnlocked) {
                                    "Merci pour ton soutien ! Export des stats, historique étendu, thèmes et réglages avancés sont débloqués."
                                } else {
                                    "Export des stats, historique étendu, thèmes additionnels, réglages avancés de l'heuristique."
                                },
                            icon = Icons.Rounded.WorkspacePremium,
                            onClick =
                                onProClick@{
                                    if (BuildConfig.DEBUG) {
                                        proDebugTapCount++
                                        if (proDebugTapCount >= 3) {
                                            proDebugTapCount = 0
                                            val newValue = !proUnlocked
                                            coroutineScope.launch { PreferencesManager.setProDebugOverride(context, newValue) }
                                            Toast
                                                .makeText(
                                                    context,
                                                    if (newValue) {
                                                        "✅ Pro simulé activé (debug uniquement)."
                                                    } else {
                                                        "Pro simulé désactivé (debug)."
                                                    },
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            return@onProClick
                                        }
                                    }
                                    if (proUnlocked) {
                                        Toast.makeText(context, "Déjà débloqué — merci !", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            billingManager.launchPurchaseFlow(activity) { error ->
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                        ),
                    ) + if (!proUnlocked) {
                        listOf(
                            SettingsItem.Action(
                                title = "Restaurer mes achats",
                                subtitle = "Déjà acheté sur un autre appareil, ou après une réinstallation ?",
                                icon = Icons.Rounded.Restore,
                                onClick = {
                                    billingManager.restorePurchases { found ->
                                        val message =
                                            if (found) {
                                                "✅ Achat retrouvé — Sentinelle Pro est débloqué."
                                            } else {
                                                "Aucun achat Sentinelle Pro trouvé sur ce compte Google Play."
                                            }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                            ),
                        )
                    } else {
                        emptyList()
                    } + listOf(
                        SettingsItem.Action(
                            title = "Faire un don",
                            subtitle = "Soutenir le développement sans passer par un achat intégré.",
                            icon = Icons.Rounded.Favorite,
                            onClick = { openDonate(context) },
                        ),
                    ),
            )

            // Apparence Section — Ocean/Prune are Pro, Indigo always free.
            Column {
                Text(
                    text = "Apparence",
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        ),
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeVariant.entries.forEach { variant ->
                        val locked = variant != ThemeVariant.INDIGO && !proUnlocked
                        FilterChip(
                            selected = selectedThemeVariant == variant,
                            onClick = {
                                if (locked) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Thème réservé à Sentinelle Pro.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else {
                                    coroutineScope.launch { PreferencesManager.setThemeVariant(context, variant) }
                                }
                            },
                            label = { Text(variant.displayName) },
                            leadingIcon = if (locked) {
                                { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                        )
                    }
                }
            }

            // Links Section
            SettingsSection(
                title = "Liens",
                items =
                    listOf(
                        SettingsItem.Action(
                            title = "Projet d'origine (Saracroche)",
                            subtitle = "Sentinelle est un fork de Saracroche, sous licence GPLv3.",
                            icon = Icons.Rounded.Code,
                            onClick = { openGit(context) },
                        ),
                        SettingsItem.Action(
                            title = "Auteur du projet d'origine (Mastodon @cbouvat)",
                            icon = Icons.Rounded.ChatBubble,
                            onClick = { openMastodon(context) },
                        ),
                        SettingsItem.Action(
                            title = "Politique de confidentialité",
                            icon = Icons.Rounded.Shield,
                            onClick = { openPrivacyPolicy(context) },
                        ),
                    ),
            )

            // Footer
            Text(
                text = "Version ${
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        0,
                    ).versionName
                }\n\nBisou 😘",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            bisouTapCount++
                            if (bisouTapCount >= 3) {
                                showDebugSheet = true
                                bisouTapCount = 0
                            }
                        },
            )
        }

        if (showReinstallDialog) {
            AlertDialog(
                onDismissRequest = { showReinstallDialog = false },
                title = { Text("Réinstaller les listes ?", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    Text(
                        "Les listes seront supprimées puis retéléchargées depuis le serveur.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReinstallDialog = false
                            val request =
                                OneTimeWorkRequestBuilder<ListUpdateWorker>()
                                    .setInputData(workDataOf(ListUpdateWorker.KEY_REINSTALL to true))
                                    .build()
                            WorkManager.getInstance(context).enqueueUniqueWork(
                                ListUpdateWorker.WORK_NAME_LAUNCH,
                                ExistingWorkPolicy.REPLACE,
                                request,
                            )
                            Toast
                                .makeText(
                                    context,
                                    "Réinstallation lancée.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Text("Réinstaller", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showReinstallDialog = false },
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

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Réinitialiser l'application ?") },
                text = {
                    Text(
                        "Toutes les données seront supprimées. L'application repartira comme au premier lancement.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            coroutineScope.launch {
                                ListService.resetApp(context)
                                onResetApp()
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Text("Réinitialiser", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showResetDialog = false },
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

        if (showDebugSheet) {
            DebugSheet(onDismiss = { showDebugSheet = false })
        }
    }
}

// Links functions
private fun openGit(context: Context) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW, "https://codeberg.org/cbouvat/saracroche-android".toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}

private fun openMastodon(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "https://mastodon.social/@cbouvat".toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}

private fun openPrivacyPolicy(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "https://damso13-ux.github.io/sentinelle/".toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}

private fun openDonate(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "https://paypal.me/mycookies".toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently
    }
}
