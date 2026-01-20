package com.livo.works.Api

import com.livo.works.Auth.data.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signup/initiate")
    suspend fun signupInitiate(@Body request: SignupRequest): Response<ApiResponse<SignupInitiateResponse>>

    @POST("auth/signup/complete")
    suspend fun signupComplete(@Body request: OtpRequest): Response<ApiResponse<UserData>>

    @POST("auth/signup/resend-otp")
    suspend fun resendOtp(
        @Body request: Map<String, String>
    ): Response<ApiResponse<ResendResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    suspend fun logout(
        @Header("x-refresh-token") refreshToken: String
    ): Response<ApiResponse<String>>

    @POST("auth/refresh")
    fun refreshToken(
        @Header("x-refresh-token") refreshToken: String
    ): Call<ApiResponse<LoginResponse>>

    @POST("auth/forgot-pwd/initiate")
    suspend fun forgotPwdInitiate(
        @Body request: ForgotPwdInitiateRequest
    ): Response<ApiResponse<ForgotPwdInitiateResponse>>

    @POST("auth/forgot-pwd/complete")
    suspend fun forgotPwdComplete(
        @Body request: ForgotPwdCompleteRequest
    ): Response<ApiResponse<ForgotPwdCompleteResponse>>

    @POST("auth/forgot-pwd/resend-otp")
    suspend fun forgotPwdResend(
        @Body request: Map<String, String>
    ): Response<ApiResponse<ResendResponse>>
}