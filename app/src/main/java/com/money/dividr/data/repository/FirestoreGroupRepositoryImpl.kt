package com.money.dividr.data.repository

import android.system.Os
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.model.Group
import com.money.dividr.domain.model.GroupMember
import com.money.dividr.domain.model.UserData
import com.money.dividr.domain.repository.GroupRepository
import com.money.dividr.presentation.utils.roundToTwoDecimals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreGroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GroupRepository {
    fun getRandomString() : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override suspend fun createGroup(groupName: String, creator: UserData): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated for group creation"))
            }

             
            val generatedGroupKey = getRandomString()

            val newGroupRef = firestore.collection("groups").document()  

            val group = Group(
                groupId = newGroupRef.id,
                groupName = groupName,
                createdByUid = currentUser.uid,
                members = listOf(GroupMember(
                    uid=currentUser.uid,
                    name = creator.username,
                    email = creator.email,
                    role = "creator",
                    isOwner = true
                )),  
                currency = "USD",
                totalSpending = 0.0.roundToTwoDecimals(),
                groupJoinKey = generatedGroupKey,
                balances = mapOf<String,Map<String, Double>>(
                    currentUser.uid to mapOf<String, Double>(
                        currentUser.uid to 0.0.roundToTwoDecimals()
                    )
                )
            )

            val userGroupsRef = firestore.collection("users").document(currentUser.uid)
            val groupCodeMappingsDocRef = firestore.collection("groups").document("groupCodeMappings")
            val expensesRef = newGroupRef.collection("expenses")
            val settlementRef = newGroupRef.collection("settlements")

            firestore.runBatch { batch ->
                batch.set(newGroupRef, group)
                batch.update(userGroupsRef, "groupIds", FieldValue.arrayUnion(newGroupRef.id))

                 
                batch.update(groupCodeMappingsDocRef, "groupCodeMap.${group.groupJoinKey}", group.groupId)
                val expensesPlaceholderDoc = expensesRef.document("_init_")
                batch.set(expensesPlaceholderDoc, mapOf("createdAt" to FieldValue.serverTimestamp(), "info" to "Expenses collection initialized"))

                val settlementsPlaceholderDoc = settlementRef.document("_init_")
                batch.set(settlementsPlaceholderDoc, mapOf("createdAt" to FieldValue.serverTimestamp(), "info" to "Settlements collection initialized"))
            }.await()

            Result.Success(newGroupRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreGroupRepo", "Error creating group", e)
            Result.Error(e)
        }
    }

    override suspend fun joinGroupWithCode(groupJoinKey: String, joiningUser: UserData): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.Error(Exception("User not authenticated to join group"))
            }

            val groupCodeMappingsDocRef = firestore.collection("groups").document("groupCodeMappings")
            val mappingDoc = groupCodeMappingsDocRef.get().await()

            val groupCodeMap = mappingDoc.get("groupCodeMap") as? Map<*, *>  
            if (groupCodeMap == null) {
                Log.e("FirestoreGroupRepo", "'groupCodeMap' field is missing or not a map in groups/groupCodeMappings")
                return Result.Error(Exception("Group code mapping data is missing or invalid."))
            }

            val groupId = groupCodeMap[groupJoinKey] as? String

            if (groupId == null) {
                Log.d("FirestoreGroupRepo", "Group ID not found for join key: $groupJoinKey")
                return Result.Error(Exception("Invalid group join code."))
            }

            val groupRef = firestore.collection("groups").document(groupId)
            val userRef = firestore.collection("users").document(currentUser.uid)

            val newMember = GroupMember(
                uid = currentUser.uid,
                name = joiningUser.username,  
                email = joiningUser.email,     
                role = "member",
                isOwner = false
            )

            @Suppress("UNCHECKED_CAST")
            val existingBalances = groupRef.get().await().get("balances") as? Map<String, Map<String, Double>>
            Log.d("FirestoreGroupRepo", "Fetched existing balances for group $groupId: $existingBalances")  

            val newBalances = existingBalances?.mapValues { (_, innerMap) ->
                innerMap.toMutableMap()
            }?.toMutableMap() ?: mutableMapOf<String, MutableMap<String, Double>>()
             
            newBalances[joiningUser.userId] = mutableMapOf<String, Double>(joiningUser.userId to 0.0)

            firestore.runBatch { batch ->
                 
                batch.update(groupRef, "members", FieldValue.arrayUnion(newMember))
                 
                batch.update(userRef, "groupIds", FieldValue.arrayUnion(groupId))
                batch.update(groupRef, "balances", newBalances)
            }.await()

            Result.Success(groupId)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreGroupRepo", "Firestore error joining group with code: $groupJoinKey", e)
            Result.Error(Exception("Failed to join group. Error: ${e.message}", e))
        } catch (e: Exception) {
            Log.e("FirestoreGroupRepo", "Error joining group with code: $groupJoinKey", e)
            Result.Error(e)
        }
    }

    override fun getGroupById(groupId: String): Flow<Group?> = callbackFlow {
        val groupDocumentRef = firestore.collection("groups").document(groupId)

        val listenerRegistration = groupDocumentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null).isFailure  
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)?.copy(groupId = snapshot.id)
                trySend(group).isSuccess  
            } else {
                trySend(null).isSuccess  
            }
        }

         
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getUserGroups(): Flow<List<Group>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("FirestoreGroupRepo", "User not authenticated for fetching groups.")
            return emptyFlow()
        }

        val userDocRef = firestore.collection("users").document(currentUser.uid)

        return userDocRef.snapshots()  
            .flatMapLatest { userDocumentSnapshot ->
                if (!userDocumentSnapshot.exists()) {
                    Log.d("FirestoreGroupRepo", "User document does not exist.")
                    return@flatMapLatest flowOf(emptyList<Group>())
                }

                @Suppress("UNCHECKED_CAST")
                val groupIds = userDocumentSnapshot.get("groupIds") as? List<String>

                if (groupIds == null || groupIds.isEmpty()) {
                    Log.d("FirestoreGroupRepo", "User is not a member of any groups.")
                    return@flatMapLatest flowOf(emptyList<Group>())
                }

                if (groupIds.size > 30) {
                     Log.w("FirestoreGroupRepo", "User is in more than 30 groups. Fetching only the first 30.")
                     val limitedGroupIds = groupIds.take(30)
                     if (limitedGroupIds.isEmpty()) return@flatMapLatest flowOf(emptyList<Group>())

                     firestore.collection("groups")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), limitedGroupIds)
                        .snapshots()  
                        .map { querySnapshot ->
                            querySnapshot.documents.mapNotNull { document ->
                                try {
                                    document.toObject<Group>()?.copy(groupId = document.id)
                                } catch (e: Exception) {
                                    Log.e("FirestoreGroupRepo", "Error converting group document", e)
                                    null
                                }
                            }
                        }
                } else {
                     firestore.collection("groups")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), groupIds)
                        .snapshots()  
                        .map { querySnapshot ->
                            querySnapshot.documents.mapNotNull { document ->
                                try {
                                    document.toObject<Group>()?.copy(groupId = document.id)
                                } catch (e: Exception) {
                                    Log.e("FirestoreGroupRepo", "Error converting group document", e)
                                    null
                                }
                            }
                        }
                }
            }
            .catch { exception ->
                if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("FirestoreGroupRepo", "Permission denied fetching groups.", exception)
                } else {
                    Log.e("FirestoreGroupRepo", "Error fetching user groups flow", exception)
                }
                emit(emptyList())
            }
    }

    override suspend fun updateUserBalanceInGroup(
        groupId: String,
        payerUid: String?,
        payeeUid: String,
        amountToSettle: Double
    ): Result<Unit> {
        val groupDocRef = firestore.collection("groups").document(groupId)
        val settlementDocRef = groupDocRef.collection("settlements").document()
        return try {
             
            val groupSnapshot = groupDocRef.get().await()

            @Suppress("UNCHECKED_CAST")
            val currentBalances = groupSnapshot.get("balances") as? Map<String, Map<String, Double>> ?: emptyMap()

             
            val mutableBalances = currentBalances.mapValues { (_, innerMap) ->
                innerMap.toMutableMap()
            }.toMutableMap()

             
            val payerDebtsMap = mutableBalances.getOrPut(payerUid as String) { mutableMapOf() }

             
            val currentDebtToPayee: Double = payerDebtsMap[payeeUid] ?: 0.0

             
            val newDebtAmount = currentDebtToPayee - amountToSettle

            payerDebtsMap[payeeUid] = newDebtAmount.roundToTwoDecimals()

             
            groupDocRef.update("balances", mutableBalances).await()

            settlementDocRef.set(mapOf("amount" to amountToSettle, "payerUid" to payerUid, "payeeUid" to payeeUid, "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
            Result.Success(Unit)

        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreGroupRepo", "Firestore error during non-transactional balance update for group $groupId: ${e.message}", e)
            Result.Error(Exception("Firestore error: ${e.message}", e))
        } catch (e: Exception) {
            Log.e("FirestoreGroupRepo", "Error during non-transactional balance update for group $groupId: ${e.message}", e)
            Result.Error(e)
        }
    }



}

