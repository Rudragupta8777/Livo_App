package com.livo.works.Auth.data

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)