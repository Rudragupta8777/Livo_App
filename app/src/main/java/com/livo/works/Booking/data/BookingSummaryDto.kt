package com.livo.works.Booking.data

data class BookingSummaryDto(
    val bookingId: Long,
    val hotelName: String,
    val hotelCity: String,
    val startDate: String,
    val endDate: String,
    val bookingStatus: String
)