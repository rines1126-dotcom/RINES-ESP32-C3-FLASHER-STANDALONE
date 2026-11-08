
package com.rines.esp32c3flasher.protocol

import android.os.SystemClock
import com.rines.esp32c3flasher.usb.UsbSerialTransport

data class SyncResult(
    val success: Boolean,
    val message: String,
    val response: Esp32C3Response? = null
)

class Esp32C3Sync(
    private val transport: UsbSerialTransport
) {

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val RESPONSE_TIMEOUT_MS = 1500L
        private const val READ_TIMEOUT_MS = 200
    }

    /**
     * Sends SYNC commands and waits for a valid ESP32-C3 response.
     *
     * Call this function from a background thread,
     * not directly from the Android UI thread.
     */
    fun synchronize(): SyncResult {

        if (!transport.isConnected) {
            return SyncResult(
                success = false,
                message = "USB device is not connected."
            )
        }

        val decoder = SlipCodec.Decoder()
        val syncPacket = Esp32C3Command.buildSyncRequest()

        repeat(MAX_ATTEMPTS) { attempt ->

            decoder.reset()

            if (!transport.write(syncPacket)) {
                return SyncResult(
                    success = false,
                    message = "SYNC transmission failed: ${transport.lastError}"
                )
            }

            val startTime = SystemClock.elapsedRealtime()

            while (
                SystemClock.elapsedRealtime() - startTime <
                RESPONSE_TIMEOUT_MS
            ) {

                val incoming = transport.read(
                    bufferSize = 512,
                    timeout = READ_TIMEOUT_MS
                )

                if (incoming == null) {
                    return SyncResult(
                        success = false,
                        message = "USB communication error: ${transport.lastError}"
                    )
                }

                if (incoming.isEmpty()) {
                    continue
                }

                val packets = decoder.feed(incoming)

                for (packet in packets) {

                    val response =
                        Esp32C3ResponseParser.parse(packet)
                            ?: continue

                    if (
                        !Esp32C3ResponseParser.matchesCommand(
                            response,
                            Esp32C3Command.SYNC
                        )
                    ) {
                        continue
                    }

                    if (response.isSuccessful) {
                        return SyncResult(
                            success = true,
                            message = "ESP32-C3 synchronized successfully.",
                            response = response
                        )
                    }

                    return SyncResult(
                        success = false,
                        message = "ESP32-C3 returned an error. " +
                            response.describe(),
                        response = response
                    )
                }
            }
        }

        return SyncResult(
            success = false,
            message = "No valid SYNC response after $MAX_ATTEMPTS attempts. " +
                "Check USB connection and bootloader mode."
        )
    }
}
