package com.livo.works.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "auth_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthData(access: String, refresh: String, email: String, pass: String) {
        prefs.edit().apply {
            putString("access_token", access)
            putString("refresh_token", refresh)
            putString("user_email", email)
            putString("user_pass", pass)
        }.apply()
    }

    fun getAccessToken() = prefs.getString("access_token", null)
    fun getRefreshToken() = prefs.getString("refresh_token", null)
    fun getEmail() = prefs.getString("user_email", "") ?: ""
    fun getPassword() = prefs.getString("user_pass", "") ?: ""

    fun clear() = prefs.edit().clear().apply()
}