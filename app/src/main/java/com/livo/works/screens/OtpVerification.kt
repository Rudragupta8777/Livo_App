package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.livo.works.R
import com.livo.works.ViewModel.OtpViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OtpVerification : AppCompatActivity() {

    private val viewModel: OtpViewModel by viewModels()
    private var registrationId: String? = null
    private lateinit var ivLockIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var timerLayout: FrameLayout
    private lateinit var tvTimer: TextView
    private lateinit var pbTimer: ProgressBar
    private lateinit var tvResendBtn: TextView
    private lateinit var btnVerify: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var otpContainer: View // For shaking the whole container

    // OTP Input
    private lateinit var etHiddenOtp: EditText
    private val otpBoxes = ArrayList<TextView>()

    // State tracking
    private var isErrorState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)
        enableEdgeToEdge()

        initializeViews()

        registrationId = intent.getStringExtra("REG_ID")
        val nextResendAt = intent.getLongExtra("RESEND_TIME", 0L)

        setupOtpInputLogic()
        setupClickListeners()
        observeStates()

        // Start Timer
        viewModel.startTimer(nextResendAt)

        runEntranceAnimations()

        // Show Keyboard immediately
        etHiddenOtp.requestFocus()
        showKeyboard()
    }

    private fun initializeViews() {
        ivLockIcon = findViewById(R.id.ivLockIcon)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        timerLayout = findViewById(R.id.timerLayout)
        tvTimer = findViewById(R.id.tvTimer)
        pbTimer = findViewById(R.id.pbTimer)
        tvResendBtn = findViewById(R.id.tvResendBtn)
        btnVerify = findViewById(R.id.btnVerify)
        progressBar = findViewById(R.id.progressBar)
        etHiddenOtp = findViewById(R.id.etHiddenOtp)
        otpContainer = findViewById(R.id.otpContainer)

        // Bind the 6 boxes
        otpBoxes.add(findViewById(R.id.otpBox1))
        otpBoxes.add(findViewById(R.id.otpBox2))
        otpBoxes.add(findViewById(R.id.otpBox3))
        otpBoxes.add(findViewById(R.id.otpBox4))
        otpBoxes.add(findViewById(R.id.otpBox5))
        otpBoxes.add(findViewById(R.id.otpBox6))

        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            tvSubtitle.text = "We have sent the code verification to\n$email"
        }

        timerLayout.visibility = View.VISIBLE
        tvResendBtn.visibility = View.GONE

        // Prepare animations
        ivLockIcon.alpha = 0f
        tvTitle.alpha = 0f
        tvSubtitle.alpha = 0f
        btnVerify.alpha = 0f
        timerLayout.alpha = 0f
    }

    private fun setupOtpInputLogic() {
        etHiddenOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()

                // 1. Reset Error State if user types
                if (isErrorState) {
                    resetOtpBoxVisuals()
                }

                // 2. Update boxes based on hidden input
                for (i in otpBoxes.indices) {
                    if (i < text.length) {
                        otpBoxes[i].text = text[i].toString()
                        otpBoxes[i].isSelected = true   // Filled
                        otpBoxes[i].isActivated = false
                    } else if (i == text.length) {
                        otpBoxes[i].text = ""
                        otpBoxes[i].isSelected = false
                        otpBoxes[i].isActivated = true  // Cursor
                    } else {
                        otpBoxes[i].text = ""
                        otpBoxes[i].isSelected = false
                        otpBoxes[i].isActivated = false
                    }
                }

                // Removed Auto-Click Logic here as requested
                if (text.length == 6) {
                    hideKeyboard()
                }
            }
        })
    }

    private fun setupClickListeners() {
        btnVerify.setOnClickListener {
            val otp = etHiddenOtp.text.toString()

            // Check length
            if (otp.length == 6 && registrationId != null) {
                animateButtonClick(btnVerify)
                viewModel.verifyOtp(registrationId!!, otp)
            } else {
                // Shake if empty or incomplete
                showErrorVisuals()
                Toast.makeText(this, "Please enter full 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        tvResendBtn.setOnClickListener {
            if (registrationId != null) {
                animateButtonClick(tvResendBtn)
                viewModel.resendOtp(registrationId!!)
            }
        }

        otpContainer.setOnClickListener {
            etHiddenOtp.requestFocus()
            showKeyboard()
        }
    }

    private fun observeStates() {
        // 1. Verify State
        lifecycleScope.launch {
            viewModel.verifyState.collect { state ->
                handleUiState(state, isVerifyAction = true)
            }
        }

        // 2. Resend State
        lifecycleScope.launch {
            viewModel.resendState.collect { state ->
                handleUiState(state, isVerifyAction = false)
            }
        }

        // 3. Timer State (With Progress Bar Logic)
        lifecycleScope.launch {
            viewModel.timerState.collect { timeString ->
                if (timeString == "Resend Available" || timeString == "00:00") {
                    if (tvResendBtn.visibility != View.VISIBLE) {
                        crossFadeViews(timerLayout, tvResendBtn)
                    }
                } else {
                    // timeString format: "Resend code in 00:45"
                    val cleanTime = timeString.replace("Resend code in ", "")
                    tvTimer.text = cleanTime

                    // CALCULATE PROGRESS
                    updateTimerProgress(cleanTime)

                    if (timerLayout.visibility != View.VISIBLE) {
                        crossFadeViews(tvResendBtn, timerLayout)
                    }
                }
            }
        }
    }

    private fun updateTimerProgress(time: String) {
        // Parse "MM:SS" (e.g., "00:45")
        try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val seconds = parts[0].toInt() * 60 + parts[1].toInt()
                // Assuming max timer is 60 seconds (or logic in VM).
                // We map 60s -> 100 progress, 0s -> 0 progress
                val maxSeconds = 60f
                val progress = ((seconds / maxSeconds) * 100).toInt()

                // Animate the progress change
                pbTimer.setProgress(progress, true)
            }
        } catch (e: Exception) {
            pbTimer.progress = 0
        }
    }

    private fun handleUiState(state: UiState<*>, isVerifyAction: Boolean) {
        when (state) {
            is UiState.Loading -> {
                progressBar.visibility = View.VISIBLE
                if (isVerifyAction) {
                    btnVerify.text = ""
                    btnVerify.isEnabled = false
                } else {
                    tvResendBtn.isEnabled = false
                    tvResendBtn.alpha = 0.5f
                }
            }
            is UiState.Success -> {
                progressBar.visibility = View.GONE
                if (isVerifyAction) {
                    animateSuccess()
                } else {
                    Toast.makeText(this, "Code sent!", Toast.LENGTH_SHORT).show()
                    tvResendBtn.isEnabled = true
                    tvResendBtn.alpha = 1f
                    pbTimer.progress = 100
                }
            }
            is UiState.SessionExpired -> {
                // Handle 401/403 from Backend
                redirectToSignup("Session Expired")
            }
            is UiState.Error -> {
                progressBar.visibility = View.GONE

                // --- FIX: Check for Limit/Max errors here ---
                val msg = state.message.lowercase()
                if (msg.contains("limit") || msg.contains("maximum") || msg.contains("exceeded")) {
                    redirectToSignup(state.message) // Pass the actual error ("Limit reached")
                    return
                }
                // ---------------------------------------------

                if (isVerifyAction) {
                    btnVerify.text = "Verify Now"
                    btnVerify.isEnabled = true
                    showErrorVisuals()
                } else {
                    tvResendBtn.isEnabled = true
                    tvResendBtn.alpha = 1f
                }
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // Helper function to avoid code duplication
    private fun redirectToSignup(reason: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, "$reason. Please Signup again.", Toast.LENGTH_LONG).show()

        val intent = Intent(this, Signup::class.java)
        // Clear the back stack so they can't go back to OTP screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showErrorVisuals() {
        isErrorState = true
        // 1. Change background to RED
        otpBoxes.forEach { box ->
            box.setBackgroundResource(R.drawable.bg_otp_box_error)
        }
        // 2. Shake the container
        shakeView(otpContainer)
    }

    private fun resetOtpBoxVisuals() {
        isErrorState = false
        // Reset to original selector (handles focus/filled states)
        otpBoxes.forEach { box ->
            box.setBackgroundResource(R.drawable.bg_otp_box_selector) // Make sure this matches your original drawable name
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
            duration = 500
            start()
        }
    }

    private fun crossFadeViews(viewToHide: View, viewToShow: View) {
        viewToHide.animate().alpha(0f).setDuration(200).withEndAction {
            viewToHide.visibility = View.GONE
        }
        viewToShow.alpha = 0f
        viewToShow.visibility = View.VISIBLE
        viewToShow.animate().alpha(1f).setDuration(200).start()
    }

    private fun runEntranceAnimations() {
        val set = AnimatorSet()
        set.playSequentially(
            ObjectAnimator.ofFloat(ivLockIcon, "translationY", -50f, 0f).apply { duration = 400 },
            ObjectAnimator.ofFloat(ivLockIcon, "alpha", 0f, 1f).apply { duration = 300 },
            ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f).apply { duration = 300 },
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply { duration = 300 },
            ObjectAnimator.ofFloat(timerLayout, "alpha", 0f, 1f).apply { duration = 300 },
            ObjectAnimator.ofFloat(btnVerify, "translationY", 100f, 0f).apply { duration = 400 },
            ObjectAnimator.ofFloat(btnVerify, "alpha", 0f, 1f).apply { duration = 300 }
        )
        set.start()
    }

    private fun animateButtonClick(view: View) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f)
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            duration = 100
            start()
        }
    }

    private fun animateSuccess() {
        // Success Animation
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(btnVerify, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(btnVerify, "scaleY", 1f, 1.1f, 1f)
            )
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        lifecycleScope.launch {
            Toast.makeText(this@OtpVerification, "Verification Successful!", Toast.LENGTH_SHORT).show()
            delay(500)
            val intent = Intent(this@OtpVerification, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etHiddenOtp, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etHiddenOtp.windowToken, 0)
    }
}