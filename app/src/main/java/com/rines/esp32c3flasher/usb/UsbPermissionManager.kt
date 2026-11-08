
package com.rines.esp32c3flasher.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

class UsbPermissionManager(
    context: Context,
    private val onPermissionResult: (UsbDevice?, Boolean) -> Unit
) {

    private val appContext = context.applicationContext

    private val usbManager = appContext.getSystemService(
        Context.USB_SERVICE
    ) as UsbManager

    private val action =
        "${appContext.packageName}.USB_PERMISSION"

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            if (intent?.action != action) return

            val device: UsbDevice? =
                if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE
                    )
                }

            val granted = intent.getBooleanExtra(
                UsbManager.EXTRA_PERMISSION_GRANTED,
                false
            )

            onPermissionResult(device, granted)
        }
    }

    fun register() {
        if (isRegistered) return

        val filter = IntentFilter(action)

        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }

        isRegistered = true
    }

    fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            onPermissionResult(device, true)
            return
        }

        register()

        val permissionIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(action).setPackage(appContext.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_MUTABLE
        )

        usbManager.requestPermission(
            device,
            permissionIntent
        )
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun unregister() {
        if (!isRegistered) return

        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already unregistered.
        }

        isRegistered = false
    }
}
