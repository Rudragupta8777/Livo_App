package com.livo.works.ViewModel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Auth.repository.AuthRepository
import com.livo.works.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val sharedPreferences: SharedPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination = _destination.asStateFlow()

    init {
        determineNavigationDestination()
    }

    private fun determineNavigationDestination() {
        viewModelScope.launch {
            delay(SPLASH_DISPLAY_DURATION)

            val destination = when {
                isFirstTimeUser() -> DESTINATION_ONBOARDING
                !hasValidSession() -> DESTINATION_LOGIN
                isSessionValid() -> DESTINATION_DASHBOARD
                else -> DESTINATION_LOGIN
            }

            Log.d(TAG, "Navigation destination determined: $destination")
            _destination.value = destination
        }
    }

    private fun isFirstTimeUser(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_FIRST_TIME, true)
    }

    private fun hasValidSession(): Boolean {
        val hasRefreshToken = !tokenManager.getRefreshToken().isNullOrEmpty()
        Log.d(TAG, "Session check - Has refresh token: $hasRefreshToken")
        return hasRefreshToken
    }

    private suspend fun isSessionValid(): Boolean {
        Log.d(TAG, "Validating session with server...")

        val isValid = authRepository.performSilentRefresh()

        if (isValid) {
            Log.d(TAG, "Session validation successful")
        } else {
            Log.w(TAG, "Session validation failed - user needs to login")
        }

        return isValid
    }

    companion object {
        private const val TAG = "SplashViewModel"
        private const val PREF_IS_FIRST_TIME = "is_first_time"
        private const val SPLASH_DISPLAY_DURATION = 2000L

        // Navigation destinations
        const val DESTINATION_ONBOARDING = "ONBOARDING"
        const val DESTINATION_LOGIN = "LOGIN"
        const val DESTINATION_DASHBOARD = "DASHBOARD"
    }
}