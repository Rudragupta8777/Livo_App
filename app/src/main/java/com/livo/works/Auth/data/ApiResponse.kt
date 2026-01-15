package com.livo.works.Auth.data

data class ApiResponse<T>(
    val timeStamp: String,
    val data: T?,
    val error: ApiError?
)