
package com.rines.esp32c3flasher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rines.esp32c3flasher.firmware.*
import com.rines.esp32c3flasher.usb.UsbDeviceInfo
import com.rines.esp32c3flasher.usb.UsbDeviceScanner
import com.rines.esp32c3flasher.usb.UsbPermissionManager

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

    var devices by remember {
        mutableStateOf(emptyList<UsbDeviceInfo>())
    }

    var selectedDevice by remember {
        mutableStateOf<android.hardware.usb.UsbDevice?>(null)
    }

    var eraseSize by remember {
        mutableStateOf("4194304")
    }

    var showEraseDialog by remember {
        mutableStateOf(false)
    }

    var permissionManager by remember {
        mutableStateOf<UsbPermissionManager?>(null)
    }

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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "USB CONNECTION",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            if (state.connected) {
                                "Connected"
                            } else {
                                "Disconnected"
                            },
                            color = if (state.connected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )

                        Button(
                            onClick = {
                                devices = UsbDeviceScanner.scan(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isBusy
                        ) {
                            Text("Scan USB Devices")
                        }

                        if (devices.isEmpty()) {
                            Text("No USB devices listed.")
                        }

                        devices.forEach { info ->
                            OutlinedButton(
                                onClick = {
                                    selectedDevice = info.device
                                    permissionManager?.requestPermission(
                                        info.device
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isBusy
                            ) {
                                Text(
                                    "${info.displayName} (${info.vendorIdHex}:${info.productIdHex})"
                                )
                            }
                        }

                        if (state.connected) {
                            OutlinedButton(
                                onClick = {
                                    vm.disconnect()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isBusy
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                // Error is displayed in operation logs
                            }
                        )

                        Text(
                            if (state.firmwareName.isNotEmpty()) {
                                state.firmwareName
                            } else {
                                "No firmware selected"
                            }
                        )

                        if (state.firmwareSize > 0) {
                            Text(
                                "Size: ${state.firmwareSize} bytes"
                            )
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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "FLASH OPERATIONS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = { vm.synchronize() },
                            enabled = state.connected && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SYNC ESP32-C3")
                        }

                        Button(
                            onClick = { vm.flashFirmware() },
                            enabled = state.connected &&
                                state.firmwareValid &&
                                !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FLASH FIRMWARE")
                        }

                        OutlinedButton(
                            onClick = { showEraseDialog = true },
                            enabled = state.connected && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ERASE FLASH")
                        }

                        OutlinedButton(
                            onClick = { vm.verifyFirmware() },
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

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "FLASH PROGRESS",
                            style = MaterialTheme.typography.titleMedium
                        )

                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("${state.progress}%")
                        Text(state.message)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                onClick = { vm.clearLogs() }
                            ) {
                                Text("Clear Logs")
                            }
                        }

                        state.logs.forEach { log ->
                            Text(
                                "[${log.timestamp}] [${log.level}] ${log.message}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "RINES • Independent ESP32-C3 Flasher",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

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
                    Text("Enter erase size in bytes. Must be a multiple of 4096.")

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

                        if (size != null && size > 0) {
                            vm.eraseFlash(size)
                            showEraseDialog = false
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
