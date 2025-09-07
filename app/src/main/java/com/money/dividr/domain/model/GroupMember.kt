package com.money.dividr.domain.model

data class GroupMember(
    val uid: String = "",
    val name: String? = null,
    val email: String? = null,
    val role: String = "",
    val isOwner: Boolean = false
)
