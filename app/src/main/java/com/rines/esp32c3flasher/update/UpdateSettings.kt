
package com.rines.esp32c3flasher.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun UpdateSettings(
    installedVersion: String,
    updateStatus: String,
    isChecking: Boolean,
    onCheckUpdates: () -> Unit,
    latestVersion: String? = null,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    apkDownloaded: Boolean = false,
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SOFTWARE UPDATE",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "RINES ESP32-C3 FLASHER",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Installed Version: $installedVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = updateStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onCheckUpdates,
                enabled = !isChecking && !isDownloading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Checking...")
                } else {
                    Text("CHECK FOR UPDATES")
                }
            }

            if (latestVersion != null) {
                Text(
                    text = "Latest Version: $latestVersion",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00E5A8)
                )

                if (isDownloading) {
                    Text(
                        text = "Downloading: $downloadProgress%",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LinearProgressIndicator(
                        progress = {
                            (downloadProgress / 100f).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (!apkDownloaded && !isDownloading) {
                    Button(
                        onClick = onDownloadUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DOWNLOAD UPDATE")
                    }
                }

                if (apkDownloaded) {
                    OutlinedButton(
                        onClick = onInstallUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("INSTALL UPDATE")
                    }
                }
            }

            Text(
                text = "Updates are provided through GitHub Releases.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
