package com.money.dividr.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun CreateGroupDialog(
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onCreate: (groupName: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupNameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        groupNameError = null  
                    },
                    label = { Text("Group Name") },
                    singleLine = true,
                    isError = groupNameError != null
                )
                if (groupNameError != null) {
                    Text(
                        text = groupNameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName)
                    } else {
                        groupNameError = "Group name cannot be empty."
                    }
                },
                shape = RoundedCornerShape(50)  
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                shape = RoundedCornerShape(50)  
            ) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false)  
    )
}

@Preview(showBackground = true)
@Composable
fun CreateGroupDialogPreview() {
    MaterialTheme {
         
        var showDialog by remember { mutableStateOf(true) }
        if (showDialog) {
            CreateGroupDialog(
                onDismissRequest = { showDialog = false },
                onCancel = { showDialog = false },
                onCreate = { groupName ->
                    println("Group created: $groupName")
                    showDialog = false
                }
            )
        }
    }
}
