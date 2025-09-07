package com.money.dividr.presentation.group_details

import android.content.ClipData
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.money.dividr.navigation.AppRoutes
import com.money.dividr.presentation.components.ExpenseDetailCard
import kotlinx.coroutines.launch

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    groupId: String?,
    groupJoinKey: String?, 
    viewModel: GroupDetailsViewModel = hiltViewModel()
) {
    val expensesState by viewModel.expensesState.collectAsState()

     
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
     
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

     
    val clipboardManager = LocalClipboard.current  
    val context = LocalContext.current

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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        navController.navigate(AppRoutes.showBalancesRoute(groupId))
                    }) {
                        Text("Show Balances", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Expense") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add new expense") },
                onClick = {
                    if (groupId != null && groupId.isNotBlank()) {
                        navController.navigate(AppRoutes.addExpenseRoute(groupId))
                    } else {
                         
                         
                        Log.e("GroupDetailsScreen", "FAB clicked but groupId is null or blank.")
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = expensesState) {
                is ExpensesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ExpensesUiState.Success -> {
                    if (state.expenses.isEmpty()) {
                        Text(
                            text = "No expenses yet. Tap '+' to add one!",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Column {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Group Code", style = MaterialTheme.typography.titleMedium, fontSize = 24.sp)

                                Card(
                                    modifier = Modifier
                                        .padding(all = 12.dp)
                                        .clickable {
                                            val codeToCopy: String? =
                                                groupJoinKey
                                            val clipData = ClipData.newPlainText("groupJoinKey", codeToCopy)
                                            val clipEntry= ClipEntry(clipData)
                                            viewModel.viewModelScope.launch {
                                                clipboardManager.setClipEntry(clipEntry)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Group code copied!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        }
                                ) {
                                    Text(
                                        text = groupJoinKey?:"",  
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        letterSpacing = 4.sp
                                    )
                                }

                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                items(state.expenses) { expense ->
                                    ExpenseDetailCard(
                                        expenseDescription = expense.description ?: "N/A",
                                        paidBy = expense.paidByInfo?.get("name")?.toString() ?: expense.paidByUid,
                                         
                                        expenseAmount = currencyFormat.format(expense.amount),  
                                        contentDescription = "Expense icon",
                                        modifier = Modifier.clickable { navController.navigate(AppRoutes.expenseDetailsRoute(groupId,expense.expenseId)) }
                                    )
                                }
                            }
                        }
                    }
                }
                is ExpensesUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
