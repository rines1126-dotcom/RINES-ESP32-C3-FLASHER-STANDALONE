
package com.rines.esp32c3flasher.protocol

object Esp32C3Command {

    // ESP32 ROM bootloader command opcodes
    const val FLASH_BEGIN = 0x02
    const val FLASH_DATA = 0x03
    const val FLASH_END = 0x04

    const val MEM_BEGIN = 0x05
    const val MEM_END = 0x06
    const val MEM_DATA = 0x07

    const val SYNC = 0x08
    const val WRITE_REG = 0x09
    const val READ_REG = 0x0A

    const val SPI_SET_PARAMS = 0x0B
    const val SPI_ATTACH = 0x0D
    const val CHANGE_BAUDRATE = 0x0F
    const val SPI_FLASH_MD5 = 0x13

    const val REQUEST_DIRECTION = 0x00
    const val RESPONSE_DIRECTION = 0x01

    const val DEFAULT_CHECKSUM = 0x00
    const val FLASH_CHECKSUM_INIT = 0xEF

    const val FLASH_BLOCK_SIZE = 0x400
    const val FLASH_SECTOR_SIZE = 0x1000

    /**
     * Builds a standard ESP ROM bootloader request.
     *
     * Packet structure:
     * Direction | Command | Data Length | Checksum | Data
     */
    fun buildRequest(
        command: Int,
        data: ByteArray = byteArrayOf(),
        checksum: Int = DEFAULT_CHECKSUM
    ): ByteArray {

        require(command in 0..0xFF) {
            "Invalid command opcode"
        }

        require(data.size <= 0xFFFF) {
            "Command data is too large"
        }

        require(checksum in 0..0xFF) {
            "Invalid checksum"
        }

        val packet = ByteArray(8 + data.size)

        packet[0] = REQUEST_DIRECTION.toByte()
        packet[1] = command.toByte()

        writeUInt16LE(
            packet,
            2,
            data.size
        )

        packet[4] = checksum.toByte()

        packet[5] = 0
        packet[6] = 0
        packet[7] = 0

        data.copyInto(
            destination = packet,
            destinationOffset = 8
        )

        return packet
    }

    /**
     * Creates the ESP32-C3 ROM synchronization payload.
     */
    fun createSyncPayload(): ByteArray {
        val payload = ByteArray(36)

        payload[0] = 0x07
        payload[1] = 0x07
        payload[2] = 0x12
        payload[3] = 0x20

        for (index in 4 until 36) {
            payload[index] = 0x55
        }

        return payload
    }

    /**
     * Builds the complete SLIP-encoded SYNC request.
     */
    fun buildSyncRequest(): ByteArray {
        val packet = buildRequest(
            command = SYNC,
            data = createSyncPayload()
        )

        return SlipCodec.encode(packet)
    }

    /**
     * Converts an integer into four little-endian bytes.
     */
    fun intToBytesLE(value: Long): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    /**
     * Reads a four-byte little-endian unsigned integer.
     */
    fun readUInt32LE(
        data: ByteArray,
        offset: Int = 0
    ): Long {

        require(offset >= 0 && offset + 4 <= data.size) {
            "Invalid data offset"
        }

        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    /**
     * Writes a two-byte little-endian integer.
     */
    private fun writeUInt16LE(
        output: ByteArray,
        offset: Int,
        value: Int
    ) {
        output[offset] = (value and 0xFF).toByte()
        output[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    /**
     * Calculates the ESP flash-data XOR checksum.
     */
    fun calculateChecksum(data: ByteArray): Int {
        var checksum = FLASH_CHECKSUM_INIT

        for (byte in data) {
            checksum = checksum xor (byte.toInt() and 0xFF)
        }

        return checksum and 0xFF
    }
}
