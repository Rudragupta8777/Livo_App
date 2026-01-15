package com.livo.works.Auth

import android.util.Log
import com.livo.works.Api.AuthApiService
import com.livo.works.security.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: AuthApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops - if request already has retry marker, fail
        if (response.request.header("X-Token-Refresh-Attempted") != null) {
            Log.w(TAG, "Token refresh already attempted. Clearing session.")
            tokenManager.clear()
            return null
        }

        // Don't attempt refresh if we're already calling an auth endpoint
        // This prevents recursive loops when the refresh endpoint itself fails
        val requestPath = response.request.url.encodedPath
        if (requestPath.contains("/auth/", ignoreCase = true)) {
            Log.w(TAG, "Auth endpoint failed. Clearing session.")
            tokenManager.clear()
            return null
        }

        // Get refresh token
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "No refresh token available. User needs to login.")
            return null
        }

        // Attempt token refresh
        return synchronized(this) {
            try {
                Log.d(TAG, "Attempting automatic token refresh for failed request")
                val refreshResponse = authApi.refreshToken(refreshToken).execute()

                when {
                    refreshResponse.isSuccessful && refreshResponse.body()?.data != null -> {
                        val newTokens = refreshResponse.body()!!.data!!

                        // Save new tokens
                        tokenManager.saveAuthData(
                            newTokens.accessToken,
                            newTokens.refreshToken,
                            tokenManager.getEmail(),
                            tokenManager.getPassword()
                        )

                        Log.d(TAG, "Token refresh successful. Retrying original request.")

                        // Retry the original request with new access token
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .header("X-Token-Refresh-Attempted", "true")
                            .build()
                    }

                    refreshResponse.code() in 400..499 -> {
                        // Client error - token is invalid, expired, or revoked
                        Log.e(TAG, "Refresh token invalid (${refreshResponse.code()}). Clearing session.")
                        tokenManager.clear()
                        null
                    }

                    else -> {
                        // Server error - don't clear session, just fail this request
                        Log.e(TAG, "Token refresh failed with server error: ${refreshResponse.code()}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh network error: ${e.message}")
                null
            }
        }
    }

    companion object {
        private const val TAG = "AuthAuthenticator"
    }
}