package com.livo.works.Search.data

data class HotelSearchRequest(
    val city: String,
    val startDate: String, // Format: YYYY-MM-DD
    val endDate: String,   // Format: YYYY-MM-DD
    val roomsCount: Int
)