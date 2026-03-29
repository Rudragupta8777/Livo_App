package com.livo.works.Auth.data

import androidx.annotation.Keep

@Keep
data class ApiResponse<T>(
    val timeStamp: String,
    val data: T?,
    val error: ApiError?
)