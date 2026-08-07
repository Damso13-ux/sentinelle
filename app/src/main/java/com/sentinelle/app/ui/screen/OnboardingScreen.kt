package com.sentinelle.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sentinelle.app.ui.theme.AppTheme
import com.sentinelle.app.util.PermissionUtils
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.launch

/**
 * First-launch walkthrough.
 *
 * Sentinelle is inert until the user grants the call-screening role, and
 * SMS filtering additionally needs notification access. Android never
 * prompts for either on its own, and neither is a runtime permission
 * dialog — both are buried in system settings. Someone who installs the
 * app and taps nothing gets an app that silently does nothing, which
 * reads as broken rather than as unconfigured.
 *
 * So this is deliberately *not* a swipeable carousel of marketing slides:
 * each step is an action, shows its own live state, and can be skipped.
 * Nothing here grants anything by itself — it only routes to the system
 * screens where the user decides.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    var callScreeningEnabled by remember { mutableStateOf(PermissionUtils.isCallScreeningEnabled(context)) }
    var notificationListenerEnabled by remember { mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context)) }

    // Both activations happen in system settings, so the app is backgrounded
    // and comes back — re-read on resume rather than trusting the value
    // captured when the step was first shown.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    callScreeningEnabled = PermissionUtils.isCallScreeningEnabled(context)
                    notificationListenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val finish: () -> Unit = {
        coroutineScope.launch { PreferencesManager.setOnboardingCompleted(context, true) }
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepIndicator(current = step, total = TOTAL_STEPS)

            Spacer(Modifier.height(32.dp))

            when (step) {
                0 ->
                    OnboardingStep(
                        icon = Icons.Rounded.Shield,
                        title = "Bienvenue dans Sentinelle",
                        body =
                            "Sentinelle bloque les appels et les SMS indésirables : démarchage, " +
                                "arnaques, numéros signalés.\n\n" +
                                "Deux réglages rapides suffisent à la rendre opérationnelle. " +
                                "Sans eux, l'application ne peut rien bloquer.",
                    ) {
                        PrivacyNote()
                    }

                1 ->
                    OnboardingStep(
                        icon = Icons.Rounded.PhoneDisabled,
                        title = "Bloquer les appels",
                        body =
                            "Android demande de désigner Sentinelle comme application de " +
                                "filtrage des appels. C'est ce qui lui permet de raccrocher " +
                                "automatiquement sur un numéro indésirable.\n\n" +
                                "Sentinelle ne remplace pas votre application Téléphone et " +
                                "n'accède ni à vos contacts, ni à votre journal d'appels.",
                    ) {
                        ActivationRow(
                            enabled = callScreeningEnabled,
                            enabledLabel = "Filtrage des appels actif",
                            actionLabel = "Activer le filtrage des appels",
                            onActivate = { PermissionUtils.openCallScreeningSettings(context) },
                        )
                    }

                2 ->
                    OnboardingStep(
                        icon = Icons.AutoMirrored.Rounded.Message,
                        title = "Masquer les SMS indésirables",
                        body =
                            "Android ne propose aucun moyen officiel de filtrer les SMS pour " +
                                "une application tierce. Sentinelle passe donc par l'accès aux " +
                                "notifications, pour masquer celles qui viennent d'un expéditeur " +
                                "indésirable.\n\n" +
                                "Seules les notifications de votre application de messagerie sont " +
                                "lues, et uniquement pour décider de les masquer ou non. Cette " +
                                "étape est facultative : le blocage des appels fonctionne sans.",
                    ) {
                        ActivationRow(
                            enabled = notificationListenerEnabled,
                            enabledLabel = "Masquage des SMS actif",
                            actionLabel = "Activer le masquage des SMS",
                            onActivate = { PermissionUtils.openNotificationListenerSettings(context) },
                        )
                    }
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { if (step < TOTAL_STEPS - 1) step++ else finish() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = if (step < TOTAL_STEPS - 1) "Continuer" else "Terminer",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            // Always available. A walkthrough that traps someone who just
            // wants to look around is worse than one they skipped — the
            // same two actions stay one tap away on the home screen.
            if (step < TOTAL_STEPS - 1) {
                TextButton(onClick = finish) {
                    Text("Passer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private const val TOTAL_STEPS = 3

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.height(24.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(28.dp))

    content()
}

@Composable
private fun ActivationRow(
    enabled: Boolean,
    enabledLabel: String,
    actionLabel: String,
    onActivate: () -> Unit,
) {
    if (enabled) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = enabledLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    } else {
        OutlinedButton(
            onClick = onActivate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = actionLabel,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text =
                "Tout se passe sur votre téléphone. Vos appels, vos SMS et vos " +
                    "contacts ne sont jamais envoyés en ligne.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepIndicator(
    current: Int,
    total: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .height(8.dp)
                        .width(if (index == current) 24.dp else 8.dp)
                        .background(
                            color =
                                if (index <= current) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            shape = CircleShape,
                        ),
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    AppTheme {
        OnboardingScreen(onFinished = {})
    }
}
