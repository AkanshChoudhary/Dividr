package com.money.dividr.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.money.dividr.R  

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onJoinGroupClick: (String) -> Unit
) {
    if (showDialog) {
        var groupCode by remember { mutableStateOf("") }
        val maxCodeLength = 6

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "Join Group") },  
            text = {
                Column {
                    Text("Enter the 6-character code")  
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupCode,
                        onValueChange = {
                            if (it.length <= maxCodeLength) {
                                groupCode = it.filter { char -> char.isLetterOrDigit() }
                            }
                        },
                        label = { Text("Group Code") },  
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Unspecified
                        ),
                        placeholder = { Text("ABCXYZ") }  
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (groupCode.length == maxCodeLength) {
                            onJoinGroupClick(groupCode)
                        }
                         
                    },
                    enabled = groupCode.length == maxCodeLength
                ) {
                    Text("Join")  
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")  
                }
            }
        )
    }
}

 
/*
@Preview(showBackground = true)
@Composable
fun JoinGroupDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }
    MaterialTheme {  
        JoinGroupDialog(
            showDialog = showDialog,
            onDismissRequest = { showDialog = false },
            onJoinGroupClick = { code ->
                println("Joining group with code: $code")
                showDialog = false
            }
        )
    }
}
*/
