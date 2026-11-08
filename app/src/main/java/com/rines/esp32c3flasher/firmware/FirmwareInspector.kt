
package com.rines.esp32c3flasher.firmware

import java.security.MessageDigest

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
    private const val SEGMENT_HEADER_SIZE = 8
    private const val MAX_SEGMENTS = 16
    private const val CHECKSUM_INIT = 0xEF
    private const val SHA256_LENGTH = 32

    fun inspect(
        fileName: String,
        firmware: ByteArray
    ): FirmwareInfo {

        if (firmware.size < HEADER_SIZE) {
            return invalid(fileName, firmware, "Firmware file is too small.")
        }

        if ((firmware[0].toInt() and 0xFF) != ESP_IMAGE_MAGIC) {
            return invalid(fileName, firmware, "Invalid ESP image magic.")
        }

        val segmentCount = firmware[1].toInt() and 0xFF

        if (segmentCount !in 1..MAX_SEGMENTS) {
            return invalid(fileName, firmware, "Invalid segment count.")
        }

        val chipId = readUInt16LE(firmware, 12)

        if (chipId != ESP32_C3_CHIP_ID) {
            return FirmwareInfo(
                fileName,
                firmware.size.toLong(),
                false,
                chipId,
                segmentCount,
                "Firmware is not identified as ESP32-C3."
            )
        }

        val hashAppended =
            (firmware[23].toInt() and 0xFF) == 1

        var offset = HEADER_SIZE
        var checksum = CHECKSUM_INIT

        for (segment in 0 until segmentCount) {

            if (offset > firmware.size - SEGMENT_HEADER_SIZE) {
                return invalid(
                    fileName,
                    firmware,
                    "Segment $segment header is incomplete.",
                    chipId,
                    segmentCount
                )
            }

            val dataLength = readUInt32LE(
                firmware,
                offset + 4
            )

            offset += SEGMENT_HEADER_SIZE

            if (dataLength > firmware.size.toLong() - offset) {
                return invalid(
                    fileName,
                    firmware,
                    "Segment $segment exceeds firmware file size.",
                    chipId,
                    segmentCount
                )
            }

            val end = offset + dataLength.toInt()

            for (index in offset until end) {
                checksum = checksum xor
                    (firmware[index].toInt() and 0xFF)
            }

            offset = end
        }

        // Checksum occupies the last byte before the next
        // 16-byte boundary, including when offset is aligned.
        val checksumOffset =
            (((offset.toLong() / 16L) + 1L) * 16L - 1L)

        if (checksumOffset >= firmware.size.toLong()) {
            return invalid(
                fileName,
                firmware,
                "Firmware checksum is missing.",
                chipId,
                segmentCount
            )
        }

        val checksumIndex = checksumOffset.toInt()

        val storedChecksum =
            firmware[checksumIndex].toInt() and 0xFF

        if (storedChecksum != checksum) {
            return invalid(
                fileName,
                firmware,
                "Firmware checksum mismatch.",
                chipId,
                segmentCount
            )
        }

        if (hashAppended) {
            val hashStart = checksumIndex + 1
            val hashEnd = hashStart + SHA256_LENGTH

            if (hashEnd > firmware.size) {
                return invalid(
                    fileName,
                    firmware,
                    "Appended SHA-256 hash is incomplete.",
                    chipId,
                    segmentCount
                )
            }

            val calculatedHash = MessageDigest
                .getInstance("SHA-256")
                .digest(firmware.copyOfRange(0, hashStart))

            val storedHash = firmware.copyOfRange(
                hashStart,
                hashEnd
            )

            if (!calculatedHash.contentEquals(storedHash)) {
                return invalid(
                    fileName,
                    firmware,
                    "Firmware SHA-256 hash mismatch.",
                    chipId,
                    segmentCount
                )
            }
        }

        return FirmwareInfo(
            fileName = fileName,
            fileSize = firmware.size.toLong(),
            isValid = true,
            chipId = chipId,
            segmentCount = segmentCount,
            message = "ESP32-C3 firmware structure and checksum are valid."
        )
    }

    private fun readUInt16LE(
        data: ByteArray,
        offset: Int
    ): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt32LE(
        data: ByteArray,
        offset: Int
    ): Long {
        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun invalid(
        fileName: String,
        firmware: ByteArray,
        message: String,
        chipId: Int? = null,
        segmentCount: Int? = null
    ): FirmwareInfo {
        return FirmwareInfo(
            fileName = fileName,
            fileSize = firmware.size.toLong(),
            isValid = false,
            chipId = chipId,
            segmentCount = segmentCount,
            message = message
        )
    }
}
