package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Booking.data.BookingData
import com.livo.works.Booking.data.BookingDetailsDto
import com.livo.works.Booking.data.BookingSummaryDto
import com.livo.works.Booking.data.GuestDto
import com.livo.works.Booking.repository.BookingRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _myBookingsState = MutableStateFlow<UiState<List<BookingSummaryDto>>>(UiState.Loading)
    val myBookingsState: StateFlow<UiState<List<BookingSummaryDto>>> = _myBookingsState

    init {
        fetchMyBookings(forceRefresh = false)
    }

    fun fetchMyBookings(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getMyBookings(forceRefresh).collect {
                _myBookingsState.value = it
            }
        }
    }

    private val _bookingDetailsState = MutableStateFlow<UiState<BookingDetailsDto>>(UiState.Loading)
    val bookingDetailsState = _bookingDetailsState.asStateFlow()

    fun fetchBookingDetails(bookingId: Long) {
        viewModelScope.launch {
            repository.getBookingDetails(bookingId).collect {
                _bookingDetailsState.value = it
            }
        }
    }

    private val _cancelBookingState = MutableStateFlow<UiState<BookingDetailsDto>>(UiState.Idle)
    val cancelBookingState = _cancelBookingState.asStateFlow()

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId).collect {
                _cancelBookingState.value = it
            }
        }
    }

    fun resetCancelState() {
        _cancelBookingState.value = UiState.Idle
    }

    private val visitIdempotencyKey: String = UUID.randomUUID().toString()
    private val _bookingState = MutableStateFlow<UiState<BookingData>>(UiState.Idle)
    val bookingState: StateFlow<UiState<BookingData>> = _bookingState

    private val _addGuestState = MutableStateFlow<UiState<BookingData>>(UiState.Idle)
    val addGuestState: StateFlow<UiState<BookingData>> = _addGuestState

    fun initiateBooking(roomId: Long, start: String, end: String, rooms: Int) {
        viewModelScope.launch {
            repository.initBooking(roomId, start, end, rooms, visitIdempotencyKey)
                .collect { _bookingState.value = it }
        }
    }

    fun addGuests(bookingId: Long, guests: List<GuestDto>?) {
        viewModelScope.launch {
            repository.addGuests(bookingId, guests)
                .collect { _addGuestState.value = it }
        }
    }

    fun resetBookingState() {
        _bookingState.value = UiState.Idle
    }
    fun resetAddGuestState() {
        _addGuestState.value = UiState.Idle
    }
}