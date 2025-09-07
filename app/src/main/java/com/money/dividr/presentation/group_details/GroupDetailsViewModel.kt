package com.money.dividr.presentation.group_details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
 
 
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.repository.ExpenseRepository  
import com.money.dividr.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch  
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExpensesUiState {
    object Loading : ExpensesUiState
    data class Success(val expenses: List<Expense>) : ExpensesUiState
    data class Error(val message: String) : ExpensesUiState
}

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository  
) : ViewModel() {

    val groupId: String? = savedStateHandle[AppRoutes.GROUP_DETAILS_ARG_ID]

    private val _expensesState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val expensesState: StateFlow<ExpensesUiState> = _expensesState.asStateFlow()

    init {
        if (groupId != null) {
            fetchExpenses(groupId)
        } else {
            _expensesState.value = ExpensesUiState.Error("Group ID is missing.")
            Log.e("GroupDetailsViewModel", "GroupId is null in SavedStateHandle")
        }
    }

    private fun fetchExpenses(currentGroupId: String) {
        viewModelScope.launch {
            _expensesState.value = ExpensesUiState.Loading  
            expenseRepository.getExpensesForGroup(currentGroupId)
                .catch { e ->  
                    Log.e("GroupDetailsViewModel", "Error fetching expenses for group $currentGroupId: ", e)
                    _expensesState.value = ExpensesUiState.Error(e.localizedMessage ?: "Failed to fetch expenses.")
                }
                .collect { expensesList ->
                    Log.d("GroupDetailsViewModel", "Expenses received for group $currentGroupId: ${expensesList.size} items")
                    _expensesState.value = ExpensesUiState.Success(expensesList)
                }
        }
    }
}

