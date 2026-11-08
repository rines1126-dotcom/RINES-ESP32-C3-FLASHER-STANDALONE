
package com.rines.esp32c3flasher

import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rines.esp32c3flasher.firmware.*
import com.rines.esp32c3flasher.usb.UsbDeviceInfo
import com.rines.esp32c3flasher.usb.UsbDeviceScanner
import com.rines.esp32c3flasher.usb.UsbPermissionManager
import com.rines.esp32c3flasher.usb.UsbDeviceReceiver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RinesFlasherScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RinesFlasherScreen(
    vm: FlashViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by vm.uiState.collectAsState()

    var devices by remember { mutableStateOf(emptyList<UsbDeviceInfo>()) }
    var eraseSize by remember { mutableStateOf("4194304") }
    var showEraseDialog by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }

    var permissionManager by remember {
        mutableStateOf<UsbPermissionManager?>(null)
    }

    // USB permission manager
    DisposableEffect(context) {
        val manager = UsbPermissionManager(context) { device, granted ->
            if (granted && device != null) {
                vm.connect(device)
            }
        }

        manager.register()
        permissionManager = manager

        onDispose {
            manager.unregister()
            permissionManager = null
        }
    }

    // Initial USB scan
    LaunchedEffect(Unit) {
        devices = UsbDeviceScanner.scan(context)
    }

    // Automatic USB attach/detach detection
    DisposableEffect(context) {
        val receiver = UsbDeviceReceiver {
            devices = UsbDeviceScanner.scan(context)
        }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("RINES ESP32-C3 FLASHER")
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
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "USB CONNECTION",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            if (state.connected) "Connected" else "Disconnected"
                        )

                        Button(
                            onClick = {
                                devices = UsbDeviceScanner.scan(context)
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan USB Devices")
                        }

                        devices.forEach { info ->
                            OutlinedButton(
                                onClick = {
                                    permissionManager?.requestPermission(info.device)
                                },
                                enabled = !state.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${info.displayName} " +
                                        "(${info.vendorIdHex}:${info.productIdHex})"
                                )
                            }
                        }

                        if (state.connected) {
                            OutlinedButton(
                                onClick = {
                                    vm.disconnect()
                                },
                                enabled = !state.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            // FIRMWARE
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "FIRMWARE",
                            style = MaterialTheme.typography.titleMedium
                        )

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
                            }
                        )

                        if (state.firmwareSize > 0) {
                            Text("Size: ${state.firmwareSize} bytes")
                        }

                        Text(
                            if (state.firmwareValid) {
                                "Firmware validated"
                            } else {
                                "Firmware not validated"
                            }
                        )
                    }
                }
            }

            // FLASH OPERATIONS
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "FLASH OPERATIONS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = {
                                vm.synchronize()
                            },
                            enabled = state.connected && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SYNC ESP32-C3")
                        }

                        Button(
                            onClick = {
                                vm.flashFirmware()
                            },
                            enabled = state.connected &&
                                state.firmwareValid &&
                                !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FLASH FIRMWARE")
                        }

                        OutlinedButton(
                            onClick = {
                                showEraseDialog = true
                            },
                            enabled = state.connected && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ERASE FLASH")
                        }

                        OutlinedButton(
                            onClick = {
                                vm.verifyFirmware()
                            },
                            enabled = state.connected &&
                                state.firmwareValid &&
                                !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("VERIFY FIRMWARE")
                        }
                    }
                }
            }

            // FLASH PROGRESS
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "FLASH PROGRESS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        LinearProgressIndicator(
                            progress = {
                                state.progress / 100f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("${state.progress}%")
                        Text(state.message)
                    }
                }
            }

            // FLASH LOGS
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "FLASH LOGS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    vm.clearLogs()
                                }
                            ) {
                                Text("Clear Logs")
                            }

                            TextButton(
                                onClick = {
                                    val exported = FlashLogExporter.export(
                                        context,
                                        state.logs.joinToString("\n") {
                                            "[${it.timestamp}] " +
                                                "[${it.level}] ${it.message}"
                                        }
                                    )

                                    exportMessage = if (exported) {
                                        "Log sharing opened."
                                    } else {
                                        "Unable to export logs."
                                    }
                                }
                            ) {
                                Text("Export Logs")
                            }
                        }

                        if (exportMessage.isNotEmpty()) {
                            Text(
                                exportMessage,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        state.logs.forEach { log ->
                            Text(
                                "[${log.timestamp}] " +
                                    "[${log.level}] ${log.message}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // APP FOOTER
            item {
                Text(
                    "RINES • Independent ESP32-C3 Flasher",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    // ERASE FLASH DIALOG
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
                        "Enter erase size in bytes. " +
                            "Must be a multiple of 4096."
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

                        if (size != null &&
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
