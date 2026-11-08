
package com.rines.esp32c3flasher.firmware

import com.rines.esp32c3flasher.protocol.Esp32C3Command
import com.rines.esp32c3flasher.protocol.Esp32C3RomClient

data class FlashEraseResult(
    val success: Boolean,
    val erasedBytes: Long,
    val message: String
)

class Esp32C3FlashEraser(
    private val romClient: Esp32C3RomClient
) {

    companion object {
        private const val SECTOR_SIZE = 4096L
        private const val BLOCK_SIZE = 1024L
        private const val MAX_UINT32 = 0xFFFFFFFFL
        private const val ADDRESS_SPACE_SIZE = 0x100000000L
    }

    /**
     * Erases a flash region using the ROM FLASH_BEGIN command.
     * Address and size must be sector-aligned.
     */
    fun eraseRegion(
        flashAddress: Long,
        sizeBytes: Long
    ): FlashEraseResult {

        if (flashAddress < 0 || sizeBytes <= 0) {
            return failure("Invalid flash address or size.")
        }

        if (
            flashAddress % SECTOR_SIZE != 0L ||
            sizeBytes % SECTOR_SIZE != 0L
        ) {
            return failure(
                "Address and size must be multiples of 4096 bytes."
            )
        }

        if (
            flashAddress > MAX_UINT32 ||
            sizeBytes > MAX_UINT32 ||
            sizeBytes > ADDRESS_SPACE_SIZE - flashAddress
        ) {
            return failure(
                "Flash region exceeds the supported 32-bit address range."
            )
        }

        val beginPayload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(sizeBytes),
            *Esp32C3Command.intToBytesLE(0),
            *Esp32C3Command.intToBytesLE(BLOCK_SIZE),
            *Esp32C3Command.intToBytesLE(flashAddress)
        )

        val beginResult = romClient.execute(
            command = Esp32C3Command.FLASH_BEGIN,
            data = beginPayload,
            timeoutMs = 30000
        )

        if (!beginResult.success) {
            return failure(
                "Flash erase initialization failed: ${beginResult.message}"
            )
        }

        // One 32-bit word: 1 means stay in ROM download mode.
        val endPayload = Esp32C3Command.intToBytesLE(1)

        val endResult = romClient.execute(
            command = Esp32C3Command.FLASH_END,
            data = endPayload,
            timeoutMs = 10000
        )

        if (!endResult.success) {
            return failure(
                "Flash erase completion failed: ${endResult.message}"
            )
        }

        return FlashEraseResult(
            success = true,
            erasedBytes = sizeBytes,
            message = "Flash region erased successfully."
        )
    }

    private fun failure(message: String) =
        FlashEraseResult(
            success = false,
            erasedBytes = 0,
            message = message
        )
}
