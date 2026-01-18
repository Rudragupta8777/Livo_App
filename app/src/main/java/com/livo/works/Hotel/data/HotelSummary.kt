package com.livo.works.Hotel.data

data class HotelSummary(
    val id: Long,
    val name: String,
    val city: String,
    val photos: List<String>,
    val amenities: List<String>,
    val contactInfo: ContactInfo,
    val pricePerDay: Double,
    val active: Boolean
)