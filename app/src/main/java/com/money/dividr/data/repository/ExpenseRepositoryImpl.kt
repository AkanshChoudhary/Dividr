package com.money.dividr.data.repository

import android.system.Os
import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.model.Group
import com.money.dividr.domain.model.GroupMember
import com.money.dividr.domain.repository.ExpenseRepository
import com.money.dividr.presentation.add_expense.AddExpenseUiState
import com.money.dividr.presentation.utils.roundToTwoDecimals
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.roundToInt

class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    override fun getGroupMembers(groupId: String): Flow<List<GroupMember>> = callbackFlow {
        val groupDocRef = firestore.collection("groups").document(groupId)

        val listenerRegistration = groupDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)  
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val membersList = snapshot.get("members") as? List<Map<String, Any>>
                if (membersList != null) {
                    val groupMembers = membersList.mapNotNull { memberMap ->
                        val uid = memberMap["uid"] as? String
                        val name = memberMap["name"] as? String  
                        if (uid != null && name != null) {
                            GroupMember(
                                uid = uid,
                                name = name,
                                email = memberMap["email"] as? String ?: "",
                                 role = memberMap["role"] as? String ?: "",
                                isOwner = memberMap["owner"] as? Boolean ?: false
                            )
                        } else {
                            null
                        }
                    }
                    trySend(groupMembers).isSuccess  
                } else {
                    trySend(emptyList()).isSuccess  
                }
            } else {
                trySend(emptyList()).isSuccess  
            }
        }
        awaitClose { listenerRegistration.remove() }  
    }

    override suspend fun createExpense(groupId: String, expense: Expense): Flow<AddExpenseUiState> = callbackFlow {
        trySend(AddExpenseUiState.Loading)  
        val newExpenseDocRef = firestore.collection("groups").document(groupId)
            .collection("expenses").document()

         
        val newExpenseId = newExpenseDocRef.id

         
         
         
        val expenseWithId = expense.copy(expenseId = newExpenseId)

        newExpenseDocRef.set(expenseWithId)
            .addOnSuccessListener {
                trySend(AddExpenseUiState.Success("Expense created successfully!"))
                firestore.runTransaction { transaction ->
                    val groupDocRef: DocumentReference = firestore.collection("groups").document(groupId)
                    val groupSnapshot: DocumentSnapshot = transaction.get(groupDocRef)
                    val balancesData = groupSnapshot.get("balances")
                    val balancesMap = balancesData as? Map<String, Map<String, Double>> ?: emptyMap()

                    val newBalances = balancesMap.mapValues { (_, innerMap) -> innerMap.toMutableMap() }.toMutableMap()

                    Log.d("ExpenseRepo", "Initial balances before update (simple logic): $newBalances")

                    val creditorUid = expense.paidByUid

                    for (splitDetail in expense.splitDetails) {
                        val debtorUid = splitDetail.userId
                        val amountOwedByParticipant = splitDetail.owesAmount

                        if (amountOwedByParticipant <= 0.0) continue  

                         
                        if (debtorUid == creditorUid) continue

                        Log.d("ExpenseRepo", "Processing split (simple): $debtorUid owes $creditorUid amount $amountOwedByParticipant")

                         
                        val debtorSpecificDebts = newBalances.getOrPut(debtorUid) { mutableMapOf() }

                         
                        val currentDebtToCreditor = debtorSpecificDebts.getOrPut(creditorUid) { 0.0 }

                         
                        val updatedDebtToCreditor = (currentDebtToCreditor + amountOwedByParticipant).roundToTwoDecimals()
                        debtorSpecificDebts[creditorUid] = updatedDebtToCreditor

                        Log.d("ExpenseRepo", "$debtorUid now owes $creditorUid: $updatedDebtToCreditor (was $currentDebtToCreditor, added $amountOwedByParticipant)")

                         
                        newBalances[debtorUid] = debtorSpecificDebts
                    }

                    Log.d("ExpenseRepo", "Final balances to update (simple logic): $newBalances")
                    transaction.update(firestore.collection("groups").document(groupId), "balances", newBalances)
                    val totalSpending = groupSnapshot.get("totalSpending") as Double
                    transaction.update(groupDocRef, "totalSpending", totalSpending.coerceAtLeast(0.0)+expense.amount)
                    null  
                }.addOnSuccessListener {
                    Log.d("ExpenseRepo", "Balances updated successfully (simple logic).")
                    trySend(AddExpenseUiState.Success("Expense created and balances updated successfully!"))
                    close()
                }.addOnFailureListener { eTransaction ->
                    Log.e("ExpenseRepo", "Balance update transaction failed after expense creation (simple logic).", eTransaction)
                    trySend(AddExpenseUiState.Error(
                        eTransaction.toString()))
                    close()
                }
                close()  
            }
            .addOnFailureListener { e ->
                trySend(AddExpenseUiState.Error(e.toString()))
                close()  
            }
        awaitClose {}  
    }

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> = callbackFlow {
         
        val expensesQuery = firestore.collection("groups").document(groupId)
            .collection("expenses")
            .orderBy("paidAt", Query.Direction.DESCENDING)  

         
        val listenerRegistration = expensesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreExpenseRepo", "Error listening to expenses for group $groupId: ", error)
                 
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val expensesList = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Expense::class.java)?.copy(expenseId = document.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreExpenseRepo", "Error converting document ${document.id} to Expense: ", e)
                        null  
                    }
                }
                Log.d("FirestoreExpenseRepo", "Expenses fetched for group $groupId: ${expensesList.size} items")
                 
                trySend(expensesList).isSuccess  
            } else {
                Log.d("FirestoreExpenseRepo", "Expense snapshot was null for group $groupId (no error)")
                trySend(emptyList()).isSuccess  
            }
        }

         
        awaitClose {
            Log.d("FirestoreExpenseRepo", "Closing expenses listener for group $groupId")
            listenerRegistration.remove()
        }
    }

    override fun getExpenseDetails(groupId: String, expenseId: String): Flow<Expense?> = callbackFlow {
        val expenseDocumentRef = firestore.collection("groups").document(groupId)
            .collection("expenses").document(expenseId)

        val listenerRegistration = expenseDocumentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)  
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val expense = snapshot.toObject(Expense::class.java)?.copy(expenseId = snapshot.id)
                trySend(expense).isSuccess  
            } else {
                trySend(null).isSuccess  
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit> {
        if (groupId.isBlank() || expenseId.isBlank()) {
            return Result.Error(IllegalArgumentException("Group ID and Expense ID must not be blank."))
        }

        val groupDocRef = firestore.collection("groups").document(groupId)
        val expenseDocRef = groupDocRef.collection("expenses").document(expenseId)


        return try {
            firestore.runTransaction { transaction ->
                 
                val expenseSnapshot = transaction.get(expenseDocRef)
                val groupSnapshot = transaction.get(groupDocRef)
                val floatPrecisionThreshold = 0.001

                val expense = expenseSnapshot.toObject(Expense::class.java)
                    ?: throw FirebaseFirestoreException("Failed to parse expense $expenseId.", FirebaseFirestoreException.Code.DATA_LOSS)
                val group = groupSnapshot.toObject(Group::class.java)
                    ?: throw FirebaseFirestoreException("Failed to parse group $groupId.", FirebaseFirestoreException.Code.DATA_LOSS)

                 
                val originalPayerUid = expense.paidByUid
                val amountToReverseFromExpense = expense.amount  

                @Suppress("UNCHECKED_CAST")
                val currentBalances = group.balances  
                val mutableBalances = currentBalances.mapValues { (_, innerMap) ->
                    innerMap.toMutableMap()
                }.toMutableMap()

                 
                 
                 
                for (split in expense.splitDetails) {
                    val participantUid = split.userId
                    val shareAmountReverted = split.owesAmount  

                     
                    if (participantUid == originalPayerUid) continue

                     
                    val debtorDebtsMap = mutableBalances.getOrPut(participantUid) { mutableMapOf() }
                    val currentDebtToOriginalPayer = debtorDebtsMap[originalPayerUid] ?: 0.0

                    val newCalculatedDebt = currentDebtToOriginalPayer - shareAmountReverted

                    Log.d("DeleteExpense", "User $participantUid: current debt to $originalPayerUid was $currentDebtToOriginalPayer. Reverting $shareAmountReverted. New calculated: $newCalculatedDebt")

                    if (newCalculatedDebt < 0) {  
                        val amountOwedByOriginalPayer = abs(newCalculatedDebt)
                         
                        debtorDebtsMap[originalPayerUid]=0.0.roundToTwoDecimals()
                         
                        val originalPayerDebtsToOthers = mutableBalances.getOrPut(originalPayerUid) { mutableMapOf() }
                        val currentDebtToParticipant = originalPayerDebtsToOthers[participantUid] ?: 0.0
                        originalPayerDebtsToOthers[participantUid] = (currentDebtToParticipant + amountOwedByOriginalPayer).roundToTwoDecimals()
                        Log.d("DeleteExpense", "Debt reversed. $originalPayerUid now owes $participantUid $amountOwedByOriginalPayer (new total ${originalPayerDebtsToOthers[participantUid]}).")
                    } else {  
                        debtorDebtsMap[originalPayerUid] = newCalculatedDebt.roundToTwoDecimals()
                        Log.d("DeleteExpense", "Debt for $originalPayerUid in $participantUid's debts updated to $newCalculatedDebt.")
                    }
                }

                 
                val newTotalSpending = group.totalSpending - amountToReverseFromExpense

                 
                transaction.update(groupDocRef, "balances", mutableBalances)
                transaction.update(groupDocRef, "totalSpending", newTotalSpending.coerceAtLeast(0.0))  
                transaction.delete(expenseDocRef)
            }.await()

            Log.i("FirestoreGroupRepo", "Expense $expenseId deleted successfully from group $groupId.")
            Result.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e("FirestoreGroupRepo", "Firestore error deleting expense $expenseId: ${e.message}", e)
            Result.Error(Exception("Firestore error: ${e.message} - ${e.message}", e))
        } catch (e: Exception) {
            Log.e("FirestoreGroupRepo", "Error deleting expense $expenseId: ${e.message}", e)
            Result.Error(e)
        }
    }
}

 
 
sealed class Result<out T> {
    object Loading : Result<Nothing>()  
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

