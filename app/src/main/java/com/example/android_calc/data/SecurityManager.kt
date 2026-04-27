package com.example.android_calc.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassKey(passKey: String) {
        sharedPreferences.edit().putString("pass_key", passKey).apply()
    }

    fun getPassKey(): String? {
        return sharedPreferences.getString("pass_key", null)
    }

    fun hasPassKey(): Boolean {
        return getPassKey() != null
    }

    fun clearPassKey() {
        sharedPreferences.edit().remove("pass_key").apply()
    }
}
