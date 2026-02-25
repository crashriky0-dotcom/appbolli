package com.example.levabolliapp

import android.content.Context

object Storage {

    private const val PREFS = "levabolli_prefs"

    fun saveString(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun loadString(context: Context, key: String, defaultValue: String = ""): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, defaultValue) ?: defaultValue
    }

    fun deleteKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
}
