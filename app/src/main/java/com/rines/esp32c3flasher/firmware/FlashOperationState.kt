
package com.rines.esp32c3flasher.firmware

enum class FlashOperationStatus {
    IDLE,
    CONNECTING,
    SYNCING,
    FLASHING,
    ERASING,
    VERIFYING,
    SUCCESS,
    ERROR
}

data class FlashOperationState(
    val status: FlashOperationStatus = FlashOperationStatus.IDLE,
    val progress: Int = 0,
    val message: String = "Ready",
    val isBusy: Boolean = false
)

class FlashOperationStateManager {

    private var currentState = FlashOperationState()

    fun getState(): FlashOperationState = currentState

    fun connecting() {
        update(FlashOperationStatus.CONNECTING, 0, "Connecting to ESP32-C3...")
    }

    fun syncing() {
        update(FlashOperationStatus.SYNCING, 0, "Synchronizing with ESP32-C3...")
    }

    fun flashing(progress: Int, message: String) {
        update(
            FlashOperationStatus.FLASHING,
            progress.coerceIn(0, 100),
            message
        )
    }

    fun erasing() {
        update(FlashOperationStatus.ERASING, 0, "Erasing flash...")
    }

    fun verifying() {
        update(FlashOperationStatus.VERIFYING, 0, "Verifying firmware...")
    }

    fun success(message: String) {
        update(FlashOperationStatus.SUCCESS, 100, message)
    }

    fun error(message: String) {
        update(FlashOperationStatus.ERROR, 0, message)
    }

    fun reset() {
        currentState = FlashOperationState()
    }

    private fun update(
        status: FlashOperationStatus,
        progress: Int,
        message: String
    ) {
        currentState = FlashOperationState(
            status = status,
            progress = progress,
            message = message,
            isBusy = status in setOf(
                FlashOperationStatus.CONNECTING,
                FlashOperationStatus.SYNCING,
                FlashOperationStatus.FLASHING,
                FlashOperationStatus.ERASING,
                FlashOperationStatus.VERIFYING
            )
        )
    }
}
