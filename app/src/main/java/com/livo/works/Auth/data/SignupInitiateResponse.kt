package com.livo.works.Auth.data

data class SignupInitiateResponse(
    val registrationId: String,
    val nextResendAt: Long
)