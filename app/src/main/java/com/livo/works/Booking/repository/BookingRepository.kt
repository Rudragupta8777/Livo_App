package com.livo.works.Booking.repository

import com.livo.works.Api.BookingApiService
import com.livo.works.Booking.data.BookingData
import com.livo.works.Booking.data.BookingInitRequest
import com.livo.works.Booking.data.GuestDto
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BookingRepository @Inject constructor(
    private val api: BookingApiService
) {

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
                // Success: 200 OK
                emit(UiState.Success(response.body()!!.data))
            } else {
                // Error: 400 Bad Request, 401, etc.
                val errorBody = response.body()
                val errorMsg = errorBody?.error?.message ?: "Booking Failed"

                // IMPORTANT: Pass the error code too, so ViewModel can check for 400
                if (response.code() == 400 && errorMsg == "Booking is already initiated.") {
                    // Special State for "Already Exists" if you want to handle it specifically
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
                // Success: 200 OK
                emit(UiState.Success(response.body()!!.data))
            } else {
                // Handle Errors
                val errorBody = response.body()
                val errorMsg = errorBody?.error?.message ?: "Failed to add guests"

                // Check specifically for 400 Bad Request
                if (response.code() == 400) {
                    emit(UiState.Error("BAD_REQUEST")) // Specific flag for ViewModel
                } else {
                    emit(UiState.Error(errorMsg))
                }
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }
}