package com.example.ui.components

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import com.example.data.model.PAYMENT_METHODS_EN
import com.example.data.model.PhoneItem
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.PhoneHisabViewModel

/**
 * Sale Entry Dialog with complete Partial Payment / Credit Breakdown:
 * 1. Captures `Total Sale Price` and `Cash Received`.
 * 2. Calculates `Due Amount = Total Sale Price - Cash Received`.
 * 3. Shows live visual breakdown of Cash In vs Due.
 * 4. Mandates Customer Name & Phone Number when Due Amount > 0 so that it correctly posts to `/customer_dues`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSaleDialog(
    phone: PhoneItem,
    defaultStaffName: String,
    staff1Name: String,
    staff2Name: String,
    isBengali: Boolean = false,
    customerProfiles: List<CustomerDueProfile> = emptyList(),
    phones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSale: (
        totalSalePrice: Double,
        cashReceived: Double,
        dueAmount: Double,
        saleDate: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        soldByStaff: String,
        paymentMethod: String,
        warrantyDays: Int,
        notes: String
    ) -> Unit
) {
    var dynamicStaffOptions by remember {
        mutableStateOf(
            listOf(staff1Name, staff2Name, "Admin").filter { it.isNotBlank() }.distinct() + listOf("Custom...")
        )
    }
    var dynamicPaymentMethods by remember { mutableStateOf(PAYMENT_METHODS_EN) }

    var totalSalePriceText by remember { mutableStateOf("") }
    var cashReceivedText by remember { mutableStateOf("") }
    var isCashReceivedManuallyEdited by remember { mutableStateOf(false) }

    var saleDate by remember { mutableStateOf(PhoneHisabViewModel.getTodayDate()) }
    var customerName by remember { mutableStateOf(phone.customerName.uppercase()) }
    var customerPhone by remember { mutableStateOf(phone.customerPhone) }
    var customerEmail by remember { mutableStateOf(phone.customerEmail) }
    var soldByStaff by remember { mutableStateOf(defaultStaffName) }
    var staffExpanded by remember { mutableStateOf(false) }
    var showCustomStaffDialog by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Aggregate distinct existing customers from customer profiles and phone sales
    val knownCustomers = remember(customerProfiles, phones) {
        val map = linkedMapOf<String, Triple<String, String, Double>>() // Name Upper -> (DisplayName, Phone, remainingDue)
        for (profile in customerProfiles) {
            val name = profile.customerName.trim()
            val phoneNum = profile.customerPhone.trim()
            if (name.isNotBlank() && !name.equals("Walk-in Customer", ignoreCase = true)) {
                val key = name.uppercase()
                val existing = map[key]
                if (existing == null) {
                    map[key] = Triple(name, phoneNum, profile.remainingDue)
                } else {
                    val bestPhone = if (existing.second.isBlank() && phoneNum.isNotBlank()) phoneNum else existing.second
                    val bestDue = maxOf(existing.third, profile.remainingDue)
                    map[key] = Triple(existing.first, bestPhone, bestDue)
                }
            }
        }
        for (item in phones) {
            val name = item.customerName.trim()
            val phoneNum = item.customerPhone.trim()
            if (name.isNotBlank() && !name.equals("Walk-in Customer", ignoreCase = true)) {
                val key = name.uppercase()
                val existing = map[key]
                if (existing == null) {
                    map[key] = Triple(name, phoneNum, 0.0)
                } else if (existing.second.isBlank() && phoneNum.isNotBlank()) {
                    map[key] = Triple(existing.first, phoneNum, existing.third)
                }
            }
        }
        map.values.toList()
    }

    // Filter live suggestions based on entered text (matches name or phone number)
    val matchedSuggestions = remember(customerName, knownCustomers, showSuggestions) {
        if (!showSuggestions || customerName.trim().isBlank()) {
            emptyList()
        } else {
            val q = customerName.trim().lowercase()
            val cleanDigits = q.replace(Regex("[^0-9]"), "")
            knownCustomers.filter { (name, phoneNum, _) ->
                val nameMatch = name.lowercase().contains(q)
                val phoneMatch = phoneNum.lowercase().contains(q) || (cleanDigits.length >= 3 && phoneNum.replace(Regex("[^0-9]"), "").contains(cleanDigits))
                nameMatch || phoneMatch
            }.take(6)
        }
    }

    var paymentMethod by remember { mutableStateOf(dynamicPaymentMethods.first()) }
    var paymentExpanded by remember { mutableStateOf(false) }
    var showCustomPaymentMethodDialog by remember { mutableStateOf(false) }

    var warrantyDaysText by remember { mutableStateOf("10") }
    var dueNotesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDark = isAppInDarkTheme()

    // Calculated amounts
    val parsedTotalSalePrice = totalSalePriceText.toDoubleOrNull() ?: 0.0
    val parsedCashReceived = if (cashReceivedText.isBlank()) {
        if (!isCashReceivedManuallyEdited) parsedTotalSalePrice else 0.0
    } else {
        cashReceivedText.toDoubleOrNull() ?: 0.0
    }
    val calculatedDueAmount = (parsedTotalSalePrice - parsedCashReceived).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCartCheckout,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sale Entry & Credit Ledger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "${phone.brand} ${phone.model} (${phone.ramRom})",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFA0AEC0) else Slate700
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Phone Specs Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "IMEI: ${phone.imei}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Slate900
                            )
                            Text(
                                text = "${phone.color} | ${phone.conditionGrade}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                        if (phone.askingPriceBdt > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎯 Target Asking Price: ৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Sale Price (BDT)
                OutlinedTextField(
                    value = totalSalePriceText,
                    onValueChange = { input ->
                        totalSalePriceText = input
                        errorMessage = null
                        if (!isCashReceivedManuallyEdited) {
                            cashReceivedText = input
                        }
                    },
                    label = { Text("Total Sale Price (৳ BDT) *") },
                    placeholder = { Text("e.g. 55000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_sale_price_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cash Received (BDT)
                OutlinedTextField(
                    value = cashReceivedText,
                    onValueChange = { input ->
                        cashReceivedText = input
                        isCashReceivedManuallyEdited = true
                        errorMessage = null
                    },
                    label = { Text("Cash Received (৳ BDT) *") },
                    placeholder = { Text("e.g. 40000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_received_input")
                )

                // Quick presets for Cash Received
                if (parsedTotalSalePrice > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                cashReceivedText = String.format(java.util.Locale.US, "%.0f", parsedTotalSalePrice)
                                isCashReceivedManuallyEdited = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (parsedCashReceived == parsedTotalSalePrice && parsedTotalSalePrice > 0) ProfitGreen.copy(alpha = 0.15f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (parsedCashReceived == parsedTotalSalePrice) ProfitGreen else CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Full Cash",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (parsedCashReceived == parsedTotalSalePrice) ProfitGreen else if (isDark) Color(0xFFCBD5E1) else Slate700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                val half = (parsedTotalSalePrice / 2.0)
                                cashReceivedText = String.format(java.util.Locale.US, "%.0f", half)
                                isCashReceivedManuallyEdited = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "50% Cash",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                cashReceivedText = "0"
                                isCashReceivedManuallyEdited = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (parsedCashReceived == 0.0 && parsedTotalSalePrice > 0) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (parsedCashReceived == 0.0 && parsedTotalSalePrice > 0) ExpenseRed else CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Full Credit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (parsedCashReceived == 0.0 && parsedTotalSalePrice > 0) ExpenseRed else if (isDark) Color(0xFFCBD5E1) else Slate700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LIVE DUE & CASH RECONCILIATION CARD
                if (parsedTotalSalePrice > 0) {
                    val isDue = calculatedDueAmount > 0.0
                    val bannerBg = if (isDue) {
                        if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                    } else {
                        if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                    }
                    val bannerBorder = if (isDue) Color(0xFFF59E0B) else ProfitGreen
                    val bannerTextColor = if (isDue) Color(0xFFD97706) else ProfitGreen

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bannerBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDue) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = bannerTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDue) "Remaining Due Amount" else "Full Payment Complete",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = bannerTextColor
                                    )
                                }
                                Text(
                                    text = if (isDue) "৳ ${String.format("%,.0f", calculatedDueAmount)}" else "৳ 0 Due",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = bannerTextColor
                                )
                            }
                            if (isDue) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ৳ ${String.format("%,.0f", parsedCashReceived)} will be added to Cash In & physical drawer.\n• ৳ ${String.format("%,.0f", calculatedDueAmount)} will be saved to Customer's Due Ledger (/customer_dues).",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Customer Name (Mandatory if Due > 0) with live auto-suggestions & phone auto-fill
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            customerName = it.uppercase()
                            errorMessage = null
                            showSuggestions = it.isNotBlank()
                        },
                        label = {
                            Text(if (calculatedDueAmount > 0) "Customer Name * (Required for Due)" else "Customer Name")
                        },
                        placeholder = { Text("e.g. TANVIR AHMED") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (customerName.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        customerName = ""
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
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
                                .testTag("sale_customer_suggestions_list")
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
                                                    customerName = sName.uppercase()
                                                    if (sPhone.isNotBlank()) {
                                                        customerPhone = sPhone
                                                    }
                                                    showSuggestions = false
                                                    errorMessage = null
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
                                                                color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (sRemainingDue > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                                                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                                ) {
                                                    Text(
                                                        text = "Due: ৳ ${String.format("%,.0f", sRemainingDue)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (index < matchedSuggestions.lastIndex) {
                                            HorizontalDivider(color = CardBorder.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Phone (Mandatory if Due > 0)
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                        errorMessage = null
                    },
                    label = {
                        Text(if (calculatedDueAmount > 0) "Customer Phone * (Required for Due)" else "Customer Phone Number")
                    },
                    placeholder = { Text("017XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sale Date (Calendar Date Picker)
                AppDatePickerField(
                    label = "Sale Date (YYYY-MM-DD) *",
                    selectedDate = saleDate.ifBlank { PhoneHisabViewModel.getTodayDate() },
                    onDateSelected = { saleDate = it },
                    testTag = "sale_date_input",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seller / Staff Selector
                ExposedDropdownMenuBox(
                    expanded = staffExpanded,
                    onExpandedChange = { staffExpanded = !staffExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = soldByStaff,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seller (Staff / Admin) *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("salesman_select")
                    )
                    ExposedDropdownMenu(
                        expanded = staffExpanded,
                        onDismissRequest = { staffExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicStaffOptions.forEach { opt ->
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
                                        showCustomStaffDialog = true
                                    } else {
                                        soldByStaff = opt
                                    }
                                    staffExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method (for cash portion)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("payment_method_select")
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        dynamicPaymentMethods.forEach { method ->
                            val isCustom = method.startsWith("Custom") || method == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = method,
                                        color = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900),
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else (if (isCustom) PrimaryNavy else Slate900)
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomPaymentMethodDialog = true
                                    } else {
                                        paymentMethod = method
                                    }
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Email (Optional)
                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Customer Email (Optional - For Auto Invoice)") },
                    placeholder = { Text("buyer@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_email_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Warranty Days
                OutlinedTextField(
                    value = warrantyDaysText,
                    onValueChange = { warrantyDaysText = it },
                    label = { Text("Warranty (Days)") },
                    placeholder = { Text("10") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("warranty_days_input")
                )

                // Due Remarks / Promise Date
                if (calculatedDueAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dueNotesText,
                        onValueChange = { dueNotesText = it },
                        label = { Text("Due Notes / Promise Date (Optional)") },
                        placeholder = { Text("e.g. Promised to clear remaining by 20th") },
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("due_notes_input")
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = totalSalePriceText.toDoubleOrNull()
                    if (price == null || price <= 0) {
                        errorMessage = "Please enter a valid total sale price!"
                        return@Button
                    }
                    val cash = if (cashReceivedText.isBlank()) {
                        if (!isCashReceivedManuallyEdited) price else 0.0
                    } else {
                        cashReceivedText.toDoubleOrNull() ?: -1.0
                    }
                    if (cash < 0.0 || cash > price) {
                        errorMessage = "Cash received cannot be negative or greater than sale price!"
                        return@Button
                    }
                    val due = price - cash

                    if (due > 0) {
                        if (customerName.trim().isBlank()) {
                            errorMessage = "Customer Name is required when recording a credit/due sale!"
                            return@Button
                        }
                        if (customerPhone.trim().isBlank()) {
                            errorMessage = "Customer Phone is required to track credit in the Due Ledger!"
                            return@Button
                        }
                    }

                    val days = warrantyDaysText.toIntOrNull() ?: 10
                    onConfirmSale(
                        price,
                        cash,
                        due,
                        saleDate,
                        customerName.trim().uppercase(),
                        customerPhone.trim(),
                        customerEmail.trim(),
                        soldByStaff,
                        paymentMethod,
                        days,
                        dueNotesText.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calculatedDueAmount > 0) Color(0xFFD97706) else ProfitGreen
                ),
                modifier = Modifier.testTag("confirm_sale_submit_btn")
            ) {
                Text(
                    text = if (calculatedDueAmount > 0) "Post Credit Sale (৳ ${String.format("%,.0f", calculatedDueAmount)} Due)" else "Complete Sale",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_sale_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700, fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )

    // Custom Staff / Seller Dialog
    if (showCustomStaffDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Seller / Staff Name",
            label = "Staff / Seller Name",
            placeholder = "e.g. Shakil Ahmed, Manager",
            initialValue = if (soldByStaff in dynamicStaffOptions && !soldByStaff.startsWith("Custom")) "" else soldByStaff,
            onDismiss = { showCustomStaffDialog = false },
            onConfirm = { customValue ->
                soldByStaff = customValue
                if (!dynamicStaffOptions.contains(customValue)) {
                    val customIdx = dynamicStaffOptions.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicStaffOptions = if (customIdx >= 0) {
                        dynamicStaffOptions.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicStaffOptions + customValue
                    }
                }
                showCustomStaffDialog = false
            }
        )
    }

    // Custom Payment Method Dialog
    if (showCustomPaymentMethodDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Payment Method",
            label = "Payment Method",
            placeholder = "e.g. Crypto / USDT, City Bank POS",
            initialValue = if (paymentMethod in PAYMENT_METHODS_EN) "" else paymentMethod,
            onDismiss = { showCustomPaymentMethodDialog = false },
            onConfirm = { customValue ->
                paymentMethod = customValue
                if (!dynamicPaymentMethods.contains(customValue)) {
                    val customIdx = dynamicPaymentMethods.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicPaymentMethods = if (customIdx >= 0) {
                        dynamicPaymentMethods.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicPaymentMethods + customValue
                    }
                }
                showCustomPaymentMethodDialog = false
            }
        )
    }
}
