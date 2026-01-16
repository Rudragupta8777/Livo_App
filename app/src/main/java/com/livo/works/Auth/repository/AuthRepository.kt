package com.livo.works.Auth.repository

import android.util.Log
import com.livo.works.Api.AuthApiService
import com.livo.works.Auth.data.*
import com.livo.works.security.TokenManager
import com.livo.works.util.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun signupInitiate(name: String, email: String, pass: String) = flow {
        emit(UiState.Loading)
        try {
            val response = api.signupInitiate(SignupRequest(name, email, pass))
            if (response.isSuccessful) emit(UiState.Success(response.body()?.data))
            else emit(UiState.Error(response.body()?.error?.message ?: "Signup Failed"))
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Network Error"))
        }
    }

    suspend fun performSilentRefresh(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = tokenManager.getRefreshToken()

        // No refresh token means user is not logged in
        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "performSilentRefresh: No refresh token available")
            return@withContext false
        }

        Log.d(TAG, "performSilentRefresh: Starting with token: ${refreshToken.take(20)}...")

        return@withContext try {
            val response = api.refreshToken(refreshToken).execute()

            when {
                // Success: Got new tokens
                response.isSuccessful && response.body()?.data != null -> {
                    val newTokens = response.body()!!.data!!

                    // Save new tokens
                    tokenManager.saveAuthData(
                        newTokens.accessToken,
                        newTokens.refreshToken,
                        tokenManager.getEmail()
                    )

                    Log.d(TAG, "✅ performSilentRefresh: SUCCESS - New tokens saved")
                    true
                }

                // Client error (4xx) - Token is invalid/expired/revoked
                response.code() in 400..499 -> {
                    Log.e(TAG, "❌ performSilentRefresh: FAILED - Status ${response.code()}")
                    Log.e(TAG, "Token is invalid/expired. Clearing local session.")

                    // Clear invalid tokens so user can login fresh
                    tokenManager.clear()
                    false
                }

                // Server error (5xx) - Don't clear tokens, might be temporary
                else -> {
                    Log.e(TAG, "⚠️ performSilentRefresh: Server error ${response.code()}")
                    Log.e(TAG, "Keeping tokens, server might be temporarily down")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ performSilentRefresh: Network exception - ${e.message}")
            Log.e(TAG, "Keeping tokens, network might be temporarily unavailable")
            // Don't clear tokens on network errors - user might just be offline
            false
        }
    }

    suspend fun verifyOtp(regId: String, otp: String) = flow {
        emit(UiState.Loading)
        try {
            val response = api.signupComplete(OtpRequest(regId, otp))
            if (response.isSuccessful) {
                emit(UiState.Success(response.body()?.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Invalid OTP"
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Network Error"))
        }
    }

    suspend fun resendOtp(regId: String) = flow {
        emit(UiState.Loading)
        try {
            val requestBody = mapOf("registrationId" to regId)
            val response = api.resendOtp(requestBody)

            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()?.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Resend Failed"

                if (response.code() == 401 || response.code() == 429 || response.code() == 403) {
                    emit(UiState.SessionExpired)
                } else {
                    emit(UiState.Error(errorMsg))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "Network Error"))
        }
    }

    suspend fun loginUser(email: String, pass: String): Flow<UiState<LoginResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = api.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body()?.data != null) {
                val loginData = response.body()!!.data!!

                // SAVE TOKENS AND CREDENTIALS
                tokenManager.saveAuthData(
                    loginData.accessToken,
                    loginData.refreshToken,
                    email
                )

                emit(UiState.Success(loginData))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Invalid credentials"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Connection failed: ${e.localizedMessage}"))
        }
    }

    suspend fun initiateForgotPassword(email: String) = flow {
        emit(UiState.Loading)
        try {
            val response = api.forgotPwdInitiate(ForgotPwdInitiateRequest(email))
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()?.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Request Failed"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun completeResetPassword(regId: String, otp: String, newPass: String) = flow {
        emit(UiState.Loading)
        try {
            val response = api.forgotPwdComplete(ForgotPwdCompleteRequest(regId, otp, newPass))

            if (response.isSuccessful) {
                emit(UiState.Success("Password reset successfully"))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Reset Failed"

                // CHECK FOR 401 UNAUTHORIZED
                if (response.code() == 401) {
                    emit(UiState.SessionExpired)
                } else {
                    // 400 Bad Request or others
                    emit(UiState.Error(errorMsg))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun resendForgotOtp(regId: String) = flow {
        emit(UiState.Loading)
        try {
            val response = api.forgotPwdResend(mapOf("registrationId" to regId))
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()?.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Resend Failed"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun logoutUser() = flow {
        emit(UiState.Loading)
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: ""

            // We only pass the Refresh Token manually.
            // Access token is injected by the Interceptor.
            val response = api.logout(refreshToken)

            if (response.isSuccessful) {
                tokenManager.clear()
                emit(UiState.Success("Logged Out Successfully"))
            } else {
                tokenManager.clear()
                emit(UiState.Error("Logout failed, but local session cleared"))
            }
        } catch (e: Exception) {
            tokenManager.clear()
            emit(UiState.Error("Network error: Local session cleared"))
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}