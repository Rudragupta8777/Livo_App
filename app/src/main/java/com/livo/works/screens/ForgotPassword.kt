package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.livo.works.R
import com.livo.works.ViewModel.ForgotPasswordViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPassword : AppCompatActivity() {
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var loadingOverlay: View
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private val dotAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        enableEdgeToEdge()

        initializeViews()
        setupListeners()
        observeViewModel()
        runEntranceAnimations()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnSend = findViewById(R.id.btnSend)

        loadingOverlay = findViewById(R.id.loadingOverlay)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        dot4 = findViewById(R.id.dot4)

        // Initial Alpha for Animations
        tvTitle.alpha = 0f
        tvSubtitle.alpha = 0f
        tilEmail.alpha = 0f
        btnSend.alpha = 0f
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()

            // Clear previous errors
            tilEmail.error = null

            // Validation
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                shakeView(tilEmail)
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"
                shakeView(tilEmail)
            } else {
                animateButtonClick(btnSend)
                viewModel.sendOtp(email)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.initiateState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showLoading(true)
                    }
                    is UiState.Success -> {
                        showLoading(false)

                        val prefs = getSharedPreferences("livo_auth", Context.MODE_PRIVATE)
                        prefs.edit().putString("REG_ID", state.data?.registrationId).apply()

                        animateSuccess {
                            val intent = Intent(this@ForgotPassword, ResetPassword::class.java)
                            intent.putExtra("REG_ID", state.data?.registrationId)
                            intent.putExtra("RESEND_TIME", state.data?.nextResendAt)
                            intent.putExtra("EMAIL", etEmail.text.toString())
                            startActivity(intent)
                        }
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        shakeView(tilEmail) // Shake the input on API error
                        Toast.makeText(this@ForgotPassword, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            btnSend.text = ""
            btnSend.isEnabled = false
            etEmail.isEnabled = false

            loadingOverlay.visibility = View.VISIBLE
            loadingOverlay.alpha = 0f
            loadingOverlay.animate().alpha(1f).setDuration(300).start()
            startDotAnimation()
        } else {
            btnSend.text = "Send Code"
            btnSend.isEnabled = true
            etEmail.isEnabled = true

            stopDotAnimation()
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    loadingOverlay.visibility = View.GONE
                }
                .start()
        }
    }

    private fun startDotAnimation() {
        val dots = listOf(dot1, dot2, dot3, dot4)
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

    private fun stopDotAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDotAnimation()
    }

    private fun runEntranceAnimations() {
        lifecycleScope.launch {
            delay(150)

            // Title
            ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tvTitle, "translationY", -20f, 0f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            delay(100)
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply { duration = 400; start() }

            delay(100)
            // Email Field
            ObjectAnimator.ofFloat(tilEmail, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilEmail, "translationX", -30f, 0f).apply { duration = 400; start() }

            delay(100)
            // Button
            ObjectAnimator.ofFloat(btnSend, "alpha", 0f, 1f).apply { duration = 400; start() }
            val btnScaleX = ObjectAnimator.ofFloat(btnSend, "scaleX", 0.8f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator()
            }
            val btnScaleY = ObjectAnimator.ofFloat(btnSend, "scaleY", 0.8f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator()
            }
            AnimatorSet().apply {
                playTogether(btnScaleX, btnScaleY)
                start()
            }
        }
    }

    private fun animateButtonClick(view: View) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = 100
        }
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 100
        }
        AnimatorSet().apply { playSequentially(scaleDown, scaleUp); start() }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(
            view, "translationX",
            0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        ).apply {
            duration = 500
            start()
        }
    }

    private fun animateSuccess(onComplete: () -> Unit) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(btnSend, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(btnSend, "scaleY", 1f, 1.1f, 1f)
            )
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        lifecycleScope.launch {
            delay(300)
            onComplete()
        }
    }
}