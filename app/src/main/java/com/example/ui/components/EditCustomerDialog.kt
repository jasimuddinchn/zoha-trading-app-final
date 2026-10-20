package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme

/**
 * Admin Dialog to Edit a Customer's Name and Phone Number.
 * Propagates updates to all linked records in Firebase Realtime Database.
 */
@Composable
fun EditCustomerDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newPhone: String) -> Unit
) {
    val isDark = isAppInDarkTheme()

    var name by remember { mutableStateOf(initialName.uppercase()) }
    var phone by remember { mutableStateOf(initialPhone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryNavy.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Edit Customer Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "Updates customer name & phone across ledger",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("Customer Name *") },
                    placeholder = { Text("e.g. Tanvir Ahmed") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (name.isNotBlank()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = appTextFieldColors(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_customer_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Customer Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorMessage = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 01811-999999") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (phone.isNotBlank()) {
                            IconButton(onClick = { phone = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_customer_phone_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        errorMessage = "Customer name cannot be empty!"
                        return@Button
                    }
                    onConfirm(cleanName, phone.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_edit_customer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_edit_customer_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
