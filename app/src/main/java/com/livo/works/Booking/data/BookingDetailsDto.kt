package com.livo.works.Booking.data

data class BookingDetailsDto(
    val id: Long,
    val hotelId: Long,
    val roomId: Long,
    val userId: Long,
    val hotelName: String,
    val roomType: String,
    val hotelCity: String,
    val roomsCount: Int,
    val startDate: String,
    val endDate: String,
    val amount: Double,
    val createdAt: String,
    val bookingStatus: String,
    val guests: List<GuestDetailDto>
)