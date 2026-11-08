package com.rines.esp32c3flasher.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                enabled = !isChecking && !isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator()
                } else {
                    Text("Check for Updates")
                }
            }

            if (latestVersion != null) {
                Text("Latest Version: $latestVersion")

                if (isDownloading) {
                    Text("Downloading update: $downloadProgress%")

                    LinearProgressIndicator(
                        progress = {
                            downloadProgress / 100f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!apkDownloaded && !isDownloading) {
                    Button(
                        onClick = onDownloadUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download Update")
                    }
                }

                if (apkDownloaded) {
                    OutlinedButton(
                        onClick = onInstallUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install Update")
                    }
                }
            }
        }
    }
}
