package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Booking.data.BookingData
import com.livo.works.Booking.data.GuestDto
import com.livo.works.Booking.repository.BookingRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    // 1. Generate Key ONCE when this screen/ViewModel is created.
    // This ensures the key stays the same for this specific visit.
    private val visitIdempotencyKey: String = UUID.randomUUID().toString()
    private val _bookingState = MutableStateFlow<UiState<BookingData>>(UiState.Idle)
    val bookingState: StateFlow<UiState<BookingData>> = _bookingState

    fun initiateBooking(roomId: Long, start: String, end: String, rooms: Int) {
        viewModelScope.launch {
            // 2. Use the visit-specific key
            repository.initBooking(roomId, start, end, rooms, visitIdempotencyKey)
                .collect { state ->
                    _bookingState.value = state
                }
        }
    }

    fun resetBookingState() {
        _bookingState.value = UiState.Idle
    }

    private val _addGuestState = MutableStateFlow<UiState<BookingData>>(UiState.Idle)
    val addGuestState: StateFlow<UiState<BookingData>> = _addGuestState

    fun addGuests(bookingId: Long, guests: List<GuestDto>?) {
        viewModelScope.launch {
            repository.addGuests(bookingId, guests)
                .collect { state ->
                    _addGuestState.value = state
                }
        }
    }

    fun resetAddGuestState() {
        _addGuestState.value = UiState.Idle
    }
}