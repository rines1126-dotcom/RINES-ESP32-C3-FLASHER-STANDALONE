package com.rines.esp32c3flasher.protocol

data class FlashIdResult(
    val success: Boolean,
    val flashId: Long? = null,
    val manufacturerId: Int? = null,
    val deviceId: Int? = null,
    val message: String
)

class Esp32C3FlashDetector(
    private val registers: Esp32C3Registers
) {

    companion object {
        private const val SPI_BASE = 0x60002000L

        private const val SPI_CMD = SPI_BASE + 0x00L
        private const val SPI_USR = SPI_BASE + 0x18L
        private const val SPI_USR2 = SPI_BASE + 0x20L
        private const val SPI_MOSI_DLEN = SPI_BASE + 0x24L
        private const val SPI_MISO_DLEN = SPI_BASE + 0x28L
        private const val SPI_W0 = SPI_BASE + 0x58L

        private const val JEDEC_RDID = 0x9F

        private const val SPI_USR_COMMAND = 1L shl 31
        private const val SPI_USR_MISO = 1L shl 28
        private const val SPI_CMD_USR = 1L shl 18

        private const val COMMAND_LENGTH = 8
        private const val READ_LENGTH = 24

        private const val MAX_POLL_ATTEMPTS = 20
    }

    /**
     * Reads the 24-bit JEDEC Flash ID.
     *
     * This method does not modify flash contents.
     */
    @Synchronized
    fun detectFlashId(): FlashIdResult {

        val oldUsrResult = registers.readRegister(SPI_USR)

        if (!oldUsrResult.success || oldUsrResult.value == null) {
            return FlashIdResult(
                success = false,
                message = "Unable to read SPI_USR register: ${oldUsrResult.message}"
            )
        }

        val oldUsr2Result = registers.readRegister(SPI_USR2)

        if (!oldUsr2Result.success || oldUsr2Result.value == null) {
            return FlashIdResult(
                success = false,
                message = "Unable to read SPI_USR2 register: ${oldUsr2Result.message}"
            )
        }

        val oldUsr = oldUsrResult.value
        val oldUsr2 = oldUsr2Result.value

        var flashId: Long? = null
        var errorMessage: String? = null

        try {
            // Configure 24-bit MISO and 8-bit command.
            val mosiResult = registers.writeRegister(
                SPI_MOSI_DLEN,
                0
            )

            if (!mosiResult.success) {
                errorMessage = mosiResult.message
                return FlashIdResult(
                    success = false,
                    message = "SPI MOSI configuration failed: $errorMessage"
                )
            }

            val misoResult = registers.writeRegister(
                SPI_MISO_DLEN,
                READ_LENGTH - 1L
            )

            if (!misoResult.success) {
                errorMessage = misoResult.message
                return FlashIdResult(
                    success = false,
                    message = "SPI MISO configuration failed: $errorMessage"
                )
            }

            // Clear the receive data register.
            val clearResult = registers.writeRegister(
                SPI_W0,
                0
            )

            if (!clearResult.success) {
                errorMessage = clearResult.message
                return FlashIdResult(
                    success = false,
                    message = "SPI data register clear failed: $errorMessage"
                )
            }

            // Enable command and MISO phases.
            val usrResult = registers.writeRegister(
                SPI_USR,
                SPI_USR_COMMAND or SPI_USR_MISO
            )

            if (!usrResult.success) {
                errorMessage = usrResult.message
                return FlashIdResult(
                    success = false,
                    message = "SPI_USR configuration failed: $errorMessage"
                )
            }

            // Set 8-bit JEDEC command: 0x9F.
            val usr2Result = registers.writeRegister(
                SPI_USR2,
                (COMMAND_LENGTH - 1L shl 28) or JEDEC_RDID.toLong()
            )

            if (!usr2Result.success) {
                errorMessage = usr2Result.message
                return FlashIdResult(
                    success = false,
                    message = "SPI_USR2 configuration failed: $errorMessage"
                )
            }

            // Start SPI transaction.
            val startResult = registers.writeRegister(
                SPI_CMD,
                SPI_CMD_USR
            )

            if (!startResult.success) {
                errorMessage = startResult.message
                return FlashIdResult(
                    success = false,
                    message = "SPI transaction failed to start: $errorMessage"
                )
            }

            // Wait until SPI transaction completes.
            var completed = false

            repeat(MAX_POLL_ATTEMPTS) {
                val status = registers.readRegister(SPI_CMD)

                if (!status.success || status.value == null) {
                    errorMessage = status.message
                    return FlashIdResult(
                        success = false,
                        message = "Unable to read SPI status: $errorMessage"
                    )
                }

                if ((status.value and SPI_CMD_USR) == 0L) {
                    completed = true
                    return@repeat
                }

                Thread.sleep(1)
            }

            if (!completed) {
                return FlashIdResult(
                    success = false,
                    message = "SPI Flash ID transaction timed out."
                )
            }

            // Read the 24-bit JEDEC response.
            val dataResult = registers.readRegister(SPI_W0)

            if (!dataResult.success || dataResult.value == null) {
                errorMessage = dataResult.message
                return FlashIdResult(
                    success = false,
                    message = "Unable to read Flash ID: $errorMessage"
                )
            }

            flashId = dataResult.value and 0xFFFFFFL

        } catch (exception: Exception) {
            errorMessage = exception.message ?: "Unknown SPI error."
        } finally {

            // Restore original SPI configuration.
            registers.writeRegister(
                SPI_USR,
                oldUsr
            )

            registers.writeRegister(
                SPI_USR2,
                oldUsr2
            )
        }

        val id = flashId ?: return FlashIdResult(
            success = false,
            message = errorMessage ?: "Flash ID detection failed."
        )

        if (id == 0L || id == 0xFFFFFFL) {
            return FlashIdResult(
                success = false,
                flashId = id,
                message = "Invalid or unreadable JEDEC Flash ID: 0x${id.toString(16)}"
            )
        }

        val manufacturer = ((id shr 16) and 0xFF).toInt()
        val device = (id and 0xFFFF).toInt()

        return FlashIdResult(
            success = true,
            flashId = id,
            manufacturerId = manufacturer,
            deviceId = device,
            message = "JEDEC Flash ID detected successfully."
        )
    }
}
