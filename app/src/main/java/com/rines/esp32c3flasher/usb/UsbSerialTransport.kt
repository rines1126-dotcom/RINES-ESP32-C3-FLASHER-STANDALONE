
package com.rines.esp32c3flasher.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class UsbSerialTransport(context: Context) {

    private val appContext = context.applicationContext

    private val usbManager = appContext.getSystemService(
        Context.USB_SERVICE
    ) as UsbManager

    private var usbConnection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialPort? = null

    var isConnected: Boolean = false
        private set

    var lastError: String = ""
        private set

    @Synchronized
    fun connect(device: UsbDevice): Boolean {
        disconnect()
        lastError = ""

        try {
            if (!usbManager.hasPermission(device)) {
                lastError = "USB permission not granted."
                return false
            }

            val driver = UsbSerialProber
                .getDefaultProber()
                .findAllDrivers(usbManager)
                .firstOrNull {
                    it.device.deviceId == device.deviceId
                }

            if (driver == null) {
                lastError = "No compatible USB serial driver found."
                return false
            }

            val connection = usbManager.openDevice(device)

            if (connection == null) {
                lastError = "Unable to open USB device."
                return false
            }

            val port = driver.ports.firstOrNull()

            if (port == null) {
                connection.close()
                lastError = "No serial port available."
                return false
            }

            try {
                port.open(connection)

                port.setParameters(
                    115200,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                usbConnection = connection
                serialPort = port
                isConnected = true

                return true

            } catch (error: Exception) {
                try {
                    port.close()
                } catch (_: Exception) {
                }

                connection.close()
                throw error
            }

        } catch (error: Exception) {
            lastError = error.message
                ?: "Unknown USB connection error."

            disconnect()
            return false
        }
    }

    @Synchronized
    fun write(
        data: ByteArray,
        timeout: Int = 1000
    ): Boolean {
        val port = serialPort

        if (!isConnected || port == null) {
            lastError = "USB device is not connected."
            return false
        }

        return try {
            port.write(data, timeout)
            true
        } catch (error: Exception) {
            lastError = error.message
                ?: "USB write failed."
            false
        }
    }

    @Synchronized
    fun read(
        bufferSize: Int = 4096,
        timeout: Int = 1000
    ): ByteArray? {
        val port = serialPort

        if (!isConnected || port == null) {
            lastError = "USB device is not connected."
            return null
        }

        return try {
            val buffer = ByteArray(bufferSize)
            val bytesRead = port.read(buffer, timeout)

            if (bytesRead <= 0) {
                ByteArray(0)
            } else {
                buffer.copyOf(bytesRead)
            }

        } catch (error: Exception) {
            lastError = error.message
                ?: "USB read failed."
            null
        }
    }

    @Synchronized
    fun disconnect() {
        try {
            serialPort?.close()
        } catch (_: Exception) {
        }

        try {
            usbConnection?.close()
        } catch (_: Exception) {
        }

        serialPort = null
        usbConnection = null
        isConnected = false
    }
}
