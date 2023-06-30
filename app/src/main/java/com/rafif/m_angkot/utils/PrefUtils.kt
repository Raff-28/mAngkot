package com.rafif.m_angkot.utils

import android.content.Context

class PrefUtils(
    private val context: Context,
    private val prefKey: String,
    private val prefMode: Int
) {
    private var sharedPreferences =
        context.getSharedPreferences(prefKey, prefMode)

    fun saveBoolToPref(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun saveStringToPref(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getBoolFromPref(key: String) =
        sharedPreferences.getBoolean(key, false)

    fun getStringFromPref(key: String) =
        sharedPreferences.getString(key, "")!!

    fun deleteFromPref(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}