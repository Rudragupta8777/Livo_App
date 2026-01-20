package com.livo.works.Hotel.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Room(
    val id: Long,
    val type: String,
    val pricePerDay: Double,
    val capacity: Int,
    val available: Boolean,
    val amenities: List<String>,
    val photos: List<String>,
    val description: String? = null
) : Parcelable