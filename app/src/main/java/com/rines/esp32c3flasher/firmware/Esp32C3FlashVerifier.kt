
package com.rines.esp32c3flasher.firmware

import com.rines.esp32c3flasher.protocol.Esp32C3Command
import com.rines.esp32c3flasher.protocol.Esp32C3RomClient
import java.security.MessageDigest

data class FlashVerifyResult(
    val success: Boolean,
    val expectedMd5: String,
    val actualMd5: String,
    val message: String
)

class Esp32C3FlashVerifier(
    private val romClient: Esp32C3RomClient
) {

    fun verifyFirmware(
        firmware: ByteArray,
        flashAddress: Long = 0L
    ): FlashVerifyResult {

        if (firmware.isEmpty()) {
            return failure("Firmware file is empty.")
        }

        if (flashAddress < 0) {
            return failure("Invalid flash address.")
        }

        val expectedMd5 = calculateMd5(firmware)

        // SPI_FLASH_MD5 payload:
        // address, size, 0, 0
        val payload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(flashAddress),
            *Esp32C3Command.intToBytesLE(firmware.size.toLong()),
            *Esp32C3Command.intToBytesLE(0),
            *Esp32C3Command.intToBytesLE(0)
        )

        val result = romClient.execute(
            command = Esp32C3Command.SPI_FLASH_MD5,
            data = payload,
            timeoutMs = 30000
        )

        if (!result.success || result.response == null) {
            return FlashVerifyResult(
                success = false,
                expectedMd5 = expectedMd5,
                actualMd5 = "",
                message = "Flash MD5 command failed: ${result.message}"
            )
        }

        // ESP32-C3 ROM returns 32 ASCII hexadecimal characters,
        // followed by four status bytes.
        val responseData = result.response.data

        if (responseData.size < 36) {
            return FlashVerifyResult(
                success = false,
                expectedMd5 = expectedMd5,
                actualMd5 = "",
                message = "Invalid MD5 response from ESP32-C3."
            )
        }

        val actualMd5 = responseData
            .copyOfRange(0, 32)
            .toString(Charsets.US_ASCII)
            .lowercase()

        if (!actualMd5.matches(Regex("[0-9a-f]{32}"))) {
            return FlashVerifyResult(
                success = false,
                expectedMd5 = expectedMd5,
                actualMd5 = actualMd5,
                message = "ESP32-C3 returned an invalid MD5 value."
            )
        }

        val verified = expectedMd5 == actualMd5

        return FlashVerifyResult(
            success = verified,
            expectedMd5 = expectedMd5,
            actualMd5 = actualMd5,
            message = if (verified) {
                "Firmware verification successful. MD5 matches."
            } else {
                "Firmware verification failed. MD5 mismatch."
            }
        )
    }

    private fun calculateMd5(data: ByteArray): String {
        val digest = MessageDigest
            .getInstance("MD5")
            .digest(data)

        return digest.joinToString("") {
            "%02x".format(it.toInt() and 0xFF)
        }
    }

    private fun failure(message: String): FlashVerifyResult {
        return FlashVerifyResult(
            success = false,
            expectedMd5 = "",
            actualMd5 = "",
            message = message
        )
    }
}
