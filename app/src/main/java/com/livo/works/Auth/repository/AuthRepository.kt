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
    private val tokenManager: TokenManager // Fixed: Added injection here
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
        val oldRefresh = tokenManager.getRefreshToken() ?: return@withContext false
        Log.d("LIVO_AUTH", "Attempting Silent Refresh with: $oldRefresh")

        return@withContext try {
            val response = api.refreshToken(oldRefresh).execute()
            if (response.isSuccessful && response.body()?.data != null) {
                val newData = response.body()!!.data!!

                // Save new tokens
                tokenManager.saveAuthData(
                    newData.accessToken,
                    newData.refreshToken,
                    tokenManager.getEmail(),
                    tokenManager.getPassword()
                )

                Log.d("LIVO_AUTH", "Refresh Success! New Access: ${newData.accessToken}")
                Log.d("LIVO_AUTH", "New Refresh: ${newData.refreshToken}")
                true
            } else {
                Log.e("LIVO_AUTH", "Refresh Failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("LIVO_AUTH", "Network Error during refresh: ${e.message}")
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

    suspend fun loginUser(email: String, pass: String): Flow<UiState<LoginResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = api.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body()?.data != null) {
                val loginData = response.body()!!.data!!

                // SAVE TOKENS AND CREDENTIALS
                // Your friend's logout route needs email/pass in the body
                tokenManager.saveAuthData(
                    loginData.accessToken,
                    loginData.refreshToken,
                    email,
                    pass
                )

                emit(UiState.Success(loginData))
            } else {
                // Get error message from the backend response
                val errorMsg = response.body()?.error?.message ?: "Invalid credentials"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Connection failed: ${e.localizedMessage}"))
        }
    }

    suspend fun logoutUser() = flow {
        emit(UiState.Loading)
        try {
            val refreshToken = tokenManager.getRefreshToken() ?: ""
            val email = tokenManager.getEmail()
            val pass = tokenManager.getPassword()

            // We only pass the Refresh Token manually.
            // Access token is injected by the Interceptor.
            val response = api.logout(refreshToken, LoginRequest(email, pass))

            if (response.isSuccessful) {
                tokenManager.clear()
                emit(UiState.Success("Logged Out Successfully"))
            } else {
                // Even if server rejects it, we clear local session to prevent "Zombie" login
                tokenManager.clear()
                emit(UiState.Error("Logout failed, but local session cleared"))
            }
        } catch (e: Exception) {
            // Network error? Force clear anyway.
            tokenManager.clear()
            emit(UiState.Error("Network error: Local session cleared"))
        }
    }
}