package com.livo.works.Manager.data

data class HotelBookingDto(
    val bookingId: Long,
    val roomType: String,
    val roomCapacity: Int,
    val roomsCount: Int,
    val startDate: String,
    val endDate: String,
    val bookingStatus: String
)