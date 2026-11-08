
package com.rines.esp32c3flasher.firmware

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FlashLogExporter {

    fun export(
        context: Context,
        logText: String
    ): Boolean {
        return try {
            val directory = File(
                context.cacheDir,
                "flash_logs"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(
                directory,
                "rines_flash_log.txt"
            )

            file.writeText(logText)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "RINES ESP32-C3 Flash Log")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Share Flash Log"
                )
            )

            true
        } catch (_: Exception) {
            false
        }
    }
}
