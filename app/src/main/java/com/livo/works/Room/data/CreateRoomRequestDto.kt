package com.livo.works.Room.data

data class CreateRoomRequestDto(
    val type: String,
    val basePrice: Double,
    val photos: List<String>,
    val amenities: List<String>,
    val totalCount: Int,
    val capacity: Int
)