package com.livo.works.Booking.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestDto(
    val id: Long? = null,
    val name: String,
    val age: Int,
    val gender: String,
    val userId: Long? = null
) : Parcelable