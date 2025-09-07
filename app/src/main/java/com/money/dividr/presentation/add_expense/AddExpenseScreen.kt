package com.money.dividr.presentation.add_expense

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.dividr.domain.model.GroupMember
import com.money.dividr.domain.model.SplitType
import com.money.dividr.ui.theme.DividrTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    navController: androidx.navigation.NavController,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val description by viewModel.description.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()
    val isLoadingMembers by viewModel.isLoadingMembers.collectAsState()
    val selectedPaidByMemberId by viewModel.selectedPaidByMemberId.collectAsState()
    val paymentDateLong by viewModel.paymentDate.collectAsState()
    val selectedSplitType by viewModel.selectedSplitType.collectAsState()
    val unequalShares = viewModel.unequalShares  
    val totalUnequalShares by viewModel.totalUnequalShares.collectAsState()


    val addExpenseUiState by viewModel.addExpenseUiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(addExpenseUiState) {
        when (val state = addExpenseUiState) {
            is AddExpenseUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                navController.popBackStack()
                viewModel.resetAddExpenseUiState()  
            }
            is AddExpenseUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetAddExpenseUiState()  
            }
            else -> Unit  
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Expense") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (addExpenseUiState == AddExpenseUiState.Loading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = { Text(text = "$") }  
                    )
                }

                item {
                    if (isLoadingMembers) {
                        CircularProgressIndicator()
                    } else if (groupMembers.isNotEmpty()) {
                        PaidByDropDown(
                            members = groupMembers,
                            selectedMemberId = selectedPaidByMemberId,
                            onMemberSelected = viewModel::onPaidByMemberSelected
                        )
                    } else {
                        Text("No members found in group to select 'Paid by'.")
                    }
                }

                item {
                    PaymentDatePicker(
                        selectedDateEpochMillis = paymentDateLong,
                        onDateSelected = { year, month, day ->
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.set(year, month, day)
                            viewModel.onPaymentDateSelected(calendar.timeInMillis)
                        },
                        showDialogState = showDatePicker,
                        onDismissDialog = { showDatePicker = false }
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.padding(end = 8.dp))
                        Text(
                            paymentDateLong?.let {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Select Payment Date"
                        )
                    }
                }

                item {
                    Text("Split Type", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SplitType.entries.forEach { splitType ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { viewModel.onSplitTypeSelected(splitType) }
                                    .padding(end = 16.dp)
                            ) {
                                RadioButton(
                                    selected = selectedSplitType == splitType,
                                    onClick = { viewModel.onSplitTypeSelected(splitType) }
                                )
                                Text(splitType.name)
                            }
                        }
                    }
                }

                if (selectedSplitType == SplitType.UNEQUAL) {
                    item {
                        Column {
                            Text(
                                "Enter Shares (must sum to ${"%.2f".format(amount.toDoubleOrNull() ?: 0.0)})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            val currentTotalAmount = amount.toDoubleOrNull() ?: 0.0
                            val remainingToAllocate = currentTotalAmount - totalUnequalShares
                            Text(
                                "Total Entered: ${"%.2f".format(totalUnequalShares)} / Remaining: ${"%.2f".format(remainingToAllocate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (kotlin.math.abs(remainingToAllocate) < 0.01 && currentTotalAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    items(groupMembers, key = { it.uid }) { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            member.name?.let {
                                Text(
                                    it,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = unequalShares[member.uid] ?: "0.00",
                                onValueChange = { viewModel.onUnequalShareChanged(member.uid, it) },
                                label = { Text("Share") },
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }


                item {
                    Button(
                        onClick = { viewModel.createExpense() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = description.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedPaidByMemberId != null && paymentDateLong != null
                    ) {
                        Text("Create Expense")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaidByDropDown(
    members: List<GroupMember>,
    selectedMemberId: String?,
    onMemberSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMemberName = members.find { it.uid == selectedMemberId }?.name ?: "Select who paid"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedMemberName,
            onValueChange = {},  
            readOnly = true,
            label = { Text("Paid By") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = { member.name?.let { Text(it) } },
                    onClick = {
                        onMemberSelected(member.uid)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDatePicker(
    selectedDateEpochMillis: Long?,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,  
    showDialogState: Boolean,
    onDismissDialog: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateEpochMillis ?: System.currentTimeMillis()
    )

    if (showDialogState) {
        DatePickerDialog(
            onDismissRequest = onDismissDialog,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = millis
                        onDateSelected(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),  
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    onDismissDialog()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewAddExpenseScreen() {
    DividrTheme {
         
         
         
        AddExpenseScreen(navController = androidx.navigation.compose.rememberNavController())
    }
}
