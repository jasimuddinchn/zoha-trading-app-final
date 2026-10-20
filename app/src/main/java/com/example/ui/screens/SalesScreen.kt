package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.formatDeviceDisplayName
import com.example.data.model.getDisplayNameWithGrade
import com.example.data.model.sortedBySaleDateDescending
import com.example.ui.components.AdminCardEditButton
import com.example.ui.components.AdminSecurityConfirmDialog
import com.example.ui.components.AppOutlinedActionButton
import com.example.ui.components.AppSearchBar
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.ImeiCameraScannerDialog
import com.example.ui.components.SectionBadgeTitle
import com.example.ui.components.formatPhoneSpecsLine
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
import com.example.util.MonthFilterHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class SalesSecuredAction {
    data class EditSale(val phone: PhoneItem) : SalesSecuredAction()
    data class DeleteSale(val phone: PhoneItem) : SalesSecuredAction()
    data class EditOtherIncome(val item: OtherIncomeItem) : SalesSecuredAction()
    data class DeleteOtherIncome(val item: OtherIncomeItem) : SalesSecuredAction()
}

data class CustomerLedgerSummary(
    val key: String,
    val name: String,
    val phone: String,
    val purchases: List<PhoneItem>,
    val totalUnits: Int,
    val totalSpent: Double,
    val lastPurchaseDate: String
)

sealed class UnifiedIncomeListItem {
    abstract val sortTimestamp: Long
    abstract val secondaryTimestamp: Long
    abstract val keyId: String

    data class PhoneSale(val phone: PhoneItem) : UnifiedIncomeListItem() {
        override val sortTimestamp: Long =
            DateUtils.parseDateToMillis(phone.saleDate.ifBlank { phone.entryDate })
        override val secondaryTimestamp: Long =
            if (phone.updatedAt > 0L) phone.updatedAt else phone.createdAt
        override val keyId: String = "phone_${phone.id}"
    }

    data class ExtraIncome(val income: OtherIncomeItem) : UnifiedIncomeListItem() {
        override val sortTimestamp: Long =
            if (income.timestamp > 0L) income.timestamp else DateUtils.parseDateToMillis(income.date)
        override val secondaryTimestamp: Long =
            if (income.timestamp > 0L) income.timestamp else 0L
        override val keyId: String = "other_${income.id}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    phones: List<PhoneItem>,
    otherIncomes: List<OtherIncomeItem> = emptyList(),
    currentRole: UserRole,
    isBengali: Boolean = false,
    adminPin: String = "1111",
    initialDateFilter: String = "",
    onDateFilterChange: ((String) -> Unit)? = null,
    onViewMemoClick: (PhoneItem) -> Unit,
    onViewInvoiceClick: (PhoneItem) -> Unit = {},
    onEditSaleRecordClick: ((PhoneItem) -> Unit)? = null,
    onDeleteSaleRecordClick: ((PhoneItem) -> Unit)? = null,
    onAddOtherIncomeClick: (() -> Unit)? = null,
    onEditOtherIncomeClick: ((OtherIncomeItem) -> Unit)? = null,
    onDeleteOtherIncomeClick: ((OtherIncomeItem) -> Unit)? = null,
    onNavigateToDue: (() -> Unit)? = null,
    onRequireAdminAuth: ((title: String, actionTitle: String, actionDesc: String, icon: ImageVector, confirmText: String, isDanger: Boolean, onAuth: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val soldPhones = phones.filter { it.status == PhoneStatus.SOLD.code }
    val isAdmin = currentRole == UserRole.ADMIN

    // Admin PIN Security State for sensitive sales modifications (fallback)
    var pendingSecuredAction by remember { mutableStateOf<SalesSecuredAction?>(null) }

    fun handleEditSale(phone: PhoneItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Edit Sales Record",
                "Authorize editing sale record & invoice for ${phone.brand} ${phone.model} (Invoice #${phone.id})",
                Icons.Default.Edit,
                "Authorize Edit",
                false
            ) { onEditSaleRecordClick?.invoke(phone) }
        } else {
            pendingSecuredAction = SalesSecuredAction.EditSale(phone)
        }
    }

    fun handleDeleteSale(phone: PhoneItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Delete Sale Record",
                "Permanently delete past sale record for ${phone.brand} ${phone.model} (Invoice #${phone.id})",
                Icons.Default.Delete,
                "Delete Record",
                true
            ) { onDeleteSaleRecordClick?.invoke(phone) }
        } else {
            pendingSecuredAction = SalesSecuredAction.DeleteSale(phone)
        }
    }

    fun handleEditOtherIncome(item: OtherIncomeItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Edit Other Income",
                "Authorize editing other income entry (${item.category}: ৳ ${String.format(Locale.US, "%,.0f", item.amountBdt)})",
                Icons.Default.Edit,
                "Authorize Edit",
                false
            ) { onEditOtherIncomeClick?.invoke(item) }
        } else {
            pendingSecuredAction = SalesSecuredAction.EditOtherIncome(item)
        }
    }

    fun handleDeleteOtherIncome(item: OtherIncomeItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Delete Other Income",
                "Permanently delete other income entry for ${item.category} (৳ ${String.format(Locale.US, "%,.0f", item.amountBdt)})",
                Icons.Default.Delete,
                "Delete Record",
                true
            ) { onDeleteOtherIncomeClick?.invoke(item) }
        } else {
            pendingSecuredAction = SalesSecuredAction.DeleteOtherIncome(item)
        }
    }

    val todayDate = remember { DateUtils.getTodayStorageDate() }
    val yesterdayDate = remember { DateUtils.getYesterdayStorageDate() }
    val currentYm = remember { MonthFilterHelper.getCurrentYearMonth() }
    val lastYm = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    var salesSearchQuery by remember { mutableStateOf("") }
    var selectedSaleDateFilter by remember(initialDateFilter) {
        mutableStateOf(if (initialDateFilter.isNotBlank()) initialDateFilter else currentYm)
    }
    var selectedIncomeTypeFilter by remember { mutableStateOf("SET_SALES") } // Default to "SET_SALES" as requested
    var showSaleDatePickerDialog by remember { mutableStateOf(false) }
    var showMonthYearPickerDialog by remember { mutableStateOf(false) }
    var showSalesImeiScanner by remember { mutableStateOf(false) }

    val todayItemCount = remember(soldPhones, otherIncomes, todayDate, selectedIncomeTypeFilter) {
        val phonesCount = soldPhones.filterNot { isDueCollectionTransaction(it) }
            .count { (it.saleDate.ifBlank { it.entryDate }).trim().startsWith(todayDate) }
        val incomeCount = otherIncomes.count { it.date.trim().startsWith(todayDate) }
        when (selectedIncomeTypeFilter) {
            "SET_SALES" -> phonesCount
            "OTHER_INCOME" -> incomeCount
            else -> phonesCount + incomeCount
        }
    }
    val yesterdayItemCount = remember(soldPhones, otherIncomes, yesterdayDate, selectedIncomeTypeFilter) {
        val phonesCount = soldPhones.filterNot { isDueCollectionTransaction(it) }
            .count { (it.saleDate.ifBlank { it.entryDate }).trim().startsWith(yesterdayDate) }
        val incomeCount = otherIncomes.count { it.date.trim().startsWith(yesterdayDate) }
        when (selectedIncomeTypeFilter) {
            "SET_SALES" -> phonesCount
            "OTHER_INCOME" -> incomeCount
            else -> phonesCount + incomeCount
        }
    }
    val thisMonthItemCount = remember(soldPhones, otherIncomes, currentYm, selectedIncomeTypeFilter) {
        val phonesCount = soldPhones.filterNot { isDueCollectionTransaction(it) }
            .count {
                val d = (it.saleDate.ifBlank { it.entryDate }).trim()
                d.startsWith(currentYm) || MonthFilterHelper.extractYearMonth(d, it.updatedAt) == currentYm
            }
        val incomeCount = otherIncomes.count {
            val d = it.date.trim()
            d.startsWith(currentYm) || MonthFilterHelper.extractYearMonth(d, it.timestamp) == currentYm
        }
        when (selectedIncomeTypeFilter) {
            "SET_SALES" -> phonesCount
            "OTHER_INCOME" -> incomeCount
            else -> phonesCount + incomeCount
        }
    }
    val lastMonthItemCount = remember(soldPhones, otherIncomes, lastYm, selectedIncomeTypeFilter) {
        val phonesCount = soldPhones.filterNot { isDueCollectionTransaction(it) }
            .count {
                val d = (it.saleDate.ifBlank { it.entryDate }).trim()
                d.startsWith(lastYm) || MonthFilterHelper.extractYearMonth(d, it.updatedAt) == lastYm
            }
        val incomeCount = otherIncomes.count {
            val d = it.date.trim()
            d.startsWith(lastYm) || MonthFilterHelper.extractYearMonth(d, it.timestamp) == lastYm
        }
        when (selectedIncomeTypeFilter) {
            "SET_SALES" -> phonesCount
            "OTHER_INCOME" -> incomeCount
            else -> phonesCount + incomeCount
        }
    }

    // Filtered sales based on date and search query
    val filteredSoldPhones = remember(soldPhones, selectedSaleDateFilter, salesSearchQuery) {
        soldPhones.filterNot { isDueCollectionTransaction(it) }.filter { phone ->
            val dateMatch = if (selectedSaleDateFilter.isBlank() || selectedSaleDateFilter == "ALL") {
                true
            } else {
                val effectiveDate = phone.saleDate.ifBlank { phone.entryDate }.trim()
                effectiveDate.startsWith(selectedSaleDateFilter) ||
                        (selectedSaleDateFilter.length == 7 && MonthFilterHelper.extractYearMonth(effectiveDate, phone.updatedAt) == selectedSaleDateFilter)
            }

            val searchMatch = if (salesSearchQuery.isBlank()) {
                true
            } else {
                val q = salesSearchQuery.trim().lowercase()
                phone.brand.lowercase().contains(q) ||
                        phone.model.lowercase().contains(q) ||
                        phone.imei.lowercase().contains(q) ||
                        phone.customerName.lowercase().contains(q) ||
                        phone.customerPhone.lowercase().contains(q) ||
                        phone.soldByStaff.lowercase().contains(q) ||
                        phone.saleDate.lowercase().contains(q) ||
                        DateUtils.formatDisplayDate(phone.saleDate).lowercase().contains(q) ||
                        phone.entryDate.lowercase().contains(q) ||
                        DateUtils.formatDisplayDate(phone.entryDate).lowercase().contains(q)
            }

            dateMatch && searchMatch
        }.sortedBySaleDateDescending()
    }

    // Filtered other income based on date and search query
    val filteredOtherIncomes = remember(otherIncomes, selectedSaleDateFilter, salesSearchQuery) {
        otherIncomes.filter { income ->
            val dateMatch = if (selectedSaleDateFilter.isBlank() || selectedSaleDateFilter == "ALL") {
                true
            } else {
                val effectiveDate = income.date.trim()
                effectiveDate.startsWith(selectedSaleDateFilter) ||
                    (selectedSaleDateFilter.length == 7 && MonthFilterHelper.extractYearMonth(effectiveDate, income.timestamp) == selectedSaleDateFilter)
            }

            val searchMatch = if (salesSearchQuery.isBlank()) {
                true
            } else {
                val q = salesSearchQuery.trim().lowercase()
                income.category.lowercase().contains(q) ||
                    income.notes.lowercase().contains(q) ||
                    income.addedBy.lowercase().contains(q) ||
                    income.date.lowercase().contains(q) ||
                    DateUtils.formatLedgerDateOnly(income.date).lowercase().contains(q)
            }

            dateMatch && searchMatch
        }.sortedWith(
            compareByDescending<OtherIncomeItem> {
                if (it.timestamp > 0L) it.timestamp else DateUtils.parseDateToMillis(it.date)
            }.thenByDescending { it.id }
        )
    }

    // Unified income list displayed in chronological descending order (newest first)
    val unifiedIncomeItems = remember(filteredSoldPhones, filteredOtherIncomes, selectedIncomeTypeFilter) {
        val list = mutableListOf<UnifiedIncomeListItem>()
        if (selectedIncomeTypeFilter == "ALL" || selectedIncomeTypeFilter == "SET_SALES") {
            filteredSoldPhones.forEach { list.add(UnifiedIncomeListItem.PhoneSale(it)) }
        }
        if (selectedIncomeTypeFilter == "ALL" || selectedIncomeTypeFilter == "OTHER_INCOME") {
            filteredOtherIncomes.forEach { list.add(UnifiedIncomeListItem.ExtraIncome(it)) }
        }
        list.sortedWith(
            compareByDescending<UnifiedIncomeListItem> { it.sortTimestamp }
                .thenByDescending { it.secondaryTimestamp }
                .thenByDescending { it.keyId }
        )
    }

    // Summary calculations for filtered/selected scope (Single Source of Truth: Other Income included directly into Net Revenue)
    val totalOtherIncomeRevenue = otherIncomes.sumOf { it.amountBdt }
    val totalDeviceRevenue = soldPhones.sumOf { it.sellingPriceBdt }
    val totalRevenue = totalDeviceRevenue + totalOtherIncomeRevenue
    val totalUnitsSold = soldPhones.size
    val avgSalePrice = if (totalUnitsSold > 0) totalDeviceRevenue / totalUnitsSold else 0.0

    // Filtered summary calculations
    val filteredDeviceRevenue = filteredSoldPhones.sumOf { it.sellingPriceBdt }
    val filteredOtherIncomeRevenue = filteredOtherIncomes.sumOf { it.amountBdt }
    val filteredRevenue = filteredDeviceRevenue + filteredOtherIncomeRevenue
    val filteredUnitsSold = filteredSoldPhones.size
    val filteredAvgPrice = if (filteredUnitsSold > 0) filteredDeviceRevenue / filteredUnitsSold else 0.0
    val filteredTotalProfit = filteredSoldPhones.sumOf { it.netProfitBdt } + filteredOtherIncomeRevenue

    // DYNAMIC TAB STATE CALCULATIONS (strictly active tab without mixing data)
    val activeFilteredRevenue = when (selectedIncomeTypeFilter) {
        "SET_SALES" -> filteredDeviceRevenue
        "OTHER_INCOME" -> filteredOtherIncomeRevenue
        else -> filteredRevenue
    }

    val activeFilteredProfit = when (selectedIncomeTypeFilter) {
        "SET_SALES" -> filteredSoldPhones.sumOf { it.netProfitBdt }
        "OTHER_INCOME" -> filteredOtherIncomeRevenue
        else -> filteredTotalProfit
    }

    val activeAllTimeRevenue = when (selectedIncomeTypeFilter) {
        "SET_SALES" -> totalDeviceRevenue
        "OTHER_INCOME" -> totalOtherIncomeRevenue
        else -> totalRevenue
    }

    val isDateFilterActive = selectedSaleDateFilter.isNotBlank() && selectedSaleDateFilter != "ALL"

    // Group sales by salesman (retained for business logic & reporting)
    val salesByStaff = remember(filteredSoldPhones) {
        filteredSoldPhones.groupBy { it.soldByStaff.ifBlank { "Staff" } }
    }

    // Customer ledger summary (retained for business logic)
    val customerSummaries = remember(soldPhones) {
        val grouped = soldPhones.filter { it.customerName.isNotBlank() || it.customerPhone.isNotBlank() }
            .groupBy { phone ->
                val cleanPhone = phone.customerPhone.trim()
                val cleanName = phone.customerName.trim().lowercase()
                if (cleanPhone.isNotBlank()) cleanPhone else cleanName
            }

        grouped.map { (key, items) ->
            val primaryName = items.firstOrNull { it.customerName.isNotBlank() }?.customerName?.trim()?.uppercase() ?: "WALK-IN CUSTOMER"
            val primaryPhone = items.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone?.trim() ?: "N/A"
            val sortedPurchases = items.sortedBySaleDateDescending()
            val lastDate = sortedPurchases.firstOrNull()?.let { it.saleDate.ifBlank { it.entryDate } } ?: "N/A"
            val totalSpent = items.sumOf { it.sellingPriceBdt }

            CustomerLedgerSummary(
                key = key,
                name = primaryName,
                phone = primaryPhone,
                purchases = sortedPurchases,
                totalUnits = items.size,
                totalSpent = totalSpent,
                lastPurchaseDate = lastDate
            )
        }.sortedByDescending { it.totalUnits }
    }

    val isDark = isAppInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBg)
            .testTag("sales_screen")
    ) {
        // Streamlined Top Search Bar with Integrated Camera Barcode / QR Scanner on the right
        AppSearchBar(
            query = salesSearchQuery,
            onQueryChange = { salesSearchQuery = it },
            placeholder = "Search brand, model, IMEI, customer, or date...",
            testTag = "sales_search_input",
            onScanImeiClick = { showSalesImeiScanner = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        // CONSOLIDATED SPLIT DROPDOWN FILTERS: "Cash In" and "Time Flow"
        var isCashInMenuExpanded by remember { mutableStateOf(false) }
        var isTimeFlowMenuExpanded by remember { mutableStateOf(false) }

        val activeCashInDisplayLabel = when (selectedIncomeTypeFilter) {
            "SET_SALES" -> "Set Sale"
            "OTHER_INCOME" -> "Direct Cash"
            else -> "All"
        }

        val activeTimeFlowDisplayLabel = when {
            selectedSaleDateFilter.isBlank() || selectedSaleDateFilter == "ALL" -> "All dates"
            selectedSaleDateFilter == currentYm -> "This Month"
            selectedSaleDateFilter == lastYm -> "Last month"
            selectedSaleDateFilter == todayDate -> "Today"
            selectedSaleDateFilter.length == 7 && selectedSaleDateFilter.contains("-") -> MonthFilterHelper.formatMonthDisplayName(selectedSaleDateFilter)
            else -> DateUtils.formatDisplayDate(selectedSaleDateFilter)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. "Cash In" Split Button / Dropdown Button
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (selectedIncomeTypeFilter != "ALL") PrimaryNavy else CardBorder),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("cash_in_split_dropdown_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Main Action Button (Left side): Dynamically displays selected category option ("All", "Set Sale", "Direct Cash")
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { isCashInMenuExpanded = !isCashInMenuExpanded }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (selectedIncomeTypeFilter) {
                                    "SET_SALES" -> Icons.Default.PointOfSale
                                    "OTHER_INCOME" -> Icons.Default.Payments
                                    else -> Icons.Default.Payments
                                },
                                contentDescription = activeCashInDisplayLabel,
                                tint = when (selectedIncomeTypeFilter) {
                                    "SET_SALES" -> Color(0xFF4F46E5)
                                    "OTHER_INCOME" -> ProfitGreen
                                    else -> if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeCashInDisplayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Split Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(if (isDark) Color(0xFF334155) else CardBorder)
                        )

                        // Dropdown Arrow Trigger (Right side)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { isCashInMenuExpanded = !isCashInMenuExpanded }
                                .testTag("cash_in_dropdown_trigger_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Cash In Menu",
                                tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isCashInMenuExpanded,
                    onDismissRequest = { isCashInMenuExpanded = false },
                    modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                ) {
                    // Option: All
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "All",
                                    fontWeight = if (selectedIncomeTypeFilter == "ALL") FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIncomeTypeFilter == "ALL") PrimaryNavy else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedIncomeTypeFilter == "ALL") {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedIncomeTypeFilter = "ALL"
                            isCashInMenuExpanded = false
                        },
                        modifier = Modifier.testTag("cash_in_option_all")
                    )

                    // Option: Set Sale
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Set Sale",
                                    fontWeight = if (selectedIncomeTypeFilter == "SET_SALES") FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIncomeTypeFilter == "SET_SALES") Color(0xFF4F46E5) else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedIncomeTypeFilter == "SET_SALES") {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedIncomeTypeFilter = "SET_SALES"
                            isCashInMenuExpanded = false
                        },
                        modifier = Modifier.testTag("cash_in_option_set_sale")
                    )

                    // Option: Direct Cash (renamed from "Cash In")
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Direct Cash",
                                    fontWeight = if (selectedIncomeTypeFilter == "OTHER_INCOME") FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIncomeTypeFilter == "OTHER_INCOME") ProfitGreen else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedIncomeTypeFilter == "OTHER_INCOME") {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedIncomeTypeFilter = "OTHER_INCOME"
                            isCashInMenuExpanded = false
                        },
                        modifier = Modifier.testTag("cash_in_option_direct_cash")
                    )
                }
            }

            // 2. "Time Flow" Split Button / Dropdown Button
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDateFilterActive) PrimaryNavy else CardBorder),
                    shadowElevation = 1.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("time_flow_split_dropdown_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Main Action Button (Left side): Dynamically displays selected date range option ("All dates", "This Month", etc.)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { isTimeFlowMenuExpanded = !isTimeFlowMenuExpanded }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = activeTimeFlowDisplayLabel,
                                tint = if (isDateFilterActive) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeTimeFlowDisplayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Split Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(if (isDark) Color(0xFF334155) else CardBorder)
                        )

                        // Dropdown Arrow Trigger (Right side)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { isTimeFlowMenuExpanded = !isTimeFlowMenuExpanded }
                                .testTag("time_flow_dropdown_trigger_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Time Flow Menu",
                                tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isTimeFlowMenuExpanded,
                    onDismissRequest = { isTimeFlowMenuExpanded = false },
                    modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                ) {
                    // Option: All dates
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "All dates",
                                    fontWeight = if (!isDateFilterActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isDateFilterActive) PrimaryNavy else (if (isDark) Color.White else Slate900)
                                )
                                if (!isDateFilterActive) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedSaleDateFilter = ""
                            onDateFilterChange?.invoke("")
                            isTimeFlowMenuExpanded = false
                        },
                        modifier = Modifier.testTag("time_flow_option_all_dates")
                    )

                    // Option: This Month
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "This Month",
                                    fontWeight = if (selectedSaleDateFilter == currentYm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSaleDateFilter == currentYm) Color(0xFF2563EB) else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedSaleDateFilter == currentYm) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedSaleDateFilter = currentYm
                            onDateFilterChange?.invoke(currentYm)
                            isTimeFlowMenuExpanded = false
                        },
                        modifier = Modifier.testTag("time_flow_option_this_month")
                    )

                    // Option: Last month
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Last month",
                                    fontWeight = if (selectedSaleDateFilter == lastYm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSaleDateFilter == lastYm) Color(0xFF7C3AED) else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedSaleDateFilter == lastYm) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedSaleDateFilter = lastYm
                            onDateFilterChange?.invoke(lastYm)
                            isTimeFlowMenuExpanded = false
                        },
                        modifier = Modifier.testTag("time_flow_option_last_month")
                    )

                    // Option: Select Month (opens Month-Year Picker dialog)
                    val isCustomMonthActive = selectedSaleDateFilter.length == 7 && selectedSaleDateFilter != currentYm && selectedSaleDateFilter != lastYm
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isCustomMonthActive) "Select Month (${MonthFilterHelper.formatMonthDisplayName(selectedSaleDateFilter)})" else "Select Month",
                                    fontWeight = if (isCustomMonthActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCustomMonthActive) Color(0xFF0284C7) else (if (isDark) Color.White else Slate900)
                                )
                                if (isCustomMonthActive) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            isTimeFlowMenuExpanded = false
                            showMonthYearPickerDialog = true
                        },
                        modifier = Modifier.testTag("time_flow_option_select_month")
                    )

                    // Option: Today
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Today (${DateUtils.getTodayDisplayDate()})",
                                    fontWeight = if (selectedSaleDateFilter == todayDate) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSaleDateFilter == todayDate) Color(0xFF059669) else (if (isDark) Color.White else Slate900)
                                )
                                if (selectedSaleDateFilter == todayDate) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            selectedSaleDateFilter = todayDate
                            onDateFilterChange?.invoke(todayDate)
                            isTimeFlowMenuExpanded = false
                        },
                        modifier = Modifier.testTag("time_flow_option_today")
                    )

                    // Option: Select date (opens single-date picker dialog)
                    val isCustomSingleDateActive = selectedSaleDateFilter.length > 7 && selectedSaleDateFilter != todayDate
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isCustomSingleDateActive) "Select date (${DateUtils.formatDisplayDate(selectedSaleDateFilter)})" else "Select date",
                                    fontWeight = if (isCustomSingleDateActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCustomSingleDateActive) PrimaryNavy else (if (isDark) Color.White else Slate900)
                                )
                                if (isCustomSingleDateActive) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            isTimeFlowMenuExpanded = false
                            showSaleDatePickerDialog = true
                        },
                        modifier = Modifier.testTag("time_flow_option_select_date")
                    )
                }
            }
        }

        // Sales Records List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // DAILY / MONTHLY SALES SUMMARY BANNER (WHEN DATE FILTER IS ACTIVE)
            if (isDateFilterActive) {
                item {
                    val isThisMonth = selectedSaleDateFilter == currentYm
                    val isLastMonth = selectedSaleDateFilter == lastYm
                    val isMonthFilter = isThisMonth || isLastMonth || (selectedSaleDateFilter.length == 7 && selectedSaleDateFilter.contains("-"))
                    val dateHeadingLabel = when {
                        isThisMonth -> "This Month (${MonthFilterHelper.formatMonthDisplayName(currentYm)})"
                        isLastMonth -> "Last Month (${MonthFilterHelper.formatMonthDisplayName(lastYm)})"
                        selectedSaleDateFilter.length == 7 -> MonthFilterHelper.formatMonthDisplayName(selectedSaleDateFilter)
                        else -> DateUtils.formatDateHeading(selectedSaleDateFilter)
                    }
                    val displaySaleDate = when {
                        isThisMonth -> MonthFilterHelper.formatMonthDisplayName(currentYm)
                        isLastMonth -> MonthFilterHelper.formatMonthDisplayName(lastYm)
                        selectedSaleDateFilter.length == 7 -> MonthFilterHelper.formatMonthDisplayName(selectedSaleDateFilter)
                        else -> DateUtils.formatDisplayDate(selectedSaleDateFilter)
                    }
                    val trackerTitle = if (isMonthFilter) {
                        when (selectedIncomeTypeFilter) {
                            "SET_SALES" -> "Monthly Set Sales Tracker"
                            "OTHER_INCOME" -> "Monthly Cash In Tracker"
                            else -> "Monthly Sales Tracker"
                        }
                    } else {
                        when (selectedIncomeTypeFilter) {
                            "SET_SALES" -> "Daily Set Sales Tracker"
                            "OTHER_INCOME" -> "Daily Cash In Tracker"
                            else -> "Daily Sales Tracker"
                        }
                    }
                    val trackerSubtitle = if (isMonthFilter) {
                        when (selectedIncomeTypeFilter) {
                            "SET_SALES" -> "Set sales in $displaySaleDate"
                            "OTHER_INCOME" -> "Cash in received in $displaySaleDate"
                            else -> "Sold in $displaySaleDate"
                        }
                    } else {
                        when (selectedIncomeTypeFilter) {
                            "SET_SALES" -> "Set sales on $displaySaleDate • $dateHeadingLabel"
                            "OTHER_INCOME" -> "Cash in on $displaySaleDate • $dateHeadingLabel"
                            else -> "Sold on $displaySaleDate • $dateHeadingLabel"
                        }
                    }
                    val trackerBadgeText = when (selectedIncomeTypeFilter) {
                        "SET_SALES" -> "$filteredUnitsSold PCS"
                        "OTHER_INCOME" -> "${filteredOtherIncomes.size} Entries"
                        else -> "${filteredUnitsSold + filteredOtherIncomes.size} Items"
                    }

                    val revenueMetricLabel = when (selectedIncomeTypeFilter) {
                        "SET_SALES" -> if (isMonthFilter) "Monthly Set Sale Total" else "Daily Set Sale Total"
                        "OTHER_INCOME" -> if (isMonthFilter) "Monthly Cash In Total" else "Daily Cash In Total"
                        else -> if (isMonthFilter) "Total Monthly Revenue" else "Total Daily Revenue"
                    }

                    val profitMetricLabel = when (selectedIncomeTypeFilter) {
                        "SET_SALES" -> if (isMonthFilter) "Monthly Set Sale Profit" else "Daily Set Sale Profit"
                        "OTHER_INCOME" -> if (isMonthFilter) "Monthly Cash In Total" else "Daily Cash In Total"
                        else -> if (isMonthFilter) "Total Monthly Profit" else "Total Daily Profit"
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_sales_summary_banner")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (isMonthFilter) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sales_monthly_sales_tracker_header"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF064E3B) else Color(0xFFE8F5E9),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PointOfSale,
                                                contentDescription = null,
                                                tint = ProfitGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val prefixText = when (selectedIncomeTypeFilter) {
                                        "OTHER_INCOME" -> "Total Cash In $displaySaleDate: "
                                        else -> "Total Sales In $displaySaleDate: "
                                    }
                                    val countText = when (selectedIncomeTypeFilter) {
                                        "OTHER_INCOME" -> "${filteredOtherIncomes.size} Entries"
                                        else -> "$filteredUnitsSold Pcs"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = prefixText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF1F5F9) else Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF),
                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFF93C5FD))
                                        ) {
                                            Text(
                                                text = countText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                AppSectionHeader(
                                    title = trackerTitle,
                                    subtitle = trackerSubtitle,
                                    icon = Icons.Default.PointOfSale,
                                    iconTint = ProfitGreen,
                                    iconBgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFE8F5E9),
                                    badgeText = trackerBadgeText,
                                    badgeColor = ProfitGreen,
                                    badgeBgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFE8F5E9),
                                    containerBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                    containerBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    testTag = "sales_daily_sales_tracker_header"
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Sales Financial Metrics (Dynamically based on active tab)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = revenueMetricLabel,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                                    )
                                    Text(
                                        text = "৳ ${String.format("%,.0f", activeFilteredRevenue)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen
                                    )
                                }

                                if (currentRole == UserRole.ADMIN) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = profitMetricLabel,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                                        )
                                        Text(
                                            text = "৳ ${String.format("%,.0f", activeFilteredProfit)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (activeFilteredProfit >= 0) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else ExpenseRed
                                        )
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        val secondaryLabel = when (selectedIncomeTypeFilter) {
                                            "SET_SALES" -> "Avg Sale Price"
                                            "OTHER_INCOME" -> "Avg Entry"
                                            else -> "Avg Sale Price"
                                        }
                                        val secondaryValue = when (selectedIncomeTypeFilter) {
                                            "SET_SALES" -> filteredAvgPrice
                                            "OTHER_INCOME" -> if (filteredOtherIncomes.isNotEmpty()) filteredOtherIncomeRevenue / filteredOtherIncomes.size else 0.0
                                            else -> filteredAvgPrice
                                        }
                                        Text(
                                            text = secondaryLabel,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                                        )
                                        Text(
                                            text = "৳ ${String.format("%,.0f", secondaryValue)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF8FAFC) else Slate900
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ALL-TIME REVENUE SUMMARY BANNER (Dynamically based on active tab)
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val allTimeTitle = when (selectedIncomeTypeFilter) {
                                "SET_SALES" -> "Total Set Sales"
                                "OTHER_INCOME" -> "Total Cash In"
                                else -> "Total Sales & Revenue"
                            }
                            Text(
                                text = allTimeTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "৳ ${String.format("%,.0f", activeAllTimeRevenue)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFF8FAFC) else Slate900
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = if (isDark) Color(0xFF334155) else CardBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    val countLabel = when (selectedIncomeTypeFilter) {
                                        "SET_SALES" -> "Units Sold:"
                                        "OTHER_INCOME" -> "Cash In Entries:"
                                        else -> "Units Sold:"
                                    }
                                    val countValue = when (selectedIncomeTypeFilter) {
                                        "SET_SALES" -> "$totalUnitsSold Units"
                                        "OTHER_INCOME" -> "${otherIncomes.size} Entries"
                                        else -> "$totalUnitsSold Units"
                                    }
                                    Text(countLabel, fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700)
                                    Text(countValue, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF8FAFC) else Slate900)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val avgLabel = when (selectedIncomeTypeFilter) {
                                        "SET_SALES" -> "Avg Price / Unit:"
                                        "OTHER_INCOME" -> "Avg / Entry:"
                                        else -> "Avg Price / Unit:"
                                    }
                                    val avgValue = when (selectedIncomeTypeFilter) {
                                        "SET_SALES" -> avgSalePrice
                                        "OTHER_INCOME" -> if (otherIncomes.isNotEmpty()) totalOtherIncomeRevenue / otherIncomes.size else 0.0
                                        else -> avgSalePrice
                                    }
                                    Text(avgLabel, fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Slate700)
                                    Text("৳ ${String.format("%,.0f", avgValue)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF8FAFC) else Slate900)
                                }
                            }
                        }
                    }
                }
            }

            // SALES & INCOME HISTORY LIST HEADER
            item {
                val headerTitle = when (selectedIncomeTypeFilter) {
                    "OTHER_INCOME" -> "Cash In Records"
                    "SET_SALES" -> "Set Sale Records"
                    else -> "Sales & Cash In Records"
                }
                SectionBadgeTitle(
                    title = headerTitle,
                    countBadge = if (isDateFilterActive) "${unifiedIncomeItems.size} items" else "${unifiedIncomeItems.size} total",
                    badgeColor = Color(0xFF059669),
                    badgeBgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                )
            }

            if (unifiedIncomeItems.isEmpty()) {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isDateFilterActive) "No records found on ${DateUtils.formatDisplayDate(selectedSaleDateFilter)}" else "No sales or income records found!",
                                color = if (isDark) Color(0xFFF1F5F9) else Slate800,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDateFilterActive || salesSearchQuery.isNotBlank() || selectedIncomeTypeFilter != "ALL") {
                                Spacer(modifier = Modifier.height(12.dp))
                                AppOutlinedActionButton(
                                    text = "Clear Filters",
                                    onClick = {
                                        selectedSaleDateFilter = ""
                                        salesSearchQuery = ""
                                        selectedIncomeTypeFilter = "ALL"
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                items(unifiedIncomeItems, key = { it.keyId }) { unifiedItem ->
                    when (unifiedItem) {
                        is UnifiedIncomeListItem.ExtraIncome -> {
                            OtherIncomeCard(
                                item = unifiedItem.income,
                                isAdmin = isAdmin,
                                isDark = isDark,
                                onEditClick = { handleEditOtherIncome(unifiedItem.income) },
                                onDeleteClick = { handleDeleteOtherIncome(unifiedItem.income) }
                            )
                        }
                        is UnifiedIncomeListItem.PhoneSale -> {
                            val phone = unifiedItem.phone
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF141936) else Color(0xFFF0F4FF)
                                ),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF3730A3).copy(alpha = 0.65f) else Color(0xFFC7D2FE)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sale_item_${phone.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                                .clickable {
                                                    salesSearchQuery = "${phone.brand} ${phone.model}".trim()
                                                }
                                        ) {
                                    val glitterBrush = if (isDark) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFDE047), // Gold
                                                Color(0xFF38BDF8), // Cyan shimmer
                                                Color(0xFFF59E0B), // Vivid Amber
                                                Color(0xFF67E8F9), // Electric Sky
                                                Color(0xFFFDE047)  // Gold highlight
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFB45309), // Amber gold
                                                Color(0xFF0284C7), // Luminous cyan
                                                Color(0xFFD97706), // Warm amber
                                                Color(0xFF0369A1), // Sapphire shimmer
                                                Color(0xFFB45309)  // Deep gold
                                            )
                                        )
                                    }
                                    val glitterShadow = Shadow(
                                        color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFF0284C7).copy(alpha = 0.35f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 8f
                                    )

                                    // Line 1: Phone Model Name (15.sp font, Pure White in Dark Mode / Pure Black in Light Mode)
                                    val modelTitle = formatDeviceDisplayName(phone.brand, phone.model)
                                    Text(
                                        text = modelTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Line 2: IMEI Number (e.g., IMEI: 358291048291023)
                                    Text(
                                        text = "IMEI: ${phone.imei.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Line 3: Color, Storage, Grade (e.g., Black | 256GB | Grade A)
                                    val specsLine = formatPhoneSpecsLine(phone.color, phone.ramRom, phone.conditionGrade)
                                    if (specsLine.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = specsLine,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen,
                                        maxLines = 1
                                    )
                                    if (phone.askingPriceBdt > 0) {
                                        Text(
                                            text = "Asking: ৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF94A3B8) else Slate700,
                                            maxLines = 1
                                        )
                                    }
                                    val stockInDate = phone.entryDate.trim()
                                    if (stockInDate.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Stock In: ${DateUtils.formatSoldDateDisplay(stockInDate)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF0E1228) else Color(0xFFE5EDFF),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF3730A3).copy(alpha = 0.4f) else Color(0xFFD5E0FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val custNameVal = phone.customerName.trim().ifBlank { "Walk-in Customer" }
                                    val mobileVal = phone.customerPhone.trim().ifBlank { "N/A" }
                                    val saleDateStr = phone.saleDate.ifBlank { phone.entryDate }

                                    // Line 1: Customer Name (strictly left-aligned)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val query = phone.customerName.ifBlank { phone.customerPhone }.trim()
                                                if (query.isNotBlank()) salesSearchQuery = query
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Customer",
                                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Customer: $custNameVal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Line 2: Mobile Number on a NEW line directly below Customer Name formatted strictly as Mobile No: 01XXXXXXXXX
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable(enabled = phone.customerPhone.isNotBlank()) {
                                                salesSearchQuery = phone.customerPhone.trim()
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Mobile No",
                                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mobile No: $mobileVal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Line 3: Sold Date on a NEW line directly below Mobile Number formatted strictly as Sold Date: DD MMM YYYY (e.g., "Sold Date: 24 Sep 2026")
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable(enabled = saleDateStr.isNotBlank()) {
                                                selectedSaleDateFilter = saleDateStr
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Sold Date",
                                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sold Date: ${DateUtils.formatLedgerDateOnly(saleDateStr)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    HorizontalDivider(
                                        color = if (isDark) Color(0xFF3730A3).copy(alpha = 0.3f) else Color(0xFFD5E0FF),
                                        thickness = 0.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Inv #PH-${phone.id} • Sold by: ${phone.soldByStaff.ifBlank { "Staff" }}",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = phone.paymentMethod.ifBlank { "Cash" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (phone.effectiveDueAmount > 0.0) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Cash: ৳ ${String.format("%,.0f", phone.effectiveCashReceived)} • Due: ৳ ${String.format("%,.0f", phone.effectiveDueAmount)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEA580C)
                                            )
                                            if (onNavigateToDue != null) {
                                                Text(
                                                    text = "Collect / Ledger →",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFEA580C),
                                                    modifier = Modifier.clickable { onNavigateToDue() }
                                                )
                                            }
                                        }
                                    }

                                    if (currentRole == UserRole.ADMIN && phone.netProfitBdt != 0.0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Profit: ৳ ${String.format("%,.0f", phone.netProfitBdt)} (${String.format("%.1f", phone.profitMarginPercent)}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (phone.netProfitBdt >= 0) ProfitGreen else ExpenseRed,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(end = if (isAdmin && onEditSaleRecordClick != null) 84.dp else 0.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onViewMemoClick(phone) },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.6f) else PrimaryNavy.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("open_memo_btn_${phone.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Memo",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onViewInvoiceClick(phone) },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF0284C7).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("open_invoice_btn_${phone.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Invoice",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Admin-Only Edit and Delete Buttons strictly positioned at bottom-right corner (Alignment.BottomEnd)
                                if (isAdmin) {
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (onEditSaleRecordClick != null) {
                                            AdminCardEditButton(
                                                onClick = { handleEditSale(phone) },
                                                testTag = "edit_sale_btn_${phone.id}"
                                            )
                                        }
                                        if (onDeleteSaleRecordClick != null) {
                                            Surface(
                                                onClick = { handleDeleteSale(phone) },
                                                shape = CircleShape,
                                                color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFFFEE2E2),
                                                border = BorderStroke(1.dp, if (isDark) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFF87171)),
                                                modifier = Modifier.size(32.dp).testTag("delete_sale_btn_${phone.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete / Revert Sale Record",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(16.dp)
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

    item {
        Spacer(modifier = Modifier.height(20.dp))
    }
}
}

        // DYNAMIC BOTTOM TOTAL CALCULATION BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales_bottom_total_bar"),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total $activeCashInDisplayLabel (${unifiedIncomeItems.size} items)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                    )
                    Text(
                        text = activeTimeFlowDisplayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDateFilterActive) (if (isDark) Color(0xFF38BDF8) else PrimaryNavy) else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${String.format(Locale.US, "%,.0f", activeFilteredRevenue)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProfitGreen,
                        modifier = Modifier.testTag("sales_bottom_total_amount")
                    )
                    if (isAdmin) {
                        Text(
                            text = "Profit: ৳ ${String.format(Locale.US, "%,.0f", activeFilteredProfit)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeFilteredProfit >= 0) ProfitGreen else ExpenseRed
                        )
                    }
                }
            }
        }
}

    // CALENDAR DATE PICKER DIALOG FOR SALES
    if (showSaleDatePickerDialog) {
        val initialMillis = remember(selectedSaleDateFilter) {
            val target = if (selectedSaleDateFilter.isNotBlank() && selectedSaleDateFilter != "ALL") selectedSaleDateFilter else todayDate
            DateUtils.parseDateToMillis(target)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showSaleDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat(DateUtils.STORAGE_DATE_PATTERN, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            selectedSaleDateFilter = sdf.format(Date(millis))
                        }
                        showSaleDatePickerDialog = false
                    }
                ) {
                    Text("Filter By Date", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaleDatePickerDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PrimaryNavy,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = PrimaryNavy
                )
            )
        }
    }

    // MONTH & YEAR PICKER DIALOG FOR SALES ("Select Month" option)
    if (showMonthYearPickerDialog) {
        MonthYearPickerDialog(
            initialYearMonth = if (selectedSaleDateFilter.length == 7 && selectedSaleDateFilter.contains("-")) selectedSaleDateFilter else currentYm,
            onDismissRequest = { showMonthYearPickerDialog = false },
            onMonthSelected = { ym ->
                selectedSaleDateFilter = ym
                onDateFilterChange?.invoke(ym)
                showMonthYearPickerDialog = false
            }
        )
    }

    // Camera Barcode & IMEI Scanner Dialog
    if (showSalesImeiScanner) {
        ImeiCameraScannerDialog(
            onDismiss = { showSalesImeiScanner = false },
            onImeiScanned = { scannedImei, _ ->
                salesSearchQuery = scannedImei
                showSalesImeiScanner = false
            }
        )
    }

    // Admin PIN Security Dialog for Sales Modifications (fallback)
    if (onRequireAdminAuth == null) {
        val currentAction = pendingSecuredAction
        if (currentAction != null) {
            val actionTitle: String
            val actionDesc: String
            val isDanger: Boolean
            val icon: ImageVector

            when (currentAction) {
                is SalesSecuredAction.EditSale -> {
                    actionTitle = "Edit Sales Record"
                    actionDesc = "Authorize editing sale record & invoice for ${currentAction.phone.brand} ${currentAction.phone.model} (Invoice #${currentAction.phone.id})"
                    isDanger = false
                    icon = Icons.Default.Edit
                }
                is SalesSecuredAction.DeleteSale -> {
                    actionTitle = "Delete Sale Record"
                    actionDesc = "Permanently delete past sale record for ${currentAction.phone.brand} ${currentAction.phone.model} (Invoice #${currentAction.phone.id})"
                    isDanger = true
                    icon = Icons.Default.Delete
                }
                is SalesSecuredAction.EditOtherIncome -> {
                    actionTitle = "Edit Other Income"
                    actionDesc = "Authorize editing extra income record for ${currentAction.item.category} (৳ ${String.format(Locale.US, "%,.0f", currentAction.item.amountBdt)})"
                    isDanger = false
                    icon = Icons.Default.Edit
                }
                is SalesSecuredAction.DeleteOtherIncome -> {
                    actionTitle = "Delete Other Income"
                    actionDesc = "Permanently delete extra income entry for ${currentAction.item.category} (৳ ${String.format(Locale.US, "%,.0f", currentAction.item.amountBdt)})"
                    isDanger = true
                    icon = Icons.Default.Delete
                }
            }

            AdminSecurityConfirmDialog(
                title = "Admin Authentication",
                actionTitle = actionTitle,
                actionDescription = actionDesc,
                actionIcon = icon,
                confirmButtonText = if (isDanger) "Delete Record" else "Authorize Edit",
                isDangerAction = isDanger,
                onDismiss = { pendingSecuredAction = null },
                onVerifyPin = { inputPin: String -> inputPin == adminPin },
                onAuthorized = {
                    val act = pendingSecuredAction
                    pendingSecuredAction = null
                    when (act) {
                        is SalesSecuredAction.EditSale -> onEditSaleRecordClick?.invoke(act.phone)
                        is SalesSecuredAction.DeleteSale -> onDeleteSaleRecordClick?.invoke(act.phone)
                        is SalesSecuredAction.EditOtherIncome -> onEditOtherIncomeClick?.invoke(act.item)
                        is SalesSecuredAction.DeleteOtherIncome -> onDeleteOtherIncomeClick?.invoke(act.item)
                        null -> {}
                    }
                }
            )
        }
    }
}

@Composable
fun SalesDateFilterChip(
    label: String,
    count: Int? = null,
    selected: Boolean,
    activeColor: Color = PrimaryNavy,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) activeColor else (if (isDark) Color(0xFF1E293B) else Color.White),
        animationSpec = tween(durationMillis = 200),
        label = "sale_date_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Slate800),
        animationSpec = tween(durationMillis = 200),
        label = "sale_date_chip_content"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) activeColor else CardBorder,
        animationSpec = tween(durationMillis = 200),
        label = "sale_date_chip_border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .height(32.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    color = if (selected) Color.White.copy(alpha = 0.25f) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$count",
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Detects whether an item represents a "Due Collection Payment from Razu vai" transaction
 * or any static due collection transaction alert box, ensuring it is hidden from the Sales
 * layout while strictly preserving all underlying ViewModel calculations and database bindings.
 */
private fun isDueCollectionTransaction(phone: PhoneItem): Boolean {
    val searchBlob = "${phone.brand} ${phone.model} ${phone.customerName} ${phone.notes} ${phone.customerPhone} ${phone.paymentMethod} ${phone.soldByStaff}".lowercase()
    return searchBlob.contains("razu") ||
           searchBlob.contains("due collection") ||
           searchBlob.contains("due payment") ||
           phone.brand.trim().equals("Due Collection", ignoreCase = true) ||
           phone.model.contains("Due Collection", ignoreCase = true) ||
           phone.notes.contains("Due Collection", ignoreCase = true)
}

/**
 * Modern Material 3 Card displaying an "Other Income" entry in the income ledger.
 * Features:
 * - Distinct custom-styled green "Other Income: [Category]" pill chip
 * - Date strictly formatted as Date-only "DD/MM/YYYY"
 * - Amount in bold ProfitGreen
 * - Staff / Admin who recorded it
 * - Admin Edit & Delete buttons (secured via PIN/Biometric authentication)
 */
@Composable
fun OtherIncomeCard(
    item: OtherIncomeItem,
    isAdmin: Boolean,
    isDark: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F241C) else Color(0xFFF0FDF4)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("other_income_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Custom-styled green Pill/Chip label: "Other Income: [Category]" & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("other_income_chip_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cash In: ${item.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                        )
                    }
                }

                // Amount
                Text(
                    text = "+ ৳ ${String.format(Locale.US, "%,.0f", item.amountBdt)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfitGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF071B14) else Color(0xFFE8FDF0),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.3f) else Color(0xFFD1FAE5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Line 1: Date-only format ("DD/MM/YYYY")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Income Date",
                            tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Date: ${DateUtils.formatLedgerDateOnly(item.date)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700
                        )
                    }

                    // Line 2: Added by Staff / Admin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Added By",
                            tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recorded by: ${item.addedBy.ifBlank { "Staff" }}",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    // Optional Notes
                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Note: ${item.notes}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color(0xFFE2E8F0) else Slate800,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Admin Action Buttons (Edit / Delete)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button
                    Surface(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                        modifier = Modifier.testTag("other_income_edit_btn_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Button
                    Surface(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2D161B) else Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFE11D48).copy(alpha = 0.5f) else Color(0xFFFECDD3)),
                        modifier = Modifier.testTag("other_income_delete_btn_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month-Year Picker dialog allowing users to choose any Month and Year (e.g. "August 2026")
 * and filter sales data strictly for that selected month.
 */
@Composable
fun MonthYearPickerDialog(
    initialYearMonth: String = "",
    onDismissRequest: () -> Unit,
    onMonthSelected: (String) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val cal = remember { Calendar.getInstance() }
    val initialYear = remember(initialYearMonth) {
        if (initialYearMonth.length >= 4) {
            initialYearMonth.substring(0, 4).toIntOrNull() ?: cal.get(Calendar.YEAR)
        } else {
            cal.get(Calendar.YEAR)
        }
    }
    val initialMonthIndex = remember(initialYearMonth) {
        if (initialYearMonth.length >= 7) {
            (initialYearMonth.substring(5, 7).toIntOrNull()?.minus(1))?.coerceIn(0, 11) ?: cal.get(Calendar.MONTH)
        } else {
            cal.get(Calendar.MONTH)
        }
    }

    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonthIndex by remember { mutableStateOf(initialMonthIndex) }

    val monthNames = remember {
        listOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                )
                Text(
                    text = "Select Month",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year Navigator Row: [<] [ 2026 ] [>]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedYear -= 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Year",
                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                        )
                    }

                    Text(
                        text = "$selectedYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Slate900
                    )

                    IconButton(
                        onClick = { selectedYear += 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Year",
                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                        )
                    }
                }

                // Month Grid: 4 rows x 3 columns
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val monthIdx = row * 3 + col
                                val isSelected = monthIdx == selectedMonthIndex
                                val monthName = monthNames[monthIdx]
                                val shortName = monthName.take(3)

                                Surface(
                                    onClick = { selectedMonthIndex = monthIdx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryNavy else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PrimaryNavy else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("month_picker_btn_${monthIdx + 1}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = shortName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Slate800)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Month Preview Callout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFEBF5FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Filtering strictly for: ${monthNames[selectedMonthIndex]} $selectedYear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedYm = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonthIndex + 1)
                    onMonthSelected(formattedYm)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("month_picker_confirm_btn")
            ) {
                Text("Apply Filter", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("month_picker_cancel_btn")
            ) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

