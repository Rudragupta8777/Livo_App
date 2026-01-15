package com.livo.works.Auth.data

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)