
package com.rines.esp32c3flasher.firmware

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

data class SelectedFirmware(
    val fileName: String,
    val data: ByteArray,
    val uri: Uri
)

object FirmwareFileReader {

    fun read(
        context: Context,
        uri: Uri
    ): SelectedFirmware {

        val fileName = getFileName(context, uri)

        require(fileName.lowercase().endsWith(".bin")) {
            "Please select a .bin firmware file."
        }

        val data = context.contentResolver
            .openInputStream(uri)
            ?.use { input ->
                input.readBytes()
            }
            ?: throw IllegalStateException(
                "Unable to read firmware file."
            )

        require(data.isNotEmpty()) {
            "Firmware file is empty."
        }

        return SelectedFirmware(
            fileName = fileName,
            data = data,
            uri = uri
        )
    }

    private fun getFileName(
        context: Context,
        uri: Uri
    ): String {

        var name: String? = null

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name
            ?: uri.lastPathSegment
            ?: "firmware.bin"
    }
}

@Composable
fun FirmwarePicker(
    onFirmwareSelected: (SelectedFirmware) -> Unit,
    onError: (String) -> Unit = {}
) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val firmware = FirmwareFileReader.read(
                context,
                uri
            )

            onFirmwareSelected(firmware)

        } catch (error: Exception) {
            onError(
                error.message ?: "Unable to load firmware."
            )
        }
    }

    Button(
        onClick = {
            launcher.launch(arrayOf("*/*"))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Select Firmware (.bin)")
    }
}
