
package com.rines.esp32c3flasher.protocol

data class Esp32C3Response(
    val direction: Int,
    val command: Int,
    val dataLength: Int,
    val value: Long,
    val data: ByteArray
) {
    val isResponse: Boolean
        get() = direction == Esp32C3Command.RESPONSE_DIRECTION

    // ROM response status and error are the final two data bytes.
    val status: Int?
        get() = if (data.size >= 2) {
            data[data.size - 2].toInt() and 0xFF
        } else {
            null
        }

    val errorCode: Int?
        get() = if (data.size >= 2) {
            data[data.size - 1].toInt() and 0xFF
        } else {
            null
        }

    val isSuccessful: Boolean
        get() = isResponse && status == 0 && errorCode == 0

    fun describe(): String {
        return buildString {
            append("Command: 0x%02X".format(command))
            append(", Length: $dataLength")
            append(", Value: 0x%08X".format(value))
            append(", Status: ${status ?: "Unknown"}")
            append(", Error: ${errorCode ?: "Unknown"}")
        }
    }
}

object Esp32C3ResponseParser {

    private const val HEADER_SIZE = 8

    fun parse(packet: ByteArray): Esp32C3Response? {
        if (packet.size < HEADER_SIZE) return null

        val direction = packet[0].toInt() and 0xFF
        val command = packet[1].toInt() and 0xFF

        val dataLength =
            (packet[2].toInt() and 0xFF) or
            ((packet[3].toInt() and 0xFF) shl 8)

        if (packet.size != HEADER_SIZE + dataLength) {
            return null
        }

        val value = Esp32C3Command.readUInt32LE(packet, 4)

        val data = packet.copyOfRange(
            HEADER_SIZE,
            packet.size
        )

        return Esp32C3Response(
            direction = direction,
            command = command,
            dataLength = dataLength,
            value = value,
            data = data
        )
    }

    fun matchesCommand(
        response: Esp32C3Response,
        expectedCommand: Int
    ): Boolean {
        return response.isResponse &&
            response.command == expectedCommand
    }

    fun isSuccessful(
        response: Esp32C3Response,
        expectedCommand: Int
    ): Boolean {
        return matchesCommand(response, expectedCommand) &&
            response.isSuccessful
    }
}
