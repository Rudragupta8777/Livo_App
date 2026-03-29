package com.livo.works.util

import androidx.annotation.Keep

@Keep
sealed class UiState<out T> {
    @Keep object Idle : UiState<Nothing>()
    @Keep object Loading : UiState<Nothing>()
    @Keep data class Success<T>(val data: T?) : UiState<T>()
    @Keep data class Error(val message: String) : UiState<Nothing>()
    @Keep object SessionExpired : UiState<Nothing>()
}