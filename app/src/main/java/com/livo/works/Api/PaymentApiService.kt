package com.livo.works.Api

import com.livo.works.Payment.data.PaymentInitResponse
import com.livo.works.Payment.data.PaymentVerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApiService {

    @POST("payments/{bookingId}/init")
    suspend fun initPayment(
        @Path("bookingId") bookingId: Long,
        @Header("Idempotency-Key") idempotencyKey: String
    ): Response<PaymentInitResponse>

    @POST("payments/verify")
    suspend fun verifyPayment(
        @Body request: PaymentVerifyRequest
    ): Response<Unit>
}