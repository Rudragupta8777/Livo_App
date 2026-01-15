package com.livo.works.Auth

import com.livo.works.Api.AuthApiService
import com.livo.works.security.TokenManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiService: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Get the 6-month refresh token
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // 2. Synchronously call the refresh route
        val refreshResponse = apiService.get().refreshToken(refreshToken).execute()

        return if (refreshResponse.isSuccessful && refreshResponse.body()?.data != null) {
            val newTokens = refreshResponse.body()!!.data!!

            // 3. Save new tokens (The user is now good for another 10 mins)
            tokenManager.saveAuthData(
                newTokens.accessToken,
                newTokens.refreshToken,
                tokenManager.getEmail(),
                tokenManager.getPassword()
            )

            // 4. Retry the original request with the NEW access token
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        } else {
            // 5. Refresh token expired (6 months passed) or invalidated
            tokenManager.clear()
            null
        }
    }
}