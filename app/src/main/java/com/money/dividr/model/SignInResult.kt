package com.money.dividr.model

import com.money.dividr.domain.model.UserData

data class SignInResult(
    val data: UserData?,
    val error: String?
)


