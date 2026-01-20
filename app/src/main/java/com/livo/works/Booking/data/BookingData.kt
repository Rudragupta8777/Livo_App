package com.livo.works.Booking.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookingData(
    val id: Long,
    val hotelId: Long,
    val roomId: Long,
    val userId: Long,
    val hotelName: String,
    val roomType: String,
    val hotelCity: String,
    val roomsCount: Int,
    val startDate: String,
    val endDate: String,
    val amount: Double,
    val bookingStatus: String,
    val idempotencyKey: String?,
    val guests: List<GuestResponse>? = null
) : Parcelable