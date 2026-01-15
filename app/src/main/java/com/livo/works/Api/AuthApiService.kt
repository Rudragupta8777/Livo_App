package com.livo.works.Api

import com.livo.works.Auth.data.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/signup/initiate")
    suspend fun signupInitiate(@Body request: SignupRequest): Response<ApiResponse<SignupInitiateResponse>>

    @POST("api/v1/auth/signup/complete")
    suspend fun signupComplete(@Body request: OtpRequest): Response<ApiResponse<UserData>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    // FIXED: Now uses 'x-refresh-token' header
    // The 'Authorization' header is added automatically by OkHttp Interceptor
    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("x-refresh-token") refreshToken: String,
        @Body request: LoginRequest
    ): Response<ApiResponse<String>>

    // Keeping 'refreshtoken' here based on your earlier route #6 description
    // (If this should also be x-refresh-token, change it here too)
    @POST("api/v1/auth/refresh")
    fun refreshToken(
        @Header("x-refresh-token") refreshToken: String
    ): Call<ApiResponse<LoginResponse>>
}