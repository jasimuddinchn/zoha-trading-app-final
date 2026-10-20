package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.PhoneItem
import com.example.data.model.UserRole
import com.example.ui.components.AppSearchBar
import com.example.ui.components.CollectDuePaymentDialog
import com.example.ui.components.CustomerLedgerDialog
import com.example.ui.components.SectionBadgeTitle
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.AppDatePickerField
import com.example.ui.theme.appTextFieldColors

import com.example.ui.components.AddExistingDueDialog
import androidx.compose.material.icons.filled.FilterList

/**
 * Dedicated Customer Due Management Page & Ledger:
 * - Single filter button: "Due customers only"
 * - "+ Add Existing Due" button allows direct entry/adjustment of historical dues with live auto-suggestions.
 * - Protects Edit and Delete actions across dues with Admin PIN / Biometric Authentication.
 * - Shows total aggregate outstanding customer due amount at the top.
 * - Collect Due action immediately reconciles cash drawer without inflating Monthly Cash In.
 */
@Composable
fun DueManagementScreen(
    customerProfiles: List<CustomerDueProfile>,
    totalOutstandingDue: Double,
    totalDueCollected: Double,
    currentRole: UserRole,
    staffOptions: List<String> = listOf("Sumon", "Deedat", "Admin"),
    isBengali: Boolean = false,
    phones: List<PhoneItem> = emptyList(),
    onCollectDue: (
        customerKey: String,
        customerName: String,
        customerPhone: String,
        amount: Double,
        paymentMethod: String,
        collectedBy: String,
        date: String,
        notes: String
    ) -> Unit,
    onAddExistingDue: ((
        customerName: String,
        customerPhone: String,
        dueAmount: Double,
        date: String,
        notes: String
    ) -> Unit)? = null,
    onDeleteCustomerDue: ((dueRecordId: String, customerKey: String) -> Unit)? = null,
    onDeleteDuePayment: ((paymentRecordId: String, customerKey: String) -> Unit)? = null,
    onUpdateCustomerDetails: ((customerKey: String, newName: String, newPhone: String) -> Unit)? = null,
    onUpdateCustomerDueRecord: ((
        recordId: String,
        customerKey: String,
        newDueAmount: Double,
        newTotalSalePrice: Double,
        newCashReceived: Double,
        newDate: String,
        newNotes: String
    ) -> Unit)? = null,
    onUpdateDuePayment: ((
        paymentId: String,
        customerKey: String,
        newAmount: Double,
        newPaymentMethod: String,
        newCollectedBy: String,
        newDate: String,
        newNotes: String
    ) -> Unit)? = null,
    onRequireAdminAuth: ((
        title: String,
        actionTitle: String,
        actionDesc: String,
        icon: ImageVector,
        confirmText: String,
        isDanger: Boolean,
        onAuth: () -> Unit
    ) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val isAdmin = currentRole == UserRole.ADMIN

    var searchQuery by remember { mutableStateOf("") }
    // Tab selection: false = "All Customers", true = "Due Customers Only"
    var filterOnlyDueCustomers by remember { mutableStateOf(true) }
    var selectedProfileForLedger by remember { mutableStateOf<CustomerDueProfile?>(null) }
    var selectedProfileForCollect by remember { mutableStateOf<CustomerDueProfile?>(null) }
    var showAddExistingDueDialog by remember { mutableStateOf(false) }

    val distinctProfiles = remember(customerProfiles) {
        customerProfiles.distinctBy { it.customerKey }
    }

    val dueCustomersCount = distinctProfiles.count { it.remainingDue > 0.0 }
    val allCustomersCount = distinctProfiles.size

    val filteredList = remember(distinctProfiles, searchQuery, filterOnlyDueCustomers) {
        distinctProfiles.filter { profile ->
            val filterMatch = if (filterOnlyDueCustomers) profile.remainingDue > 0.0 else true
            val queryMatch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                profile.customerName.lowercase().contains(q) ||
                profile.customerPhone.contains(q) ||
                profile.creditSales.any {
                    it.phoneModel.lowercase().contains(q) || it.imei.contains(q)
                }
            }
            filterMatch && queryMatch
        }.distinctBy { it.customerKey }.sortedByDescending { it.remainingDue }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBg)
            .testTag("due_management_screen")
    ) {
        // Search Bar & + Add Existing Due Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search customer name, phone, or IMEI...",
                testTag = "due_search_input",
                modifier = Modifier.weight(1f)
            )

            if (isAdmin && onAddExistingDue != null) {
                Button(
                    onClick = {
                        if (onRequireAdminAuth != null) {
                            onRequireAdminAuth(
                                "Admin Authentication",
                                "Add Existing Due",
                                "Authorize Direct Due entry for customer ledger",
                                Icons.Default.PersonAdd,
                                "Authorize Entry",
                                false
                            ) { showAddExistingDueDialog = true }
                        } else {
                            showAddExistingDueDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_existing_due_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Existing Due",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Due",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AGGREGATE TOTAL DUE BANNER CARD
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF27130A) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aggregate_due_banner_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Due",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                )
                            }

                            Surface(
                                color = if (dueCustomersCount > 0) Color(0xFFEF4444).copy(alpha = 0.15f) else ProfitGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$dueCustomersCount Customers Due",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dueCustomersCount > 0) Color(0xFFEF4444) else ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Aggregate Amount Display
                        Text(
                            text = "৳ ${String.format("%,.0f", totalOutstandingDue)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalOutstandingDue > 0) Color(0xFFEA580C) else ProfitGreen,
                            modifier = Modifier.testTag("aggregate_total_due_text")
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFFDE68A).copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Aggregate metric
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Due Collected:", fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Slate700)
                                Text(
                                    text = "৳ ${String.format("%,.0f", totalDueCollected)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                }
            }

            // TABS: "All Customers" and ONE "Due customers only" filter button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Customers Tab
                    Surface(
                        onClick = { filterOnlyDueCustomers = false },
                        shape = RoundedCornerShape(20.dp),
                        color = if (!filterOnlyDueCustomers) PrimaryNavy else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (!filterOnlyDueCustomers) PrimaryNavy else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_all_customers_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "All Customers",
                                fontSize = 12.sp,
                                fontWeight = if (!filterOnlyDueCustomers) FontWeight.Bold else FontWeight.Medium,
                                color = if (!filterOnlyDueCustomers) Color.White else if (isDark) Color(0xFFCBD5E1) else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (!filterOnlyDueCustomers) Color.White.copy(alpha = 0.25f) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$allCustomersCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!filterOnlyDueCustomers) Color.White else if (isDark) Color.White else Slate900,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // ONE "Due customers only" Filter Button
                    Surface(
                        onClick = { filterOnlyDueCustomers = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (filterOnlyDueCustomers) Color(0xFFEA580C) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (filterOnlyDueCustomers) Color(0xFFEA580C) else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_due_customers_only_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (filterOnlyDueCustomers) Color.White else Color.Transparent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Due customers only",
                                fontSize = 12.sp,
                                fontWeight = if (filterOnlyDueCustomers) FontWeight.Bold else FontWeight.Medium,
                                color = if (filterOnlyDueCustomers) Color.White else if (isDark) Color(0xFFCBD5E1) else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (filterOnlyDueCustomers) Color.White.copy(alpha = 0.25f) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$dueCustomersCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filterOnlyDueCustomers) Color.White else if (isDark) Color.White else Slate900,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // CUSTOMER DUE LIST
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching customer accounts found" else "All customer due balances are 100% settled!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Slate800,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Zero pending credit balances recorded.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.customerKey }) { profile ->
                    val hasDue = profile.remainingDue > 0.0
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF141936) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (hasDue) Color(0xFFF59E0B).copy(alpha = 0.5f) else CardBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_due_card_${profile.customerKey}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProfileForCollect = null
                                        selectedProfileForLedger = profile
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (hasDue) Color(0xFFF59E0B).copy(alpha = 0.15f) else ProfitGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = if (hasDue) Color(0xFFD97706) else ProfitGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = profile.customerName.trim().uppercase().ifBlank { "WALK-IN CUSTOMER" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = profile.customerPhone.ifBlank { "No phone" },
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                            )
                                        }
                                    }
                                }

                                // Due Badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = if (hasDue) Color(0xFFDC2626).copy(alpha = 0.15f) else ProfitGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (hasDue) "Due: ৳ ${String.format("%,.0f", profile.remainingDue)}" else "BDT 0 - Clear",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (hasDue) Color(0xFFDC2626) else ProfitGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (profile.creditSales.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${profile.creditSales.size} credit sales",
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Stats breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Credit: ৳ ${String.format("%,.0f", profile.totalCreditSales)}",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                    )
                                    Text(
                                        text = "Due Collected: ৳ ${String.format("%,.0f", profile.totalDueCollected)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ProfitGreen
                                    )
                                    if (profile.lastActivityDate.isNotBlank()) {
                                        Text(
                                            text = "Activity: ${DateUtils.formatLedgerDateOnly(profile.lastActivityDate)}",
                                            fontSize = 9.5.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            selectedProfileForLedger = null
                                            selectedProfileForCollect = profile
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasDue) Color(0xFF059669) else Color(0xFF0D9488),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("collect_button_${profile.customerKey}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Collect Due Payment",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Collect",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            selectedProfileForCollect = null
                                            selectedProfileForLedger = profile
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Color(0xFF1E3A8A) else PrimaryNavy,
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8) else PrimaryNavy),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("ledger_button_${profile.customerKey}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "Customer Ledger Statement",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Ledger",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Customer History Ledger & Payment Collection Modal
    val activeLedgerProfile = selectedProfileForLedger?.let { sel ->
        customerProfiles.find { it.customerKey == sel.customerKey } ?: sel
    }
    activeLedgerProfile?.let { profile ->
        CustomerLedgerDialog(
            profile = profile,
            staffOptions = staffOptions,
            isBengali = isBengali,
            isAdmin = isAdmin,
            onDismiss = { selectedProfileForLedger = null },
            onCollectDue = { amount, method, collector, date, notes ->
                onCollectDue(
                    profile.customerKey,
                    profile.customerName,
                    profile.customerPhone,
                    amount,
                    method,
                    collector,
                    date,
                    notes
                )
                selectedProfileForLedger = null
                Toast.makeText(context, "Collected ৳ ${String.format("%,.0f", amount)} from ${profile.customerName}! Cash drawer updated.", Toast.LENGTH_LONG).show()
            },
            onDeletePayment = { paymentId ->
                onDeleteDuePayment?.invoke(paymentId, profile.customerKey)
                Toast.makeText(context, "Payment record voided.", Toast.LENGTH_SHORT).show()
            },
            onUpdateDuePayment = { paymentId, newAmount, newMethod, newStaff, newDate, newNotes ->
                onUpdateDuePayment?.invoke(
                    paymentId,
                    profile.customerKey,
                    newAmount,
                    newMethod,
                    newStaff,
                    newDate,
                    newNotes
                )
            },
            onDeleteCustomerDue = { dueId ->
                onDeleteCustomerDue?.invoke(dueId, profile.customerKey)
                Toast.makeText(context, "Direct due entry deleted.", Toast.LENGTH_SHORT).show()
            },
            onUpdateCustomerDue = { dueId, newAmount, newDate, newNotes ->
                onUpdateCustomerDueRecord?.invoke(
                    dueId,
                    profile.customerKey,
                    newAmount,
                    newAmount,
                    0.0,
                    newDate,
                    newNotes
                )
                Toast.makeText(context, "Direct due entry updated successfully.", Toast.LENGTH_SHORT).show()
            },
            onRequireAdminAuth = onRequireAdminAuth
        )
    }

    // Dedicated Customer Payment Collection Dialog Popup
    val activeCollectProfile = selectedProfileForCollect?.let { sel ->
        customerProfiles.find { it.customerKey == sel.customerKey } ?: sel
    }
    activeCollectProfile?.let { profile ->
        CollectDuePaymentDialog(
            profile = profile,
            staffOptions = staffOptions,
            onDismiss = { selectedProfileForCollect = null },
            onConfirm = { amount, method, collector, date, notes ->
                onCollectDue(
                    profile.customerKey,
                    profile.customerName,
                    profile.customerPhone,
                    amount,
                    method,
                    collector,
                    date,
                    notes
                )
                selectedProfileForCollect = null
                Toast.makeText(
                    context,
                    "Collected ৳ ${String.format(java.util.Locale.US, "%,.0f", amount)} from ${profile.customerName}! Cash drawer updated.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Direct Due Entry / Adjustment Dialog ("+ Add Existing Due") with live auto-suggestions
    if (showAddExistingDueDialog) {
        AddExistingDueDialog(
            customerProfiles = customerProfiles,
            phones = phones,
            onDismiss = { showAddExistingDueDialog = false },
            onConfirm = { name, phone, amount, date, notes ->
                onAddExistingDue?.invoke(name, phone, amount, date, notes)
                showAddExistingDueDialog = false
                Toast.makeText(context, "Direct due of ৳ ${String.format("%,.0f", amount)} added for $name!", Toast.LENGTH_LONG).show()
            }
        )
    }
}
