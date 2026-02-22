package com.livo.works.Role.data

data class RoleRequestDto(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val requestedRole: String,
    val status: String
)