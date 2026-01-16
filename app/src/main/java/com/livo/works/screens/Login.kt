package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
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
import com.livo.works.ViewModel.LoginViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Login : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignup: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()

        // Safety: Mark onboarding as completed if user reaches login
        if (sharedPreferences.getBoolean("is_first_time", true)) {
            sharedPreferences.edit()
                .putBoolean("is_first_time", false)
                .apply()
        }

        initializeViews()
        setupClickListeners()
        observeLoginState()
        runEntranceAnimations()
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignup = findViewById(R.id.tvSignup)

        // Set initial alpha for entrance animations
        tvWelcome.alpha = 0f
        tvSubtitle.alpha = 0f
        tilEmail.alpha = 0f
        tilPassword.alpha = 0f
        btnLogin.alpha = 0f
        tvForgotPassword.alpha = 0f
        tvSignup.alpha = 0f
    }


    private fun setupClickListeners() {
        // Login Button
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Clear previous errors
            tilEmail.error = null
            tilPassword.error = null

            // Validation
            var isValid = true

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Enter a valid email"
                isValid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                isValid = false
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                isValid = false
            }

            if (isValid) {
                animateButtonClick(btnLogin)
                viewModel.login(email, password)
            } else {
                // Shake on validation error
                if (tilEmail.error != null) shakeView(tilEmail)
                if (tilPassword.error != null) shakeView(tilPassword)
            }
        }

        // Sign Up Link
        tvSignup.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Forgot Password
        tvForgotPassword.setOnClickListener {
            animateButtonClick(tvForgotPassword)
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showLoading(true)
                    }

                    is UiState.Success -> {
                        showLoading(false)
                        animateSuccess {
                            // Navigate to Dashboard
                            val intent = Intent(this@Login, Dashboard::class.java)
                            startActivity(intent)
                            finish()
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    }

                    is UiState.Error -> {
                        showLoading(false)

                        // Shake inputs on error
                        shakeView(tilEmail)
                        shakeView(tilPassword)

                        Toast.makeText(
                            this@Login,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {
                        // Idle state - do nothing
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            btnLogin.text = ""
            btnLogin.isEnabled = false
            progressBar.visibility = View.VISIBLE
            etEmail.isEnabled = false
            etPassword.isEnabled = false
        } else {
            btnLogin.text = "Login"
            btnLogin.isEnabled = true
            progressBar.visibility = View.GONE
            etEmail.isEnabled = true
            etPassword.isEnabled = true
        }
    }


    private fun runEntranceAnimations() {
        lifecycleScope.launch {
            delay(150)

            // 1. Welcome Title (Fade In + Slide Down)
            ObjectAnimator.ofFloat(tvWelcome, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tvWelcome, "translationY", -50f, 0f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            delay(100)

            // 2. Subtitle
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply { duration = 400; start() }

            delay(150)

            // 3. Email Field (Fade In + Slide from Left)
            ObjectAnimator.ofFloat(tilEmail, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilEmail, "translationX", -50f, 0f).apply { duration = 400; start() }

            delay(100)

            // 4. Password Field (Fade In + Slide from Left)
            ObjectAnimator.ofFloat(tilPassword, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tilPassword, "translationX", -50f, 0f).apply { duration = 400; start() }

            // 5. Forgot Password (FIXED: Fade In + Slide Up)
            // It runs with the Password field for a nice effect
            ObjectAnimator.ofFloat(tvForgotPassword, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tvForgotPassword, "translationY", 20f, 0f).apply { duration = 400; start() }

            delay(100)

            // 6. Login Button (Fade In + Pop Scale)
            ObjectAnimator.ofFloat(btnLogin, "alpha", 0f, 1f).apply { duration = 400; start() }
            val btnScaleX = ObjectAnimator.ofFloat(btnLogin, "scaleX", 0.8f, 1f)
            val btnScaleY = ObjectAnimator.ofFloat(btnLogin, "scaleY", 0.8f, 1f)

            AnimatorSet().apply {
                playTogether(btnScaleX, btnScaleY)
                duration = 400
                interpolator = OvershootInterpolator()
                start()
            }

            // 7. Signup Link (Bottom)
            ObjectAnimator.ofFloat(tvSignup, "alpha", 0f, 1f).apply { duration = 400; start() }
            ObjectAnimator.ofFloat(tvSignup, "translationY", 20f, 0f).apply { duration = 400; start() }
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

        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        ).apply {
            duration = 500
            start()
        }
    }

    private fun animateSuccess(onComplete: () -> Unit) {
        // Success scale animation
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(btnLogin, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(btnLogin, "scaleY", 1f, 1.1f, 1f)
            )
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        lifecycleScope.launch {
            Toast.makeText(this@Login, "✓ Login Successful!", Toast.LENGTH_SHORT).show()
            delay(500)
            onComplete()
        }
    }
}