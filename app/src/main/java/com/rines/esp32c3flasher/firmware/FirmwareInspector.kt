
package com.rines.esp32c3flasher.firmware

data class FirmwareInfo(
    val fileName: String,
    val fileSize: Long,
    val isValid: Boolean,
    val chipId: Int?,
    val segmentCount: Int?,
    val message: String
)

object FirmwareInspector {

    private const val ESP_IMAGE_MAGIC = 0xE9
    private const val ESP32_C3_CHIP_ID = 5
    private const val HEADER_SIZE = 24

    fun inspect(
        fileName: String,
        firmware: ByteArray
    ): FirmwareInfo {

        if (firmware.isEmpty()) {
            return invalid(fileName, firmware.size, "Firmware file is empty.")
        }

        if (firmware.size < HEADER_SIZE) {
            return invalid(fileName, firmware.size, "Firmware file is too small.")
        }

        val magic = firmware[0].toInt() and 0xFF

        if (magic != ESP_IMAGE_MAGIC) {
            return invalid(
                fileName,
                firmware.size,
                "Invalid ESP image magic. Expected 0xE9."
            )
        }

        val segmentCount = firmware[1].toInt() and 0xFF

        if (segmentCount == 0 || segmentCount > 16) {
            return invalid(
                fileName,
                firmware.size,
                "Invalid firmware segment count."
            )
        }

        val chipId = readUInt16LE(firmware, 12)

        if (chipId != ESP32_C3_CHIP_ID) {
            return FirmwareInfo(
                fileName = fileName,
                fileSize = firmware.size.toLong(),
                isValid = false,
                chipId = chipId,
                segmentCount = segmentCount,
                message = "This firmware is not identified as ESP32-C3 firmware."
            )
        }

        return FirmwareInfo(
            fileName = fileName,
            fileSize = firmware.size.toLong(),
            isValid = true,
            chipId = chipId,
            segmentCount = segmentCount,
            message = "ESP32-C3 firmware image header is valid."
        )
    }

    private fun readUInt16LE(
        data: ByteArray,
        offset: Int
    ): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun invalid(
        fileName: String,
        size: Int,
        message: String
    ): FirmwareInfo {
        return FirmwareInfo(
            fileName = fileName,
            fileSize = size.toLong(),
            isValid = false,
            chipId = null,
            segmentCount = null,
            message = message
        )
    }
}
