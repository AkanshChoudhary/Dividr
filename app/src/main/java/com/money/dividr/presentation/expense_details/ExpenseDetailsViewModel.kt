package com.money.dividr.presentation.expense_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.model.Group
import com.money.dividr.domain.repository.ExpenseRepository
import com.money.dividr.domain.repository.GroupRepository
import com.money.dividr.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.money.dividr.data.repository.Result
import com.money.dividr.presentation.expense_details.DeleteExpenseUiState.*

data class ExpenseDetailsCombined(
    val expense: Expense,
    val group: Group?  
)

sealed interface ExpenseDetailsUiState {
    object Loading : ExpenseDetailsUiState
    data class Success(val details: ExpenseDetailsCombined) : ExpenseDetailsUiState
    data class Error(val message: String) : ExpenseDetailsUiState
}
 
sealed interface DeleteExpenseUiState {
    object Idle : DeleteExpenseUiState
    object Loading : DeleteExpenseUiState
    object Success : DeleteExpenseUiState
    data class Error(val message: String) : DeleteExpenseUiState
}
@HiltViewModel
class ExpenseDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val groupId: String = savedStateHandle[AppRoutes.GROUP_DETAILS_ARG_ID] ?: ""
    private val expenseId: String = savedStateHandle[AppRoutes.EXPENSE_DETAILS_ARG_ID] ?: ""

    private val _expenseDetailsState = MutableStateFlow<ExpenseDetailsUiState>(ExpenseDetailsUiState.Loading)
    val expenseDetailsState: StateFlow<ExpenseDetailsUiState> = _expenseDetailsState.asStateFlow()

     
    private val _deleteExpenseState = MutableStateFlow<DeleteExpenseUiState>(DeleteExpenseUiState.Idle)
    val deleteExpenseState: StateFlow<DeleteExpenseUiState> = _deleteExpenseState.asStateFlow()

    init {
        if (groupId.isNotEmpty() && expenseId.isNotEmpty()) {
            loadExpenseDetails()
        } else {
            _expenseDetailsState.value = ExpenseDetailsUiState.Error("Group ID or Expense ID is missing.")
        }
    }

    private fun loadExpenseDetails() {
        viewModelScope.launch {
            _expenseDetailsState.value = ExpenseDetailsUiState.Loading
            combine(
                expenseRepository.getExpenseDetails(groupId, expenseId),
                groupRepository.getGroupById(groupId)  
            ) { expenseResult, groupResult ->
                if (expenseResult == null) {
                    throw IllegalStateException("Expense not found.")
                }
                ExpenseDetailsCombined(expense = expenseResult, group = groupResult)
            }.catch { e ->
                _expenseDetailsState.value = ExpenseDetailsUiState.Error(e.localizedMessage ?: "An unknown error occurred")
            }.collect { combinedDetails ->
                _expenseDetailsState.value = ExpenseDetailsUiState.Success(combinedDetails)
            }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            _deleteExpenseState.value = Loading
             
            val result = expenseRepository.deleteExpense(groupId = groupId, expenseId = expenseId)

            _deleteExpenseState.value = when (result) {
                is Result.Success -> {
                    Success
                }
                is Result.Error -> {
                    Error(result.exception.message ?: "Unknown error occurred during deletion.")
                }
                Result.Loading -> TODO()
            }
        }
    }
    fun resetDeleteExpenseState() {
        _deleteExpenseState.value = Idle
    }
}
