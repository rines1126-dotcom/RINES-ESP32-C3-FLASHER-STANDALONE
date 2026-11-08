
package com.rines.esp32c3flasher.firmware

data class FirmwareManagerResult(
    val success: Boolean,
    val firmware: SelectedFirmware?,
    val info: FirmwareInfo?,
    val message: String
)

class FirmwareManager {

    private var selectedFirmware: SelectedFirmware? = null
    private var firmwareInfo: FirmwareInfo? = null

    fun loadFirmware(
        firmware: SelectedFirmware
    ): FirmwareManagerResult {

        return try {
            val inspection = FirmwareInspector.inspect(
                fileName = firmware.fileName,
                firmware = firmware.data
            )

            if (!inspection.isValid) {
                selectedFirmware = null
                firmwareInfo = inspection

                return FirmwareManagerResult(
                    success = false,
                    firmware = null,
                    info = inspection,
                    message = inspection.message
                )
            }

            selectedFirmware = firmware
            firmwareInfo = inspection

            FirmwareManagerResult(
                success = true,
                firmware = firmware,
                info = inspection,
                message = "Firmware loaded and validated successfully."
            )

        } catch (error: Exception) {
            clear()

            FirmwareManagerResult(
                success = false,
                firmware = null,
                info = null,
                message = error.message
                    ?: "Firmware inspection failed."
            )
        }
    }

    fun getSelectedFirmware(): SelectedFirmware? {
        return selectedFirmware
    }

    fun getFirmwareInfo(): FirmwareInfo? {
        return firmwareInfo
    }

    fun hasValidFirmware(): Boolean {
        return selectedFirmware != null &&
            firmwareInfo?.isValid == true
    }

    fun clear() {
        selectedFirmware = null
        firmwareInfo = null
    }
}
