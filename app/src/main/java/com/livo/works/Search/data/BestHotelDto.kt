package com.livo.works.Search.data

data class BestHotelDto(
    val id: Long,
    val name: String,
    val city: String,
    val photos: List<String>
)