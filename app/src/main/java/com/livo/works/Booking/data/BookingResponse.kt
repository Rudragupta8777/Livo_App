package com.livo.works.Booking.data

data class BookingResponse(
    val timeStamp: String,
    val data: BookingData?,
    val error: ApiError?
)