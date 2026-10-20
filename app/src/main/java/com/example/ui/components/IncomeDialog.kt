package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.OTHER_INCOME_CATEGORIES
import com.example.data.model.OtherIncomeItem
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils

/**
 * Modern Material 3 Dialog for recording and editing "Other Income" / "Extra Income" entries.
 *
 * Preset categories:
 * - "Accessory Sale"
 * - "Mobile Repair Service"
 * - "Commission"
 * - "Other"
 *
 * Rules:
 * - Date auto-defaults to current date in "DD/MM/YYYY" format.
 * - Included directly into both Total Net Balance and Monthly Cash In.
 * - DOES NOT interact with or reduce Phone/Device stock inventory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDialog(
    initialIncome: OtherIncomeItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        category: String,
        amount: Double,
        date: String,
        notes: String
    ) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val isEditing = initialIncome != null

    val defaultDate = remember {
        initialIncome?.date?.ifBlank { null } ?: DateUtils.formatLedgerDateOnly(System.currentTimeMillis())
    }

    var selectedCategory by remember {
        mutableStateOf(initialIncome?.category ?: OTHER_INCOME_CATEGORIES.first())
    }
    var amountText by remember {
        mutableStateOf(if (initialIncome != null && initialIncome.amountBdt > 0) String.format(java.util.Locale.US, "%.0f", initialIncome.amountBdt) else "")
    }
    var incomeDate by remember { mutableStateOf(defaultDate) }
    var notesText by remember { mutableStateOf(initialIncome?.notes ?: "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProfitGreen.copy(alpha = if (isDark) 0.25f else 0.15f),
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isEditing) "Edit Other Income" else "Add Other Income",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Track non-device shop cash inflows (accessories, repairs, servicing, or commission). Automatically reconciles with Monthly Cash In & Total Net Balance.",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                // Income Source / Category Dropdown
                Column {
                    Text(
                        text = "Income Source / Category *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Category",
                                        tint = if (isDark) Color(0xFF94A3B8) else PrimaryNavy
                                    )
                                }
                            },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("income_dialog_category_field")
                        )
                        // Clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { categoryDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            OTHER_INCOME_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = category,
                                            fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                                            color = if (category == selectedCategory) ProfitGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Numeric Input Field
                Column {
                    Text(
                        text = "Amount (BDT ৳) *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            amountText = filtered
                            if (errorMessage != null) errorMessage = null
                        },
                        placeholder = { Text("e.g. 1500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("income_dialog_amount_field")
                    )
                }

                // Date Picker (Defaults to DD/MM/YYYY)
                Column {
                    Text(
                        text = "Date (DD/MM/YYYY) *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AppDatePickerField(
                        label = "Income Date",
                        selectedDate = incomeDate,
                        onDateSelected = { selected ->
                            incomeDate = DateUtils.formatLedgerDateOnly(selected)
                        },
                        testTag = "income_dialog_date_field",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notes / Description (Optional)
                Column {
                    Text(
                        text = "Notes / Description (Optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("e.g. iPhone 13 Back Glass replacement, Fast Charger sale") },
                        minLines = 2,
                        maxLines = 3,
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("income_dialog_notes_field")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("income_dialog_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        errorMessage = "Please enter a valid income amount greater than 0"
                        return@Button
                    }
                    val finalDate = incomeDate.ifBlank { DateUtils.formatLedgerDateOnly(System.currentTimeMillis()) }
                    onConfirm(
                        selectedCategory,
                        amount,
                        finalDate,
                        notesText.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("income_dialog_confirm_button")
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "+ Save Income",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("income_dialog_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
            }
        }
    )
}
