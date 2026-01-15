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
            // Show splash animation for 2 seconds
            delay(SPLASH_DISPLAY_DURATION)

            // Check user state
            val isFirstTime = isFirstTimeUser()
            val hasTokens = hasValidSession()

            Log.d(TAG, "=== SPLASH NAVIGATION DECISION ===")
            Log.d(TAG, "Is first time user: $isFirstTime")
            Log.d(TAG, "Has refresh token: $hasTokens")

            val destination = when {
                isFirstTime -> {
                    Log.d(TAG, "Decision: NEW USER → Navigate to ONBOARDING")
                    DESTINATION_ONBOARDING
                }

                !hasTokens -> {
                    Log.d(TAG, "Decision: NO TOKENS → Navigate to LOGIN")
                    DESTINATION_LOGIN
                }

                else -> {
                    Log.d(TAG, "Decision: HAS TOKENS → Validating session...")

                    // Try to refresh tokens (validate session)
                    val isSessionValid = isSessionValid()

                    if (isSessionValid) {
                        Log.d(TAG, "Session VALID → Navigate to DASHBOARD")
                        DESTINATION_DASHBOARD
                    } else {
                        // FLOW 3: User has tokens but they're expired/invalid → Login
                        Log.d(TAG, "Session INVALID → Navigate to LOGIN")
                        DESTINATION_LOGIN
                    }
                }
            }

            Log.d(TAG, "=== FINAL DESTINATION: $destination ===")
            _destination.value = destination
        }
    }

    private fun isFirstTimeUser(): Boolean {
        return sharedPreferences.getBoolean(PREF_IS_FIRST_TIME, true)
    }

    private fun hasValidSession(): Boolean {
        val refreshToken = tokenManager.getRefreshToken()
        val hasToken = !refreshToken.isNullOrEmpty()
        Log.d(TAG, "Refresh token exists: $hasToken")
        return hasToken
    }
    private suspend fun isSessionValid(): Boolean {
        Log.d(TAG, "Attempting silent token refresh...")

        val isValid = authRepository.performSilentRefresh()

        if (isValid) {
            Log.d(TAG, "✅ Silent refresh SUCCESS - tokens are valid")
        } else {
            Log.w(TAG, "❌ Silent refresh FAILED - tokens expired/invalid")
        }

        return isValid
    }

    companion object {
        private const val TAG = "SplashViewModel"
        private const val PREF_IS_FIRST_TIME = "is_first_time"
        private const val SPLASH_DISPLAY_DURATION = 2000L // 2 seconds

        // Navigation destinations
        const val DESTINATION_ONBOARDING = "ONBOARDING"
        const val DESTINATION_LOGIN = "LOGIN"
        const val DESTINATION_DASHBOARD = "DASHBOARD"
    }
}