
package com.rines.esp32c3flasher.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbDeviceReceiver(
    private val onDeviceChanged: () -> Unit,
    private val onDeviceAttached: (UsbDevice?) -> Unit = {}
) : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(
                    UsbManager.EXTRA_DEVICE
                )

                onDeviceAttached(device)
                onDeviceChanged()
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                onDeviceChanged()
            }
        }
    }
}
