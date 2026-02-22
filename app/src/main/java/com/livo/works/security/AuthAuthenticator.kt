package com.livo.works.security

import android.util.Log
import com.livo.works.Api.AuthApiService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiProvider: Provider<AuthApiService>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. FAIL FAST checks
        if (response.request.header("X-Token-Refresh-Attempted") != null) {
            tokenManager.clear()
            return null
        }
        val requestPath = response.request.url.encodedPath
        if (requestPath.contains("/auth/", ignoreCase = true)) {
            tokenManager.clear()
            return null
        }

        // 2. OPTIMIZATION check
        val currentAccessToken = tokenManager.getAccessToken()
        val requestAccessToken = response.request.header("Authorization")?.substringAfter("Bearer ")

        if (currentAccessToken != null && currentAccessToken != requestAccessToken) {
            return newRequestWithToken(response.request, currentAccessToken)
        }

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return null
        }

        // 3. THREAD-SAFE REFRESH
        return runBlocking {
            mutex.withLock {
                val updatedAccessToken = tokenManager.getAccessToken()
                val updatedRefreshToken = tokenManager.getRefreshToken()

                if (updatedAccessToken != null && updatedAccessToken != requestAccessToken) {
                    return@withLock newRequestWithToken(response.request, updatedAccessToken)
                }

                try {
                    Log.d(TAG, "Attempting automatic token refresh...")

                    val refreshResponse = apiProvider.get().refreshToken(updatedRefreshToken ?: "").execute()

                    if (refreshResponse.isSuccessful && refreshResponse.body()?.data != null) {
                        val newTokens = refreshResponse.body()!!.data!!

                        tokenManager.saveAuthData(
                            newTokens.accessToken,
                            newTokens.refreshToken,
                            tokenManager.getEmail()
                        )

                        Log.d(TAG, "Token refresh successful.")
                        return@withLock newRequestWithToken(response.request, newTokens.accessToken)

                    } else if (refreshResponse.code() in 400..499) {
                        // ACTUAL EXPIRED/INVALID REFRESH TOKEN - Clear session
                        Log.e(TAG, "Refresh token invalid (${refreshResponse.code()}). Clearing session.")
                        tokenManager.clear()
                        return@withLock null
                    } else {
                        // SERVER ERROR (5xx) - Do not log the user out!
                        Log.e(TAG, "Server error: ${refreshResponse.code()}")
                        throw IOException("Server error during token refresh")
                    }
                } catch (e: Exception) {
                    // CRITICAL FIX: Network error (No internet).
                    // Throw IOException instead of returning null to prevent false 401 Session Expired!
                    Log.e(TAG, "Network error: ${e.message}")
                    throw IOException("Network error during token refresh", e)
                }
            }
        }
    }

    private fun newRequestWithToken(request: Request, newToken: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("X-Token-Refresh-Attempted", "true")
            .build()
    }

    companion object {
        private const val TAG = "AuthAuthenticator"
    }
}