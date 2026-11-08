
package com.rines.esp32c3flasher.firmware

import android.content.Context
import android.hardware.usb.UsbDevice
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

    private val writer = Esp32C3FlashWriter(romClient)
    private val eraser = Esp32C3FlashEraser(romClient)
    private val verifier = Esp32C3FlashVerifier(romClient)

    val isConnected: Boolean
        get() = transport.isConnected

    val lastError: String
        get() = transport.lastError

    /**
     * Connects to an ESP32-C3 USB serial device.
     */
    fun connect(device: UsbDevice): FlashControllerResult {
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
     * Synchronizes with the ESP32-C3 ROM bootloader.
     * Run on a background thread.
     */
    fun synchronize(): FlashControllerResult {
        val result = sync.synchronize()

        return FlashControllerResult(
            result.success,
            result.message
        )
    }

    /**
     * Writes the selected firmware.
     * Run on a background thread.
     */
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

        return writer.writeFirmware(
            firmware = firmware,
            flashAddress = 0x000000L,
            reboot = false,
            progressCallback = progress
        )
    }

    /**
     * Erases the requested flash region.
     * The size must be sector-aligned.
     */
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

        return eraser.eraseRegion(
            flashAddress = 0x000000L,
            sizeBytes = sizeBytes
        )
    }

    /**
     * Verifies firmware using ROM MD5.
     */
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

        return verifier.verifyFirmware(
            firmware = firmware,
            flashAddress = 0x000000L
        )
    }

    fun disconnect() {
        transport.disconnect()
    }
}
