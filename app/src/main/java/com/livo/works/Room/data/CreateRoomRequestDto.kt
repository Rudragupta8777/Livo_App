package com.livo.works.Room.data

data class CreateRoomRequestDto(
    val type: String,
    val basePrice: Double,
    // Newly uploaded temp paths from the presign flow; rooms have no update
    // flow yet, so there are never existing photos to merge in here.
    val photos: List<String>,
    val amenities: List<String>,
    val totalCount: Int,
    val capacity: Int
)
