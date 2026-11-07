package com.spywer.yoho.data

import android.content.Context
import androidx.core.content.edit

/** Пользовательские настройки приложения. */
object AppSettings {
    private const val PREFS = "yoho_settings"
    private const val KEY_PIP_ENABLED = "pip_enabled"

    fun pipEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PIP_ENABLED, true)

    fun setPipEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_PIP_ENABLED, enabled) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
