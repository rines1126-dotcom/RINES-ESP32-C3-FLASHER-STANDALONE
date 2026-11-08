
package com.rines.esp32c3flasher.protocol

class Esp32C3Registers(
    private val romClient: Esp32C3RomClient
) {

    /**
     * Reads a 32-bit ESP32-C3 register.
     */
    fun readRegister(address: Long): RegisterResult {

        val payload = Esp32C3Command.intToBytesLE(address)

        val result = romClient.execute(
            command = Esp32C3Command.READ_REG,
            data = payload
        )

        if (!result.success || result.response == null) {
            return RegisterResult(
                success = false,
                value = null,
                message = result.message
            )
        }

        return RegisterResult(
            success = true,
            value = result.response.value,
            message = "Register read successfully."
        )
    }

    /**
     * Writes a 32-bit value to an ESP32-C3 register.
     */
    fun writeRegister(
        address: Long,
        value: Long,
        mask: Long = 0xFFFFFFFFL,
        delayUs: Long = 0
    ): RegisterResult {

        val payload = byteArrayOf(
            *Esp32C3Command.intToBytesLE(address),
            *Esp32C3Command.intToBytesLE(value),
            *Esp32C3Command.intToBytesLE(mask),
            *Esp32C3Command.intToBytesLE(delayUs)
        )

        val result = romClient.execute(
            command = Esp32C3Command.WRITE_REG,
            data = payload
        )

        return RegisterResult(
            success = result.success,
            value = null,
            message = result.message
        )
    }
}

data class RegisterResult(
    val success: Boolean,
    val value: Long?,
    val message: String
)
