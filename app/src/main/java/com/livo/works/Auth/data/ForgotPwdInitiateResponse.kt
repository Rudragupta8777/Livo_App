package com.livo.works.Auth.data

data class ForgotPwdInitiateResponse(
    val registrationId: String,
    val nextResendAt: Long
)