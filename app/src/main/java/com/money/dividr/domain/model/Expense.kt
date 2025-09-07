package com.money.dividr.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Expense(
    val expenseId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",  
    val paidByUid: String = "",
    val paidByInfo: Map<String, Any?>? = null,  
    @ServerTimestamp
    val paidAt: Date? = null,  
    @ServerTimestamp
    val createdAt: Date? = null,
    val splitType: String = SplitType.EQUAL.name,  
    val splitDetails: List<SplitDetail> = emptyList(),
    val participantsUids: List<String> = emptyList()
)

 
enum class SplitType {
    EQUAL,
    UNEQUAL
}
