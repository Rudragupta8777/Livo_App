package com.livo.works.Booking.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestResponse(
    val id: Long,
    val name: String,
    val age: Int,
    val gender: String,
    val userId: Long
) : Parcelable