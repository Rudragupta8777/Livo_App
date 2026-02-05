package com.livo.works.screens

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.livo.works.Booking.data.BookingDetailsDto
import com.livo.works.R
import com.livo.works.ViewModel.BookingViewModel
import com.livo.works.databinding.ActivityBookingDetailsBinding
import com.livo.works.screens.fragments.BookingCancelledFragment
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class BookingDetails : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailsBinding
    private val viewModel: BookingViewModel by viewModels()
    private val dotAnimators = mutableListOf<ObjectAnimator>()
    private var bookingId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get ID from Intent
        bookingId = intent.getLongExtra("BOOKING_ID", -1)

        if (bookingId != -1L) {
            viewModel.fetchBookingDetails(bookingId)
        } else {
            Toast.makeText(this, "Invalid Booking ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish() // Close Activity
        }

        observeDetails()
        observeCancelState()
    }

    private fun observeDetails() {
        lifecycleScope.launch {
            viewModel.bookingDetailsState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        hideLoading()
                        state.data?.let { setupUI(it) }
                    }
                    is UiState.Error -> {
                        hideLoading()
                        Toast.makeText(this@BookingDetails, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeCancelState() {
        lifecycleScope.launch {
            viewModel.cancelBookingState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        stopAnimationsSafe()
                        viewModel.resetCancelState()

                        // Show Cancelled Fragment inside this Activity or Navigate
                        // Simplest: Show Cancelled Layout overlay or just finish and refresh
                        // Better: Replace content with Cancelled Fragment
                        supportFragmentManager.beginTransaction()
                            .replace(android.R.id.content, BookingCancelledFragment()) // Replaces entire view
                            .commit()
                    }
                    is UiState.Error -> {
                        hideLoading()
                        Toast.makeText(this@BookingDetails, state.message, Toast.LENGTH_SHORT).show()
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

            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val uiFormat = SimpleDateFormat("yyyy", Locale.US)
            val cardDateFormat = SimpleDateFormat("dd MMM", Locale.US)
            var checkInDateObj: java.util.Date? = null

            try {
                val start = apiFormat.parse(data.startDate)
                val originalEnd = apiFormat.parse(data.endDate)
                checkInDateObj = start
                if (start != null && originalEnd != null) {
                    val cal = Calendar.getInstance()
                    cal.time = originalEnd
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val adjustedEnd = cal.time
                    tvCheckInDate.text = cardDateFormat.format(start)
                    tvCheckOutDate.text = cardDateFormat.format(adjustedEnd)
                    tvCheckIn.text = uiFormat.format(start)
                    tvCheckOut.text = uiFormat.format(adjustedEnd)
                }
            } catch (e: Exception) {
                tvCheckInDate.text = data.startDate
                tvCheckOutDate.text = data.endDate
            }

            tvStatus.text = data.bookingStatus
            val isConfirmed = data.bookingStatus == "CONFIRMED"

            var isDateValidForCancel = false
            if (checkInDateObj != null) {
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                if (!today.time.after(checkInDateObj)) {
                    isDateValidForCancel = true
                }
            }

            if (isConfirmed) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed)
                layoutCancelSection.visibility = if (isDateValidForCancel) View.VISIBLE else View.GONE
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_failed)
                layoutCancelSection.visibility = View.GONE
            }

            layoutGuestList.removeAllViews()
            data.guests.forEachIndexed { index, guest ->
                val guestView = TextView(this@BookingDetails)
                guestView.text = "${index + 1}. ${guest.name}(${guest.age} years) - ${guest.gender}"
                guestView.textSize = 15f
                guestView.setTextColor(ContextCompat.getColor(this@BookingDetails, R.color.on_boarding_description))
                guestView.setPadding(0, 8, 0, 8)
                layoutGuestList.addView(guestView)
            }

            btnCancelBooking.setOnClickListener {
                showConfirmationDialog()
            }
        }
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_cancel_booking)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnNo = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNo)
        val btnYes = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnYes)

        btnNo.setOnClickListener { dialog.dismiss() }
        btnYes.setOnClickListener {
            dialog.dismiss()
            if (bookingId != -1L) {
                viewModel.cancelBooking(bookingId)
            }
        }
        dialog.show()
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.loadingOverlay.alpha = 0f
        binding.loadingOverlay.animate().alpha(1f).setDuration(300).start()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR)
            binding.contentScrollView.setRenderEffect(blurEffect)
        }

        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

        dots.forEachIndexed { index, dot ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha)
            animator.duration = 800
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.startDelay = (index * 150).toLong()

            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun hideLoading() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.contentScrollView.setRenderEffect(null)
        }

        binding.loadingOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.loadingOverlay.visibility = View.GONE
            }
            .start()
    }

    private fun stopAnimationsSafe() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
        binding.loadingOverlay.animate().cancel()
        binding.loadingOverlay.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        dotAnimators.forEach { it.cancel() }
    }
}