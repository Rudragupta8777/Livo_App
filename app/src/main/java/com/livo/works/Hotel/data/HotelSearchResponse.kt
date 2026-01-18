package com.livo.works.Hotel.data

data class HotelSearchResponse(
    val content: List<HotelSummary>,
    val page: PageInfo
)