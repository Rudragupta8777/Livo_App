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
    private val repository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination = _destination.asStateFlow()

    init {
        decideNextScreen()
    }

    private fun decideNextScreen() {
        viewModelScope.launch {
            // 1. Branding Delay (1 second)
            delay(1000)

            // 2. Read Local State
            val isFirstTime = sharedPreferences.getBoolean("is_first_time", true)
            val refreshToken = tokenManager.getRefreshToken()

            Log.d("LIVO_SPLASH", "FirstTime: $isFirstTime, HasRefreshToken: ${!refreshToken.isNullOrEmpty()}")

            val target = when {
                // CASE A: Fresh Install -> Onboarding
                isFirstTime -> "ONBOARDING"

                // CASE B: User logged out manually (Token is null/empty)
                // STRICT RULE: Do NOT hit network. Go straight to Login.
                refreshToken.isNullOrEmpty() -> "LOGIN"

                // CASE C: User has a session (Refresh token exists)
                // ACTION: Validate session with backend (Silent Refresh)
                else -> {
                    Log.d("LIVO_SPLASH", "Token found. Verifying session...")
                    // If refresh works, user goes to Dashboard.
                    // If refresh fails (e.g., token revoked), user goes to Login.
                    val isSessionValid = repository.performSilentRefresh()
                    if (isSessionValid) "DASHBOARD" else "LOGIN"
                }
            }
            _destination.value = target
        }
    }
}