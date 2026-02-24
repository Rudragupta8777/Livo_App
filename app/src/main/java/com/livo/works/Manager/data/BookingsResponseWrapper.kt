package com.livo.works.Manager.data

data class BookingsResponseWrapper(
    val timeStamp: String?,
    val data: PagedHotelBookings,
    val error: String?
)