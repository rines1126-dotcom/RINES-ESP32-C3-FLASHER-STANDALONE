package com.rines.esp32c3flasher.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ApkInstaller {

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {

        require(apkUrl.startsWith("https://")) {
            "Only secure HTTPS downloads are allowed."
        }

        val url = URL(apkUrl)

        require(url.host == "github.com" ||
                url.host.endsWith(".githubusercontent.com") ||
                url.host == "objects.githubusercontent.com") {
            "APK download must come from GitHub."
        }

        val connection =
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "RINES-ESP32-C3-FLASHER"
                )
            }

        try {
            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "APK download failed. HTTP $responseCode"
                )
            }

            val totalBytes = connection.contentLengthLong

            val updateDirectory = File(
                context.cacheDir,
                "updates"
            )

            if (!updateDirectory.exists()) {
                updateDirectory.mkdirs()
            }

            val apkFile = File(
                updateDirectory,
                "rines-esp32-c3-flasher-update.apk"
            )

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->

                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L

                    while (true) {
                        val count = input.read(buffer)

                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        if (totalBytes > 0) {
                            val progress =
                                ((downloadedBytes * 100) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)

                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            if (!apkFile.exists() || apkFile.length() == 0L) {
                throw IllegalStateException(
                    "Downloaded APK is empty."
                )
            }

            apkFile
        } finally {
            connection.disconnect()
        }
    }

    fun installApk(
        context: Context,
        apkFile: File
    ) {
        require(apkFile.exists()) {
            "APK file does not exist."
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(
            Intent.ACTION_VIEW
        ).apply {
            setDataAndType(
                apkUri,
                "application/vnd.android.package-archive"
            )

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }
}
