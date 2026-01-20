package com.livo.works.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
   private val _logoutSignal = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val logoutSignal = _logoutSignal.asSharedFlow()

    fun saveAuthData(access: String, refresh: String, email: String) {
        prefs.edit().apply {
            putString("access_token", access)
            putString("refresh_token", refresh)
            putString("user_email", email)
        }.apply()
    }

    fun getAccessToken() = prefs.getString("access_token", null)
    fun getRefreshToken() = prefs.getString("refresh_token", null)
    fun getEmail() = prefs.getString("user_email", "") ?: ""

    // 2. Update clear() to emit the signal
    fun clear() {
        prefs.edit().clear().apply()
        _logoutSignal.tryEmit(Unit)
    }
}