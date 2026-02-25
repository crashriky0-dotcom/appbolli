package com.example.levabolliapp

import android.content.Context

object Storage {

    private const val PREF_NAME = "levabolli_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getString(context: Context, key: String, default: String = ""): String {
        return prefs(context).getString(key, default) ?: default
    }

    fun putString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }
}
