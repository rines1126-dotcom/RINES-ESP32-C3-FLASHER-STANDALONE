
package com.rines.esp32c3flasher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val releaseUrl: String,
    val notes: String,
    val apkUrl: String?
)

object GitHubUpdateChecker {

    private const val RELEASE_API =
        "https://api.github.com/repos/rines1126-dotcom/RINES-ESP32-C3-FLASHER-STANDALONE/releases/latest"

    suspend fun fetchLatestRelease(): ReleaseInfo =
        withContext(Dispatchers.IO) {

            val connection =
                URL(RELEASE_API).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    "RINES-ESP32-C3-FLASHER"
                )

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception("GitHub error: HTTP $responseCode")
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val json = JSONObject(response)
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)

                        if (asset.optString("name")
                                .endsWith(".apk", ignoreCase = true)
                        ) {
                            apkUrl = asset.optString(
                                "browser_download_url"
                            ).takeIf { it.isNotBlank() }

                            break
                        }
                    }
                }

                ReleaseInfo(
                    tagName = json.optString("tag_name"),
                    releaseUrl = json.optString("html_url"),
                    notes = json.optString("body"),
                    apkUrl = apkUrl
                )

            } finally {
                connection.disconnect()
            }
        }
}
