
package com.rines.esp32c3flasher

import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rines.esp32c3flasher.firmware.*
import com.rines.esp32c3flasher.usb.UsbDeviceInfo
import com.rines.esp32c3flasher.usb.UsbDeviceScanner
import com.rines.esp32c3flasher.usb.UsbPermissionManager
import com.rines.esp32c3flasher.usb.UsbDeviceReceiver
import com.rines.esp32c3flasher.update.GitHubUpdateChecker
import com.rines.esp32c3flasher.update.ReleaseInfo
import com.rines.esp32c3flasher.update.VersionComparator
import com.rines.esp32c3flasher.update.ApkInstaller
import com.rines.esp32c3flasher.ui.theme.RinesTheme
import com.rines.esp32c3flasher.ui.components.RinesSectionCard
import com.rines.esp32c3flasher.ui.components.RinesStatusPill
import com.rines.esp32c3flasher.ui.components.RinesPrimaryButton
import com.rines.esp32c3flasher.ui.components.RinesOutlineButton
import com.rines.esp32c3flasher.ui.components.FlashProgressCard
import com.rines.esp32c3flasher.ui.components.FlashLogsCard
import kotlinx.coroutines.launch
import java.io.File

private const val GITHUB_REPOSITORY =
    "https://github.com/rines1126-dotcom/RINES-ESP32-C3-FLASHER-STANDALONE"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var darkMode by remember {
                mutableStateOf(
                    AppPreferences.isDarkMode(this@MainActivity)
                )
            }

            var keepScreenAwake by remember {
                mutableStateOf(
                    AppPreferences.isKeepScreenAwake(this@MainActivity)
                )
            }

            SideEffect {
                if (keepScreenAwake) {
                    window.addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                } else {
                    window.clearFlags(
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                }
            }

            RinesTheme(darkTheme = darkMode) {

                RinesFlasherScreen(
                    darkMode = darkMode,
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        AppPreferences.setDarkMode(
                            this@MainActivity,
                            enabled
                        )
                    },
                    keepScreenAwake = keepScreenAwake,
                    onKeepScreenAwakeChange = { enabled ->
                        keepScreenAwake = enabled
                        AppPreferences.setKeepScreenAwake(
                            this@MainActivity,
                            enabled
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RinesFlasherScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    vm: FlashViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showSettings by remember {
        mutableStateOf(false)
    }

    var devices by remember {
        mutableStateOf(emptyList<UsbDeviceInfo>())
    }

    var eraseSize by remember {
        mutableStateOf("4194304")
    }

    var showEraseDialog by remember {
        mutableStateOf(false)
    }

    var exportMessage by remember {
        mutableStateOf("")
    }

    var pendingUsbDevice by remember {
        mutableStateOf<UsbDevice?>(null)
    }

    var promptedDeviceKey by remember {
        mutableStateOf<String?>(null)
    }

    var permissionManager by remember {
        mutableStateOf<UsbPermissionManager?>(null)
    }

    val installedVersion = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    var updateStatus by remember {
        mutableStateOf(
            "Tap Check for Updates to check for a new version."
        )
    }

    var isCheckingUpdate by remember {
        mutableStateOf(false)
    }

    var latestRelease by remember {
        mutableStateOf<ReleaseInfo?>(null)
    }

    var isDownloadingUpdate by remember {
        mutableStateOf(false)
    }

    var downloadProgress by remember {
        mutableIntStateOf(0)
    }

    var downloadedApk by remember {
        mutableStateOf<File?>(null)
    }

    // USB PERMISSION MANAGER
    DisposableEffect(context) {
        val manager = UsbPermissionManager(context) { device, granted ->
            if (granted && device != null) {
                vm.connect(device)
                exportMessage =
                    "USB permission granted. Connecting to device..."
            } else {
                exportMessage =
                    "USB permission denied. Allow USB access and try again."
            }
        }

        manager.register()
        permissionManager = manager

        onDispose {
            manager.unregister()
            permissionManager = null
        }
    }

    // INITIAL USB SCAN
    LaunchedEffect(Unit) {
        devices = UsbDeviceScanner.scan(context)
    }

    // USB ATTACH / DETACH
    DisposableEffect(context) {
        val receiver = UsbDeviceReceiver(
            onDeviceAttached = { device ->
                if (device != null) {
                    val deviceKey =
                        "${device.deviceName}_${device.vendorId}_${device.productId}"

                    if (promptedDeviceKey != deviceKey) {
                        promptedDeviceKey = deviceKey
                        pendingUsbDevice = device
                    }
                }
            },
            onDeviceChanged = {
                val scannedDevices =
                    UsbDeviceScanner.scan(context)

                devices = scannedDevices

                val currentKeys = scannedDevices.map {
                    "${it.device.deviceName}_${it.device.vendorId}_${it.device.productId}"
                }.toSet()

                if (
                    promptedDeviceKey != null &&
                    promptedDeviceKey !in currentKeys
                ) {
                    promptedDeviceKey = null
                    pendingUsbDevice = null
                }
            }
        )

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                receiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    // CHECK UPDATES
    fun checkForUpdates() {
        if (isCheckingUpdate || isDownloadingUpdate) return

        scope.launch {
            isCheckingUpdate = true
            updateStatus = "Checking GitHub Releases..."
            latestRelease = null
            downloadedApk = null
            downloadProgress = 0

            try {
                val release =
                    GitHubUpdateChecker.fetchLatestRelease()

                latestRelease = release

                val hasUpdate =
                    VersionComparator.isUpdateAvailable(
                        installedVersion,
                        release.tagName
                    )

                updateStatus = when {
                    release.tagName.isBlank() ->
                        "Unable to read latest version."

                    hasUpdate && release.apkUrl != null ->
                        "Update available: ${release.tagName}"

                    hasUpdate ->
                        "New version found, but no APK was attached."

                    else ->
                        "Your app is up to date. Installed: $installedVersion"
                }

            } catch (e: Exception) {
                updateStatus = "Update check failed: ${
                    e.message ?: "Unknown error"
                }"
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    // DOWNLOAD UPDATE
    fun downloadUpdate() {
        val apkUrl = latestRelease?.apkUrl

        if (apkUrl.isNullOrBlank()) {
            updateStatus = "No APK download link found."
            return
        }

        if (isDownloadingUpdate) return

        scope.launch {
            isDownloadingUpdate = true
            downloadProgress = 0
            downloadedApk = null
            updateStatus = "Downloading update..."

            try {
                val apkFile = ApkInstaller.downloadApk(
                    context = context,
                    apkUrl = apkUrl,
                    onProgress = { progress ->
                        downloadProgress = progress
                    }
                )

                downloadedApk = apkFile
                downloadProgress = 100
                updateStatus =
                    "APK downloaded successfully. Ready to install."

            } catch (e: Exception) {
                downloadedApk = null
                updateStatus = "Download failed: ${
                    e.message ?: "Unknown error"
                }"

            } finally {
                isDownloadingUpdate = false
            }
        }
    }

    // INSTALL UPDATE
    fun installUpdate() {
        val apkFile = downloadedApk

        if (apkFile == null || !apkFile.exists()) {
            updateStatus =
                "Downloaded APK not found. Download again."
            downloadedApk = null
            return
        }

        try {
            ApkInstaller.installApk(context, apkFile)
            updateStatus = "Opening Android installer..."

        } catch (e: Exception) {
            updateStatus = "Unable to open installer: ${
                e.message ?: "Unknown error"
            }"
        }
    }

    // OPEN GITHUB RELEASE OR REPOSITORY
    fun openGithubPage() {
        val url = latestRelease?.releaseUrl
            ?.takeIf { it.isNotBlank() }
            ?: GITHUB_REPOSITORY

        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        } catch (_: Exception) {
            exportMessage = "Unable to open GitHub."
        }
    }

    // SETTINGS SCREEN
    if (showSettings) {

        SettingsScreen(
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            keepScreenAwake = keepScreenAwake,
            onKeepScreenAwakeChange = onKeepScreenAwakeChange,
            installedVersion = installedVersion,
            latestVersion = latestRelease?.tagName?.takeIf {
                VersionComparator.isUpdateAvailable(
                    installedVersion,
                    it
                ) && latestRelease?.apkUrl != null
            },
            updateStatus = updateStatus,
            isChecking = isCheckingUpdate,
            onCheckUpdates = {
                checkForUpdates()
            },
            isDownloading = isDownloadingUpdate,
            downloadProgress = downloadProgress,
            apkDownloaded = downloadedApk != null,
            onDownloadUpdate = {
                downloadUpdate()
            },
            onInstallUpdate = {
                installUpdate()
            },
            onOpenGithubRelease = {
                openGithubPage()
            },
            onBack = {
                showSettings = false
            }
        )

    } else {

        // HOME SCREEN
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "ESP32-C3",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                "FLASHER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                showSettings = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // USB CONNECTION
                item {
                    RinesSectionCard(
                        title = "USB CONNECTION",
                        subtitle = "Connect your ESP32-C3 device"
                    ) {
                        RinesStatusPill(
                            text = if (state.connected) {
                                "CONNECTED"
                            } else {
                                "DISCONNECTED"
                            },
                            connected = state.connected
                        )

                        RinesPrimaryButton(
                            text = "SCAN USB DEVICES",
                            onClick = {
                                devices =
                                    UsbDeviceScanner.scan(context)
                            },
                            enabled = !state.isBusy
                        )

                        devices.forEach { info ->
                            RinesOutlineButton(
                                text = "${info.displayName} (${info.vendorIdHex}:${info.productIdHex})",
                                onClick = {
                                    permissionManager?.requestPermission(
                                        info.device
                                    )
                                },
                                enabled = !state.isBusy
                            )
                        }

                        if (state.connected) {
                            RinesOutlineButton(
                                text = "DISCONNECT DEVICE",
                                onClick = {
                                    vm.disconnect()
                                },
                                enabled = !state.isBusy
                            )
                        }
                    }
                }

                // FIRMWARE
                item {
                    RinesSectionCard(
                        title = "FIRMWARE",
                        subtitle = "Select and validate your firmware file"
                    ) {
                        FirmwarePicker(
                            onFirmwareSelected = {
                                vm.selectFirmware(it)
                            },
                            onError = {
                                exportMessage = it
                            }
                        )

                        Text(
                            state.firmwareName.ifEmpty {
                                "No firmware selected"
                            },
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (state.firmwareSize > 0) {
                            Text(
                                "Size: ${state.firmwareSize} bytes"
                            )
                        }

                        RinesStatusPill(
                            text = if (state.firmwareValid) {
                                "FIRMWARE VALID"
                            } else {
                                "NOT VALIDATED"
                            },
                            connected = state.firmwareValid
                        )
                    }
                }

                // FLASH OPERATIONS
                item {
                    RinesSectionCard(
                        title = "FLASH OPERATIONS",
                        subtitle = "ESP32-C3 programming controls"
                    ) {
                        RinesPrimaryButton(
                            text = "SYNC ESP32-C3",
                            onClick = {
                                vm.synchronize()
                            },
                            enabled = state.connected && !state.isBusy,
                            leadingIcon = Icons.Outlined.Usb
                        )

                        RinesPrimaryButton(
                            text = "FLASH FIRMWARE",
                            onClick = {
                                vm.flashFirmware()
                            },
                            enabled = state.connected &&
                                state.firmwareValid &&
                                !state.isBusy
                        )

                        RinesOutlineButton(
                            text = "ERASE FLASH",
                            onClick = {
                                showEraseDialog = true
                            },
                            enabled = state.connected && !state.isBusy
                        )

                        RinesOutlineButton(
                            text = "VERIFY FIRMWARE",
                            onClick = {
                                vm.verifyFirmware()
                            },
                            enabled = state.connected &&
                                state.firmwareValid &&
                                !state.isBusy
                        )
                    }
                }

                // FLASH PROGRESS
                item {
                    FlashProgressCard(
                        progress = state.progress,
                        status = state.message,
                        isBusy = state.isBusy
                    )
                }

                // FLASH LOGS
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlashLogsCard(
                            logs = state.logs.map { log ->
                                "[${log.timestamp}] [${log.level}] ${log.message}"
                            },
                            onClearLogs = {
                                vm.clearLogs()
                                exportMessage = ""
                            },
                            onExportLogs = {
                                val exported = FlashLogExporter.export(
                                    context,
                                    state.logs.joinToString("\n") {
                                        "[${it.timestamp}] [${it.level}] ${it.message}"
                                    }
                                )

                                exportMessage = if (exported) {
                                    "Log sharing opened."
                                } else {
                                    "Unable to export logs."
                                }
                            }
                        )

                        if (exportMessage.isNotEmpty()) {
                            Text(
                                text = exportMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // FOOTER
                item {
                    Text(
                        "RINES • Independent ESP32-C3 Flasher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // USB DEVICE DETECTED DIALOG
    pendingUsbDevice?.let { device ->

        AlertDialog(
            onDismissRequest = {
                pendingUsbDevice = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Usb,
                    contentDescription = "USB device"
                )
            },
            title = {
                Text("USB DEVICE DETECTED")
            },
            text = {
                Text(
                    "An ESP32-C3 USB device has been connected.\n\n" +
                        "Would you like to connect to this device?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUsbDevice = null
                        permissionManager?.requestPermission(device)
                    }
                ) {
                    Text("CONNECT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingUsbDevice = null
                    }
                ) {
                    Text("NOT NOW")
                }
            }
        )
    }

    // ERASE DIALOG
    if (showEraseDialog) {

        AlertDialog(
            onDismissRequest = {
                showEraseDialog = false
            },
            title = {
                Text("Erase Flash Region")
            },
            text = {
                Column {
                    Text(
                        "Enter erase size in bytes. Must be a multiple of 4096."
                    )

                    OutlinedTextField(
                        value = eraseSize,
                        onValueChange = {
                            eraseSize = it.filter(Char::isDigit)
                        },
                        label = {
                            Text("Size in bytes")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val size = eraseSize.toLongOrNull()

                        if (
                            size != null &&
                            size > 0 &&
                            size % 4096L == 0L
                        ) {
                            vm.eraseFlash(size)
                            showEraseDialog = false
                        } else {
                            exportMessage =
                                "Enter a valid multiple of 4096."
                        }
                    }
                ) {
                    Text("Erase")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEraseDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
