package com.livo.works.Auth.data

data class ForgotPwdCompleteRequest(
    val registrationId: String,
    val otp: String,
    val newPassword: String
)