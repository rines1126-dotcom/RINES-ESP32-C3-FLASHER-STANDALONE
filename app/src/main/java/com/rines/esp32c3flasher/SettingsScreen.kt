
package com.rines.esp32c3flasher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rines.esp32c3flasher.update.UpdateSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    installedVersion: String,
    latestVersion: String?,
    updateStatus: String,
    isChecking: Boolean,
    onCheckUpdates: () -> Unit,
    isDownloading: Boolean,
    downloadProgress: Int,
    apkDownloaded: Boolean,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenGithubRelease: () -> Unit,
    onBack: () -> Unit,
    onOpenGithubRepository: () -> Unit = onOpenGithubRelease
) {
    var showPrivacyPolicy by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("SETTINGS")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // APPEARANCE
            SettingsSection(
                title = "APPEARANCE",
                icon = {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SettingsSwitchRow(
                    title = "Dark Mode",
                    description = "Use the dark color theme",
                    checked = darkMode,
                    onCheckedChange = onDarkModeChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsInfoRow(
                    title = "Current Theme",
                    description = if (darkMode) {
                        "Dark Mode"
                    } else {
                        "Light Mode"
                    }
                )
            }

            // DISPLAY
            SettingsSection(
                title = "DISPLAY",
                icon = {
                    Icon(
                        Icons.Outlined.ScreenLockPortrait,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SettingsSwitchRow(
                    title = "Keep Screen Awake",
                    description = "Prevent the screen from turning off while using the app",
                    checked = keepScreenAwake,
                    onCheckedChange = onKeepScreenAwakeChange
                )
            }

            // SOFTWARE UPDATE
            SettingsSection(
                title = "SOFTWARE UPDATE",
                icon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                UpdateSettings(
                    installedVersion = installedVersion,
                    updateStatus = updateStatus,
                    isChecking = isChecking,
                    onCheckUpdates = onCheckUpdates,
                    latestVersion = latestVersion,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    apkDownloaded = apkDownloaded,
                    onDownloadUpdate = onDownloadUpdate,
                    onInstallUpdate = onInstallUpdate
                )

                if (latestVersion != null) {
                    TextButton(
                        onClick = onOpenGithubRelease,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OPEN GITHUB RELEASE")
                    }
                }
            }

            // VERSION INFORMATION
            SettingsSection(
                title = "VERSION INFORMATION",
                icon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SettingsInfoRow(
                    title = "Current Version",
                    description = installedVersion
                )

                if (latestVersion != null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    SettingsInfoRow(
                        title = "Latest Version",
                        description = latestVersion
                    )
                }
            }

            // PRIVACY POLICY
            SettingsSection(
                title = "PRIVACY",
                icon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SettingsInfoRow(
                    title = "Privacy Policy",
                    description = "USB access and software update information"
                )

                TextButton(
                    onClick = {
                        showPrivacyPolicy = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VIEW PRIVACY POLICY")
                }
            }

            // DEVELOPER INFORMATION
            SettingsSection(
                title = "DEVELOPER INFORMATION",
                icon = {
                    Icon(
                        Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                SettingsInfoRow(
                    title = "Developer",
                    description = "RINES"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsInfoRow(
                    title = "Application",
                    description = "RINES ESP32-C3 FLASHER"
                )

                TextButton(
                    onClick = onOpenGithubRepository,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VISIT GITHUB REPOSITORY")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "RINES • Independent ESP32-C3 Flasher",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // PRIVACY POLICY DIALOG
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = {
                showPrivacyPolicy = false
            },
            title = {
                Text("Privacy Policy")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("RINES ESP32-C3 FLASHER")

                    Text(
                        "USB Connection\n" +
                            "The app uses USB access to communicate " +
                            "with a connected ESP32-C3 device for " +
                            "firmware operations."
                    )

                    Text(
                        "Software Updates\n" +
                            "The update feature connects to GitHub Releases " +
                            "to check for new versions and download APK files."
                    )

                    Text(
                        "Permissions\n" +
                            "USB access and other Android permissions are " +
                            "used for the app's features."
                    )

                    Text(
                        "Contact\n" +
                            "For questions about this application, visit " +
                            "the RINES GitHub repository."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPrivacyPolicy = false
                    }
                ) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon()

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
