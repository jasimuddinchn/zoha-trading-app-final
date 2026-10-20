package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
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
import com.example.util.DateUtils

sealed class LedgerHistoryEntry {
    data class CreditSale(val record: CustomerDueRecord) : LedgerHistoryEntry()
    data class PaymentCollection(val record: DuePaymentRecord) : LedgerHistoryEntry()

    val timestamp: Long
        get() = when (this) {
            is CreditSale -> {
                val parsed = if (record.saleDate.isNotBlank()) DateUtils.parseDateToMillis(record.saleDate) else 0L
                if (parsed > 0L) parsed else record.timestamp
            }
            is PaymentCollection -> {
                val parsed = if (record.date.isNotBlank()) DateUtils.parseDateToMillis(record.date) else 0L
                if (parsed > 0L) parsed else record.timestamp
            }
        }
}

/**
 * Spacious Popup Dialog for Customer Ledger displaying all past credit sales,
 * collected payments, remaining due balance, and inline collection workflow.
 *
 * Rules:
 * - Dates strictly formatted in DD-MMM-YYYY (e.g. 12-Sep-2026).
 * - Date edits immediately update both timestamp and date children in database and UI.
 * - Ample padding, clear typography, and high-contrast pill/chip designs.
 * - Admin Edit & Delete controls protected by PIN/Biometric authentication.
 * - Chronological descending order (newest entry at the top).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerDialog(
    profile: CustomerDueProfile,
    staffOptions: List<String> = listOf("Sumon", "Deedat", "Admin"),
    isBengali: Boolean = false,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onCollectDue: (
        amount: Double,
        paymentMethod: String,
        collectedBy: String,
        date: String,
        notes: String
    ) -> Unit,
    onDeletePayment: ((paymentId: String) -> Unit)? = null,
    onUpdateDuePayment: ((
        paymentId: String,
        newAmount: Double,
        newPaymentMethod: String,
        newCollectedBy: String,
        newDate: String,
        newNotes: String
    ) -> Unit)? = null,
    onDeleteCustomerDue: ((dueId: String) -> Unit)? = null,
    onUpdateCustomerDue: ((
        dueId: String,
        newDueAmount: Double,
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
    ) -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    var showCollectionModal by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<DuePaymentRecord?>(null) }
    var editingDirectDue by remember { mutableStateOf<CustomerDueRecord?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "SALES", "PAYMENTS"

    val historyEntries = remember(profile) {
        val sales = profile.creditSales
            .distinctBy {
                when {
                    it.phoneId > 0L -> "phone_${it.phoneId}"
                    it.imei.isNotBlank() -> "imei_${it.imei.trim()}"
                    it.id.isNotBlank() -> "id_${it.id}"
                    else -> "sale_${it.saleDate}_${it.dueAmount}_${it.timestamp}"
                }
            }
            .map { LedgerHistoryEntry.CreditSale(it) }
        val payments = profile.paymentCollections
            .distinctBy {
                if (it.id.isNotBlank()) "pay_${it.id}"
                else "pay_${it.customerKey}_${it.date}_${it.amount}_${it.timestamp}"
            }
            .map { LedgerHistoryEntry.PaymentCollection(it) }
        (sales + payments).distinctBy {
            when (it) {
                is LedgerHistoryEntry.CreditSale -> "sale_${it.record.phoneId}_${it.record.id.ifBlank { "${it.record.saleDate}_${it.record.dueAmount}" }}"
                is LedgerHistoryEntry.PaymentCollection -> "pay_${it.record.id.ifBlank { "${it.record.date}_${it.record.amount}" }}"
            }
        }.sortedByDescending { it.timestamp }
    }

    val filteredEntries = remember(historyEntries, selectedFilter) {
        val list = when (selectedFilter) {
            "SALES" -> historyEntries.filterIsInstance<LedgerHistoryEntry.CreditSale>()
            "PAYMENTS" -> historyEntries.filterIsInstance<LedgerHistoryEntry.PaymentCollection>()
            else -> historyEntries
        }
        list.distinctBy {
            when (it) {
                is LedgerHistoryEntry.CreditSale -> "sale_${it.record.phoneId}_${it.record.id.ifBlank { "${it.record.saleDate}_${it.record.dueAmount}" }}"
                is LedgerHistoryEntry.PaymentCollection -> "pay_${it.record.id.ifBlank { "${it.record.date}_${it.record.amount}" }}"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .testTag("customer_ledger_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Header Row with Customer Identity (Upper-case guaranteed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, PrimaryNavy.copy(alpha = 0.3f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = PrimaryNavy,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile.customerName.trim().uppercase().ifBlank { "WALK-IN CUSTOMER" },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profile.customerPhone.ifBlank { "No Phone" },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color.White else Slate900,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Outstanding Due Summary Card with High-Contrast Pill/Chip Styling
                val isDue = profile.remainingDue > 0.0
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDue) {
                            if (isDark) Color(0xFF3B1803) else Color(0xFFFFFBEB)
                        } else {
                            if (isDark) Color(0xFF04382A) else Color(0xFFECFDF5)
                        }
                    ),
                    border = BorderStroke(1.5.dp, if (isDue) Color(0xFFF59E0B) else ProfitGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_summary_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Due Balance" Pill / Chip Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDue) {
                                    if (isDark) Color(0xFF5A2506) else Color(0xFFFEF3C7)
                                } else {
                                    if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)
                                },
                                border = BorderStroke(1.5.dp, if (isDue) Color(0xFFF59E0B) else ProfitGreen),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("remaining_due_chip")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text(
                                        text = if (isDue) "Due Balance" else "Account Status",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDue) Color(0xFFD97706) else ProfitGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isDue) "৳ ${String.format("%,.0f", profile.remainingDue)}" else "ALL CLEARED ✓",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDue) Color(0xFFDC2626) else ProfitGreen
                                    )
                                }
                            }

                            if (isDue) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { showCollectionModal = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ProfitGreen,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("collect_due_open_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Collect",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = if (isDue) Color(0xFFFDE68A).copy(alpha = 0.6f) else ProfitGreen.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Polished 3-chip stats breakdown tags: Credit:, Paid on sale:, Due Paid:
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // "Credit:" chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color.White,
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ledger_credit_chip")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Credit:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "৳ ${String.format("%,.0f", profile.totalCreditSales)}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // "Paid on sale:" chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color.White,
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ledger_paid_chip")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Paid on sale:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "৳ ${String.format("%,.0f", profile.totalCashReceivedAtSale)}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color.White else Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // "Due Paid:" chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
                                border = BorderStroke(1.5.dp, ProfitGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ledger_due_paid_chip")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Due Paid:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "৳ ${String.format("%,.0f", profile.totalDueCollected)}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Chips with spacious, comfortable touch targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All History (${historyEntries.size})",
                        "SALES" to "Credit Sales (${profile.creditSales.size})",
                        "PAYMENTS" to "Collections (${profile.paymentCollections.size})"
                    ).forEach { (filterKey, label) ->
                        val isSelected = selectedFilter == filterKey
                        Surface(
                            onClick = { selectedFilter = filterKey },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryNavy else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryNavy else CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Slate700,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chronological History Entries (Newest First, Standard DD-MMM-YYYY format e.g. "12-Sep-2026")
                if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ledger entries in this category.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredEntries, key = {
                            when (it) {
                                is LedgerHistoryEntry.CreditSale -> "sale_${it.record.phoneId}_${it.record.id.ifBlank { "${it.record.saleDate}_${it.record.dueAmount}" }}"
                                is LedgerHistoryEntry.PaymentCollection -> "pay_${it.record.id.ifBlank { "${it.record.date}_${it.record.amount}" }}"
                            }
                        }) { entry ->
                            when (entry) {
                                is LedgerHistoryEntry.CreditSale -> {
                                    val sale = entry.record
                                    val isDirectDue = sale.phoneModel.equals("Direct Due Adjustment", ignoreCase = true) ||
                                            sale.phoneModel.equals("Existing / Manual Due Entry", ignoreCase = true) ||
                                            sale.imei == "DIRECT_DUE" || sale.phoneId == 0L
                                    val formattedDate = remember(sale.timestamp, sale.saleDate) {
                                        when {
                                            sale.saleDate.isNotBlank() -> DateUtils.formatLedgerDateOnly(sale.saleDate)
                                            sale.timestamp > 0L -> DateUtils.formatLedgerDateOnly(sale.timestamp)
                                            else -> "N/A"
                                        }
                                    }

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(1.dp, CardBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ledger_sale_item_${sale.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isDirectDue) "Direct Due" else "Credit Mobile Sell",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Slate900,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Clean Date badge at top-right corner, completely separated from action buttons
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CalendarMonth,
                                                            contentDescription = null,
                                                            tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = formattedDate,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDark) Color(0xFFE2E8F0) else Slate900
                                                        )
                                                    }
                                                }
                                            }

                                            if (!isDirectDue) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "${sale.phoneModel} • IMEI: ${sale.imei.ifBlank { "N/A" }}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            if (isDirectDue) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Initial Amount: ৳ ${String.format("%,.0f", sale.dueAmount)}",
                                                        fontSize = 12.sp,
                                                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                                                    )
                                                    Text(
                                                        text = "Due: ৳ ${String.format("%,.0f", sale.dueAmount)}",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = ExpenseRed
                                                    )
                                                }
                                            } else {
                                                // First line: Total Price and Paid on sale
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Total: ৳ ${String.format("%,.0f", sale.totalSalePrice)}  |  Paid on sale: ৳ ${String.format("%,.0f", sale.cashReceived)}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                // Second line: Due amount on new line
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Due: ৳ ${String.format("%,.0f", sale.dueAmount)}",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = ExpenseRed
                                                    )
                                                }
                                            }

                                            if (sale.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Details: ${sale.notes}",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            // Distinct, compact button-style bottom row for Edit & Delete actions
                                            if (isAdmin && (onUpdateCustomerDue != null || onDeleteCustomerDue != null)) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(
                                                    color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                                                    thickness = 0.8.dp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (onUpdateCustomerDue != null) {
                                                        Surface(
                                                            onClick = {
                                                                if (onRequireAdminAuth != null) {
                                                                    onRequireAdminAuth(
                                                                        "Admin Authentication",
                                                                        if (isDirectDue) "Edit Direct Due" else "Edit Credit Due",
                                                                        "Authorize modifying due entry #${sale.id} for ${profile.customerName}",
                                                                        Icons.Default.Fingerprint,
                                                                        "Authorize Edit",
                                                                        false
                                                                    ) {
                                                                        editingDirectDue = sale
                                                                    }
                                                                } else {
                                                                    editingDirectDue = sale
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
                                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFBFDBFE)),
                                                            modifier = Modifier.testTag("edit_direct_due_${sale.id}")
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Edit Due",
                                                                    tint = PrimaryNavy,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(5.dp))
                                                                Text(
                                                                    text = "Edit",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = PrimaryNavy
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (onUpdateCustomerDue != null && onDeleteCustomerDue != null) {
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                    }

                                                    if (onDeleteCustomerDue != null) {
                                                        Surface(
                                                            onClick = {
                                                                if (onRequireAdminAuth != null) {
                                                                    onRequireAdminAuth(
                                                                        "Admin Authentication",
                                                                        if (isDirectDue) "Delete Direct Due" else "Delete Credit Due",
                                                                        "Authorize deleting due entry #${sale.id} of ৳ ${String.format("%,.0f", sale.dueAmount)} for ${profile.customerName}",
                                                                        Icons.Default.Fingerprint,
                                                                        "Delete Due",
                                                                        true
                                                                    ) {
                                                                        onDeleteCustomerDue(sale.id)
                                                                    }
                                                                } else {
                                                                    onDeleteCustomerDue(sale.id)
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                                                            border = BorderStroke(1.dp, if (isDark) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFFFECACA)),
                                                            modifier = Modifier.testTag("delete_direct_due_${sale.id}")
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Delete Due",
                                                                    tint = ExpenseRed,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(5.dp))
                                                                Text(
                                                                    text = "Delete",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ExpenseRed
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                is LedgerHistoryEntry.PaymentCollection -> {
                                    val pay = entry.record
                                    val formattedDate = remember(pay.timestamp, pay.date) {
                                        when {
                                            pay.date.isNotBlank() -> DateUtils.formatLedgerDateOnly(pay.date)
                                            pay.timestamp > 0L -> DateUtils.formatLedgerDateOnly(pay.timestamp)
                                            else -> "N/A"
                                        }
                                    }

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFECFDF5)
                                        ),
                                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ledger_payment_item_${pay.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Due Paid:",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfitGreen,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Clean Date badge at top-right corner, completely separated from action buttons
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                                    border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CalendarMonth,
                                                            contentDescription = null,
                                                            tint = ProfitGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = formattedDate,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ProfitGreen
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Via ${pay.paymentMethod} • Received by ${pay.collectedBy}",
                                                    fontSize = 11.5.sp,
                                                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                                                )
                                                Text(
                                                    text = "- ৳ ${String.format("%,.0f", pay.amount)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ProfitGreen
                                                )
                                            }

                                            if (pay.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Remarks: ${pay.notes}",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            // Distinct, compact button-style bottom row for Edit & Delete actions
                                            if (isAdmin && (onUpdateDuePayment != null || onDeletePayment != null)) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(
                                                    color = if (isDark) Color(0xFF065F46).copy(alpha = 0.5f) else ProfitGreen.copy(alpha = 0.2f),
                                                    thickness = 0.8.dp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (onUpdateDuePayment != null) {
                                                        Surface(
                                                            onClick = {
                                                                if (onRequireAdminAuth != null) {
                                                                    onRequireAdminAuth(
                                                                        "Admin Authentication",
                                                                        "Edit Due Payment",
                                                                        "Authorize modifying payment entry #${pay.id} for ${profile.customerName}",
                                                                        Icons.Default.Fingerprint,
                                                                        "Authorize Edit",
                                                                        false
                                                                    ) {
                                                                        editingPayment = pay
                                                                    }
                                                                } else {
                                                                    editingPayment = pay
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
                                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFBFDBFE)),
                                                            modifier = Modifier.testTag("edit_payment_${pay.id}")
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Edit Payment",
                                                                    tint = PrimaryNavy,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(5.dp))
                                                                Text(
                                                                    text = "Edit",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = PrimaryNavy
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (onUpdateDuePayment != null && onDeletePayment != null) {
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                    }

                                                    if (onDeletePayment != null) {
                                                        Surface(
                                                            onClick = {
                                                                if (onRequireAdminAuth != null) {
                                                                    onRequireAdminAuth(
                                                                        "Admin Authentication",
                                                                        "Void Payment Collection",
                                                                        "Authorize voiding collected payment #${pay.id} for ${profile.customerName}",
                                                                        Icons.Default.Fingerprint,
                                                                        "Void Payment",
                                                                        true
                                                                    ) {
                                                                        onDeletePayment(pay.id)
                                                                    }
                                                                } else {
                                                                    onDeletePayment(pay.id)
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                                                            border = BorderStroke(1.dp, if (isDark) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFFFECACA)),
                                                            modifier = Modifier.testTag("delete_payment_${pay.id}")
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Void Payment",
                                                                    tint = ExpenseRed,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(5.dp))
                                                                Text(
                                                                    text = "Delete",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ExpenseRed
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Close Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Modal to Record Due Payment Collection
    if (showCollectionModal) {
        CollectDuePaymentDialog(
            profile = profile,
            staffOptions = staffOptions,
            onDismiss = { showCollectionModal = false },
            onConfirm = { amount, method, collector, date, notes ->
                onCollectDue(amount, method, collector, date, notes)
                showCollectionModal = false
            }
        )
    }

    // Modal to Edit Due Payment Collection (Admin Only)
    editingPayment?.let { pay ->
        var editAmountText by remember(pay) {
            mutableStateOf(String.format(java.util.Locale.US, "%.0f", pay.amount))
        }
        var editMethod by remember(pay) { mutableStateOf(pay.paymentMethod.ifBlank { "Cash" }) }
        var methodExpanded by remember { mutableStateOf(false) }
        var editCollectedBy by remember(pay) { mutableStateOf(pay.collectedBy.ifBlank { staffOptions.firstOrNull() ?: "Staff" }) }
        var staffExpanded by remember { mutableStateOf(false) }
        var editDate by remember(pay) { mutableStateOf(pay.date.ifBlank { CustomerDueUtils.getTodayDate() }) }
        var editNotes by remember(pay) { mutableStateOf(pay.notes) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingPayment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = ProfitGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Due Payment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Amount
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = {
                            editAmountText = it
                            editError = null
                        },
                        label = { Text("Payment Amount (৳ BDT) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_payment_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = methodExpanded,
                            onDismissRequest = { methodExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                        ) {
                            PAYMENT_METHODS_EN.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        editMethod = method
                                        methodExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Received By Dropdown
                    ExposedDropdownMenuBox(
                        expanded = staffExpanded,
                        onExpandedChange = { staffExpanded = !staffExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editCollectedBy,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Received By *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffExpanded) },
                            colors = appTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = staffExpanded,
                            onDismissRequest = { staffExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                        ) {
                            staffOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = if (isDark) Color.White else Slate900) },
                                    onClick = {
                                        editCollectedBy = opt
                                        staffExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date
                    AppDatePickerField(
                        label = "Collection Date (DD-MMM-YYYY)",
                        selectedDate = editDate,
                        onDateSelected = { editDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Remarks
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Receipt Remarks (Optional)") },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_payment_notes_input")
                    )

                    if (editError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = editError ?: "",
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
                        val amt = editAmountText.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            editError = "Please enter a valid payment amount!"
                            return@Button
                        }
                        onUpdateDuePayment?.invoke(
                            pay.id,
                            amt,
                            editMethod,
                            editCollectedBy,
                            editDate,
                            editNotes.trim()
                        )
                        editingPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_edit_payment_btn")
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPayment = null }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else CardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Modal to Edit Direct Due (Admin Only)
    editingDirectDue?.let { sale ->
        var editDueAmountText by remember(sale) {
            mutableStateOf(String.format(java.util.Locale.US, "%.0f", sale.dueAmount))
        }
        var editDate by remember(sale) {
            mutableStateOf(sale.saleDate.ifBlank { CustomerDueUtils.getTodayDate() })
        }
        var editNotes by remember(sale) { mutableStateOf(sale.notes) }
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingDirectDue = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = PrimaryNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Direct Due",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Due Amount
                    OutlinedTextField(
                        value = editDueAmountText,
                        onValueChange = {
                            editDueAmountText = it
                            editError = null
                        },
                        label = { Text("Due Amount (৳ BDT) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_direct_due_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date
                    AppDatePickerField(
                        label = "Due Date (DD-MMM-YYYY)",
                        selectedDate = editDate,
                        onDateSelected = { editDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Remarks / Notes
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Remarks / Notes (Optional)") },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_direct_due_notes_input")
                    )

                    if (editError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = editError ?: "",
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
                        val amt = editDueAmountText.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            editError = "Please enter a valid due amount!"
                            return@Button
                        }
                        onUpdateCustomerDue?.invoke(
                            sale.id,
                            amt,
                            editDate,
                            editNotes.trim()
                        )
                        editingDirectDue = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_edit_direct_due_btn")
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDirectDue = null }) {
                    Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
                }
            },
            containerColor = if (isDark) Color(0xFF1E293B) else CardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
