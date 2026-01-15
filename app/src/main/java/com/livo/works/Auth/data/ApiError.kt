package com.livo.works.Auth.data

data class ApiError(
    val status: String,
    val message: String,
    val subErrors: List<String>? = null
)