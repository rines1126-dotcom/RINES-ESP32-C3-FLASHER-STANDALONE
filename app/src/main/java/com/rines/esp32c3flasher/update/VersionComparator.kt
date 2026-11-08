
package com.rines.esp32c3flasher.update

object VersionComparator {

    fun isUpdateAvailable(
        installedVersion: String,
        latestVersion: String
    ): Boolean {
        val installed = parseVersion(installedVersion)
        val latest = parseVersion(latestVersion)

        val maxLength = maxOf(installed.size, latest.size)

        for (i in 0 until maxLength) {
            val current = installed.getOrElse(i) { 0 }
            val remote = latest.getOrElse(i) { 0 }

            if (remote > current) return true
            if (remote < current) return false
        }

        return false
    }

    private fun parseVersion(version: String): List<Int> {
        val cleanVersion = version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")

        return cleanVersion
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
    }
}
