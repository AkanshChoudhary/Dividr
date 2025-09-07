package com.money.dividr.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Settlement(
    val settlementId: String = "",
    val payerUid: String = "",
    val payeeUid: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",  
    @ServerTimestamp
    val settledAt: Date? = null,  
    @ServerTimestamp
    val createdAt: Date? = null,
)
