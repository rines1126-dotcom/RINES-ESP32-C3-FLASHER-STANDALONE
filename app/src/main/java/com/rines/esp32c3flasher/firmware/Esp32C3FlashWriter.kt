
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

        private const val MAX_UINT32 = 0xFFFFFFFFL
        private const val ADDRESS_SPACE_SIZE = 0x100000000L
    }

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

        if (
            flashAddress < 0 ||
            flashAddress > MAX_UINT32 ||
            flashAddress % SECTOR_SIZE != 0L
        ) {
            return FlashWriteResult(
                false,
                "Flash address must be a valid 32-bit, sector-aligned address."
            )
        }

        val firmwareSize = firmware.size
        val firmwareSizeLong = firmwareSize.toLong()

        val blockCount =
            (firmwareSizeLong + BLOCK_SIZE - 1L) / BLOCK_SIZE

        val eraseSize =
            ((firmwareSizeLong + SECTOR_SIZE - 1L) /
                SECTOR_SIZE) * SECTOR_SIZE

        if (blockCount <= 0 || blockCount > MAX_UINT32) {
            return FlashWriteResult(
                false,
                "Invalid firmware block count."
            )
        }

        if (eraseSize <= 0 || eraseSize > MAX_UINT32) {
            return FlashWriteResult(
                false,
                "Firmware erase size exceeds the supported 32-bit range."
            )
        }

        if (eraseSize > ADDRESS_SPACE_SIZE - flashAddress) {
            return FlashWriteResult(
                false,
                "Firmware exceeds the 32-bit flash address range."
            )
        }

        progressCallback(0, "Starting flash operation...")

        val beginPayload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(eraseSize),
            *Esp32C3Command.intToBytesLE(blockCount),
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

        var bytesWritten = 0

        for (sequence in 0L until blockCount) {

            val sourceOffset =
                (sequence * BLOCK_SIZE).toInt()

            val actualLength = min(
                BLOCK_SIZE,
                firmwareSize - sourceOffset
            )

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
                *Esp32C3Command.intToBytesLE(sequence),
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

            // Keep progress below 100% until FLASH_END succeeds.
            val progress =
                (((bytesWritten.toLong() * 100L) /
                    firmwareSizeLong).toInt()).coerceAtMost(99)

            progressCallback(
                progress,
                "Writing firmware: $progress%"
            )
        }

        // 0 = reboot, 1 = remain in download mode.
        val endPayload = Esp32C3Command.intToBytesLE(
            if (reboot) 0 else 1
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
                "FLASH COMPLETE — Device reboot requested."
            } else {
                "FLASH COMPLETE — Device remains in bootloader mode."
            }
        )

        return FlashWriteResult(
            success = true,
            message = "Firmware written successfully.",
            bytesWritten = bytesWritten
        )
    }
}
