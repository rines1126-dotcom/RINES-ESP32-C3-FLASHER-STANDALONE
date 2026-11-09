
package com.rines.esp32c3flasher

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "rines_app_preferences"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun isDarkMode(context: Context): Boolean {
        return preferences(context).getBoolean(
            KEY_DARK_MODE,
            true
        )
    }

    fun setDarkMode(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context).edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    fun isKeepScreenAwake(context: Context): Boolean {
        return preferences(context).getBoolean(
            KEY_KEEP_SCREEN_AWAKE,
            false
        )
    }

    fun setKeepScreenAwake(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context).edit()
            .putBoolean(KEY_KEEP_SCREEN_AWAKE, enabled)
            .apply()
    }
}
