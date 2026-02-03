package com.livo.works.Search.data

data class BestHotelResponse(
    val timeStamp: String,
    val data: List<BestHotelDto>?,
    val error: Any?
)