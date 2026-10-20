package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueUtils
import com.example.data.model.PAYMENT_METHODS_EN
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import java.util.Locale

/**
 * Dedicated Popup Dialog for collecting a due payment from a customer.
 * Allows entering received amount, selecting payment type (Cash, bKash, etc.),
 * collector staff, collection date, and optional receipt notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectDuePaymentDialog(
    profile: CustomerDueProfile,
    staffOptions: List<String> = listOf("Sumon", "Deedat", "Admin"),
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        paymentMethod: String,
        collectedBy: String,
        date: String,
        notes: String
    ) -> Unit
) {
    val isDark = isAppInDarkTheme()

    var collectAmountText by remember {
        mutableStateOf(String.format(Locale.US, "%.0f", profile.remainingDue))
    }
    var collectMethod by remember { mutableStateOf("Cash") }
    var methodExpanded by remember { mutableStateOf(false) }
    var collectedBy by remember { mutableStateOf(staffOptions.firstOrNull() ?: "Staff") }
    var staffExpanded by remember { mutableStateOf(false) }
    var collectionDate by remember { mutableStateOf(CustomerDueUtils.getTodayDate()) }
    var collectionNotes by remember { mutableStateOf("") }
    var modalError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ProfitGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Collect Due Payment",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = profile.customerName.trim().uppercase().ifBlank { "CUSTOMER" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                // Outstanding Due Balance Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Due Balance:",
                                fontSize = 12.5.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Slate700
                            )
                            Text(
                                text = "৳ ${String.format(Locale.US, "%,.0f", profile.remainingDue)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpenseRed
                            )
                        }

                        if (profile.customerPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profile.customerPhone,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount to Collect Input
                OutlinedTextField(
                    value = collectAmountText,
                    onValueChange = {
                        collectAmountText = it
                        modalError = null
                    },
                    label = { Text("Amount to Collect (৳ BDT) *") },
                    placeholder = { Text("Enter payment amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collect_due_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method / Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = collectMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method / Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("collect_due_method_select")
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                    ) {
                        PAYMENT_METHODS_EN.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method, color = if (isDark) Color.White else Slate900) },
                                onClick = {
                                    collectMethod = method
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Received By / Staff Dropdown
                ExposedDropdownMenuBox(
                    expanded = staffExpanded,
                    onExpandedChange = { staffExpanded = !staffExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = collectedBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Received By *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("collect_due_staff_select")
                    )
                    ExposedDropdownMenu(
                        expanded = staffExpanded,
                        onDismissRequest = { staffExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                    ) {
                        staffOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = if (isDark) Color.White else Slate900) },
                                onClick = {
                                    collectedBy = opt
                                    staffExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Collection Date Picker
                AppDatePickerField(
                    label = "Collection Date (DD-MMM-YYYY)",
                    selectedDate = collectionDate,
                    onDateSelected = { collectionDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Remarks / Notes
                OutlinedTextField(
                    value = collectionNotes,
                    onValueChange = { collectionNotes = it },
                    label = { Text("Receipt Remarks (Optional)") },
                    placeholder = { Text("e.g. Paid in shop / cash drawer") },
                    colors = appTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (modalError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = modalError ?: "",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = collectAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        modalError = "Please enter a valid amount!"
                        return@Button
                    }
                    if (amount > profile.remainingDue + 0.01) {
                        modalError = "Amount cannot exceed outstanding due of ৳ ${String.format(Locale.US, "%,.0f", profile.remainingDue)}"
                        return@Button
                    }
                    onConfirm(
                        amount,
                        collectMethod,
                        collectedBy,
                        collectionDate,
                        collectionNotes.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_collect_due_btn")
            ) {
                Text("Confirm Collection", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
            }
        },
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg,
        shape = RoundedCornerShape(18.dp)
    )
}
