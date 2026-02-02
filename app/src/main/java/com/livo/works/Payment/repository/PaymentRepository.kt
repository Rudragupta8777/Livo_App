package com.livo.works.Payment.repository

import com.livo.works.Api.PaymentApiService
import com.livo.works.Payment.data.PaymentInitData
import com.livo.works.Payment.data.PaymentVerifyRequest
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PaymentRepository @Inject constructor(
    private val api: PaymentApiService
) {

    suspend fun initPayment(bookingId: Long, key: String): Flow<UiState<PaymentInitData>> = flow {
        emit(UiState.Loading)
        try {
            val response = api.initPayment(bookingId, key)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data))
            } else {
                emit(UiState.Error(response.body()?.error?.message ?: "Payment Init Failed"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    suspend fun verifyPayment(req: PaymentVerifyRequest): Flow<UiState<Boolean>> = flow {
        emit(UiState.Loading)
        try {
            val response = api.verifyPayment(req)
            if (response.isSuccessful) {
                // 200 OK or 204 No Content -> Success
                emit(UiState.Success(true))
            } else {
                emit(UiState.Error("Payment Verification Failed"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Verification Error: ${e.message}"))
        }
    }
}