package com.livo.works.ViewModel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Auth.data.ForgotPwdInitiateResponse
import com.livo.works.Auth.data.ResendResponse
import com.livo.works.Auth.repository.AuthRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Step 1: Send Email
    private val _initiateState = MutableStateFlow<UiState<ForgotPwdInitiateResponse>>(UiState.Idle)
    val initiateState = _initiateState.asStateFlow()

    // Step 2: Reset Password
    private val _completeState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val completeState = _completeState.asStateFlow()

    // Resend Logic
    private val _resendState = MutableStateFlow<UiState<ResendResponse>>(UiState.Idle)
    val resendState = _resendState.asStateFlow()

    // Timer Logic
    private val _timerState = MutableStateFlow("00:00")
    val timerState = _timerState.asStateFlow()
    private var timer: CountDownTimer? = null

    fun sendOtp(email: String) {
        viewModelScope.launch {
            repository.initiateForgotPassword(email).collect { _initiateState.value = it }
        }
    }

    fun resetPassword(regId: String, otp: String, newPass: String) {
        viewModelScope.launch {
            repository.completeResetPassword(regId, otp, newPass).collect { _completeState.value = it }
        }
    }

    fun resendOtp(regId: String) {
        viewModelScope.launch {
            repository.resendForgotOtp(regId).collect { result ->
                _resendState.value = result
                if (result is UiState.Success) {
                    result.data?.let { startTimer(it.nextResendAt) }
                }
            }
        }
    }

    fun startTimer(targetTimeMillis: Long) {
        timer?.cancel()
        val duration = targetTimeMillis - System.currentTimeMillis()
        if (duration <= 0) {
            _timerState.value = "Resend Available"
            return
        }

        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millis: Long) {
                val sec = millis / 1000
                _timerState.value = String.format("Resend code in %02d:%02d", sec / 60, sec % 60)
            }
            override fun onFinish() {
                _timerState.value = "Resend Available"
            }
        }.start()
    }
}