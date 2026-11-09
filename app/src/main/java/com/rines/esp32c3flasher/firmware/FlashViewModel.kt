
package com.rines.esp32c3flasher.firmware

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FlashUiState(
    val connected: Boolean = false,
    val firmwareName: String = "",
    val firmwareSize: Long = 0,
    val firmwareValid: Boolean = false,
    val status: FlashOperationStatus = FlashOperationStatus.IDLE,
    val progress: Int = 0,
    val message: String = "Ready",
    val isBusy: Boolean = false,
    val logs: List<FlashLogEntry> = emptyList()
)

class FlashViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val controller = Esp32C3FlashController(application)
    private val firmwareManager = FirmwareManager()
    private val stateManager = FlashOperationStateManager()
    private val logManager = FlashLogManager()

    private val _uiState = MutableStateFlow(FlashUiState())
    val uiState: StateFlow<FlashUiState> = _uiState.asStateFlow()

    private fun refreshState() {
        val operation = stateManager.getState()
        val firmware = firmwareManager.getSelectedFirmware()

        _uiState.value = _uiState.value.copy(
            connected = controller.isConnected,
            firmwareName = firmware?.fileName ?: "",
            firmwareSize = firmware?.data?.size?.toLong() ?: 0L,
            firmwareValid = firmwareManager.hasValidFirmware(),
            status = operation.status,
            progress = operation.progress,
            message = operation.message,
            isBusy = operation.isBusy,
            logs = logManager.getEntries()
        )
    }

    fun selectFirmware(firmware: SelectedFirmware) {
        val result = firmwareManager.loadFirmware(firmware)

        if (result.success) {
            logManager.success(result.message)
        } else {
            logManager.error(result.message)
        }

        refreshState()
    }

    // USB CONNECTION
    fun connect(device: UsbDevice) {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            stateManager.connecting()
            logManager.info("Connecting to USB device...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.connect(device)
            }

            if (result.success) {
                logManager.success(result.message)

                // USB connection is not a completed flash operation.
                stateManager.ready(
                    "ESP32-C3 USB device connected. Ready to SYNC."
                )

            } else {
                logManager.error(result.message)
                stateManager.error(result.message)
            }

            refreshState()
        }
    }

    // SYNC
    fun synchronize() {
        if (_uiState.value.isBusy) return

        if (!controller.isConnected) {
            val message =
                "USB DEVICE NOT CONNECTED! Please connect your ESP32-C3 via USB and try again."

            logManager.warning(message)
            stateManager.error(message)
            refreshState()
            return
        }

        viewModelScope.launch {
            stateManager.syncing()
            logManager.info("SYNCING ESP32-C3...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.synchronize()
            }

            if (result.success) {
                val message =
                    "ESP32-C3 DETECTED! The device is connected and ready for flashing."

                logManager.success(message)
                stateManager.success(message)

            } else {
                val isBootloaderSyncFailure =
                    result.message.contains(
                        "No valid SYNC response",
                        ignoreCase = true
                    ) ||
                    result.message.contains(
                        "SYNC failed after",
                        ignoreCase = true
                    )

                val message = if (isBootloaderSyncFailure) {
                    "ESP32-C3 NOT IN DOWNLOAD MODE! Please press and hold the BOOT button, then connect the USB cable. Release the BOOT button once connected, then try SYNC again."
                } else {
                    "SYNC FAILED! ${result.message}"
                }

                logManager.error(message)
                stateManager.error(message)
            }

            refreshState()
        }
    }

    // FLASH FIRMWARE
    fun flashFirmware() {
        if (
            !controller.isConnected ||
            !firmwareManager.hasValidFirmware() ||
            _uiState.value.isBusy
        ) return

        val firmware = firmwareManager.getSelectedFirmware()
            ?: return

        viewModelScope.launch {
            stateManager.flashing(
                0,
                "Starting firmware flash..."
            )

            logManager.info("Starting firmware flash...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.flashFirmware(firmware.data) { progress, message ->
                    stateManager.flashing(progress, message)
                    logManager.info(message)

                    viewModelScope.launch {
                        refreshState()
                    }
                }
            }

            if (result.success) {
                stateManager.success(result.message)
                logManager.success(result.message)
            } else {
                stateManager.error(result.message)
                logManager.error(result.message)
            }

            refreshState()
        }
    }

    // VERIFY FIRMWARE
    fun verifyFirmware() {
        if (
            !controller.isConnected ||
            !firmwareManager.hasValidFirmware() ||
            _uiState.value.isBusy
        ) return

        val firmware = firmwareManager.getSelectedFirmware()
            ?: return

        viewModelScope.launch {
            stateManager.verifying()
            logManager.info("Starting firmware verification...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.verifyFirmware(firmware.data)
            }

            if (result.success) {
                stateManager.success(result.message)
                logManager.success(result.message)
            } else {
                stateManager.error(result.message)
                logManager.error(result.message)
            }

            refreshState()
        }
    }

    // ERASE FLASH
    fun eraseFlash(sizeBytes: Long) {
        if (!controller.isConnected || _uiState.value.isBusy) return

        viewModelScope.launch {
            stateManager.erasing()
            logManager.warning("Starting flash erase...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.eraseFlash(sizeBytes)
            }

            if (result.success) {
                logManager.success(result.message)
                stateManager.success(result.message)
            } else {
                logManager.error(result.message)
                stateManager.error(result.message)
            }

            refreshState()
        }
    }

    // DISCONNECT
    fun disconnect() {
        if (_uiState.value.isBusy) return

        viewModelScope.launch(Dispatchers.IO) {
            controller.disconnect()

            withContext(Dispatchers.Main) {
                logManager.info("USB device disconnected.")
                stateManager.reset()
                refreshState()
            }
        }
    }

    // CLEAR LOGS
    fun clearLogs() {
        logManager.clear()
        refreshState()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }
}
