package com.livo.works.Hotel.data

data class Room(
    val id: Long,
    val type: String,
    val pricePerDay: Double,
    val photos: List<String>,
    val amenities: List<String>,
    val capacity: Int,
    val available: Boolean
)