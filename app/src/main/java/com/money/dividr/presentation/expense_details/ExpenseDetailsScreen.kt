package com.money.dividr.presentation.expense_details

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.dividr.R  
import androidx.compose.ui.text.font.Font  
import androidx.compose.ui.text.style.TextAlign
import com.money.dividr.presentation.utils.formatFirebaseTimestamp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

 
fun formatCurrency(amount: Double, currencyCode: String?): String {
    return try {
        val resolvedCurrencyCode = currencyCode ?: "USD"  
        val currency = Currency.getInstance(resolvedCurrencyCode.uppercase(Locale.ROOT))
        val locale = when (resolvedCurrencyCode.uppercase(Locale.ROOT)) {
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "INR" -> Locale("en", "IN")
            else -> Locale.getDefault()
        }
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        numberFormat.currency = currency
        numberFormat.format(amount)
    } catch (e: Exception) {
        "${currencyCode ?: ""} %.2f".format(Locale.US, amount)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsScreen(
    viewModel: ExpenseDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.expenseDetailsState.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val deleteState by viewModel.deleteExpenseState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteExpenseUiState.Success -> {
                Toast.makeText(context, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetDeleteExpenseState()  
                onNavigateBack()  
            }
            is DeleteExpenseUiState.Error -> {
                val errorMessage = (deleteState as DeleteExpenseUiState.Error).message
                Toast.makeText(context, "Error deleting expense: $errorMessage", Toast.LENGTH_LONG).show()
                viewModel.resetDeleteExpenseState()  
            }
            else -> { /* Idle or Loading, handled by button state or global indicator */ }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dividr",  
                        fontFamily = FontFamily.Cursive,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val isDeleting = deleteState is DeleteExpenseUiState.Loading
                    TextButton(onClick = {
                        if (!isDeleting) {  
                            showDeleteConfirmationDialog = true
                        }
                    },enabled = !isDeleting) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is ExpenseDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ExpenseDetailsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ExpenseDetailsUiState.Success -> {
                    val expense = state.details.expense
                    val group = state.details.group
                    val membersMap = group?.members?.associateBy { it.uid } ?: emptyMap()

                    val payerName = membersMap[expense.paidByUid]?.name ?: expense.paidByUid

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally  
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))  

                         
                        Text(
                            text = formatCurrency(expense.amount, "USD"),

                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary  
                            )
                        )
                        Spacer(modifier = Modifier.height(48.dp))  

                         
                        Text(
                            text = "Paid by: $payerName",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Paid On: "+expense.paidAt.formatFirebaseTimestamp(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(64.dp))  

                         
                        if (expense.description.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Description:",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = expense.description,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                            }
                            Spacer(modifier = Modifier.height(24.dp))  
                        }

                         
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Participants:",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))  

                            if (expense.splitDetails.isEmpty()) {
                                Text("No participants in this split.", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                expense.splitDetails.forEach { (participantUid, amountOwed) ->
                                    if (amountOwed != 0.0) {
                                        val participantName = membersMap[participantUid]?.name ?: participantUid
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),  
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = participantName,
                                                style = MaterialTheme.typography.titleMedium  
                                            )
                                            Text(
                                                text = formatCurrency(amountOwed, "USD"),
                                                style = MaterialTheme.typography.titleMedium,  
                                                 
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))  
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))  
                    }
                }
            }
        }
    }
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense()
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
