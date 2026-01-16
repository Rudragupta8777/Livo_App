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

    @POST("api/v1/auth/signup/resend-otp")
    suspend fun resendOtp(
        @Body request: Map<String, String>
    ): Response<ApiResponse<ResendResponse>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("x-refresh-token") refreshToken: String
    ): Response<ApiResponse<String>>

    @POST("api/v1/auth/refresh")
    fun refreshToken(
        @Header("x-refresh-token") refreshToken: String
    ): Call<ApiResponse<LoginResponse>>

    @POST("api/v1/auth/forgot-pwd/initiate")
    suspend fun forgotPwdInitiate(
        @Body request: ForgotPwdInitiateRequest
    ): Response<ApiResponse<ForgotPwdInitiateResponse>>

    @POST("api/v1/auth/forgot-pwd/complete")
    suspend fun forgotPwdComplete(
        @Body request: ForgotPwdCompleteRequest
    ): Response<ApiResponse<ForgotPwdCompleteResponse>>

    @POST("api/v1/auth/forgot-pwd/resend-otp")
    suspend fun forgotPwdResend(
        @Body request: Map<String, String>
    ): Response<ApiResponse<ResendResponse>>
}