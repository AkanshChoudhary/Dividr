package com.money.dividr.presentation.show_balances

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.money.dividr.data.repository.Result

import com.money.dividr.domain.repository.GroupRepository
import com.money.dividr.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DisplayDebt(
    val owedToName: String,
    val owedToUid: String,
    val amount: Double,
    val currencyCode: String = "USD"
)
sealed interface SettlementState {
    object Idle : SettlementState
    object Loading : SettlementState
    object Success : SettlementState
    data class Error(val message: String) : SettlementState
}
sealed interface ShowBalancesUiState {
    object Loading : ShowBalancesUiState
    data class Success(val debtsOwed: List<DisplayDebt>) : ShowBalancesUiState
    data class Error(val message: String) : ShowBalancesUiState
    object NoDebts : ShowBalancesUiState  
}

@HiltViewModel
class ShowBalancesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val groupId: String = savedStateHandle[AppRoutes.GROUP_DETAILS_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow<ShowBalancesUiState>(ShowBalancesUiState.Loading)
    val uiState: StateFlow<ShowBalancesUiState> = _uiState.asStateFlow()
    private val _settlementState = MutableStateFlow<SettlementState>(SettlementState.Idle)
    val settlementState: StateFlow<SettlementState> = _settlementState.asStateFlow()
    init {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (groupId.isNotEmpty() && currentUserId != null) {
            loadUserDebts(currentUserId)
        } else if (currentUserId == null) {
            _uiState.value = ShowBalancesUiState.Error("User not authenticated.")
        } else {
            _uiState.value = ShowBalancesUiState.Error("Group ID is missing.")
        }
    }

    private fun loadUserDebts(currentUserId: String) {
        _uiState.value = ShowBalancesUiState.Loading
        viewModelScope.launch {
            groupRepository.getGroupById(groupId)
                .map { group ->
                    if (group == null) {
                         
                        return@map ShowBalancesUiState.Error("Group not found.")
                    }

                     
                     
                     
                    val uidToNameMap = group.members.associate { member ->
                        member.uid to (member.name ?: member.uid)  
                    }

                    val balancesMap = group.balances
                    run {
                        val debtsOwedByUser = balancesMap[currentUserId]
                        if (debtsOwedByUser.isNullOrEmpty()) {
                            ShowBalancesUiState.NoDebts
                        } else {
                            val displayDebts = debtsOwedByUser.mapNotNull { (owedToUid, amount) ->
                                if (owedToUid == currentUserId) {
                                    null  
                                } else {
                                    DisplayDebt(
                                        owedToName = uidToNameMap[owedToUid]
                                            ?: "",  
                                        owedToUid = owedToUid,
                                        amount = amount
                                         
                                    )
                                }
                            }
                            if (displayDebts.isEmpty()) {
                                ShowBalancesUiState.NoDebts
                            } else {
                                ShowBalancesUiState.Success(displayDebts)
                            }
                        }
                    }
                }
                .catch { e ->
                    _uiState.value = ShowBalancesUiState.Error("Failed to load debts: ${e.message}")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun settleDebt(debtToSettle: DisplayDebt, settledAmount: String) {
        val settledAmount = settledAmount.toDoubleOrNull()
        if (settledAmount == null || settledAmount <= 0) {
            _settlementState.value = SettlementState.Error("Invalid settlement amount.")
            return
        }
        _settlementState.value = SettlementState.Loading
        viewModelScope.launch {

            val result = groupRepository.updateUserBalanceInGroup(
                groupId = groupId,
                payerUid = firebaseAuth.currentUser?.uid,
                payeeUid = debtToSettle.owedToUid,
                amountToSettle = settledAmount  
            )
            when (result) {
                is Result.Success -> {
                    _settlementState.value = SettlementState.Success
                     
                    loadUserDebts(firebaseAuth.currentUser?.uid ?: "")
                     
                }
                is Result.Error -> {
                    _settlementState.value = SettlementState.Error(result.exception.message ?: "Failed to settle debt.")
                }
                 
                is Result.Loading -> {
                    _settlementState.value= SettlementState.Loading
                }
            }
        }
    }


}
