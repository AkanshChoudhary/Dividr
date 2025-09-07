package com.money.dividr.domain.repository

import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import com.google.firebase.firestore.FirebaseFirestoreException
import com.money.dividr.domain.model.Group
import com.money.dividr.domain.model.UserData
import kotlinx.coroutines.flow.Flow
import com.money.dividr.data.repository.Result
import kotlin.io.path.exists
import kotlin.math.abs

interface GroupRepository {
    suspend fun createGroup(groupName: String, creator: UserData): Result<String>  
    fun getUserGroups(): Flow<List<Group>>
    suspend fun joinGroupWithCode(groupJoinKey: String, joiningUser: UserData): Result<String>  

    fun getGroupById(groupId: String): Flow<Group?>

    suspend fun updateUserBalanceInGroup(
        groupId: String,
        payerUid: String?,
        payeeUid: String,
        amountToSettle: Double
    ): Result<Unit>


}
