package com.money.dividr.domain.repository

import com.money.dividr.data.repository.Result
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.model.GroupMember
import com.money.dividr.presentation.add_expense.AddExpenseUiState
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getGroupMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun createExpense(groupId: String, expense: Expense): Flow<AddExpenseUiState>
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>> 

    fun getExpenseDetails(groupId: String, expenseId: String): Flow<Expense?>  
    suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit>
}
