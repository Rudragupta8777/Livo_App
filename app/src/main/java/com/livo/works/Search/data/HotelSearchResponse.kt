package com.livo.works.Search.data

data class HotelSearchResponse(
    val content: List<HotelSummary>,
    val page: PageInfo
)