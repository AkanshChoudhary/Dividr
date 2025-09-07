package com.money.dividr.presentation.add_expense

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.dividr.domain.model.Expense
import com.money.dividr.domain.model.GroupMember
import com.money.dividr.domain.model.SplitDetail
import com.money.dividr.domain.model.SplitType
import com.money.dividr.domain.repository.ExpenseRepository  
import com.money.dividr.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date  
import javax.inject.Inject
import com.money.dividr.presentation.add_expense.AddExpenseUiState.*
import com.money.dividr.presentation.utils.roundToTwoDecimals

 
sealed interface AddExpenseUiState {
    object Idle : AddExpenseUiState
    object Loading : AddExpenseUiState
    data class Success(val message: String) : AddExpenseUiState
    data class Error(val message: String) : AddExpenseUiState
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository  
) : ViewModel() {

    val groupId: String = savedStateHandle[AppRoutes.GROUP_DETAILS_ARG_ID] ?: ""

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _selectedPaidByMemberId = MutableStateFlow<String?>(null)
    val selectedPaidByMemberId: StateFlow<String?> = _selectedPaidByMemberId.asStateFlow()

    private val _paymentDate = MutableStateFlow<Long?>(System.currentTimeMillis())
    val paymentDate: StateFlow<Long?> = _paymentDate.asStateFlow()

    private val _selectedSplitType = MutableStateFlow(SplitType.EQUAL)
    val selectedSplitType: StateFlow<SplitType> = _selectedSplitType.asStateFlow()

    val unequalShares = mutableStateMapOf<String, String>()

    private val _isLoadingMembers = MutableStateFlow(true)
    val isLoadingMembers: StateFlow<Boolean> = _isLoadingMembers.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers.asStateFlow()

    private val _addExpenseUiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val addExpenseUiState: StateFlow<AddExpenseUiState> = _addExpenseUiState.asStateFlow()

    val totalUnequalShares: StateFlow<Double> = snapshotFlow {
        unequalShares.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    }
        .onEach { sum -> Log.d("AddExpenseVM", "totalUnequalShares recalculated: $sum. Current unequalShares: ${unequalShares.toMap()}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        if (groupId.isNotBlank()) {
            fetchGroupMembers()
        } else {
            _isLoadingMembers.value = false
             
            _addExpenseUiState.value = AddExpenseUiState.Error("Group ID is missing. Cannot fetch members.")
        }
    }

    fun onDescriptionChange(newDescription: String) {
        _description.value = newDescription
    }

    fun onAmountChange(newAmount: String) {
        val regex = Regex("^\\d*\\.?\\d{0,2}$")
        if (newAmount.isEmpty() || newAmount.matches(regex)) {
            _amount.value = newAmount
        }
    }

    fun onPaidByMemberSelected(memberId: String) {
        _selectedPaidByMemberId.value = memberId
    }

    fun onPaymentDateSelected(epochMillis: Long?) {
        _paymentDate.value = epochMillis
    }

    fun onSplitTypeSelected(splitType: SplitType) {
        _selectedSplitType.value = splitType
        if (splitType == SplitType.EQUAL) {
            unequalShares.clear()
        } else {
             
            _groupMembers.value.forEach { member ->
                unequalShares.putIfAbsent(member.uid, "0.00")
            }
        }
    }

    fun onUnequalShareChanged(memberId: String, share: String) {
        val regex = Regex("^\\d*\\.?\\d{0,2}$")
        if (share.isEmpty() || share.matches(regex)) {
            unequalShares[memberId] = share
        }
    }

    private fun fetchGroupMembers() {
        viewModelScope.launch {
            expenseRepository.getGroupMembers(groupId)
                .onStart {
                    Log.d("AddExpenseVM", "Fetching group members...")
                    _isLoadingMembers.value = true
                }
                .catch { e ->
                    Log.e("AddExpenseVM", "Error fetching group members", e)
                    _addExpenseUiState.value = AddExpenseUiState.Error("Failed to fetch group members: ${e.message}")
                    _isLoadingMembers.value = false
                }
                .collect { members ->
                    Log.d("AddExpenseVM", "Fetched members: ${members.size}")
                    _groupMembers.value = members
                     
                    if (_selectedPaidByMemberId.value == null && members.isNotEmpty()) {
                        _selectedPaidByMemberId.value = members.first().uid
                    }
                     
                    members.forEach { member ->
                        unequalShares.putIfAbsent(member.uid, "0.00")
                    }
                    _isLoadingMembers.value = false
                }
        }
    }

    fun createExpense() {
        val currentDescription = _description.value.trim()
        val currentAmountStr = _amount.value
        val currentPayerId = _selectedPaidByMemberId.value
        val currentDateLong = _paymentDate.value  
        val currentSplitType = _selectedSplitType.value

         
        if (currentDescription.isBlank()) {
            _addExpenseUiState.value = Error("Description cannot be empty.")
            return
        }
        val expenseAmount = currentAmountStr.toDoubleOrNull()
        if (expenseAmount == null || expenseAmount <= 0) {
            _addExpenseUiState.value = AddExpenseUiState.Error("Please enter a valid amount.")
            return
        }
        if (currentPayerId == null) {
            _addExpenseUiState.value = AddExpenseUiState.Error("Please select who paid.")
            return
        }
        if (currentDateLong == null) {
            _addExpenseUiState.value = AddExpenseUiState.Error("Please select a payment date.")
            return
        }

        val paidByMember = _groupMembers.value.find { it.uid == currentPayerId }
        val paidByInfo = paidByMember?.let {
            mapOf(
                "name" to it.name,
            )
        } ?: emptyMap()


        val splitDetails = mutableListOf<SplitDetail>()
        val participantsUids = mutableListOf<String>()

        when (currentSplitType) {
            SplitType.EQUAL -> {
                if (_groupMembers.value.isEmpty()) {
                    _addExpenseUiState.value = AddExpenseUiState.Error("No members in group to split equally.")
                    return
                }
                val amountPerMember = (expenseAmount / _groupMembers.value.size).roundToTwoDecimals()
                val discrepancy = expenseAmount - (amountPerMember * _groupMembers.value.size)
                var discrepancyNeedsToBeAdded=false
                if(discrepancy != 0.0){
                    discrepancyNeedsToBeAdded = true
                }
                _groupMembers.value.forEach { member ->
                    if(discrepancyNeedsToBeAdded){
                        splitDetails.add(SplitDetail(userId = member.uid, owesAmount = (amountPerMember + discrepancy).roundToTwoDecimals()))
                        discrepancyNeedsToBeAdded=false
                    }else{
                        splitDetails.add(SplitDetail(userId = member.uid, owesAmount = amountPerMember.roundToTwoDecimals()))
                    }
                    participantsUids.add(member.uid)
                }
            }
            SplitType.UNEQUAL -> {
                var sumOfShares = 0.0
                _groupMembers.value.forEach { member ->
                    val shareStr = unequalShares[member.uid] ?: "0.0"
                    val shareAmount = shareStr.toDoubleOrNull()?.roundToTwoDecimals() ?: 0.0
                    if (shareAmount < 0) {
                        _addExpenseUiState.value = AddExpenseUiState.Error("Share amount for ${member.name} cannot be negative.")
                        return
                    }
                    if (shareAmount > 0) {
                        splitDetails.add(SplitDetail(userId = member.uid, owesAmount = shareAmount))
                        participantsUids.add(member.uid)
                    }
                    sumOfShares += shareAmount
                }
                if (kotlin.math.abs(sumOfShares - expenseAmount) > 0.01) {
                    _addExpenseUiState.value = AddExpenseUiState.Error("Sum of shares (${"%.2f".format(sumOfShares)}) must equal total amount (${"%.2f".format(expenseAmount)}).")
                    return
                }
                if (splitDetails.isEmpty() && expenseAmount > 0) {
                    _addExpenseUiState.value = AddExpenseUiState.Error("At least one member must have a share greater than zero for unequal split if amount is > 0.")
                    return
                }
            }
        }

        if (participantsUids.isEmpty() && expenseAmount > 0) {
            _addExpenseUiState.value = AddExpenseUiState.Error("Cannot create an expense with no participants when amount is greater than zero.")
            return
        }

         
        val expense = Expense(
            description = currentDescription,
            amount = expenseAmount,
            currency = "USD",  
            paidByUid = currentPayerId,
            paidByInfo = paidByInfo,  
            paidAt = Date(currentDateLong),  
            splitType = currentSplitType.name,
            splitDetails = splitDetails,
            participantsUids = participantsUids.distinct()
        )

        Log.d("AddExpenseVM", "Attempting to create expense via repository: $expense")
        viewModelScope.launch {
            expenseRepository.createExpense(groupId, expense)
                .onStart {
                    Log.d("AddExpenseVM", "createExpense Flow started. Setting UI state to Loading.")
                    _addExpenseUiState.value = AddExpenseUiState.Loading
                }
                .catch { e ->  
                    Log.e("AddExpenseVM", "Error in createExpense flow (outer catch)", e)
                    _addExpenseUiState.value = AddExpenseUiState.Error("Failed to create expense (repo flow catch): ${e.message}")
                }
                .collect { result ->
                    Log.d("AddExpenseVM", "Collected result from createExpense flow: $result")
                    when (result) {
                        is Success -> {
                            _addExpenseUiState.value = Success("Expense created successfully!")
                        }
                        is Error -> {
                            _addExpenseUiState.value = Error("Failed to create expense: ${result.message}")
                        }
                        is Loading -> {  
                            _addExpenseUiState.value = Loading
                        }
                        is Idle -> {}
                    }
                }
        }
    }

    fun resetAddExpenseUiState() {
        _addExpenseUiState.value = Idle
    }
}

