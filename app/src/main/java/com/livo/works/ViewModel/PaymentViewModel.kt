package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Booking.data.GuestDto
import com.livo.works.Booking.repository.BookingRepository
import com.livo.works.Payment.data.PaymentInitData
import com.livo.works.Payment.data.PaymentVerifyRequest
import com.livo.works.Payment.repository.PaymentRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository // Injected BookingRepo
) : ViewModel() {

    private val paymentIdempotencyKey = UUID.randomUUID().toString()

    private val _initParamsState = MutableStateFlow<UiState<PaymentInitData>>(UiState.Idle)
    val initParamsState: StateFlow<UiState<PaymentInitData>> = _initParamsState

    private val _verifyState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val verifyState: StateFlow<UiState<Boolean>> = _verifyState

    // NEW: Combined Flow
    fun processBookingAndPayment(bookingId: Long, guests: List<GuestDto>) {
        viewModelScope.launch {
            _initParamsState.value = UiState.Loading // Start Loading UI

            // Step 1: Add Guests
            bookingRepository.addGuests(bookingId, guests).collect { guestState ->
                when (guestState) {
                    is UiState.Success -> {
                        // Step 2: Guests added, now Init Payment
                        // We call the internal suspend function
                        initiatePaymentInternal(bookingId)
                    }
                    is UiState.Error -> {
                        _initParamsState.value = UiState.Error(guestState.message)
                    }
                    // Ignore Loading state from addGuests to avoid flickering
                    else -> {}
                }
            }
        }
    }

    // Helper for Init Payment
    private suspend fun initiatePaymentInternal(bookingId: Long) {
        paymentRepository.initPayment(bookingId, paymentIdempotencyKey).collect { paymentState ->
            _initParamsState.value = paymentState
        }
    }

    // For manual retry if needed (optional)
    fun startPayment(bookingId: Long) {
        viewModelScope.launch { initiatePaymentInternal(bookingId) }
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String) {
        val request = PaymentVerifyRequest(orderId, paymentId, signature)
        viewModelScope.launch {
            paymentRepository.verifyPayment(request).collect {
                _verifyState.value = it
            }
        }
    }
}