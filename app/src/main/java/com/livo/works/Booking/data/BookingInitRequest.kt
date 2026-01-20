package com.livo.works.Booking.data

data class BookingInitRequest(
    val roomId: Long,
    val startDate: String,
    val endDate: String,
    val roomsCount: Int,
    val idempotencyKey: String
)