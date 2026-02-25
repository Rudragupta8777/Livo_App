package com.livo.works.Room.data

data class RoomDto(
    val id: Long,
    val type: String,
    val active: Boolean,
    val deleted: Boolean,
    val photos: List<String>,
    val amenities: List<String>,
    val capacity: Int
)