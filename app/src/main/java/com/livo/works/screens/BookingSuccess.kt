package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.livo.works.databinding.ActivityBookingSuccessBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookingSuccess : AppCompatActivity() {

    private lateinit var binding: ActivityBookingSuccessBinding
    private var bookingId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookingSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get the Booking ID passed from Checkout Screen
        bookingId = intent.getLongExtra("BOOKING_ID", -1)

        binding.successCard.alpha = 0f
        binding.successCard.scaleX = 0.8f
        binding.successCard.scaleY = 0.8f

        startPremiumAnimation()
    }

    private fun startPremiumAnimation() {
        // Step 1: Pop the Card into view
        binding.successCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setStartDelay(100)
            .withEndAction {
                // Step 2: Play Lottie Animation once card is settled
                playLottieAndFeedback()
            }
            .start()
    }

    private fun playLottieAndFeedback() {
        binding.lottieSuccess.playAnimation()

        // Haptic Feedback
        Handler(Looper.getMainLooper()).postDelayed({
            binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }, 600)

        // Fade in Text
        binding.tvTitle.animate().alpha(1f).setDuration(400).setStartDelay(300).start()
        binding.tvSubtitle.animate().alpha(1f).setDuration(400).setStartDelay(400).start()

        // Step 3: Wait 3 seconds, then redirect
        lifecycleScope.launch {
            delay(3000)
            navigateToDetails()
        }
    }

    private fun navigateToDetails() {
        if (bookingId == -1L) {
            // Safety: If ID is missing, go to Dashboard (Bookings Tab) instead of crashing details
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra("OPEN_TAB", "BOOKINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            // Normal Flow: Dashboard -> Booking Details
            val detailsIntent = Intent(this, BookingDetails::class.java)
            detailsIntent.putExtra("BOOKING_ID", bookingId)

            val dashboardIntent = Intent(this, Dashboard::class.java)
            dashboardIntent.putExtra("OPEN_TAB", "BOOKINGS")
            dashboardIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Start both: Dashboard is the "base", Details is on "top"
            startActivities(arrayOf(dashboardIntent, detailsIntent))
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}