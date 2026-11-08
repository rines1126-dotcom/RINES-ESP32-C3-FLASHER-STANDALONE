
package com.rines.esp32c3flasher.protocol

data class FlashInitResult(
    val success: Boolean,
    val message: String
)

class Esp32C3FlashInit(
    private val romClient: Esp32C3RomClient
) {

    /**
     * Attaches the default SPI flash interface.
     * ESP32-C3 ROM requires two zero-valued words.
     */
    fun attachFlash(): FlashInitResult {

        val payload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(0),
            *Esp32C3Command.intToBytesLE(0)
        )

        val result = romClient.execute(
            command = Esp32C3Command.SPI_ATTACH,
            data = payload,
            timeoutMs = 3000
        )

        return FlashInitResult(
            success = result.success,
            message = if (result.success) {
                "ESP32-C3 SPI flash attached successfully."
            } else {
                "SPI attach failed: ${result.message}"
            }
        )
    }

    /**
     * Configures the SPI flash parameters.
     * Flash capacity must come from actual chip detection.
     */
    fun setFlashParameters(
        flashSizeBytes: Long
    ): FlashInitResult {

        if (flashSizeBytes <= 0) {
            return FlashInitResult(
                false,
                "Invalid flash capacity."
            )
        }

        val payload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(0),
            *Esp32C3Command.intToBytesLE(flashSizeBytes),
            *Esp32C3Command.intToBytesLE(65536),
            *Esp32C3Command.intToBytesLE(4096),
            *Esp32C3Command.intToBytesLE(256),
            *Esp32C3Command.intToBytesLE(0xFFFF)
        )

        val result = romClient.execute(
            command = Esp32C3Command.SPI_SET_PARAMS,
            data = payload,
            timeoutMs = 3000
        )

        return FlashInitResult(
            success = result.success,
            message = if (result.success) {
                "SPI flash parameters configured successfully."
            } else {
                "SPI parameter configuration failed: ${result.message}"
            }
        )
    }
}
