package com.livo.works.Search.data

data class HotelDetailsResponse(
    val hotel: HotelSummary,
    val rooms: List<Room>
)