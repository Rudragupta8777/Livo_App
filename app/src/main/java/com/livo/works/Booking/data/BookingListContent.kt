package com.livo.works.Booking.data

data class BookingListContent(
    val content: List<BookingSummaryDto>,
    val page: PageDto
)