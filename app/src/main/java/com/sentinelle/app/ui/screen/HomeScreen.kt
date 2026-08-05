package com.sentinelle.app.ui.screen

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.AddModerator
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.ui.dialog.CallScreeningFailedDialog
import com.sentinelle.app.ui.sheet.CallSettingsSheet
import com.sentinelle.app.ui.sheet.InfoSheet
import com.sentinelle.app.ui.sheet.SmsSettingsSheet
import com.sentinelle.app.util.PermissionUtils
import com.sentinelle.app.util.PermissionUtils.isNotificationPermissionGranted
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.worker.ListUpdateWorker
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HomeScreen(
    onOpenDashboard: () -> Unit = {},
    onOpenLookup: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val totalBlockedNumbers by AppDatabase
        .getInstance(context)
        .patternListDao()
        .getTotalCoveredFlow()
        .collectAsState(initial = 0L)
    var isCallScreeningEnabled by remember { mutableStateOf(false) }
    var isBackgroundRestricted by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showCallSettingsSheet by remember { mutableStateOf(false) }
    var showSmsSettingsSheet by remember { mutableStateOf(false) }
    val defaultSmsAppName =
        remember {
            val packageName = Telephony.Sms.getDefaultSmsPackage(context)
            packageName?.let {
                try {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
                } catch (_: Exception) {
                    it
                }
            }
        }

    val isCallFilteringEnabled by PreferencesManager
        .getCallFilteringEnabledFlow(context)
        .collectAsState(initial = true)
    val isBlockAnonymousCalls by PreferencesManager
        .getBlockAnonymousCallsFlow(context)
        .collectAsState(initial = false)
    val isAllowOnlyContacts by PreferencesManager
        .getAllowOnlyContactsFlow(context)
        .collectAsState(initial = false)
    val isBlockedCallNotification by PreferencesManager
        .getBlockedCallNotificationFlow(context)
        .collectAsState(initial = false)
    val isSmsBlockingEnabled by PreferencesManager
        .getSmsBlockingEnabledFlow(context)
        .collectAsState(initial = false)
    val isBlockedSmsNotification by PreferencesManager
        .getBlockedSmsNotificationFlow(context)
        .collectAsState(initial = false)
    val isHeuristicDetectionEnabled by PreferencesManager
        .getCallHistoryTrackingEnabledFlow(context)
        .collectAsState(initial = false)
    val isHeuristicShadowModeEnabled by PreferencesManager
        .getHeuristicShadowModeEnabledFlow(context)
        .collectAsState(initial = false)
    val isCallerIdBubbleEnabled by PreferencesManager
        .getCallerIdBubbleEnabledFlow(context)
        .collectAsState(initial = false)
    var isNotificationListenerEnabled by remember { mutableStateOf(false) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(true) }
    val lastListUpdate by PreferencesManager
        .getLastListUpdateFlow(context)
        .collectAsState(initial = 0L)
    val coroutineScope = rememberCoroutineScope()
    var showNotificationPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showCallScreeningFailedDialog by remember { mutableStateOf(false) }
    var hasAttemptedEnable by remember { mutableStateOf(false) }

    val blockedCallsCountFlow by AppDatabase
        .getInstance(context)
        .blockedEventDao()
        .getCountByChannelFlow(PatternListEntity.CHANNEL_PHONE)
        .collectAsState(initial = 0)

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                coroutineScope.launch {
                    PreferencesManager.setBlockedCallNotification(context, isGranted)
                }
            },
        )
    val workInfos by WorkManager
        .getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(ListUpdateWorker.WORK_NAME)
        .observeAsState(emptyList())
    val launchWorkInfos by WorkManager
        .getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(ListUpdateWorker.WORK_NAME_LAUNCH)
        .observeAsState(emptyList())
    val allWorkInfos = workInfos + launchWorkInfos
    val isWorkScheduled =
        allWorkInfos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    val isWorkRunning =
        allWorkInfos.any { it.state == WorkInfo.State.RUNNING }
    val isWorkFailed =
        !isWorkRunning &&
            allWorkInfos.any { it.state == WorkInfo.State.FAILED } &&
            totalBlockedNumbers == 0L
    val isBackgroundUpdateActive = isWorkScheduled && !isBackgroundRestricted

    val callScreeningRoleLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            isCallScreeningEnabled = PermissionUtils.isCallScreeningEnabled(context)
            if (hasAttemptedEnable && !isCallScreeningEnabled) {
                showCallScreeningFailedDialog = true
            }
        }

    // Update permissions status on app resume or initial load
    fun updatePermissionsStatus() {
        isCallScreeningEnabled = PermissionUtils.isCallScreeningEnabled(context)
        isBackgroundRestricted =
            (context.getSystemService(ActivityManager::class.java))
                .isBackgroundRestricted
        isNotificationListenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)
        isIgnoringBatteryOptimizations = PermissionUtils.isIgnoringBatteryOptimizations(context)
    }

    LaunchedEffect(Unit) {
        updatePermissionsStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    updatePermissionsStatus()
                    if (hasAttemptedEnable && !isCallScreeningEnabled) {
                        showCallScreeningFailedDialog = true
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showNotificationPermissionRationaleDialog) {
        NotificationPermissionRationaleDialog(
            onDismiss = {
                coroutineScope.launch {
                    PreferencesManager.setBlockedCallNotification(context, false)
                }
            },
            onConfirm = {
                coroutineScope.launch {
                    PreferencesManager.setBlockedCallNotification(context, true)
                }
                PermissionUtils.openAppNotificationsSettings(context)
                showNotificationPermissionRationaleDialog = false
            },
        )
    }

    if (showCallScreeningFailedDialog) {
        CallScreeningFailedDialog(
            onDismiss = { showCallScreeningFailedDialog = false },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Sentinelle", fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = onOpenLookup) {
                        Icon(Icons.Rounded.Search, contentDescription = "Rechercher un numéro")
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
                    .verticalScroll(scrollState),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CallScreeningPermissionCard(
                    isEnabled = isCallScreeningEnabled,
                    callFilteringEnabled = isCallFilteringEnabled,
                    totalBlockedNumbers = totalBlockedNumbers,
                    isListUpdateInProgress = isWorkRunning,
                    isListUpdateFailed = isWorkFailed,
                    blockedCallsCount = blockedCallsCountFlow,
                    onSettingsClick = {
                        hasAttemptedEnable = true
                        val intent = PermissionUtils.createCallScreeningRoleIntent(context)
                        if (intent != null) {
                            callScreeningRoleLauncher.launch(intent)
                        } else {
                            PermissionUtils.openCallScreeningSettings(context)
                        }
                    },
                    onInfoClick = { showInfoSheet = true },
                )

                if (isCallScreeningEnabled) {
                    ProtectionCard(
                        onCallClick = { showCallSettingsSheet = true },
                        onSmsClick = { showSmsSettingsSheet = true },
                        isNotificationListenerEnabled = isNotificationListenerEnabled,
                    )

                    if (!isIgnoringBatteryOptimizations) {
                        BatteryOptimizationCard(
                            onFixClick = { PermissionUtils.openBatteryOptimizationSettings(context) },
                        )
                    }
                }
            }
        }
    }

    if (showInfoSheet) {
        InfoSheet(
            totalBlockedNumbers = totalBlockedNumbers,
            isBackgroundUpdateActive = isBackgroundUpdateActive,
            lastListUpdate = lastListUpdate,
            onOpenDashboard = {
                showInfoSheet = false
                onOpenDashboard()
            },
            onDismiss = { showInfoSheet = false },
        )
    }

    if (showSmsSettingsSheet) {
        SmsSettingsSheet(
            smsBlockingEnabled = isSmsBlockingEnabled,
            onSmsBlockingEnabledChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setSmsBlockingEnabled(context, newValue)
                }
            },
            blockedSmsNotification = isBlockedSmsNotification,
            onBlockedSmsNotificationChange = { newValue ->
                if (newValue) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val activity = context as? Activity
                        when {
                            isNotificationPermissionGranted(context) -> {
                                coroutineScope.launch {
                                    PreferencesManager.setBlockedSmsNotification(context, true)
                                }
                            }

                            activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true -> {
                                showNotificationPermissionRationaleDialog = true
                            }

                            else -> {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            PreferencesManager.setBlockedSmsNotification(context, true)
                        }
                    }
                } else {
                    coroutineScope.launch {
                        PreferencesManager.setBlockedSmsNotification(context, false)
                    }
                }
            },
            isNotificationListenerEnabled = isNotificationListenerEnabled,
            onOpenNotificationListenerSettings = {
                PermissionUtils.openNotificationListenerSettings(context)
            },
            onDismiss = { showSmsSettingsSheet = false },
            defaultSmsAppName = defaultSmsAppName,
        )
    }

    if (showCallSettingsSheet) {
        val isNotificationPermissionGrantedState =
            remember {
                isNotificationPermissionGranted(context)
            }
        CallSettingsSheet(
            callFilteringEnabled = isCallFilteringEnabled,
            onCallFilteringEnabledChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setCallFilteringEnabled(context, newValue)
                }
            },
            allowOnlyContacts = isAllowOnlyContacts,
            onAllowOnlyContactsChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setAllowOnlyContacts(context, newValue)
                }
            },
            blockAnonymousCalls = isBlockAnonymousCalls,
            onBlockAnonymousCallsChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setBlockAnonymousCalls(context, newValue)
                }
            },
            blockedCallNotification = isBlockedCallNotification,
            isNotificationPermissionGranted = isNotificationPermissionGrantedState,
            onBlockedCallNotificationChange = { isEnabled ->
                if (isEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val activity = context as? Activity ?: return@CallSettingsSheet
                        when {
                            isNotificationPermissionGranted(context) -> {
                                coroutineScope.launch {
                                    PreferencesManager.setBlockedCallNotification(
                                        context,
                                        true,
                                    )
                                }
                            }

                            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                                showNotificationPermissionRationaleDialog = true
                            }

                            else -> {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            PreferencesManager.setBlockedCallNotification(context, true)
                        }
                    }
                } else {
                    coroutineScope.launch {
                        PreferencesManager.setBlockedCallNotification(context, false)
                    }
                }
            },
            heuristicDetectionEnabled = isHeuristicDetectionEnabled,
            onHeuristicDetectionEnabledChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setCallHistoryTrackingEnabled(context, newValue)
                }
            },
            heuristicShadowModeEnabled = isHeuristicShadowModeEnabled,
            onHeuristicShadowModeEnabledChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setHeuristicShadowModeEnabled(context, newValue)
                }
            },
            callerIdBubbleEnabled = isCallerIdBubbleEnabled,
            onCallerIdBubbleEnabledChange = { newValue ->
                coroutineScope.launch {
                    PreferencesManager.setCallerIdBubbleEnabled(context, newValue)
                }
                if (newValue && !PermissionUtils.canDrawOverlays(context)) {
                    PermissionUtils.openOverlayPermissionSettings(context)
                }
            },
            onDismiss = { showCallSettingsSheet = false },
        )
    }
}

@Composable
fun CallScreeningPermissionCard(
    isEnabled: Boolean,
    callFilteringEnabled: Boolean,
    totalBlockedNumbers: Long = 0L,
    isListUpdateInProgress: Boolean = false,
    isListUpdateFailed: Boolean = false,
    blockedCallsCount: Int = 0,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit = {},
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

    if (isEnabled) {
        CallScreeningEnabledCard(
            callFilteringEnabled = callFilteringEnabled,
            totalBlockedNumbers = totalBlockedNumbers,
            isListUpdateInProgress = isListUpdateInProgress,
            isListUpdateFailed = isListUpdateFailed,
            blockedCallsCount = blockedCallsCount,
            numberFormat = numberFormat,
            onInfoClick = onInfoClick,
        )
    } else {
        CallScreeningDisabledCard(
            onSettingsClick = onSettingsClick,
        )
    }
}

@Composable
fun CallScreeningEnabledCard(
    callFilteringEnabled: Boolean,
    totalBlockedNumbers: Long,
    isListUpdateInProgress: Boolean,
    isListUpdateFailed: Boolean,
    blockedCallsCount: Int,
    numberFormat: NumberFormat,
    onInfoClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (callFilteringEnabled) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (callFilteringEnabled) Icons.Rounded.VerifiedUser else Icons.Rounded.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (callFilteringEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = if (callFilteringEnabled) "Bloqueur actif et à jour" else "Le bloqueur n'est pas activé",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (callFilteringEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (callFilteringEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Numéros dans la base de données",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text =
                                when {
                                    isListUpdateInProgress -> "Chargement..."
                                    isListUpdateFailed -> "Erreur"
                                    else -> numberFormat.format(totalBlockedNumbers)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PhoneMissed,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (callFilteringEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Appels bloqués",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text =
                                if (blockedCallsCount == 0) {
                                    "Aucun"
                                } else {
                                    "$blockedCallsCount appel${if (blockedCallsCount > 1) "s" else ""} bloqué${if (blockedCallsCount > 1) "s" else ""}"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }

                Button(
                    onClick = onInfoClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (callFilteringEnabled) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            contentColor =
                                if (callFilteringEnabled) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onError
                                },
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "En savoir plus",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun CallScreeningDisabledCard(onSettingsClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Le bloqueur n'est pas activé",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )

                Text(
                    text =
                        "Activez le bloqueur pour bloquer les appels indésirables " +
                            "en cliquant sur le bouton ci-dessous et en choisissant " +
                            "Sentinelle dans la liste des applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Button(
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddModerator,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Activer le bloqueur",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun ProtectionCard(
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit,
    isNotificationListenerEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column {
            Text(
                text = "Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCallClick() }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appels",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "Activez ou désactivez le filtre d'appels indésirables, masqués " +
                                "ou autorisez uniquement vos contacts. Gérez les notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSmsClick() }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Message,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SMS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!isNotificationListenerEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Activer la protection",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            modifier =
                                Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(16.dp),
                                    ).padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Activez ou désactivez le masquage des notifications des SMS indésirables.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard(onFixClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryAlert,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fiabilité en arrière-plan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        "Sur certains appareils (Xiaomi/MIUI notamment), le système peut arrêter " +
                            "Sentinelle en arrière-plan et l'empêcher de filtrer vos appels et SMS. " +
                            "Désactivez l'optimisation de la batterie pour plus de fiabilité.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onFixClick) {
                    Text("Corriger", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission requise") },
        text = { Text("L'application a besoin de la permission pour envoyer des notifications.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            ) {
                Text("Ouvrir les paramètres", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
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
