
package com.rines.esp32c3flasher.firmware

import android.content.Context
import android.hardware.usb.UsbDevice
import com.rines.esp32c3flasher.protocol.Esp32C3FlashInit
import com.rines.esp32c3flasher.protocol.Esp32C3RomClient
import com.rines.esp32c3flasher.protocol.Esp32C3Sync
import com.rines.esp32c3flasher.usb.UsbSerialTransport

data class FlashControllerResult(
    val success: Boolean,
    val message: String
)

class Esp32C3FlashController(context: Context) {

    private val transport = UsbSerialTransport(context)
    private val romClient = Esp32C3RomClient(transport)
    private val sync = Esp32C3Sync(transport)
    private val flashInit = Esp32C3FlashInit(romClient)

    private val writer = Esp32C3FlashWriter(romClient)
    private val eraser = Esp32C3FlashEraser(romClient)
    private val verifier = Esp32C3FlashVerifier(romClient)

    private var flashReady = false

    val isConnected: Boolean
        get() = transport.isConnected

    val lastError: String
        get() = transport.lastError

    fun connect(device: UsbDevice): FlashControllerResult {
        flashReady = false

        val connected = transport.connect(device)

        return FlashControllerResult(
            connected,
            if (connected) {
                "USB device connected."
            } else {
                "USB connection failed: ${transport.lastError}"
            }
        )
    }

    /**
     * Synchronizes with the ESP32-C3 ROM bootloader
     * and attaches the SPI flash interface.
     */
    fun synchronize(): FlashControllerResult {

        flashReady = false

        if (!transport.isConnected) {
            return FlashControllerResult(
                false,
                "ESP32-C3 is not connected."
            )
        }

        val syncResult = sync.synchronize()

        if (!syncResult.success) {
            return FlashControllerResult(
                false,
                syncResult.message
            )
        }

        val attachResult = flashInit.attachFlash()

        if (!attachResult.success) {
            return FlashControllerResult(
                false,
                "SYNC successful, but SPI flash attachment failed: " +
                    attachResult.message
            )
        }

        flashReady = true

        return FlashControllerResult(
            true,
            "ESP32-C3 synchronized and SPI flash attached successfully."
        )
    }

    fun flashFirmware(
        firmware: ByteArray,
        progress: (Int, String) -> Unit = { _, _ -> }
    ): FlashWriteResult {

        if (!transport.isConnected) {
            return FlashWriteResult(
                false,
                "ESP32-C3 is not connected."
            )
        }

        if (!flashReady) {
            return FlashWriteResult(
                false,
                "Please synchronize with ESP32-C3 first."
            )
        }

        return writer.writeFirmware(
            firmware = firmware,
            flashAddress = 0x000000L,
            reboot = false,
            progressCallback = progress
        )
    }

    fun eraseFlash(
        sizeBytes: Long
    ): FlashEraseResult {

        if (!transport.isConnected) {
            return FlashEraseResult(
                false,
                0,
                "ESP32-C3 is not connected."
            )
        }

        if (!flashReady) {
            return FlashEraseResult(
                false,
                0,
                "Please synchronize with ESP32-C3 first."
            )
        }

        return eraser.eraseRegion(
            flashAddress = 0x000000L,
            sizeBytes = sizeBytes
        )
    }

    fun verifyFirmware(
        firmware: ByteArray
    ): FlashVerifyResult {

        if (!transport.isConnected) {
            return FlashVerifyResult(
                false,
                "",
                "",
                "ESP32-C3 is not connected."
            )
        }

        if (!flashReady) {
            return FlashVerifyResult(
                false,
                "",
                "",
                "Please synchronize with ESP32-C3 first."
            )
        }

        return verifier.verifyFirmware(
            firmware = firmware,
            flashAddress = 0x000000L
        )
    }

    fun disconnect() {
        flashReady = false
        transport.disconnect()
    }
}
