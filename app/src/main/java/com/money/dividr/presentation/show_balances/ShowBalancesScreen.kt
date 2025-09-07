package com.money.dividr.presentation.show_balances

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.dividr.R  
import androidx.compose.ui.text.font.Font  
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.money.dividr.navigation.AppRoutes
import com.money.dividr.presentation.expense_details.formatCurrency  

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowBalancesScreen(
    viewModel: ShowBalancesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
     
    var showSettleDialog by remember { mutableStateOf(false) }
    var selectedDebtForSettlement by remember { mutableStateOf<DisplayDebt?>(null) }
    var settlementAmountInput by remember { mutableStateOf("") }

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
                    IconButton(onClick =  onNavigateBack ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                is ShowBalancesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ShowBalancesUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ShowBalancesUiState.NoDebts -> {
                    Text(
                        text = "You are all settled up in this group!",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ShowBalancesUiState.Success -> {
                    if (state.debtsOwed.isEmpty()||state.debtsOwed.sumOf { it.amount } == 0.0) {
                        Text(
                            text = "You are all settled up!!",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    "You Owe:",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(state.debtsOwed) { debt ->
                                BalanceOwedCard(debt = debt, onSettleClicked = {
                                    selectedDebtForSettlement = debt
                                    settlementAmountInput = debt.amount.toString()  
                                    showSettleDialog = true
                                })
                            }
                        }
                    }
                }
            }

        }
        if (showSettleDialog && selectedDebtForSettlement != null) {
            SettleDebtDialog(
                debt = selectedDebtForSettlement!!,  
                currentAmount = settlementAmountInput,
                onAmountChange = {
                    val regex = Regex("^\\d*\\.?\\d{0,2}$")
                    if (it.isEmpty() || it.matches(regex)) {
                        settlementAmountInput = it
                    }
                },
                onDismiss = { showSettleDialog = false },
                onSettleConfirm = { debtToSettle, settledAmount ->
                    viewModel.settleDebt(
                        debtToSettle=debtToSettle,
                        settledAmount=settledAmount
                    )
                    showSettleDialog = false  
                },
                context = LocalContext.current
            )
        }
    }
}

@Composable
fun BalanceOwedCard(
    debt: DisplayDebt,
    onSettleClicked: (DisplayDebt) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.owedToName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(debt.amount, debt.currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,  
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { onSettleClicked(debt) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Settle", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
@Composable
fun SettleDebtDialog(
    debt: DisplayDebt,
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSettleConfirm: (DisplayDebt, String) -> Unit,
    context: Context 
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle with ${debt.owedToName}") },
        text = {
            OutlinedTextField(
                value = currentAmount,
                onValueChange = onAmountChange,
                label = { Text("Amount to Settle") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {  
                    if (debt.currencyCode.isNotBlank()) {
                        Text(debt.currencyCode)
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                     
                    val amountToSettle = currentAmount.toDoubleOrNull()
                    if (amountToSettle != null && amountToSettle > 0 && amountToSettle <= debt.amount) {
                        onSettleConfirm(debt, currentAmount)
                        onDismiss()  
                    } else {
                        Toast.makeText(context, "Invalid settlement amount!", Toast.LENGTH_LONG).show()
                        onAmountChange("")  
                    }
                }
            ) {
                Text("Settle")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
