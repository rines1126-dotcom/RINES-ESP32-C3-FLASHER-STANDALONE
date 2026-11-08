
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
                stateManager.success(result.message)
            } else {
                logManager.error(result.message)
                stateManager.error(result.message)
            }

            refreshState()
        }
    }

    fun synchronize() {
        if (!controller.isConnected || _uiState.value.isBusy) return

        viewModelScope.launch {
            stateManager.syncing()
            logManager.info("Starting ESP32-C3 SYNC...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.synchronize()
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

    fun flashFirmware() {
        if (
            !controller.isConnected ||
            !firmwareManager.hasValidFirmware() ||
            _uiState.value.isBusy
        ) return

        val firmware = firmwareManager.getSelectedFirmware()
            ?: return

        viewModelScope.launch {
            stateManager.flashing(0, "Starting firmware flash...")
            logManager.info("Starting firmware flash...")
            refreshState()

            val result = withContext(Dispatchers.IO) {
                controller.flashFirmware(firmware.data) { progress, message ->
                    stateManager.flashing(progress, message)
                    logManager.info(message)

                    // Publish progress safely on the main thread.
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
                stateManager.success(result.message)
                logManager.success(result.message)
            } else {
                stateManager.error(result.message)
                logManager.error(result.message)
            }

            refreshState()
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            controller.disconnect()

            withContext(Dispatchers.Main) {
                logManager.info("USB device disconnected.")
                stateManager.reset()
                refreshState()
            }
        }
    }

    fun clearLogs() {
        logManager.clear()
        refreshState()
    }

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }
}
