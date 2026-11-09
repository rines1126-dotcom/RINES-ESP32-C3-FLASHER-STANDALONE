
package com.rines.esp32c3flasher.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbDeviceReceiver(
    private val onDeviceAttached: (UsbDevice?) -> Unit = {},
    private val onDeviceChanged: () -> Unit
) : BroadcastReceiver() {

    private val notifiedDevices = mutableSetOf<String>()

    private fun deviceKey(device: UsbDevice): String {
        return "${device.deviceName}_${device.vendorId}_${device.productId}"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {

                val device = intent.getParcelableExtra<UsbDevice>(
                    UsbManager.EXTRA_DEVICE
                )

                if (device != null) {
                    val key = deviceKey(device)

                    // Show the popup only once for the same attached device.
                    if (notifiedDevices.add(key)) {
                        onDeviceAttached(device)
                    }
                }

                onDeviceChanged()
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {

                val device = intent.getParcelableExtra<UsbDevice>(
                    UsbManager.EXTRA_DEVICE
                )

                if (device != null) {
                    notifiedDevices.remove(deviceKey(device))
                } else {
                    notifiedDevices.clear()
                }

                onDeviceChanged()
            }
        }
    }
}
