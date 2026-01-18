package com.livo.works.Hotel.data

data class HotelDetailsResponse(
    val hotel: HotelSummary,
    val rooms: List<Room>
)