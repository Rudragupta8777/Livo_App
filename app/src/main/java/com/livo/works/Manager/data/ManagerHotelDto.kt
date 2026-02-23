package com.livo.works.Manager.data

data class ManagerHotelDto(
    val id: Long,
    val name: String,
    val city: String,
    val photos: List<String>?,
    val active: Boolean
)