package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.livo.works.databinding.ActivityBookingSuccessBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingSuccess : AppCompatActivity() {

    private lateinit var binding: ActivityBookingSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookingSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.successCard.alpha = 0f
        binding.successCard.scaleX = 0.8f
        binding.successCard.scaleY = 0.8f

        binding.btnGoHome.translationY = 200f
        binding.btnGoHome.alpha = 0f

        setupListeners()
        startPremiumAnimation()
    }

    private fun setupListeners() {
        binding.btnGoHome.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra("OPEN_TAB", "BOOKINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
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

        // Step 3: Timed Vibration (Sync with checkmark appearing)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }, 600) // Adjust this delay to match when the checkmark actually "hits" in your specific JSON

        // Step 4: Fade in Text
        binding.tvTitle.animate().alpha(1f).setDuration(400).setStartDelay(300).start()
        binding.tvSubtitle.animate().alpha(1f).setDuration(400).setStartDelay(400).start()

        // Step 5: Slide up Button
        binding.btnGoHome.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator())
            .setStartDelay(800)
            .start()
    }
}