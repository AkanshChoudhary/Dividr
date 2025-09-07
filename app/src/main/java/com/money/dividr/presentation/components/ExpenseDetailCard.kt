package com.money.dividr.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.ColorFilter
import com.money.dividr.ui.theme.DividrTheme  

@Composable
fun ExpenseDetailCard(
    modifier: Modifier = Modifier,
    expenseDescription: String,
    paidBy: String,
    expenseAmount: String,
    contentDescription: String,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val iconWidth = 72.dp  

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),  
            verticalAlignment = Alignment.CenterVertically
        ) {
             
            Column(
                modifier = Modifier
                    .fillMaxHeight()  
                    .width(iconWidth)
                    .background(iconBackgroundColor),  
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(40.dp),  
                    colorFilter = ColorFilter.tint(iconTint),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

             
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)  
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),  
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = expenseDescription,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paid by $paidBy",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                 
                Text(
                    text = "$expenseAmount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp)  
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewExpenseDetailCard() {
    DividrTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpenseDetailCard(
                expenseDescription = "Dinner at The Grand",
                paidBy = "John Doe",
                expenseAmount = "125.50",
                contentDescription = "Shopping cart icon"
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExpenseDetailCard(
                expenseDescription = "Monthly Groceries",
                paidBy = "Alice Smith",
                expenseAmount = "85.20",
                contentDescription = "Groceries icon"
            )
        }
    }
}
