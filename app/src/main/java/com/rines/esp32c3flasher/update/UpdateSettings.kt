
package com.rines.esp32c3flasher.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpdateSettings(
    installedVersion: String,
    updateStatus: String,
    isChecking: Boolean,
    onCheckUpdates: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.titleMedium
            )

            Text("App Version: $installedVersion")

            Text(
                text = "Software Updates",
                style = MaterialTheme.typography.titleSmall
            )

            Text(updateStatus)

            Button(
                onClick = onCheckUpdates,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator()
                } else {
                    Text("Check for Updates")
                }
            }
        }
    }
}
