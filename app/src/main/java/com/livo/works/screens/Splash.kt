package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.livo.works.R
import com.livo.works.ViewModel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Splash : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { false }

        // Check internet before proceeding
        if (isNetworkAvailable()) {
            runCinematicAnimation()
            observeDestination()
        } else {
            showNoInternetUI()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun showNoInternetUI() {
        val text = findViewById<TextView>(R.id.tvAppName)
        val logo = findViewById<ImageView>(R.id.ivLogo)

        // Ensure logo is at its default resting position
        logo.translationY = 0f

        // Format the text to look like a clean, centered prompt
        text.text = "No Internet Connection\n\nTap to Retry ↻"
        text.textSize = 16f
        text.alpha = 1f
        text.translationY = 0f

        // Center the multi-line text
        text.gravity = android.view.Gravity.CENTER

        // (Optional) Make it look a bit softer, using your existing color resource
        text.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.on_boarding_description))

        text.setOnClickListener { view ->
            // Add a quick "bounce" animation for tactile feedback
            view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()

                // Check internet after the bounce animation
                if (isNetworkAvailable()) {
                    // Internet is back! Recreate the activity
                    val intent = Intent(this, Splash::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    Toast.makeText(this, "Still no internet connection", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun runCinematicAnimation() {
        val logo = findViewById<ImageView>(R.id.ivLogo)
        val text = findViewById<TextView>(R.id.tvAppName)

        val logoMoveUp = ObjectAnimator.ofFloat(logo, "translationY", -100f).apply {
            duration = 1200
            interpolator = AnticipateOvershootInterpolator(1.5f)
        }

        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 0.9f, 1.05f, 1f).apply {
            duration = 1200
        }
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 0.9f, 1.05f, 1f).apply {
            duration = 1200
        }

        val textFade = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).apply {
            duration = 800
        }

        val textMove = ObjectAnimator.ofFloat(text, "translationY", 50f, -150f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
        }

        val textExpand = ValueAnimator.ofFloat(-0.1f, 0.2f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                text.letterSpacing = animator.animatedValue as Float
            }
        }

        AnimatorSet().apply {
            play(logoMoveUp).with(logoScaleX).with(logoScaleY)
            play(textFade).with(textMove).with(textExpand).after(400)
            start()
        }
    }

    private fun observeDestination() {
        lifecycleScope.launch {
            viewModel.destination.collectLatest { target ->
                if (hasNavigated) return@collectLatest

                target?.let { destination ->
                    hasNavigated = true

                    val intent = when (destination) {
                        SplashViewModel.DESTINATION_ONBOARDING -> Intent(this@Splash, Onboarding::class.java)
                        SplashViewModel.DESTINATION_LOGIN -> Intent(this@Splash, Login::class.java)
                        SplashViewModel.DESTINATION_DASHBOARD -> Intent(this@Splash, Dashboard::class.java)
                        else -> null
                    }

                    intent?.let {
                        startActivity(it)
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
            }
        }
    }
}