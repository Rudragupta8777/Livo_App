package com.livo.works.ViewModel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Auth.data.ResendResponse
import com.livo.works.Auth.data.UserData
import com.livo.works.Auth.repository.AuthRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _verifyState = MutableStateFlow<UiState<UserData>>(UiState.Idle)
    val verifyState = _verifyState.asStateFlow()
    private val _resendState = MutableStateFlow<UiState<ResendResponse>>(UiState.Idle)
    val resendState = _resendState.asStateFlow()

    private val _timerState = MutableStateFlow("00:00")
    val timerState = _timerState.asStateFlow()

    private var timer: CountDownTimer? = null

    fun verifyOtp(regId: String, otp: String) {
        viewModelScope.launch {
            repository.verifyOtp(regId, otp).collect {
                _verifyState.value = it
            }
        }
    }

    fun resendOtp(regId: String) {
        viewModelScope.launch {
            repository.resendOtp(regId).collect { result ->
                // 1. Update the StateFlow so UI can react (Success, Error, SessionExpired)
                _resendState.value = result

                // 2. Handle internal logic like Timer
                if (result is UiState.Success) {
                    result.data?.let {
                        startTimer(it.nextResendAt)
                    }
                } else if (result is UiState.Error) {
                    _timerState.value = result.message // Show error in text (optional)
                }
            }
        }
    }

    fun startTimer(targetTimeMillis: Long) {
        timer?.cancel()
        val currentTime = System.currentTimeMillis()
        val duration = targetTimeMillis - currentTime

        if (duration <= 0) {
            _timerState.value = "Resend Available"
            return
        }

        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                _timerState.value = String.format("Resend code in %02d:%02d", minutes, remainingSeconds)
            }

            override fun onFinish() {
                _timerState.value = "Resend Available"
            }
        }.start()
    }
}