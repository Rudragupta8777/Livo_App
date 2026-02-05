package com.livo.works.Booking.data

data class BookingDetailsResponse(
    val timeStamp: String,
    val data: BookingDetailsDto?,
    val error: Any?
)