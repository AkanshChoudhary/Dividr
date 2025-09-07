package com.money.dividr.domain.model

data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val email: String? ,
    val groupIds: List<String> = emptyList()
)