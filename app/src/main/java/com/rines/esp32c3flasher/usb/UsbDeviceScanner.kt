
package com.rines.esp32c3flasher.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

data class UsbDeviceInfo(
    val device: UsbDevice,
    val deviceName: String,
    val manufacturerName: String,
    val productName: String,
    val vendorId: Int,
    val productId: Int,
    val interfaceCount: Int,
) {
    val vendorIdHex: String
        get() = "0x%04X".format(vendorId)

    val productIdHex: String
        get() = "0x%04X".format(productId)

    val displayName: String
        get() = productName.ifBlank {
            manufacturerName.ifBlank {
                deviceName
            }
        }
}

object UsbDeviceScanner {

    fun scan(context: Context): List<UsbDeviceInfo> {
        val usbManager = context.getSystemService(
            Context.USB_SERVICE
        ) as UsbManager

        return usbManager.deviceList.values.map { device ->
            toDeviceInfo(device)
        }
    }

    fun toDeviceInfo(device: UsbDevice): UsbDeviceInfo {
        return UsbDeviceInfo(
            device = device,
            deviceName = device.deviceName,
            manufacturerName = device.manufacturerName.orEmpty(),
            productName = device.productName.orEmpty(),
            vendorId = device.vendorId,
            productId = device.productId,
            interfaceCount = device.interfaceCount,
        )
    }
}
