package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Auth.data.SignupInitiateResponse
import com.livo.works.Auth.repository.AuthRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _signupState = MutableStateFlow<UiState<SignupInitiateResponse>>(UiState.Idle)
    val signupState = _signupState.asStateFlow()

    fun initiateSignup(name: String, email: String, pass: String) {
        viewModelScope.launch {
            // Trigger Route #1
            repository.signupInitiate(name, email, pass).collect { result ->
                _signupState.value = result
            }
        }
    }
}