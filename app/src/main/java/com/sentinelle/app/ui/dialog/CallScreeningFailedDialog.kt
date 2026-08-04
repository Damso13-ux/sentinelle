package com.sentinelle.app.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CallScreeningFailedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Error, contentDescription = null) },
        title = { Text("Impossible d'activer le bloqueur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pour activer le bloqueur manuellement :")
                Text("1. Ouvrez les Réglages.")
                Text("2. Applications → Applications par défaut.")
                Text("3. Appels d'identification et de blocage d'appels.")
                Text("4. Sélectionnez Sentinelle.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Si le paramètre n'est pas accessible, essayez de redémarrer votre appareil.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Revenez ensuite dans Sentinelle.")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            ) {
                Text("C'est fait", fontWeight = FontWeight.Bold)
            }
        },
    )
}
