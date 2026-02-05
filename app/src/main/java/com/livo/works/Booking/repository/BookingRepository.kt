package com.livo.works.Booking.repository

import com.livo.works.Api.BookingApiService
import com.livo.works.Booking.data.BookingData
import com.livo.works.Booking.data.BookingInitRequest
import com.livo.works.Booking.data.BookingSummaryDto
import com.livo.works.Booking.data.GuestDto
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val api: BookingApiService
) {
    private var cachedBookings: List<BookingSummaryDto>? = null

    suspend fun getMyBookings(forceRefresh: Boolean = false): Flow<UiState<List<BookingSummaryDto>>> = flow {
        if (!forceRefresh && cachedBookings != null) {
            emit(UiState.Success(cachedBookings!!))
            return@flow
        }

        emit(UiState.Loading)
        try {
            val response = api.getMyBookings()

            if (response.isSuccessful && response.body()?.data != null) {
                val bookingList = response.body()!!.data!!.content
                cachedBookings = bookingList
                emit(UiState.Success(bookingList))
            } else {
                val errorMsg = "Failed to fetch bookings"
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    suspend fun initBooking(
        roomId: Long,
        start: String,
        end: String,
        count: Int,
        idempotencyKey: String
    ) = flow {
        emit(UiState.Loading)
        try {
            val request = BookingInitRequest(roomId, start, end, count, idempotencyKey)
            val response = api.initBooking(request)

            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data))
            } else {
                val errorBody = response.body()
                val errorMsg = errorBody?.error?.message ?: "Booking Failed"

                if (response.code() == 400 && errorMsg == "Booking is already initiated.") {
                    emit(UiState.Error("ALREADY_INITIATED"))
                } else if (response.code() == 401) {
                    emit(UiState.SessionExpired)
                } else {
                    emit(UiState.Error(errorMsg))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    suspend fun addGuests(bookingId: Long, guests: List<GuestDto>?): Flow<UiState<BookingData>> = flow {
        emit(UiState.Loading)
        try {
            val response = api.addGuests(bookingId, guests)

            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Failed to add guests"
                if (response.code() == 400) emit(UiState.Error("BAD_REQUEST"))
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    suspend fun getBookingDetails(bookingId: Long) = flow {
        emit(UiState.Loading)
        try {
            val response = api.getBookingDetails(bookingId)
            val data = response.body()?.data

            if (response.isSuccessful && data != null) {
                // Now kotlin knows 'data' is not null
                emit(UiState.Success(data))
            } else {
                val errorMsg = response.body()?.error?.toString() ?: "Failed to fetch details"
                if(response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }
}