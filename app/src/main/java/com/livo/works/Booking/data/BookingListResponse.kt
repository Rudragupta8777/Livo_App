package com.livo.works.Booking.data

data class BookingListResponse(
    val timeStamp: String,
    val data: BookingListContent?,
    val error: Any?
)