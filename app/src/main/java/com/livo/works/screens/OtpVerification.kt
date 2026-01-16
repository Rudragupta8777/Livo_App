package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
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
    private lateinit var otpMainContent: ConstraintLayout
    private lateinit var ivLockIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var timerLayout: FrameLayout
    private lateinit var tvTimer: TextView
    private lateinit var pbTimer: ProgressBar
    private lateinit var tvResendBtn: TextView
    private lateinit var btnVerify: MaterialButton
    private lateinit var otpContainer: View
    private lateinit var etHiddenOtp: EditText
    private val otpBoxes = ArrayList<TextView>()

    // Success Animation Views
    private lateinit var successContainer: ConstraintLayout
    private lateinit var lottieSuccess: LottieAnimationView
    private lateinit var tvSuccessTitle: TextView
    private lateinit var tvSuccessMessage: TextView
    private lateinit var btnContinueToLogin: MaterialButton

    // Sound Player
    private var mediaPlayer: MediaPlayer? = null
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

        viewModel.startTimer(nextResendAt)
        runEntranceAnimations()

        etHiddenOtp.requestFocus()
        showKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun initializeViews() {
        otpMainContent = findViewById(R.id.otpMainContent)
        ivLockIcon = findViewById(R.id.ivLockIcon)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        timerLayout = findViewById(R.id.timerLayout)
        tvTimer = findViewById(R.id.tvTimer)
        pbTimer = findViewById(R.id.pbTimer)
        tvResendBtn = findViewById(R.id.tvResendBtn)
        btnVerify = findViewById(R.id.btnVerify)
        etHiddenOtp = findViewById(R.id.etHiddenOtp)
        otpContainer = findViewById(R.id.otpContainer)

        otpBoxes.add(findViewById(R.id.otpBox1))
        otpBoxes.add(findViewById(R.id.otpBox2))
        otpBoxes.add(findViewById(R.id.otpBox3))
        otpBoxes.add(findViewById(R.id.otpBox4))
        otpBoxes.add(findViewById(R.id.otpBox5))
        otpBoxes.add(findViewById(R.id.otpBox6))

        successContainer = findViewById(R.id.successContainer)
        lottieSuccess = findViewById(R.id.lottieSuccess)
        tvSuccessTitle = findViewById(R.id.tvSuccessTitle)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)
        btnContinueToLogin = findViewById(R.id.btnContinueToLogin)

        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            tvSubtitle.text = "We have sent the code verification to\n$email"
        }

        timerLayout.visibility = View.VISIBLE
        tvResendBtn.visibility = View.GONE

        // Initial Alpha for animations
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

                if (isErrorState) {
                    resetOtpBoxVisuals()
                }

                for (i in otpBoxes.indices) {
                    if (i < text.length) {
                        otpBoxes[i].text = text[i].toString()
                        otpBoxes[i].isSelected = true
                        otpBoxes[i].isActivated = false
                    } else if (i == text.length) {
                        otpBoxes[i].text = ""
                        otpBoxes[i].isSelected = false
                        otpBoxes[i].isActivated = true
                    } else {
                        otpBoxes[i].text = ""
                        otpBoxes[i].isSelected = false
                        otpBoxes[i].isActivated = false
                    }
                }

                if (text.length == 6) {
                    hideKeyboard()
                }
            }
        })
    }

    private fun setupClickListeners() {
        btnVerify.setOnClickListener {
            val otp = etHiddenOtp.text.toString()
            if (otp.length == 6 && registrationId != null) {
                animateButtonClick(btnVerify)
                viewModel.verifyOtp(registrationId!!, otp)
            } else {
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

        btnContinueToLogin.setOnClickListener {
            animateButtonClick(btnContinueToLogin)
            lifecycleScope.launch {
                delay(200)
                navigateToLogin()
            }
        }
    }

    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.verifyState.collect { state ->
                handleUiState(state, isVerifyAction = true)
            }
        }

        lifecycleScope.launch {
            viewModel.resendState.collect { state ->
                handleUiState(state, isVerifyAction = false)
            }
        }

        lifecycleScope.launch {
            viewModel.timerState.collect { timeString ->
                if (timeString == "Resend Available" || timeString == "00:00") {
                    if (tvResendBtn.visibility != View.VISIBLE) {
                        crossFadeViews(timerLayout, tvResendBtn)
                    }
                } else {
                    val cleanTime = timeString.replace("Resend code in ", "")
                    tvTimer.text = cleanTime
                    updateTimerProgress(cleanTime)

                    if (timerLayout.visibility != View.VISIBLE) {
                        crossFadeViews(tvResendBtn, timerLayout)
                    }
                }
            }
        }
    }

    private fun updateTimerProgress(time: String) {
        try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val seconds = parts[0].toInt() * 60 + parts[1].toInt()
                val maxSeconds = 60f
                val progress = ((seconds / maxSeconds) * 100).toInt()
                pbTimer.setProgress(progress, true)
            }
        } catch (e: Exception) {
            pbTimer.progress = 0
        }
    }

    private fun handleUiState(state: UiState<*>, isVerifyAction: Boolean) {
        when (state) {
            is UiState.Loading -> {
                if (isVerifyAction) {
                    btnVerify.text = ""
                    btnVerify.isEnabled = false
                } else {
                    tvResendBtn.isEnabled = false
                    tvResendBtn.alpha = 0.5f
                }
            }
            is UiState.Success -> {
                if (isVerifyAction) {
                    showSuccessAnimation()
                } else {
                    Toast.makeText(this, "Code sent!", Toast.LENGTH_SHORT).show()
                    tvResendBtn.isEnabled = true
                    tvResendBtn.alpha = 1f
                    pbTimer.progress = 100
                }
            }
            is UiState.SessionExpired -> {
                redirectToSignup("Session Expired")
            }
            is UiState.Error -> {
                val msg = state.message.lowercase()
                if (msg.contains("limit") || msg.contains("maximum") || msg.contains("exceeded")) {
                    redirectToSignup(state.message)
                    return
                }
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

    // ============ FIXED TIMING LOGIC ============
    private fun showSuccessAnimation() {
        hideKeyboard()

        lifecycleScope.launch {
            // 1. Fade out OTP content
            animateOutOtpContent()

            // 2. Wait for Fade out to finish
            delay(400)

            // 3. Show container
            successContainer.visibility = View.VISIBLE
            successContainer.alpha = 1f

            // 4. Play Sound & Animation TOGETHER
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(this@OtpVerification, R.raw.sound_success)
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lottieSuccess.playAnimation()

            // 5. Wait for Lottie to finish before showing Text
            delay(lottieSuccess.duration)
            animateSuccessText()
        }
    }

    private fun animateOutOtpContent() {
        otpMainContent.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateSuccessText() {
        // Title slide up
        tvSuccessTitle.translationY = 30f
        tvSuccessTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        lifecycleScope.launch {
            delay(150)
            // Message fade in
            tvSuccessMessage.animate()
                .alpha(1f)
                .setDuration(400)
                .start()

            delay(200)
            // Button scale in
            btnContinueToLogin.scaleX = 0.8f
            btnContinueToLogin.scaleY = 0.8f
            btnContinueToLogin.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    // ============ HELPER METHODS ============
    private fun redirectToSignup(reason: String) {
        Toast.makeText(this, "$reason. Please Signup again.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, Signup::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showErrorVisuals() {
        isErrorState = true
        otpBoxes.forEach { box ->
            box.setBackgroundResource(R.drawable.bg_otp_box_error)
        }
        shakeView(otpContainer)
    }

    private fun resetOtpBoxVisuals() {
        isErrorState = false
        otpBoxes.forEach { box ->
            box.setBackgroundResource(R.drawable.bg_otp_box_selector)
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

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etHiddenOtp, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etHiddenOtp.windowToken, 0)
    }
}