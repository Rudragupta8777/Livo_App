package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.livo.works.R
import com.livo.works.ViewModel.OtpViewModel
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OtpVerification : AppCompatActivity() {

    private val viewModel: OtpViewModel by viewModels()
    private var registrationId: String? = null

    private lateinit var tvTimer: TextView
    private lateinit var btnResend: Button
    private lateinit var btnVerify: Button
    private lateinit var etOtp: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        tvTimer = findViewById(R.id.tvTimer)
        btnResend = findViewById(R.id.btnResend)
        btnVerify = findViewById(R.id.btnVerify)
        etOtp = findViewById(R.id.etOtp)
        progressBar = findViewById(R.id.progressBar)

        registrationId = intent.getStringExtra("REG_ID")
        val nextResendAt = intent.getLongExtra("RESEND_TIME", 0L)

        viewModel.startTimer(nextResendAt)

        btnVerify.setOnClickListener {
            val otp = etOtp.text.toString()
            if (otp.length == 6 && registrationId != null) {
                viewModel.verifyOtp(registrationId!!, otp)
            }
        }

        btnResend.setOnClickListener {
            if (registrationId != null) {
                viewModel.resendOtp(registrationId!!)
            }
        }

        observeStates()
    }

    private fun observeStates() {
        // 1. Observe Verification (Verify Button)
        lifecycleScope.launch {
            viewModel.verifyState.collect { state ->
                handleUiState(state, isVerifyAction = true)
            }
        }

        // 2. Observe Resend (Resend Button) - NEW LOGIC
        lifecycleScope.launch {
            viewModel.resendState.collect { state ->
                handleUiState(state, isVerifyAction = false)
            }
        }

        // 3. Observe Timer
        lifecycleScope.launch {
            viewModel.timerState.collect { timeString ->
                tvTimer.text = timeString
                if (timeString == "00:00" || timeString == "Resend Available") {
                    btnResend.isEnabled = true
                    tvTimer.text = "You can now resend code"
                } else {
                    btnResend.isEnabled = false
                }
            }
        }
    }

    private fun handleUiState(state: UiState<*>, isVerifyAction: Boolean) {
        when (state) {
            is UiState.Loading -> {
                progressBar.visibility = View.VISIBLE
                if (isVerifyAction) btnVerify.isEnabled = false else btnResend.isEnabled = false
            }
            is UiState.Success -> {
                progressBar.visibility = View.GONE
                if (isVerifyAction) {
                    // Verify Success -> Login
                    Toast.makeText(this, "Verified! Please Login.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    // Resend Success -> Stay here, Timer restarts automatically via VM
                    Toast.makeText(this, "OTP Resent Successfully", Toast.LENGTH_SHORT).show()
                    btnResend.isEnabled = false // Disable until timer finishes
                }
            }
            is UiState.SessionExpired -> {
                // HANDLE REDIRECT FOR BOTH VERIFY AND RESEND
                progressBar.visibility = View.GONE
                val msg = if (isVerifyAction) "Session expired." else "Limit exceeded or Session expired."

                Toast.makeText(this, "$msg Please Signup again.", Toast.LENGTH_LONG).show()

                // Redirect to Signup
                val intent = Intent(this, Signup::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            is UiState.Error -> {
                progressBar.visibility = View.GONE
                btnVerify.isEnabled = true
                btnResend.isEnabled = true
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}