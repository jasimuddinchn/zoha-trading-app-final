package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.CustomerDueProfile
import com.example.data.model.PhoneItem
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils

/**
 * Reusable Dialog for adding / recording an existing customer due or historical balance.
 * Features live, instant Customer Name auto-suggestions matching existing customers in the database.
 * Selecting a suggested customer auto-fills Name and Phone Number instantly.
 */
@Composable
fun AddExistingDueDialog(
    customerProfiles: List<CustomerDueProfile> = emptyList(),
    phones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        customerName: String,
        customerPhone: String,
        amount: Double,
        date: String,
        notes: String
    ) -> Unit
) {
    val isDark = isAppInDarkTheme()

    var custName by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    var dueAmountText by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(DateUtils.getTodayStorageDate()) }
    var dueNote by remember { mutableStateOf("") }
    var entryError by remember { mutableStateOf<String?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Aggregate distinct existing customers from customer profiles and phone sales
    val knownCustomers = remember(customerProfiles, phones) {
        val map = linkedMapOf<String, Pair<String, Double>>() // Name -> (Phone, remainingDue)
        for (profile in customerProfiles) {
            val name = profile.customerName.trim()
            if (name.isNotBlank() && !name.equals("Walk-in Customer", ignoreCase = true)) {
                map[name] = Pair(profile.customerPhone.trim(), profile.remainingDue)
            }
        }
        for (phone in phones) {
            val name = phone.customerName.trim()
            if (name.isNotBlank()) {
                val existing = map[name]
                if (existing == null) {
                    map[name] = Pair(phone.customerPhone.trim(), 0.0)
                } else if (existing.first.isBlank() && phone.customerPhone.isNotBlank()) {
                    map[name] = Pair(phone.customerPhone.trim(), existing.second)
                }
            }
        }
        map.map { Triple(it.key, it.value.first, it.value.second) }
    }

    // Filter live suggestions based on entered text (matches name or phone number)
    val matchedSuggestions = remember(custName, knownCustomers, showSuggestions) {
        if (!showSuggestions || custName.trim().isBlank()) {
            emptyList()
        } else {
            val q = custName.trim().lowercase()
            knownCustomers.filter { (name, phone, _) ->
                name.lowercase().contains(q) || phone.contains(q)
            }.take(5)
        }
    }

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
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Add Existing Due",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "Direct Customer Due",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Record existing customer due balance or opening credit without creating a phone sale.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Customer Name Input with live instant matching auto-suggestions
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = {
                            custName = it.uppercase()
                            entryError = null
                            showSuggestions = it.isNotBlank()
                        },
                        label = { Text("Customer Name *") },
                        placeholder = { Text("Type name to search existing customer...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (custName.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        custName = ""
                                        showSuggestions = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                            .testTag("add_existing_due_name_input")
                    )

                    // LIVE AUTO-SUGGESTION DROPDOWN / LIST
                    if (matchedSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_suggestions_list")
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "SUGGESTED EXISTING CUSTOMERS",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    matchedSuggestions.forEachIndexed { index, (sName, sPhone, sRemainingDue) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    custName = sName.uppercase()
                                                    if (sPhone.isNotBlank()) {
                                                        custPhone = sPhone
                                                    }
                                                    showSuggestions = false
                                                    entryError = null
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = PrimaryNavy,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = sName,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color.White else Slate900
                                                    )
                                                    if (sPhone.isNotBlank()) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.Phone,
                                                                contentDescription = null,
                                                                tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = sPhone,
                                                                fontSize = 11.sp,
                                                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (sRemainingDue > 0) {
                                                Surface(
                                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Due ৳${String.format(java.util.Locale.US, "%,.0f", sRemainingDue)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFEF4444),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    color = ProfitGreen.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Auto-fill",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ProfitGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (index < matchedSuggestions.size - 1) {
                                            HorizontalDivider(color = CardBorder.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Number Input
                OutlinedTextField(
                    value = custPhone,
                    onValueChange = {
                        custPhone = it
                        entryError = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 01712345678") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_existing_due_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Due Amount Input
                OutlinedTextField(
                    value = dueAmountText,
                    onValueChange = {
                        dueAmountText = it
                        entryError = null
                    },
                    label = { Text("Due Amount (৳ BDT) *") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_existing_due_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Due Date
                AppDatePickerField(
                    label = "Due Date (YYYY-MM-DD)",
                    selectedDate = dueDate,
                    onDateSelected = { dueDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes / Details
                OutlinedTextField(
                    value = dueNote,
                    onValueChange = { dueNote = it },
                    label = { Text("Note / Direct Due Details (Optional)") },
                    placeholder = { Text("e.g. Previous shop due / Old ledger balance") },
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (entryError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = entryError ?: "",
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
                    val name = custName.trim().uppercase()
                    if (name.isBlank()) {
                        entryError = "Please enter customer name!"
                        return@Button
                    }
                    val amount = dueAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        entryError = "Please enter a valid due amount greater than 0!"
                        return@Button
                    }
                    onConfirm(
                        name,
                        custPhone.trim(),
                        amount,
                        dueDate,
                        dueNote.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_existing_due_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Due Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate700)
            }
        }
    )
}
