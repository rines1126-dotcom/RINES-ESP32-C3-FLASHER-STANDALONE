
package com.rines.esp32c3flasher.firmware

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashLogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class FlashLogEntry(
    val timestamp: String,
    val level: FlashLogLevel,
    val message: String
)

class FlashLogManager(
    private val maxEntries: Int = 500
) {

    private val entries = mutableListOf<FlashLogEntry>()

    private val dateFormat = SimpleDateFormat(
        "HH:mm:ss",
        Locale.getDefault()
    )

    @Synchronized
    fun add(
        level: FlashLogLevel,
        message: String
    ) {
        entries.add(
            FlashLogEntry(
                timestamp = dateFormat.format(Date()),
                level = level,
                message = message
            )
        )

        while (entries.size > maxEntries) {
            entries.removeAt(0)
        }
    }

    fun info(message: String) {
        add(FlashLogLevel.INFO, message)
    }

    fun success(message: String) {
        add(FlashLogLevel.SUCCESS, message)
    }

    fun warning(message: String) {
        add(FlashLogLevel.WARNING, message)
    }

    fun error(message: String) {
        add(FlashLogLevel.ERROR, message)
    }

    @Synchronized
    fun getEntries(): List<FlashLogEntry> {
        return entries.toList()
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    fun exportText(): String {
        return entries.joinToString("\n") { entry ->
            "[${entry.timestamp}] [${entry.level}] ${entry.message}"
        }
    }

    @Synchronized
    fun count(): Int {
        return entries.size
    }
}
