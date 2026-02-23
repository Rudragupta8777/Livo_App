package com.livo.works.Manager.data

data class ManagerHotelDetailsDto(
    val id: Long? = null, // Null when creating
    val name: String,
    val city: String,
    val photos: List<String>,
    val amenities: List<String>,
    val contactInfo: ManagerContactInfo,
    val active: Boolean = false,
    val deleted: Boolean = false
)