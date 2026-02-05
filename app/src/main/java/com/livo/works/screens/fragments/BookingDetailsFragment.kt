package com.livo.works.screens.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.livo.works.Booking.data.BookingDetailsDto
import com.livo.works.R
import com.livo.works.ViewModel.BookingViewModel
import com.livo.works.databinding.FragmentBookingDetailsBinding
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class BookingDetailsFragment : Fragment() {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()

    // Arguments
    private var bookingId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Get ID from Arguments
        bookingId = arguments?.getLong("BOOKING_ID") ?: -1

        if (bookingId != -1L) {
            viewModel.fetchBookingDetails(bookingId)
        } else {
            Toast.makeText(context, "Invalid Booking ID", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        // 2. Custom Back Button Listener
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        observeDetails()
    }

    private fun observeDetails() {
        lifecycleScope.launch {
            viewModel.bookingDetailsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        // Safely unwrap data
                        state.data?.let { data ->
                            setupUI(data)
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupUI(data: BookingDetailsDto) {
        binding.apply {
            tvHotelName.text = data.hotelName
            tvAddress.text = data.hotelCity
            tvBookingId.text = "#${data.id}"
            tvRoomType.text = data.roomType
            tvAmount.text = "₹ ${data.amount}"

            // Format Dates
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val uiFormat = SimpleDateFormat("yyyy", Locale.US) // Small Year
            val cardDateFormat = SimpleDateFormat("dd MMM", Locale.US) // Big Date (10 Feb)

            try {
                val start = apiFormat.parse(data.startDate)
                val originalEnd = apiFormat.parse(data.endDate)

                if (start != null && originalEnd != null) {
                    // --- LOGIC: ADD 1 DAY TO CHECKOUT DATE ---
                    val cal = Calendar.getInstance()
                    cal.time = originalEnd
                    cal.add(Calendar.DAY_OF_YEAR, 1) // Add 1 Day
                    val adjustedEnd = cal.time
                    // ----------------------------------------

                    // Big Dates (10 Feb)
                    tvCheckInDate.text = cardDateFormat.format(start)
                    tvCheckOutDate.text = cardDateFormat.format(adjustedEnd) // Use Adjusted End

                    // Small Dates (2026)
                    tvCheckIn.text = uiFormat.format(start)
                    tvCheckOut.text = uiFormat.format(adjustedEnd) // Use Adjusted End
                }
            } catch (e: Exception) {
                // Fallback to raw string if parsing fails
                tvCheckInDate.text = data.startDate
                tvCheckOutDate.text = data.endDate
            }

            // Status Logic
            tvStatus.text = data.bookingStatus
            if (data.bookingStatus == "CONFIRMED") {
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed)
                layoutCancelSection.visibility = View.VISIBLE
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_failed)
                layoutCancelSection.visibility = View.GONE
            }

            layoutGuestList.removeAllViews()

            data.guests.forEachIndexed { index, guest ->
                val guestView = TextView(context)

                // Format: 1. Name(11 years) - MALE
                guestView.text = "${index + 1}. ${guest.name}(${guest.age} years) - ${guest.gender}"

                guestView.textSize = 15f
                guestView.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_boarding_description))
                guestView.setPadding(0, 8, 0, 8) // Add some spacing between rows
                layoutGuestList.addView(guestView)
            }

            // Cancel Button
            btnCancelBooking.setOnClickListener {
                Toast.makeText(context, "Implement Cancel API here", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}