package com.money.dividr.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val members: List<GroupMember> = emptyList<GroupMember>(),
    val totalSpending: Double = 0.0,
    val currency: String = "USD",
    val createdByUid: String = "",
    val groupJoinKey: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val balances: Map<String, Map<String, Double>> = emptyMap()
)
