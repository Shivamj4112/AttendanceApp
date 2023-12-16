package com.pankajgaming.attendanceapp.util

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocaleHelper {
    private const val LANGUAGE_KEY = "language"

    fun setLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.locale = locale
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    fun saveLanguage(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LANGUAGE_KEY, language)
        editor.apply()
    }

    fun getLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString(LANGUAGE_KEY, "en") ?: "en"
    }
}
