
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

    companion object {
        private const val BAUD_RATE = 115200
        private const val WRITE_TIMEOUT_MS = 5000
        private const val DEFAULT_READ_TIMEOUT_MS = 1000
    }

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
                .probeDevice(device)

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
                    BAUD_RATE,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )

                // Keep DTR and RTS disabled for ESP32-C3 manual boot control.
                port.dtr = false
                port.rts = false

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
        timeout: Int = WRITE_TIMEOUT_MS
    ): Boolean {
        val port = serialPort

        if (!isConnected || port == null) {
            lastError = "USB device is not connected."
            return false
        }

        if (timeout <= 0) {
            lastError = "Invalid USB write timeout."
            return false
        }

        return try {
            port.write(
                data,
                timeout.coerceAtLeast(1)
            )

            lastError = ""
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
        timeout: Int = DEFAULT_READ_TIMEOUT_MS
    ): ByteArray? {
        val port = serialPort

        if (!isConnected || port == null) {
            lastError = "USB device is not connected."
            return null
        }

        if (bufferSize <= 0) {
            lastError = "Invalid USB read buffer size."
            return null
        }

        if (timeout <= 0) {
            lastError = "Invalid USB read timeout."
            return null
        }

        return try {
            val buffer = ByteArray(bufferSize)
            val bytesRead = port.read(buffer, timeout)

            if (bytesRead <= 0) {
                ByteArray(0)
            } else {
                lastError = ""
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
