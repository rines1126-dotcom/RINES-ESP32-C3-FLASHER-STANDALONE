
package com.rines.esp32c3flasher.protocol

import android.os.SystemClock
import com.rines.esp32c3flasher.usb.UsbSerialTransport

data class RomCommandResult(
    val success: Boolean,
    val message: String,
    val response: Esp32C3Response? = null
)

class Esp32C3RomClient(
    private val transport: UsbSerialTransport
) {

    @Synchronized
    fun execute(
        command: Int,
        data: ByteArray = byteArrayOf(),
        checksum: Int = 0,
        timeoutMs: Long = 3000
    ): RomCommandResult {

        if (!transport.isConnected) {
            return RomCommandResult(
                false,
                "ESP32-C3 is not connected."
            )
        }

        if (command !in 0..0xFF) {
            return RomCommandResult(
                false,
                "Invalid ROM command."
            )
        }

        if (checksum !in 0..0xFF) {
            return RomCommandResult(
                false,
                "Invalid command checksum."
            )
        }

        if (timeoutMs <= 0) {
            return RomCommandResult(
                false,
                "Invalid command timeout."
            )
        }

        val decoder = SlipCodec.Decoder()

        val request = try {
            Esp32C3Command.buildRequest(
                command = command,
                data = data,
                checksum = checksum
            )
        } catch (error: Exception) {
            return RomCommandResult(
                false,
                "Unable to build ROM request: ${error.message}"
            )
        }

        val encodedRequest = SlipCodec.encode(request)

        if (!transport.write(encodedRequest)) {
            return RomCommandResult(
                false,
                "Command transmission failed: ${transport.lastError}"
            )
        }

        val startTime = SystemClock.elapsedRealtime()

        while (
            SystemClock.elapsedRealtime() - startTime < timeoutMs
        ) {

            val incoming = transport.read(
                bufferSize = 1024,
                timeout = 200
            ) ?: return RomCommandResult(
                false,
                "USB read failed: ${transport.lastError}"
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
                        command
                    )
                ) {
                    continue
                }

                if (response.isSuccessful) {
                    return RomCommandResult(
                        true,
                        "Command 0x%02X completed successfully."
                            .format(command),
                        response
                    )
                }

                return RomCommandResult(
                    false,
                    "ESP32-C3 returned an error: ${response.describe()}",
                    response
                )
            }
        }

        return RomCommandResult(
            false,
            "Timeout waiting for command 0x%02X response."
                .format(command)
        )
    }
}
