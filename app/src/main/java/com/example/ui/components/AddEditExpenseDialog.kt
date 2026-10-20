package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.EXPENSE_CATEGORIES_EN
import com.example.data.model.ExpenseItem
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.ReceiptImageHelper
import com.example.viewmodel.PhoneHisabViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val EXPENSE_DIALOG_CATEGORIES = listOf(
    "Shop Rent",
    "Service Charge",
    "Electricity Bill",
    "Internet Bill",
    "Shop Cleaner",
    "Courier Bill",
    "Salary",
    "Commission",
    "Guest",
    "Parts & Repairs",
    "Accessories",
    "Brac Bank Deposit",
    "City Bank Deposit",
    "UCB Bank Deposit",
    "Custom..."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    isBengali: Boolean = false,
    initialExpense: ExpenseItem? = null,
    onDismiss: () -> Unit,
    onAddExpense: (title: String, category: String, amountBdt: Double, date: String, notes: String, receiptUrl: String?) -> Unit,
    onUpdateExpense: ((ExpenseItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var categoriesList by remember {
        val baseList = (EXPENSE_CATEGORIES_EN + EXPENSE_DIALOG_CATEGORIES).distinct().toMutableList()
        if (baseList.remove("Custom...")) {
            baseList.add("Custom...")
        }
        initialExpense?.let {
            if (it.category.isNotBlank() && baseList.none { item -> item.equals(it.category, ignoreCase = true) }) {
                baseList.add(0, it.category)
            }
        }
        mutableStateOf(baseList.toList())
    }

    // Generate list of months for current year +/- 1 year
    val defaultMonths = remember {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val list = mutableListOf<String>()
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
        val temp = Calendar.getInstance()
        for (year in (currentYear - 1)..(currentYear + 1)) {
            for (m in 0..11) {
                temp.set(year, m, 1)
                list.add(formatter.format(temp.time))
            }
        }
        list + listOf("Custom...")
    }
    var dynamicMonthOptions by remember { mutableStateOf(defaultMonths) }

    val defaultCurrentMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.US).format(Date()) }
    var payForMonth by remember { mutableStateOf(initialExpense?.title ?: defaultCurrentMonth) }
    var payForMonthExpanded by remember { mutableStateOf(false) }
    var showCustomMonthDialog by remember { mutableStateOf(false) }

    var category by remember { mutableStateOf(initialExpense?.category ?: categoriesList.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }

    var amountText by remember {
        mutableStateOf(
            if (initialExpense != null && initialExpense.amountBdt > 0)
                String.format(Locale.US, "%.0f", initialExpense.amountBdt)
            else ""
        )
    }
    var date by remember { mutableStateOf(initialExpense?.date ?: PhoneHisabViewModel.getTodayDate()) }
    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var receiptUrl by remember { mutableStateOf(initialExpense?.receiptUrl) }
    var isUploadingReceipt by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = initialExpense != null
    val isDark = isAppInDarkTheme()

    // Android Photo Picker Launcher for zero-permission photo selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingReceipt = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val compressedBytes = ReceiptImageHelper.compressImage(context, uri)
                    val uploadedUrl = ReceiptImageHelper.uploadReceiptImage(context, compressedBytes, category)
                    receiptUrl = uploadedUrl
                } catch (e: Exception) {
                    errorMessage = "Notice saving receipt: ${e.message}"
                } finally {
                    isUploadingReceipt = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Edit Expense Entry" else "Add Expense Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // 1. Expense Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("expense_category_select")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        categoriesList.forEach { opt ->
                            val isCustom = opt.startsWith("Custom") || opt == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomCategoryDialog = true
                                    } else {
                                        category = opt
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Pay For Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = payForMonthExpanded,
                    onExpandedChange = { payForMonthExpanded = !payForMonthExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = payForMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pay For Month *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payForMonthExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("expense_pay_month_select")
                    )
                    ExposedDropdownMenu(
                        expanded = payForMonthExpanded,
                        onDismissRequest = { payForMonthExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicMonthOptions.forEach { monthOpt ->
                            val isCustom = monthOpt.startsWith("Custom") || monthOpt == "Other"
                            val isCurrentMonth = monthOpt == defaultCurrentMonth
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isCurrentMonth) "$monthOpt (Current)" else monthOpt,
                                        color = if (isDark) Color.White else (if (isCustom || isCurrentMonth) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom || isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom || isCurrentMonth) PrimaryNavy else Slate900)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomMonthDialog = true
                                    } else {
                                        payForMonth = monthOpt
                                    }
                                    payForMonthExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMessage = null },
                    label = { Text("Amount (৳ BDT) *") },
                    placeholder = { Text("1500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Date (Payment / Entry Date)
                AppDatePickerField(
                    label = "Payment Date (YYYY-MM-DD)",
                    selectedDate = date.ifBlank { PhoneHisabViewModel.getTodayDate() },
                    onDateSelected = { date = it },
                    testTag = "expense_date_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Payment voucher, receipt ref, etc.") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Expense Receipt Image Upload Section with Auto-Compression (max ~150KB)
                Text(
                    text = "Receipt Voucher / Photo (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isUploadingReceipt) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryNavy
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Compressing & uploading (< 150KB)...",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Slate700
                            )
                        }
                    }
                } else if (!receiptUrl.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(receiptUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Receipt Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = 0.2f))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Receipt Attached ✓",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    Text(
                                        text = "Max 150KB JPEG • Cloud Synced",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                                    )
                                }
                            }

                            Row {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Change", fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { receiptUrl = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove receipt",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.5f) else PrimaryNavy.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("attach_receipt_photo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attach Receipt Photo (Auto < 150KB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                        )
                    }
                }

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
                    if (payForMonth.isBlank()) {
                        errorMessage = "Please select the billing month!"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount!"
                        return@Button
                    }

                    val resolvedCategory = category.trim().ifBlank { "General" }
                    val resolvedTitle = payForMonth.trim().ifBlank { resolvedCategory }

                    if (isEditing && initialExpense != null && onUpdateExpense != null) {
                        onUpdateExpense(
                            initialExpense.copy(
                                title = resolvedTitle,
                                category = resolvedCategory,
                                amountBdt = amount,
                                date = date,
                                notes = notes,
                                receiptUrl = receiptUrl,
                                timestamp = initialExpense.timestamp
                            )
                        )
                    } else {
                        onAddExpense(resolvedTitle, resolvedCategory, amount, date, notes, receiptUrl)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_expense_btn")
            ) {
                Text(
                    text = if (isEditing) "Update Expense" else "Save Expense",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_expense_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )

    // Dynamic Custom Expense Category Popup Dialog
    if (showCustomCategoryDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Expense Category",
            label = "Expense Category",
            placeholder = "e.g. Shop Renovation, Tea & Snacks",
            testTagPrefix = "expense_custom_category",
            onConfirm = { customVal ->
                category = customVal
                if (!categoriesList.contains(customVal)) {
                    val updated = categoriesList.toMutableList()
                    val customIdx = updated.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    if (customIdx != -1) {
                        updated.add(customIdx, customVal)
                    } else {
                        updated.add(customVal)
                    }
                    categoriesList = updated
                }
                showCustomCategoryDialog = false
            },
            onDismiss = { showCustomCategoryDialog = false }
        )
    }

    // Dynamic Custom Payment Month Popup Dialog
    if (showCustomMonthDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Payment Month",
            label = "Payment Month / Description",
            placeholder = "e.g. September 2026 (Advance), Eid Bonus",
            testTagPrefix = "expense_custom_month",
            onConfirm = { customMonth ->
                payForMonth = customMonth
                if (!dynamicMonthOptions.contains(customMonth)) {
                    val updated = dynamicMonthOptions.toMutableList()
                    val customIdx = updated.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    if (customIdx != -1) {
                        updated.add(customIdx, customMonth)
                    } else {
                        updated.add(customMonth)
                    }
                    dynamicMonthOptions = updated
                }
                showCustomMonthDialog = false
            },
            onDismiss = { showCustomMonthDialog = false }
        )
    }
}
