package com.livo.works.Payment.data

import com.livo.works.Booking.data.ApiError

data class PaymentInitResponse(
    val timeStamp: String,
    val data: PaymentInitData?,
    val error: ApiError?
)