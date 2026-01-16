package com.livo.works.screens

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
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
class ResetPassword : AppCompatActivity() {

    private val viewModel: ForgotPasswordViewModel by viewModels()
    private var registrationId: String? = null

    // Views
    private lateinit var resetMainContent: View
    private lateinit var etHiddenOtp: EditText
    private val otpBoxes = ArrayList<TextView>()
    private lateinit var otpContainer: View

    // Timer Views
    private lateinit var timerLayout: View
    private lateinit var tvTimer: TextView
    private lateinit var pbTimer: ProgressBar
    private lateinit var tvResendBtn: TextView

    private lateinit var etPass: TextInputEditText
    private lateinit var etConfirmPass: TextInputEditText
    private lateinit var tilPass: TextInputLayout
    private lateinit var tilConfirmPass: TextInputLayout
    private lateinit var btnReset: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Success
    private lateinit var successContainer: View
    private lateinit var lottieSuccess: LottieAnimationView
    private lateinit var tvSuccessTitle: TextView
    private lateinit var tvSuccessMsg: TextView
    private lateinit var btnBackToLogin: MaterialButton

    // Error Popup
    private lateinit var errorOverlay: View
    private lateinit var tvErrorPopupMsg: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var isErrorState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        initViews()

        registrationId = intent.getStringExtra("REG_ID")
        val nextResend = intent.getLongExtra("RESEND_TIME", 0L)
        val email = intent.getStringExtra("EMAIL")

        if (email != null) {
            findViewById<TextView>(R.id.tvSubtitle).text = "Code sent to $email"
        }

        setupOtpLogic()
        setupListeners()
        observeViewModel()

        viewModel.startTimer(nextResend)

        etHiddenOtp.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etHiddenOtp, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    private fun initViews() {
        resetMainContent = findViewById(R.id.resetMainContent)
        etHiddenOtp = findViewById(R.id.etHiddenOtp)
        otpContainer = findViewById(R.id.otpContainer)

        otpBoxes.add(findViewById(R.id.otpBox1))
        otpBoxes.add(findViewById(R.id.otpBox2))
        otpBoxes.add(findViewById(R.id.otpBox3))
        otpBoxes.add(findViewById(R.id.otpBox4))
        otpBoxes.add(findViewById(R.id.otpBox5))
        otpBoxes.add(findViewById(R.id.otpBox6))

        // Timer
        timerLayout = findViewById(R.id.timerLayout)
        tvTimer = findViewById(R.id.tvTimer)
        pbTimer = findViewById(R.id.pbTimer)
        tvResendBtn = findViewById(R.id.tvResendBtn)

        etPass = findViewById(R.id.etPass)
        etConfirmPass = findViewById(R.id.etConfirmPass)
        tilPass = findViewById(R.id.tilPass)
        tilConfirmPass = findViewById(R.id.tilConfirmPass)
        btnReset = findViewById(R.id.btnReset)
        progressBar = findViewById(R.id.progressBar)

        successContainer = findViewById(R.id.successContainer)
        lottieSuccess = findViewById(R.id.lottieSuccess)
        tvSuccessTitle = findViewById(R.id.tvSuccessTitle)
        tvSuccessMsg = findViewById(R.id.tvSuccessMsg)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        // Error Popup
        errorOverlay = findViewById(R.id.errorOverlay)
        tvErrorPopupMsg = findViewById(R.id.tvErrorPopupMsg)
    }

    private fun setupOtpLogic() {
        etHiddenOtp.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (isErrorState) resetOtpVisuals()

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
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupListeners() {
        btnReset.setOnClickListener {
            val otp = etHiddenOtp.text.toString()
            val pass = etPass.text.toString()
            val confirm = etConfirmPass.text.toString()

            tilPass.error = null
            tilConfirmPass.error = null

            if (otp.length != 6) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
                showOtpError()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                tilPass.error = "Password too short"
                return@setOnClickListener
            }
            if (pass != confirm) {
                tilConfirmPass.error = "Passwords do not match"
                return@setOnClickListener
            }

            if (registrationId != null) {
                viewModel.resetPassword(registrationId!!, otp, pass)
            }
        }

        tvResendBtn.setOnClickListener {
            if (registrationId != null) viewModel.resendOtp(registrationId!!)
        }

        btnBackToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        // TIMER LOGIC
        lifecycleScope.launch {
            viewModel.timerState.collect { timeString ->
                if (timeString == "Resend Available" || timeString == "00:00") {
                    if (tvResendBtn.visibility != View.VISIBLE) {
                        crossFadeViews(timerLayout, tvResendBtn)
                    }
                } else {
                    val cleanTime = timeString.replace("Resend code in ", "").replace("Resend in ", "")
                    tvTimer.text = cleanTime
                    updateTimerProgress(cleanTime)

                    if (timerLayout.visibility != View.VISIBLE) {
                        crossFadeViews(tvResendBtn, timerLayout)
                    }
                }
            }
        }

        // RESET PASSWORD RESPONSE HANDLER
        lifecycleScope.launch {
            viewModel.completeState.collect { state ->
                when(state) {
                    is UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnReset.isEnabled = false
                    }
                    is UiState.Success -> {
                        progressBar.visibility = View.GONE
                        showSuccessAnimation()
                    }
                    is UiState.SessionExpired -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@ResetPassword, "Session Expired. Please Login again.", Toast.LENGTH_LONG).show()
                        navigateToLogin()
                    }
                    is UiState.Error -> {
                        progressBar.visibility = View.GONE
                        btnReset.isEnabled = true

                        val msg = state.message.lowercase()

                        if (msg.contains("limit") || msg.contains("maximum") || msg.contains("exceeded")) {
                            showErrorPopup(state.message)
                        }
                        else {
                            showOtpError()
                            Toast.makeText(this@ResetPassword, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {}
                }
            }
        }

        // RESEND RESPONSE
        lifecycleScope.launch {
            viewModel.resendState.collect { state ->
                if (state is UiState.Loading) {
                    tvResendBtn.isEnabled = false
                    tvResendBtn.alpha = 0.5f
                } else {
                    tvResendBtn.isEnabled = true
                    tvResendBtn.alpha = 1f

                    if (state is UiState.Success) {
                        Toast.makeText(this@ResetPassword, "Code Sent!", Toast.LENGTH_SHORT).show()
                        pbTimer.progress = 100
                    } else if (state is UiState.Error) {
                        val msg = state.message.lowercase()
                        if (msg.contains("limit") || msg.contains("maximum")) {
                            showErrorPopup(state.message)
                        } else {
                            Toast.makeText(this@ResetPassword, state.message, Toast.LENGTH_SHORT).show()
                        }
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

    private fun showSuccessAnimation() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etHiddenOtp.windowToken, 0)

        // Start fading out the old content
        resetMainContent.animate().alpha(0f).setDuration(300).withEndAction {
            resetMainContent.visibility = View.GONE
        }.start()

        lifecycleScope.launch {
            delay(100)

            successContainer.visibility = View.VISIBLE
            successContainer.alpha = 1f

            // 3. START ANIMATION FIRST
            lottieSuccess.playAnimation()

            // 4. WAIT BEFORE SOUND
            delay(1000)

            // 5. PLAY SOUND LATER
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(this@ResetPassword, R.raw.sound_lock)
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(1000)

            tvSuccessTitle.animate().alpha(1f).translationY(-20f).duration = 500
            tvSuccessMsg.animate().alpha(1f).translationY(-20f).duration = 500

            delay(200)
            btnBackToLogin.animate().alpha(1f).scaleX(1f).scaleY(1f).duration = 400
        }
    }

    private fun showErrorPopup(message: String) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etHiddenOtp.windowToken, 0)

        tvErrorPopupMsg.text = message
        errorOverlay.visibility = View.VISIBLE
        errorOverlay.animate().alpha(1f).setDuration(300).start()

        lifecycleScope.launch {
            delay(2500)
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showOtpError() {
        isErrorState = true
        otpBoxes.forEach { it.setBackgroundResource(R.drawable.bg_otp_box_error) }
        ObjectAnimator.ofFloat(otpContainer, "translationX", 0f, 25f, -25f, 25f, -25f, 0f).setDuration(500).start()
    }

    private fun resetOtpVisuals() {
        isErrorState = false
        otpBoxes.forEach { it.setBackgroundResource(R.drawable.bg_otp_box_selector) }
    }

    private fun crossFadeViews(viewToHide: View, viewToShow: View) {
        viewToHide.animate().alpha(0f).setDuration(200).withEndAction {
            viewToHide.visibility = View.GONE
        }
        viewToShow.alpha = 0f
        viewToShow.visibility = View.VISIBLE
        viewToShow.animate().alpha(1f).setDuration(200).start()
    }
}