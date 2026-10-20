package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.UserRole
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AdminSecurityConfirmDialog
import com.example.ui.components.AppActionButton
import com.example.ui.components.AppListIconButton
import com.example.ui.components.AppOutlinedActionButton
import com.example.ui.components.AppSearchBar
import com.example.ui.components.AppSectionHeader
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils
import com.example.util.ExportImportHelper
import com.example.util.MonthFilterHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private sealed class ExpenseSecuredAction {
    object AddExpense : ExpenseSecuredAction()
    data class EditExpense(val expense: ExpenseItem) : ExpenseSecuredAction()
    data class DeleteExpense(val expense: ExpenseItem) : ExpenseSecuredAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    expenses: List<ExpenseItem>,
    currentRole: UserRole,
    isBengali: Boolean = false,
    adminPin: String = "1111",
    phones: List<PhoneItem> = emptyList(),
    onAddExpenseClick: () -> Unit,
    onDeleteExpenseClick: (ExpenseItem) -> Unit,
    onUpdateExpenseClick: ((ExpenseItem) -> Unit)? = null,
    onRequireAdminAuth: ((title: String, actionTitle: String, actionDesc: String, icon: ImageVector, confirmText: String, isDanger: Boolean, onAuth: () -> Unit) -> Unit)? = null,
    onUnlockAdminClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin = currentRole == UserRole.ADMIN
    val isDark = isAppInDarkTheme()

    val currentYearMonth = remember { MonthFilterHelper.getCurrentYearMonth() }
    var selectedYearMonth by remember { mutableStateOf(currentYearMonth) }
    var isMonthDropdownOpen by remember { mutableStateOf(false) }

    // Admin PIN Security State for sensitive expense modifications (fallback when global auth callback not provided)
    var pendingSecuredAction by remember { mutableStateOf<ExpenseSecuredAction?>(null) }
    var expenseToEdit by remember { mutableStateOf<ExpenseItem?>(null) }
    var viewingReceiptExpense by remember { mutableStateOf<ExpenseItem?>(null) }

    fun handleAddExpense() {
        onAddExpenseClick()
    }

    fun handleEditExpense(expense: ExpenseItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Edit Expense Entry",
                "Modify '${expense.title}' (${expense.category}) - ৳${String.format("%,.0f", expense.amountBdt)}",
                Icons.Default.Edit,
                "Authorize Edit",
                false
            ) { expenseToEdit = expense }
        } else {
            pendingSecuredAction = ExpenseSecuredAction.EditExpense(expense)
        }
    }

    fun handleDeleteExpense(expense: ExpenseItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Delete Expense Entry",
                "Permanently delete '${expense.title}' (${expense.category}) - ৳${String.format("%,.0f", expense.amountBdt)}",
                Icons.Default.Delete,
                "Delete Entry",
                true
            ) { onDeleteExpenseClick(expense) }
        } else {
            pendingSecuredAction = ExpenseSecuredAction.DeleteExpense(expense)
        }
    }

    val distinctExpenses = remember(expenses) {
        expenses.distinctBy { it.id }
    }

    val availableMonths = remember(distinctExpenses) {
        MonthFilterHelper.getAllAvailableMonths(emptyList(), distinctExpenses)
    }

    // Dynamic calculations based on selected month filter, sorted chronologically by timestamp/date descending (newest first)
    val filteredExpenses = remember(distinctExpenses, selectedYearMonth) {
        MonthFilterHelper.filterExpenses(distinctExpenses, selectedYearMonth)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<ExpenseItem> {
                    if (it.timestamp > 0L) it.timestamp else DateUtils.parseDateToMillis(it.date)
                }.thenByDescending { it.id }
            )
    }

    var expenseSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Filter expenses by selected category while strictly preserving chronological newest-first order
    val categoryFilteredExpenses = remember(filteredExpenses, selectedCategoryFilter) {
        val list = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter.equals("All Categories", ignoreCase = true)) {
            filteredExpenses
        } else {
            filteredExpenses.filter { item ->
                item.category.trim().equals(selectedCategoryFilter.trim(), ignoreCase = true) ||
                    cleanExpenseDisplayName(item.title, item.category).equals(selectedCategoryFilter.trim(), ignoreCase = true)
            }
        }
        list.distinctBy { it.id }.sortedWith(
            compareByDescending<ExpenseItem> {
                if (it.timestamp > 0L) it.timestamp else DateUtils.parseDateToMillis(it.date)
            }.thenByDescending { it.id }
        )
    }

    val searchedExpenses = remember(categoryFilteredExpenses, expenseSearchQuery) {
        val list = if (expenseSearchQuery.isBlank()) {
            categoryFilteredExpenses
        } else {
            val q = expenseSearchQuery.trim().lowercase()
            categoryFilteredExpenses.filter { item ->
                item.title.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.notes.lowercase().contains(q) ||
                item.date.lowercase().contains(q)
            }
        }
        list.distinctBy { it.id }.sortedWith(
            compareByDescending<ExpenseItem> {
                if (it.timestamp > 0L) it.timestamp else DateUtils.parseDateToMillis(it.date)
            }.thenByDescending { it.id }
        )
    }

    // Dynamically calculate total amount specifically for the selected category (or all categories when "ALL" is selected)
    val totalExpensesAmount = remember(categoryFilteredExpenses, searchedExpenses, expenseSearchQuery) {
        val target = if (expenseSearchQuery.isBlank()) categoryFilteredExpenses else searchedExpenses
        target.distinctBy { it.id }.sumOf { it.amountBdt }
    }

    val selectedMonthLabel = if (selectedYearMonth == "ALL") {
        "All Time"
    } else {
        val label = MonthFilterHelper.formatMonthDisplayName(selectedYearMonth)
        if (selectedYearMonth == currentYearMonth) "$label (Running)" else label
    }

    // Available categories from existing expenses
    val allCategories = remember(filteredExpenses, selectedCategoryFilter) {
        val list = mutableListOf("ALL")
        filteredExpenses.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .forEach { cat ->
                if (list.none { it.equals(cat, ignoreCase = true) }) {
                    list.add(cat)
                }
            }
        if (selectedCategoryFilter != "ALL" && list.none { it.equals(selectedCategoryFilter, ignoreCase = true) }) {
            list.add(selectedCategoryFilter)
        }
        list.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardBg)
            .padding(horizontal = 14.dp)
            .testTag("expense_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. TOP EXPENSE SEARCH BAR
        item {
            AppSearchBar(
                query = expenseSearchQuery,
                onQueryChange = { expenseSearchQuery = it },
                placeholder = "Search expenses by category, title, or date...",
                testTag = "expense_search_input",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. EXPENSE MANAGEMENT MAIN CONTAINER
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_management_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with strictly Top-aligned Title and Date Selector Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_management_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left side: Icon + "Expense" Title button
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // "Expense" Title Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFFEF2F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFFDC2626).copy(alpha = 0.4f) else Color(0xFFFECACA)
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Expense",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Right side: Date Selector Button aligned strictly at top level
                        Box(
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Surface(
                                onClick = { isMonthDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFFEF2F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFFF87171).copy(alpha = 0.5f) else Color(0xFFDC2626).copy(alpha = 0.5f)
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("expense_month_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = formatExpenseDateButtonLabel(selectedYearMonth),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMonthDropdownOpen,
                                onDismissRequest = { isMonthDropdownOpen = false },
                                modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "All Time Overview",
                                                fontWeight = if (selectedYearMonth == "ALL") FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedYearMonth == "ALL") (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color.White else Color(0xFF0F172A))
                                            )
                                            if (selectedYearMonth == "ALL") {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedYearMonth = "ALL"
                                        isMonthDropdownOpen = false
                                    }
                                )

                                HorizontalDivider(color = CardBorder)

                                availableMonths.forEach { ym ->
                                    val isCurrent = ym == currentYearMonth
                                    val isSelected = selectedYearMonth == ym
                                    val label = MonthFilterHelper.formatMonthDisplayName(ym) + if (isCurrent) " (Running)" else ""

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color.White else Color(0xFF0F172A))
                                                )
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedYearMonth = ym
                                            isMonthDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Prominently highlighted Total Amount Display Line (brackets completely removed)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Total: ৳ ${String.format("%,.0f", totalExpensesAmount)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                            .testTag("expense_total_amount_text")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated "Add Expense" action button (Export CSV completely removed)
                    AppActionButton(
                        text = "Add Expense",
                        icon = null,
                        onClick = { handleAddExpense() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "add_expense_btn"
                    )
                }
            }
        }

        // Category Filter Chips
        if (allCategories.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(allCategories) { cat ->
                        val isSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                        Surface(
                            onClick = { selectedCategoryFilter = cat },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFFDC2626) else Color(0xFFDC2626)
                            } else {
                                if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.testTag("expense_category_chip_${cat.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = if (cat == "ALL") "All Categories" else cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Slate700,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. EXPENSES LIST
        if (searchedExpenses.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (expenseSearchQuery.isNotBlank()) "No expenses match your search" else "No expenses recorded in $selectedMonthLabel",
                            color = if (isDark) Color.White else Color(0xFF334155),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Expense' to record cargo shipping, shop rent, or daily bills.",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        AppActionButton(
                            text = "Record Expense",
                            icon = null,
                            onClick = { handleAddExpense() },
                            testTag = "empty_record_expense_btn"
                        )
                    }
                }
            }
        } else {
            items(searchedExpenses, key = { expense -> "expense_${expense.id}" }) { expense ->
                val categoryColor = when {
                    expense.category.contains("Rent", ignoreCase = true) -> Color(0xFF7C3AED)
                    expense.category.contains("Courier", ignoreCase = true) || expense.category.contains("Cargo", ignoreCase = true) -> Color(0xFF2563EB)
                    expense.category.contains("Electricity", ignoreCase = true) -> Color(0xFFD97706)
                    expense.category.contains("Internet", ignoreCase = true) -> Color(0xFF0284C7)
                    expense.category.contains("Salary", ignoreCase = true) || expense.category.contains("Commission", ignoreCase = true) -> Color(0xFF059669)
                    expense.category.contains("Cleaner", ignoreCase = true) || expense.category.contains("Service Charge", ignoreCase = true) -> Color(0xFF4F46E5)
                    expense.category.contains("Guest", ignoreCase = true) -> Color(0xFFEA580C)
                    expense.category.contains("Repair", ignoreCase = true) || expense.category.contains("Part", ignoreCase = true) -> Color(0xFF0D9488)
                    expense.category.contains("Accessory", ignoreCase = true) || expense.category.contains("Accessories", ignoreCase = true) -> Color(0xFFE11D48)
                    expense.category.contains("Bank", ignoreCase = true) || expense.category.contains("Deposit", ignoreCase = true) -> Color(0xFF1D4ED8)
                    else -> Color(0xFF475569)
                }

                val categoryBg = when {
                    expense.category.contains("Rent", ignoreCase = true) -> Color(0xFFF5F3FF)
                    expense.category.contains("Courier", ignoreCase = true) || expense.category.contains("Cargo", ignoreCase = true) -> Color(0xFFEFF6FF)
                    expense.category.contains("Electricity", ignoreCase = true) -> Color(0xFFFFFBEB)
                    expense.category.contains("Internet", ignoreCase = true) -> Color(0xFFF0F9FF)
                    expense.category.contains("Salary", ignoreCase = true) || expense.category.contains("Commission", ignoreCase = true) -> Color(0xFFECFDF5)
                    expense.category.contains("Cleaner", ignoreCase = true) || expense.category.contains("Service Charge", ignoreCase = true) -> Color(0xFFEEF2FF)
                    expense.category.contains("Guest", ignoreCase = true) -> Color(0xFFFFF7ED)
                    expense.category.contains("Repair", ignoreCase = true) || expense.category.contains("Part", ignoreCase = true) -> Color(0xFFF0FDFA)
                    expense.category.contains("Accessory", ignoreCase = true) || expense.category.contains("Accessories", ignoreCase = true) -> Color(0xFFFFF1F2)
                    expense.category.contains("Bank", ignoreCase = true) || expense.category.contains("Deposit", ignoreCase = true) -> Color(0xFFEFF6FF)
                    else -> Color(0xFFF8FAFC)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_item_${expense.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Top-Right: Expense Amount (৳) right-aligned at the VERY TOP-RIGHT corner of the card
                        Text(
                            text = "৳ ${String.format("%,.0f", expense.amountBdt)}",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )

                        // Left Content: Line 1 (Title), Line 2 (Payment Month), Line 3 (Paid Date), Notes
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 96.dp)
                        ) {
                            // Line 1 (Title): Display Expense Title / Category name strictly (e.g. "Shop Rent")
                            Text(
                                text = cleanExpenseDisplayName(expense.title, expense.category),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Line 2 (Payment Month - NEW): Position directly below Title and above Paid Date. Format: Month: MMM yy (e.g. "Month: Sep 26")
                            Text(
                                text = formatExpensePaymentMonth(expense.title, expense.date, expense.timestamp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // Line 3 (Paid Date): Position directly below Payment Month line. Format: Paid: DD MMM YYYY (e.g. "Paid: 24 Sep 2026")
                            Text(
                                text = "Paid: ${formatEntryDate(expense.date, expense.timestamp)}",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (expense.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = expense.notes,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // View Receipt Button (if receipt photo exists)
                            if (!expense.receiptUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    onClick = { viewingReceiptExpense = expense },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF93C5FD)),
                                    modifier = Modifier.testTag("view_receipt_btn_${expense.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "View Receipt",
                                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "View Receipt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                                        )
                                    }
                                }
                            }

                            if (isAdmin) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Bottom-Right: Edit and Delete icon buttons right-aligned at the VERY BOTTOM-RIGHT corner of the card
                        if (isAdmin) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AppListIconButton(
                                    icon = Icons.Default.Edit,
                                    tint = PrimaryNavy,
                                    contentDescription = "Edit Expense",
                                    onClick = { handleEditExpense(expense) },
                                    testTag = "edit_expense_btn_${expense.id}"
                                )
                                AppListIconButton(
                                    icon = Icons.Default.Delete,
                                    tint = Color(0xFFEF4444),
                                    contentDescription = "Delete Expense",
                                    onClick = { handleDeleteExpense(expense) },
                                    testTag = "delete_expense_btn_${expense.id}"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // View Receipt Full-Screen Preview Dialog
    viewingReceiptExpense?.let { exp ->
        ExpenseReceiptPreviewDialog(
            expense = exp,
            onDismiss = { viewingReceiptExpense = null }
        )
    }

    // Edit Expense Dialog
    expenseToEdit?.let { exp ->
        AddEditExpenseDialog(
            isBengali = isBengali,
            initialExpense = exp,
            onDismiss = { expenseToEdit = null },
            onAddExpense = { _, _, _, _, _, _ -> },
            onUpdateExpense = { updated ->
                onUpdateExpenseClick?.invoke(updated)
                expenseToEdit = null
            }
        )
    }

    // Fallback Admin Security Dialog for direct verification
    if (onRequireAdminAuth == null) {
        pendingSecuredAction?.let { action ->
            val info = when (action) {
                is ExpenseSecuredAction.AddExpense -> ExpenseActionSecurityInfo(
                    "Add Expense Entry",
                    "Authorize recording a new expense entry",
                    false,
                    Icons.Default.Payments
                )
                is ExpenseSecuredAction.EditExpense -> ExpenseActionSecurityInfo(
                    "Edit Expense Entry",
                    "Modifying expense: ${action.expense.title} (৳${String.format("%,.0f", action.expense.amountBdt)})",
                    false,
                    Icons.Default.Edit
                )
                is ExpenseSecuredAction.DeleteExpense -> ExpenseActionSecurityInfo(
                    "Delete Expense Entry",
                    "Permanently delete expense: ${action.expense.title} (৳${String.format("%,.0f", action.expense.amountBdt)})",
                    true,
                    Icons.Default.Delete
                )
            }

            AdminSecurityConfirmDialog(
                title = "Admin Authentication",
                actionTitle = info.title,
                actionDescription = info.desc,
                actionIcon = info.icon,
                confirmButtonText = if (info.isDanger) "Delete Entry" else "Authorize Edit",
                isDangerAction = info.isDanger,
                onDismiss = { pendingSecuredAction = null },
                onVerifyPin = { inputPin -> inputPin == adminPin },
                onAuthorized = {
                    val act = action
                    pendingSecuredAction = null
                    when (act) {
                        is ExpenseSecuredAction.AddExpense -> onAddExpenseClick()
                        is ExpenseSecuredAction.EditExpense -> expenseToEdit = act.expense
                        is ExpenseSecuredAction.DeleteExpense -> onDeleteExpenseClick(act.expense)
                    }
                }
            )
        }
    }
}

private data class ExpenseActionSecurityInfo(
    val title: String,
    val desc: String,
    val isDanger: Boolean,
    val icon: ImageVector
)

private fun formatExpenseDateButtonLabel(yearMonth: String): String {
    if (yearMonth.equals("ALL", ignoreCase = true) || yearMonth.isBlank()) {
        return "Month: All"
    }
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(yearMonth) ?: return "Month: $yearMonth"
        val formatted = SimpleDateFormat("MMM yy", Locale.US).format(date)
        "Month: $formatted"
    } catch (_: Exception) {
        "Month: $yearMonth"
    }
}

private fun formatExpensePaymentMonth(title: String, dateStr: String?, timestamp: Long): String {
    val t = title.trim()
    val monthPatterns = listOf(
        "MMMM yyyy",
        "MMM yyyy",
        "MMMM yy",
        "MMM yy",
        "yyyy-MM",
        "yyyy/MM",
        "MM/yyyy",
        "MM-yyyy"
    )
    for (fmt in monthPatterns) {
        try {
            val d = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }.parse(t)
            if (d != null) {
                val formatted = SimpleDateFormat("MMM yy", Locale.US).format(d)
                return "Month: $formatted"
            }
        } catch (_: Exception) {}
    }

    try {
        val d = SimpleDateFormat("MMMM", Locale.US).apply { isLenient = false }.parse(t)
        if (d != null) {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR) % 100
            val formatted = SimpleDateFormat("MMM", Locale.US).format(d)
            return "Month: $formatted $year"
        }
    } catch (_: Exception) {}

    val ym = MonthFilterHelper.extractYearMonth(dateStr, timestamp)
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val d = parser.parse(ym)
        if (d != null) {
            val formatted = SimpleDateFormat("MMM yy", Locale.US).format(d)
            "Month: $formatted"
        } else {
            "Month: $ym"
        }
    } catch (_: Exception) {
        "Month: $ym"
    }
}

private fun cleanExpenseDisplayName(title: String, category: String): String {
    val t = title.trim()
    val c = category.trim()

    // Match full month or month + year like "September 2026", "Sep 2026", "September", "2026-09"
    val monthOnlyRegex = Regex("(?i)^(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)(\\s+\\d{2,4})?$|^\\d{4}[\\-/]\\d{1,2}$")

    if (t.matches(monthOnlyRegex)) {
        return if (c.isNotBlank()) c else t
    }

    // Strip trailing month patterns like " - September 2026", " (Sep 2026)", " September 2026", etc.
    val stripped = t.replace(Regex("(?i)[\\-\\–\\—\\(\\s]+(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)(\\s+\\d{2,4})?\\)?$"), "").trim()

    if (stripped.isNotBlank() && !stripped.matches(monthOnlyRegex)) {
        return stripped
    }

    return if (c.isNotBlank()) c else t
}

private fun formatEntryDate(dateStr: String?, timestamp: Long = 0L): String {
    if (!dateStr.isNullOrBlank()) {
        val clean = dateStr.trim()
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "dd-MMM-yyyy",
            "dd MMM yyyy"
        )
        for (fmt in formats) {
            try {
                val d = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }.parse(clean)
                if (d != null) {
                    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
                }
            } catch (_: Exception) {}
        }
    }
    if (timestamp > 0L) {
        try {
            return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timestamp))
        } catch (_: Exception) {}
    }
    return dateStr.orEmpty()
}

/**
 * Full-screen modal for reviewing receipt vouchers with Coil image caching.
 */
@Composable
fun ExpenseReceiptPreviewDialog(
    expense: ExpenseItem,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .testTag("expense_receipt_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cleanExpenseDisplayName(expense.title, expense.category),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Amount: ৳ ${String.format("%,.0f", expense.amountBdt)} • Paid: ${formatEntryDate(expense.date, expense.timestamp)}",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Slate700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PrimaryNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(expense.receiptUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Expense Receipt Voucher",
                        contentScale = ContentScale.Fit,
                        onSuccess = { isLoading = false },
                        onError = { isLoading = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${expense.notes}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Receipt Preview", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


