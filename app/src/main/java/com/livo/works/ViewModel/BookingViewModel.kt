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

    // Pagination specific state for the bottom progress bar
    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    // Pagination Variables
    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false

    init {
        fetchMyBookings(forceRefresh = false)
    }

    fun fetchMyBookings(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            currentPage = 0
            isLastPage = false
        }

        // Prevent overlapping calls when scrolling fast or if we hit the end
        if (isLoadingMore || (isLastPage && !forceRefresh)) return

        isLoadingMore = true

        // Show bottom progress bar if it's a pagination call (not page 0)
        if (currentPage > 0 && !forceRefresh) {
            _isPaginating.value = true
        }

        viewModelScope.launch {
            repository.getMyBookings(forceRefresh, currentPage).collect { state ->
                _myBookingsState.value = state

                if (state is UiState.Success) {
                    // Since your repository returns the accumulated List<BookingSummaryDto> directly,
                    // we need a way to know if we hit the last page.
                    // The simplest robust heuristic without changing the API signature is:
                    // if the data size is a multiple of page size (10), we *might* have more.
                    // If it's not a multiple of 10, or it's exactly the same size as before, we are at the end.
                    // Since we don't have the previous size easily available here, we'll increment currentPage
                    // and rely on the repository's behavior.
                    // IDEALLY: The repository should return the PageInfo object to make this bulletproof.

                    currentPage++
                    isLoadingMore = false
                    _isPaginating.value = false
                }

                if (state is UiState.Error || state is UiState.SessionExpired) {
                    isLoadingMore = false
                    _isPaginating.value = false
                }
            }
        }
    }

    fun loadNextPage() {
        if (!isLastPage && !isLoadingMore) {
            fetchMyBookings(forceRefresh = false)
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