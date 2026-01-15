package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
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
import com.livo.works.ViewModel.SignupViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Signup : AppCompatActivity() {

    private val viewModel: SignupViewModel by viewModels()

    // Views
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        enableEdgeToEdge()

        initializeViews()
        setupClickListeners()
        observeSignupState()
        runEntranceAnimations()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignup = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)

        // Initial Alpha for Animations
        tvTitle.alpha = 0f
        tvSubtitle.alpha = 0f
        tilName.alpha = 0f
        tilEmail.alpha = 0f
        tilPassword.alpha = 0f
        btnSignup.alpha = 0f
    }

    private fun setupClickListeners() {
        // Signup Button
        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Clear previous errors
            tilName.error = null
            tilEmail.error = null
            tilPassword.error = null

            var isValid = true

            // Name Validation
            if (name.isEmpty()) {
                tilName.error = "Name is required"
                isValid = false
            } else if (name.length < 2) {
                tilName.error = "Name is too short"
                isValid = false
            }

            // Email Validation
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"
                isValid = false
            }

            // Password Validation
            if (pass.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            } else if (pass.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (isValid) {
                animateButtonClick(btnSignup)
                viewModel.initiateSignup(name, email, pass)
            } else {
                // Shake specific invalid fields
                if (tilName.error != null) shakeView(tilName)
                if (tilEmail.error != null) shakeView(tilEmail)
                if (tilPassword.error != null) shakeView(tilPassword)
            }
        }

        // Login Text Link
        tvLogin.setOnClickListener {
            // Navigate back to Login
            val intent = Intent(this, Login::class.java)
            // Clear top ensures if we came from Login, we don't stack multiple Login activities
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish() // Close Signup
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun observeSignupState() {
        lifecycleScope.launch {
            viewModel.signupState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showLoading(true)
                    }

                    is UiState.Success -> {
                        showLoading(false)
                        animateSuccess {
                            val response = state.data
                            val intent = Intent(this@Signup, OtpVerification::class.java).apply {
                                putExtra("REG_ID", response?.registrationId)
                                putExtra("RESEND_TIME", response?.nextResendAt)
                                putExtra("EMAIL", etEmail.text.toString())
                            }
                            startActivity(intent)
                        }
                    }

                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@Signup, state.message, Toast.LENGTH_LONG).show()
                        // Shake the button to indicate failure
                        shakeView(btnSignup)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            btnSignup.text = ""
            btnSignup.isEnabled = false
            progressBar.visibility = View.VISIBLE
            etName.isEnabled = false
            etEmail.isEnabled = false
            etPassword.isEnabled = false
        } else {
            btnSignup.text = "Continue"
            btnSignup.isEnabled = true
            progressBar.visibility = View.GONE
            etName.isEnabled = true
            etEmail.isEnabled = true
            etPassword.isEnabled = true
        }
    }

    private fun runEntranceAnimations() {
        lifecycleScope.launch {
            delay(150)

            // Title
            ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f).apply {
                duration = 400
                start()
            }
            ObjectAnimator.ofFloat(tvTitle, "translationY", -20f, 0f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            delay(100)
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply { duration = 400; start() }

            delay(100)
            // Name Field
            ObjectAnimator.ofFloat(tilName, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilName, "translationX", -30f, 0f).apply { duration = 400; start() }

            delay(100)
            // Email Field
            ObjectAnimator.ofFloat(tilEmail, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilEmail, "translationX", -30f, 0f).apply { duration = 400; start() }

            delay(100)
            // Password Field
            ObjectAnimator.ofFloat(tilPassword, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilPassword, "translationX", -30f, 0f).apply { duration = 400; start() }

            delay(100)
            // Button
            ObjectAnimator.ofFloat(btnSignup, "alpha", 0f, 1f).apply { duration = 400; start() }
            val btnScaleX = ObjectAnimator.ofFloat(btnSignup, "scaleX", 0.8f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator()
            }
            val btnScaleY = ObjectAnimator.ofFloat(btnSignup, "scaleY", 0.8f, 1f).apply {
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
                ObjectAnimator.ofFloat(btnSignup, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(btnSignup, "scaleY", 1f, 1.1f, 1f)
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