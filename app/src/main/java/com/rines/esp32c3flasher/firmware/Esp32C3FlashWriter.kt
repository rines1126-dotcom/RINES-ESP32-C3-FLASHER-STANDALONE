
package com.rines.esp32c3flasher.firmware

import com.rines.esp32c3flasher.protocol.Esp32C3Command
import com.rines.esp32c3flasher.protocol.Esp32C3RomClient
import kotlin.math.min

data class FlashWriteResult(
    val success: Boolean,
    val message: String,
    val bytesWritten: Int = 0
)

class Esp32C3FlashWriter(
    private val romClient: Esp32C3RomClient
) {

    companion object {
        const val BLOCK_SIZE = 1024
        const val SECTOR_SIZE = 4096

        private const val FLASH_BEGIN_TIMEOUT = 30000L
        private const val FLASH_DATA_TIMEOUT = 10000L
        private const val FLASH_END_TIMEOUT = 5000L
    }

    /**
     * Writes a firmware image to ESP32-C3 SPI flash.
     *
     * progressCallback:
     * progress percentage, status message
     *
     * Keep this operation on a background thread.
     */
    fun writeFirmware(
        firmware: ByteArray,
        flashAddress: Long = 0L,
        reboot: Boolean = false,
        progressCallback: (Int, String) -> Unit = { _, _ -> }
    ): FlashWriteResult {

        if (firmware.isEmpty()) {
            return FlashWriteResult(
                false,
                "Firmware file is empty."
            )
        }

        if (flashAddress < 0 || flashAddress % SECTOR_SIZE != 0L) {
            return FlashWriteResult(
                false,
                "Flash address must be non-negative and sector-aligned."
            )
        }

        val firmwareSize = firmware.size
        val blockCount =
            (firmwareSize + BLOCK_SIZE - 1) / BLOCK_SIZE

        val eraseSize =
            ((firmwareSize + SECTOR_SIZE - 1) / SECTOR_SIZE) *
                SECTOR_SIZE

        if (flashAddress + eraseSize > 0x100000000L) {
            return FlashWriteResult(
                false,
                "Flash address range is too large."
            )
        }

        progressCallback(0, "Starting flash operation...")

        // 1. FLASH_BEGIN
        val beginPayload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(eraseSize.toLong()),
            *Esp32C3Command.intToBytesLE(blockCount.toLong()),
            *Esp32C3Command.intToBytesLE(BLOCK_SIZE.toLong()),
            *Esp32C3Command.intToBytesLE(flashAddress)
        )

        val beginResult = romClient.execute(
            command = Esp32C3Command.FLASH_BEGIN,
            data = beginPayload,
            timeoutMs = FLASH_BEGIN_TIMEOUT
        )

        if (!beginResult.success) {
            return FlashWriteResult(
                false,
                "FLASH_BEGIN failed: ${beginResult.message}"
            )
        }

        progressCallback(0, "Flash area prepared.")

        // 2. FLASH_DATA blocks
        var bytesWritten = 0

        for (sequence in 0 until blockCount) {

            val sourceOffset = sequence * BLOCK_SIZE

            val actualLength = min(
                BLOCK_SIZE,
                firmwareSize - sourceOffset
            )

            // Last block is padded with 0xFF.
            val block = ByteArray(BLOCK_SIZE) {
                0xFF.toByte()
            }

            firmware.copyInto(
                destination = block,
                destinationOffset = 0,
                startIndex = sourceOffset,
                endIndex = sourceOffset + actualLength
            )

            val dataPayload = byteArrayOf(
                *Esp32C3Command.intToBytesLE(BLOCK_SIZE.toLong()),
                *Esp32C3Command.intToBytesLE(sequence.toLong()),
                *Esp32C3Command.intToBytesLE(0),
                *Esp32C3Command.intToBytesLE(0),
                *block
            )

            val checksum =
                Esp32C3Command.calculateChecksum(block)

            val dataResult = romClient.execute(
                command = Esp32C3Command.FLASH_DATA,
                data = dataPayload,
                checksum = checksum,
                timeoutMs = FLASH_DATA_TIMEOUT
            )

            if (!dataResult.success) {
                return FlashWriteResult(
                    false,
                    "FLASH_DATA block $sequence failed: ${dataResult.message}",
                    bytesWritten
                )
            }

            bytesWritten += actualLength

            val progress =
                ((bytesWritten.toLong() * 100) / firmwareSize).toInt()

            progressCallback(
                progress,
                "Writing firmware: $progress%"
            )
        }

        // 3. FLASH_END
        // 0 = reboot; 1 = remain in download mode.
        // ROM loader also receives a second zero word.
        val endPayload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(
                if (reboot) 0 else 1
            ),
            *Esp32C3Command.intToBytesLE(0)
        )

        val endResult = romClient.execute(
            command = Esp32C3Command.FLASH_END,
            data = endPayload,
            timeoutMs = FLASH_END_TIMEOUT
        )

        if (!endResult.success) {
            return FlashWriteResult(
                false,
                "FLASH_END failed: ${endResult.message}",
                bytesWritten
            )
        }

        progressCallback(
            100,
            if (reboot) {
                "Firmware written. Device reboot requested."
            } else {
                "Firmware written. Device remains in bootloader mode."
            }
        )

        return FlashWriteResult(
            success = true,
            message = "Firmware written successfully.",
            bytesWritten = bytesWritten
        )
    }
}
