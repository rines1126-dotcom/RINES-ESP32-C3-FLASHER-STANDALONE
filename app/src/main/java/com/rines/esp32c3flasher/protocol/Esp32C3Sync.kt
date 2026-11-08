
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
     * Synchronizes with the ESP32-C3 ROM bootloader.
     * Must run on a background thread.
     */
    fun synchronize(): SyncResult {

        if (!transport.isConnected) {
            return SyncResult(
                false,
                "USB device is not connected."
            )
        }

        val decoder = SlipCodec.Decoder()
        val syncPacket = Esp32C3Command.buildSyncRequest()

        var lastError = ""

        repeat(MAX_ATTEMPTS) { attempt ->

            decoder.reset()

            if (!transport.write(syncPacket)) {
                return SyncResult(
                    false,
                    "SYNC transmission failed: ${transport.lastError}"
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
                ) ?: return SyncResult(
                    false,
                    "USB communication error: ${transport.lastError}"
                )

                if (incoming.isEmpty()) {
                    continue
                }

                for (packet in decoder.feed(incoming)) {

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
                            true,
                            "ESP32-C3 synchronized successfully.",
                            response
                        )
                    }

                    lastError = response.describe()
                }
            }
        }

        val message = if (lastError.isNotEmpty()) {
            "SYNC failed after $MAX_ATTEMPTS attempts. Last response: $lastError"
        } else {
            "No valid SYNC response after $MAX_ATTEMPTS attempts. " +
                "Check USB connection and bootloader mode."
        }

        return SyncResult(
            false,
            message
        )
    }
}
