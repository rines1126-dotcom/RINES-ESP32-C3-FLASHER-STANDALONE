
package com.rines.esp32c3flasher.protocol

data class FlashInitResult(
    val success: Boolean,
    val message: String
)

class Esp32C3FlashInit(
    private val romClient: Esp32C3RomClient
) {

    companion object {
        private const val MAX_UINT32 = 0xFFFFFFFFL
        private const val FLASH_BLOCK_SIZE = 65536L
        private const val FLASH_SECTOR_SIZE = 4096L
        private const val FLASH_PAGE_SIZE = 256L
        private const val FLASH_STATUS_MASK = 0xFFFFL
    }

    /**
     * Attaches the default SPI flash interface.
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
     * Configures SPI flash parameters.
     * Capacity must be supplied by actual flash detection.
     */
    fun setFlashParameters(
        flashSizeBytes: Long
    ): FlashInitResult {

        if (
            flashSizeBytes <= 0 ||
            flashSizeBytes > MAX_UINT32
        ) {
            return FlashInitResult(
                false,
                "Invalid flash capacity."
            )
        }

        val payload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(0),
            *Esp32C3Command.intToBytesLE(flashSizeBytes),
            *Esp32C3Command.intToBytesLE(FLASH_BLOCK_SIZE),
            *Esp32C3Command.intToBytesLE(FLASH_SECTOR_SIZE),
            *Esp32C3Command.intToBytesLE(FLASH_PAGE_SIZE),
            *Esp32C3Command.intToBytesLE(FLASH_STATUS_MASK)
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
