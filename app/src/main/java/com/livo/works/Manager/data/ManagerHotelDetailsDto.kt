package com.livo.works.Manager.data

data class ManagerHotelDetailsDto(
    val id: Long? = null, // Null when creating
    val name: String,
    val city: String,
    // On GET responses these are full CDN URLs. On POST/PUT, the backend
    // expects the same relative storage paths it handed out - full URLs must
    // be stripped back down (String.toRelativeMediaPath), and newly uploaded
    // temp paths are just appended as-is.
    val photos: List<String> = emptyList(),
    val amenities: List<String>,
    val contactInfo: ManagerContactInfo,
    val active: Boolean = false,
    val deleted: Boolean = false
)
