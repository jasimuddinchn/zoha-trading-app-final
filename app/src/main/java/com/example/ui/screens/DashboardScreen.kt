package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.PinDialog
import com.example.ui.theme.appTextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.sortedByEntryDateDescending
import com.example.util.DateUtils
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.AppActionButton
import com.example.ui.components.ImeiCameraScannerDialog
import com.example.ui.components.PhoneCard
import com.example.ui.components.SectionBadgeTitle
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StockGreen
import com.example.ui.theme.TransitAmber
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.MonthFilterHelper
import com.example.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    phones: List<PhoneItem>,
    expenses: List<ExpenseItem> = emptyList(),
    allOtherIncomes: List<OtherIncomeItem> = emptyList(),
    currentRole: UserRole,
    settings: AppSettings,
    isBengali: Boolean = false,
    onAddNewPhoneClick: () -> Unit = {},
    onAddOtherIncomeClick: () -> Unit = {},
    onSellPhoneClick: (PhoneItem) -> Unit = {},
    onViewMemoClick: (PhoneItem) -> Unit = {},
    onEditCostsClick: (PhoneItem) -> Unit = {},
    onMarkInStockClick: (PhoneItem) -> Unit = {},
    onDeletePhoneClick: (PhoneItem) -> Unit = {},
    onAddExpenseClick: () -> Unit = {},
    onNavigateToStock: () -> Unit = {},
    onNavigateToSales: (String?) -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToDue: () -> Unit = {},
    onStatusChange: ((PhoneItem, String) -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentRole == UserRole.ADMIN
    val isDark = isAppInDarkTheme()

    // Stock counts (including returned items in stock)
    val inStockPhones = phones.filter { it.status == PhoneStatus.IN_STOCK.code || it.status == PhoneStatus.RETURNED.code || it.status == "RETURN" }
    val inTransitPhones = phones.filter { it.status == PhoneStatus.IN_TRANSIT.code }
    val soldPhones = phones.filter { it.status == PhoneStatus.SOLD.code }

    // Admin overall financial summary
    val totalStockLandingCost = inStockPhones.sumOf { it.totalCostBdt }
    val totalInTransitCost = inTransitPhones.sumOf { it.totalCostBdt }
    val totalRevenue = soldPhones.sumOf { it.sellingPriceBdt }
    val totalSoldCost = soldPhones.sumOf { it.totalCostBdt }
    val grossProfit = totalRevenue - totalSoldCost

    // Running Month summary (Auto resets on 1st of month)
    val currentYearMonth = MonthFilterHelper.getCurrentYearMonth()
    val currentMonthDisplayName = MonthFilterHelper.formatMonthDisplayName(currentYearMonth)
    val runningMonthSummary = remember(phones, expenses, currentYearMonth) {
        MonthFilterHelper.calculateSummary(phones, expenses, currentYearMonth)
    }

    // Previous Month summary
    val lastYearMonth = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(cal.time)
    }
    val lastMonthSummary = remember(phones, expenses, lastYearMonth) {
        MonthFilterHelper.calculateSummary(phones, expenses, lastYearMonth)
    }

    // Collapsible states (retained for backward compatibility)
    var isSellsExpanded by remember { mutableStateOf(false) }
    var isStockExpanded by remember { mutableStateOf(false) }
    var isBalanceExpanded by remember { mutableStateOf(false) }
    var isMonthlyExpanded by remember { mutableStateOf(false) }
    var isInventoryExpanded by remember { mutableStateOf(false) }
    var isFinancialExpanded by remember { mutableStateOf(false) }

    // Active Quick Action Modal Dialog State
    var activeModal by remember { mutableStateOf<DashboardModalType?>(null) }

    // Monthly Balance & Cumulative Net Calculations via DashboardViewModel:
    // 1. Starting Baseline Capital: Editable baseline starting balance (Opening Balance & Due Collection)
    // 2. Running Month Income (from 1st day of current month to today in /sales: ONLY Cash Received portion)
    // 3. Running Month Expense (from /expenses node)
    // 4. Dynamic Total Net Balance = Editable Opening Balance + (All-Time Cash Received) + (Due Collections) - (All-Time Expenses)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        DashboardViewModel.attachRealtimeFirebaseListener(context)
    }
    val openingBalance by DashboardViewModel.globalStartingBalance.collectAsStateWithLifecycle()
    var showAdminPinForOpeningBalance by remember { mutableStateOf(false) }
    var showEditOpeningBalanceDialog by remember { mutableStateOf(false) }

    val globalOtherIncomesList by DashboardViewModel.globalOtherIncomes.collectAsStateWithLifecycle()
    val effectiveOtherIncomes = if (allOtherIncomes.isNotEmpty()) allOtherIncomes else globalOtherIncomesList

    val monthlyIncome = remember(phones, effectiveOtherIncomes, currentYearMonth) {
        DashboardViewModel.calculateRunningMonthIncome(phones, effectiveOtherIncomes, currentYearMonth)
    }
    val monthlyExpense = remember(expenses, currentYearMonth) {
        DashboardViewModel.calculateRunningMonthExpense(expenses, currentYearMonth)
    }
    val monthlyNetProfit = remember(phones, currentYearMonth) {
        DashboardViewModel.calculateMonthlyNetProfit(phones, currentYearMonth)
    }
    val allTimeSalesRevenue = remember(phones) {
        DashboardViewModel.calculateAllTimeSalesRevenue(phones)
    }
    val allTimeCashReceived = remember(phones) {
        DashboardViewModel.calculateAllTimeCashReceivedFromSales(phones)
    }
    val allTimeOtherIncome = remember(effectiveOtherIncomes) {
        DashboardViewModel.calculateAllTimeOtherIncome(effectiveOtherIncomes)
    }
    val allTimeExpenses = remember(expenses) {
        DashboardViewModel.calculateAllTimeTotalExpenses(expenses)
    }
    val globalDueCollections by DashboardViewModel.globalDueCollections.collectAsStateWithLifecycle()
    val dynamicTotalNetBalance = remember(phones, expenses, openingBalance, globalDueCollections, effectiveOtherIncomes) {
        DashboardViewModel.calculateTotalNetBalance(phones, expenses, globalDueCollections, openingBalance, effectiveOtherIncomes)
    }
    val totalOutstandingCustomerDue = remember(phones) {
        phones.filter { it.status == PhoneStatus.SOLD.code || it.status.equals("SOLD", ignoreCase = true) }
            .sumOf { it.effectiveDueAmount }
    }

    // Monthly Performance: selected month state and dropdown visibility
    var selectedPerformanceMonth by remember { mutableStateOf(currentYearMonth) }
    var showMonthDropdown by remember { mutableStateOf(false) }
    val availableMonths = remember(phones, expenses) {
        MonthFilterHelper.getAllAvailableMonths(phones, expenses)
    }
    val performanceMonthSummary = remember(phones, expenses, selectedPerformanceMonth) {
        MonthFilterHelper.calculateSummary(phones, expenses, selectedPerformanceMonth)
    }
    val performanceMonthlyNetProfit = remember(phones, selectedPerformanceMonth) {
        DashboardViewModel.calculateMonthlyNetProfit(phones, selectedPerformanceMonth)
    }

    // Visible inventory on Dashboard: hides any Due Collection Payment / Razu vai transaction card
    // or static due collection alert box from the dashboard layout while preserving underlying calculations
    val visibleDashboardPhones = remember(phones) {
        phones.filterNot { isDueCollectionTransaction(it) }
            .sortedByEntryDateDescending()
    }

    // Global Search State: real-time filtering across IMEI, model, brand, customer, supplier, notes
    var searchQuery by remember { mutableStateOf("") }
    var showImeiScanner by remember { mutableStateOf(false) }
    val searchResults = remember(visibleDashboardPhones, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            visibleDashboardPhones.filter { phone ->
                phone.imei.lowercase().contains(q) ||
                phone.imei2.lowercase().contains(q) ||
                phone.model.lowercase().contains(q) ||
                phone.brand.lowercase().contains(q) ||
                phone.color.lowercase().contains(q) ||
                phone.ramRom.lowercase().contains(q) ||
                phone.variant.lowercase().contains(q) ||
                phone.customerName.lowercase().contains(q) ||
                phone.customerPhone.lowercase().contains(q) ||
                phone.notes.lowercase().contains(q) ||
                phone.status.lowercase().contains(q) ||
                phone.itemCondition.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF121824) else CardBg)
            .padding(horizontal = 14.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SLEEK ROUNDED GLOBAL SEARCH BAR (Replaces the large Zoha Trading banner card)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_global_search_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDark) Color(0xFF94A3B8) else Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = if (isDark) Color.White else Slate900,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(if (isDark) Color.White else PrimaryNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_search_input")
                        )
                    }

                    // Action buttons at the right end: Clear button + QR/Barcode Scanner Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" }
                                    .testTag("dashboard_search_clear_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showImeiScanner = true }
                                .testTag("dashboard_search_scan_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan IMEI Barcode / QR",
                                tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // REAL-TIME SEARCH RESULTS SECTION (Shown when query is active)
        if (searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results for \"${searchQuery.trim()}\" (${searchResults.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Slate900
                    )
                    Text(
                        text = "Clear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier
                            .clickable { searchQuery = "" }
                            .padding(4.dp)
                    )
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "No items matching \"${searchQuery.trim()}\"",
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(searchResults, key = { "search_${it.id}" }) { phone ->
                    PhoneCard(
                        phone = phone,
                        currentRole = currentRole,
                        rmbRate = settings.rmbToBdtRate,
                        isBengali = false,
                        onSellClick = { onSellPhoneClick(phone) },
                        onViewMemoClick = { onViewMemoClick(phone) },
                        onEditCostsClick = { onEditCostsClick(phone) },
                        onMarkInStockClick = { onMarkInStockClick(phone) },
                        onDeleteClick = { onDeletePhoneClick(phone) },
                        onStatusChange = { newStatus -> onStatusChange?.invoke(phone, newStatus) }
                    )
                }
            }
        }

        // 3 MODERN QUICK ACTION BUTTONS DIRECTLY BENEATH SEARCH BAR (LIQUID GLASS DESIGN)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModernDashboardActionButton(
                    icon = null,
                    label = "New",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_btn_add_phone"),
                    onClick = onAddNewPhoneClick
                )

                ModernDashboardActionButton(
                    icon = null,
                    label = "Cash In",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_btn_add_other_income"),
                    onClick = onAddOtherIncomeClick
                )

                ModernDashboardActionButton(
                    icon = null,
                    label = "Expenses",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_btn_add_expense"),
                    onClick = onAddExpenseClick
                )
            }
        }

        // "QUICK ACTION" GLASSMORPHIC AMBER ACCENT CONTAINER (GROUPS 6 OVERVIEW ACTIONS)
        item {
            val ambientBorderColor = Color(0xFFFB8500).copy(alpha = 0.25f) // rgba(251, 133, 0, 0.25)
            val amberAccent = Color(0xFFFB8500)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 16.dp)
                    .testTag("quick_action_container")
            ) {
                // Outer Box: Modern glassmorphic backdrop with smooth 16dp rounded corners & ambient amber stroke
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF111827).copy(alpha = 0.92f) else Color(0xFFFBFBFD).copy(alpha = 0.95f),
                    border = BorderStroke(1.2.dp, ambientBorderColor),
                    shadowElevation = if (isDark) 5.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        amberAccent.copy(alpha = if (isDark) 0.05f else 0.03f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        val isWide = maxWidth >= 600.dp
                        if (isWide) {
                            // Responsive single row of 4 buttons on wide/tablet screens
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickActionGoldenButton(
                                    label = "Stock Overview",
                                    icon = Icons.Default.Inventory2,
                                    modifier = Modifier.weight(1f),
                                    testTag = "quick_action_btn_stock_overview",
                                    onClick = { activeModal = DashboardModalType.STOCK_OVERVIEW }
                                )
                                QuickActionGoldenButton(
                                    label = "Monthly Balance",
                                    icon = Icons.Default.ReceiptLong,
                                    modifier = Modifier.weight(1f),
                                    testTag = "quick_action_btn_monthly_balance",
                                    onClick = { activeModal = DashboardModalType.MONTHLY_BALANCE }
                                )
                                QuickActionGoldenButton(
                                    label = "Monthly Performance",
                                    icon = Icons.Default.CalendarMonth,
                                    modifier = Modifier.weight(1f),
                                    testTag = "quick_action_btn_monthly_performance",
                                    onClick = { activeModal = DashboardModalType.MONTHLY_PERFORMANCE }
                                )
                                QuickActionGoldenButton(
                                    label = "Inventory Summary",
                                    icon = Icons.Default.PhoneAndroid,
                                    modifier = Modifier.weight(1f),
                                    testTag = "quick_action_btn_inventory_summary",
                                    onClick = { activeModal = DashboardModalType.INVENTORY_SUMMARY }
                                )
                            }
                        } else {
                            // Clean balanced 2x2 grid layout for mobile view
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Row 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickActionGoldenButton(
                                        label = "Stock Overview",
                                        icon = Icons.Default.Inventory2,
                                        modifier = Modifier.weight(1f),
                                        testTag = "quick_action_btn_stock_overview",
                                        onClick = { activeModal = DashboardModalType.STOCK_OVERVIEW }
                                    )
                                    QuickActionGoldenButton(
                                        label = "Monthly Balance",
                                        icon = Icons.Default.ReceiptLong,
                                        modifier = Modifier.weight(1f),
                                        testTag = "quick_action_btn_monthly_balance",
                                        onClick = { activeModal = DashboardModalType.MONTHLY_BALANCE }
                                    )
                                }

                                // Row 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickActionGoldenButton(
                                        label = "Monthly Performance",
                                        icon = Icons.Default.CalendarMonth,
                                        modifier = Modifier.weight(1f),
                                        testTag = "quick_action_btn_monthly_performance",
                                        onClick = { activeModal = DashboardModalType.MONTHLY_PERFORMANCE }
                                    )
                                    QuickActionGoldenButton(
                                        label = "Inventory Summary",
                                        icon = Icons.Default.PhoneAndroid,
                                        modifier = Modifier.weight(1f),
                                        testTag = "quick_action_btn_inventory_summary",
                                        onClick = { activeModal = DashboardModalType.INVENTORY_SUMMARY }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // RECENT INVENTORY SECTION
        item {
            SectionBadgeTitle(
                title = "Recent Inventory",
                countBadge = "${visibleDashboardPhones.size} units",
                badgeColor = Color(0xFF059669),
                badgeBgColor = Color(0xFFECFDF5),
                actionText = "View All →",
                onActionClick = onNavigateToStock
            )
        }

        val recentPhones = visibleDashboardPhones.take(5)
        if (recentPhones.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1E1E) else CardBg
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else CardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No phones registered yet",
                            color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppActionButton(
                            text = "Add First Phone",
                            onClick = onAddNewPhoneClick
                        )
                    }
                }
            }
        } else {
            items(recentPhones, key = { it.id }) { phone ->
                if (!isDueCollectionTransaction(phone)) {
                    PhoneCard(
                        phone = phone,
                        currentRole = currentRole,
                        rmbRate = settings.rmbToBdtRate,
                        isBengali = false,
                        onSellClick = { onSellPhoneClick(phone) },
                        onViewMemoClick = { onViewMemoClick(phone) },
                        onEditCostsClick = { onEditCostsClick(phone) },
                        onMarkInStockClick = { onMarkInStockClick(phone) },
                        onDeleteClick = { onDeletePhoneClick(phone) },
                        onStatusChange = { newStatus -> onStatusChange?.invoke(phone, newStatus) }
                    )
                }
            }
        }

        if (isAdmin) {
            item {
                AllTimeCashBreakdownCard(
                    allTimeRevenue = allTimeSalesRevenue,
                    allTimeCashReceived = allTimeCashReceived,
                    allTimeOtherIncome = allTimeOtherIncome,
                    globalDueCollections = globalDueCollections,
                    totalOutstandingCustomerDue = totalOutstandingCustomerDue,
                    allTimeExpenses = allTimeExpenses,
                    openingBalance = openingBalance,
                    dynamicTotalNetBalance = dynamicTotalNetBalance,
                    isDark = isDark,
                    onNavigateToDue = onNavigateToDue
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal Popup Dialog for active Quick Action category
    if (activeModal != null) {
        DashboardOverviewModalDialog(
            activeModal = activeModal!!,
            onDismiss = { activeModal = null },
            runningMonthSummary = runningMonthSummary,
            lastMonthSummary = lastMonthSummary,
            currentMonthDisplayName = currentMonthDisplayName,
            lastYearMonth = lastYearMonth,
            inStockPhones = inStockPhones,
            inTransitPhones = inTransitPhones,
            soldPhones = soldPhones,
            phones = phones,
            totalStockLandingCost = totalStockLandingCost,
            totalInTransitCost = totalInTransitCost,
            totalRevenue = totalRevenue,
            totalSoldCost = totalSoldCost,
            grossProfit = grossProfit,
            openingBalance = openingBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            dynamicTotalNetBalance = dynamicTotalNetBalance,
            allTimeExpenses = allTimeExpenses,
            monthlyNetProfit = monthlyNetProfit,
            selectedPerformanceMonth = selectedPerformanceMonth,
            showMonthDropdown = showMonthDropdown,
            onShowMonthDropdownChange = { showMonthDropdown = it },
            onSelectMonth = { selectedPerformanceMonth = it },
            availableMonths = availableMonths,
            performanceMonthSummary = performanceMonthSummary,
            performanceMonthlyNetProfit = performanceMonthlyNetProfit,
            isAdmin = isAdmin,
            currentYearMonth = currentYearMonth,
            isBengali = isBengali,
            onOpeningBalanceClick = {
                if (isAdmin) {
                    showAdminPinForOpeningBalance = true
                }
            },
            onNavigateToSales = onNavigateToSales,
            onNavigateToStock = onNavigateToStock,
            onNavigateToReports = onNavigateToReports,
            onAddExpenseClick = onAddExpenseClick
        )
    }

    // Admin PIN & Biometric Verification for Editing Baseline Opening Balance
    if (showAdminPinForOpeningBalance) {
        AdminPinDialog(
            title = "Admin Authentication",
            actionTitle = "Edit Opening Balance",
            actionDescription = "Scan fingerprint or enter Admin PIN to update baseline opening capital & dues.",
            confirmButtonText = "Unlock & Edit",
            onDismiss = { showAdminPinForOpeningBalance = false },
            onVerifyPin = { inputPin ->
                val ownerPin = settings.ownerPin.ifBlank { "1111" }
                inputPin.trim() == ownerPin.trim()
            },
            onAuthorized = {
                showAdminPinForOpeningBalance = false
                showEditOpeningBalanceDialog = true
            }
        )
    }

    // Manual Input Dialog for Baseline Opening Balance & Due Collection
    if (showEditOpeningBalanceDialog) {
        EditOpeningBalanceDialog(
            currentAmount = openingBalance,
            onDismiss = { showEditOpeningBalanceDialog = false },
            onSave = { newAmount ->
                DashboardViewModel.saveStartingBalance(context, newAmount)
                showEditOpeningBalanceDialog = false
            }
        )
    }

    // Camera IMEI Barcode / QR Scanner Dialog
    if (showImeiScanner) {
        ImeiCameraScannerDialog(
            onDismiss = { showImeiScanner = false },
            onImeiScanned = { imei1, _ ->
                searchQuery = imei1.trim()
                showImeiScanner = false
            }
        )
    }
}

/**
 * Modern Quick Action Button with Liquid Glass / Glassmorphic visual styling:
 * - Subtle surface translucency with frosted gradient backdrop
 * - Smooth 14dp rounded corners (12dp-16dp radius)
 * - Crisp, thin border contour using soft light Gold / Amber accent (#D4AF37 / rgba(251, 133, 0, 0.35))
 * - Subtle background ambient drop shadow and luminous glow
 * - Clean high-contrast typography and interactive press feedback
 * - Displays text-only when icon is null (e.g. for "New Entry")
 */
@Composable
fun ModernDashboardActionButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "liquid_glass_btn_scale"
    )

    // Soft light Gold / Amber accent contour
    val goldAmberBorder = if (isDark) {
        Color(0xFFD4AF37).copy(alpha = 0.45f)
    } else {
        Color(0xFFD4AF37).copy(alpha = 0.50f)
    }

    // Frosted liquid glass translucency gradient
    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.85f),
                Color(0xFF0F172A).copy(alpha = 0.75f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.92f),
                Color(0xFFFFFBEB).copy(alpha = 0.82f)
            )
        )
    }

    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        shadowElevation = if (isDark) 5.dp else 4.dp,
        border = BorderStroke(1.2.dp, goldAmberBorder),
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glassBrush)
                .background(
                    // Subtle ambient liquid gold shimmer highlight on top edge
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFD4AF37).copy(alpha = if (isDark) 0.12f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFFD4AF37).copy(alpha = 0.18f) else Color(0xFFD4AF37).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFFD166) else Color(0xFFB45309),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Primary Dashboard Stat Card used for the default two visible statistics:
 * 1) Running Month Sales ("চলতি মাসের সেল")
 * 2) Total Sets Currently in Stock ("স্টকে মোট সেট")
 */
@Composable
fun PrimaryDashboardStatCard(
    title: String,
    bengaliSubtitle: String?,
    count: Int,
    unit: String,
    subInfo: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E1E) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.25f else 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$count",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFFFFFFFF) else Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFFFFFF) else Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!bengaliSubtitle.isNullOrBlank()) {
                Text(
                    text = bengaliSubtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subInfo,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean, modern Accordion Header with title, subtitle, optional badge, optional trailing action, and expand/collapse arrow icon.
 */
@Composable
fun AccordionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    badgeColor: Color = Color.Unspecified,
    trailingAction: @Composable (() -> Unit)? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF262E3D) else iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFFFFF) else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) badgeColor.copy(alpha = 0.2f) else badgeColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE2E8F0) else Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trailingAction != null) {
                trailingAction()
            }

            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    count: Int,
    unit: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF262E3D) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = if (isDark) 0.25f else 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFFFFFFFF) else Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FinancialCard(
    title: String,
    amount: Double,
    color: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val displayColor = if (isDark) {
        if (color == PrimaryNavy) Color(0xFF38BDF8) else Color(0xFF4ADE80)
    } else color

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF262E3D) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "৳ ${String.format("%,.0f", amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = displayColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFFE2E8F0) else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonthlyMetricItem(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val isDark = isAppInDarkTheme()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF262E3D) else containerColor
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF93C5FD) else accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFFFFFFFF) else Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF93C5FD) else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Redesigned Sales metric card on the Dashboard screen (Grid Card 1: Monthly Sells Overview)
 */
@Composable
fun DashboardSellsCard(
    currentMonthSets: Int,
    currentMonthRevenue: Double,
    lastMonthSets: Int,
    lastMonthRevenue: Double,
    onThisMonthClick: () -> Unit,
    onLastMonthClick: () -> Unit,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_sells_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("dashboard_sells_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sells Overview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_sells_header")
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SalesPeriodCompactRow(
                        header = "This Month",
                        setsSold = currentMonthSets,
                        revenue = currentMonthRevenue,
                        containerColor = if (isDark) Color(0xFF13233D) else Color(0xFFEFF6FF),
                        borderColor = if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE),
                        headerColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                        setsColor = if (isDark) Color.White else Slate900,
                        revenueColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF1E40AF),
                        onClick = onThisMonthClick,
                        testTag = "sells_subcard_this_month"
                    )

                    SalesPeriodCompactRow(
                        header = "Last Month",
                        setsSold = lastMonthSets,
                        revenue = lastMonthRevenue,
                        containerColor = if (isDark) Color(0xFF241A3E) else Color(0xFFFAF5FF),
                        borderColor = if (isDark) Color(0xFF7C3AED).copy(alpha = 0.5f) else Color(0xFFDDD6FE),
                        headerColor = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6D28D9),
                        setsColor = if (isDark) Color.White else Slate900,
                        revenueColor = if (isDark) Color(0xFFC084FC) else Color(0xFF5B21B6),
                        onClick = onLastMonthClick,
                        testTag = "sells_subcard_last_month"
                    )
                }
            }
        }
    }
}

@Composable
fun SalesPeriodCompactRow(
    header: String,
    setsSold: Int,
    revenue: Double,
    containerColor: Color,
    borderColor: Color,
    headerColor: Color,
    setsColor: Color,
    revenueColor: Color,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = header,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$setsSold Sets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = setsColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "৳ ${String.format(java.util.Locale.US, "%,.0f", revenue)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = revenueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Redesigned Stock Overview metric card on the Dashboard screen:
 * - Clean "Stock Overview" header (No icons, no extra text).
 * - Side-by-Side Layout:
 *     * Left Column: "Stock:", Line 1: total sets (e.g., "25 Sets"), Line 2: total financial value (e.g., "৳ 7,50,000")
 *     * Right Column: "Transit:", Line 1: total transit sets (e.g., "5 Sets"), Line 2: total financial value (e.g., "৳ 1,50,000")
 * - Bottom Summary Row: "Total Value: ৳ X,XX,XXX" (Sum of Stock Value + Transit Value).
 * - Compact visual styling, no icons, single-line constraints with TextOverflow.Ellipsis.
 * - Role-Based Visibility: Financial monetary values visible ONLY when logged in as Admin;
 *   automatically masked (••••••) for Staff members while keeping set counts clearly visible.
 * - High contrast in both Dark and Light modes.
 */
@Composable
fun DashboardStockOverviewCard(
    stockSets: Int,
    stockValue: Double,
    transitSets: Int,
    transitValue: Double,
    showFinancialValues: Boolean = true,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    onStockClick: () -> Unit = {},
    onTransitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val totalValue = stockValue + transitValue

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_stock_overview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("stock_overview_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stock Overview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_stock_overview_header")
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockStatusCompactRow(
                        label = "Stock",
                        setsCount = stockSets,
                        financialValue = stockValue,
                        showFinancialValue = showFinancialValues,
                        containerColor = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
                        borderColor = if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0),
                        labelColor = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                        setsColor = if (isDark) Color.White else Slate900,
                        valueColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                        onClick = onStockClick,
                        testTag = "stock_overview_subcard_stock"
                    )

                    StockStatusCompactRow(
                        label = "Transit",
                        setsCount = transitSets,
                        financialValue = transitValue,
                        showFinancialValue = showFinancialValues,
                        containerColor = if (isDark) Color(0xFF2E200E) else Color(0xFFFFFBEB),
                        borderColor = if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A),
                        labelColor = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                        setsColor = if (isDark) Color.White else Slate900,
                        valueColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                        onClick = {
                            if (showFinancialValues) {
                                onTransitClick()
                            }
                        },
                        enabled = showFinancialValues,
                        testTag = "stock_overview_subcard_transit"
                    )

                    if (showFinancialValues) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStockClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Value: ৳ ${String.format(java.util.Locale.US, "%,.0f", totalValue)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFE2E8F0) else Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("stock_overview_total_value")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockStatusCompactRow(
    label: String,
    setsCount: Int,
    financialValue: Double,
    showFinancialValue: Boolean = true,
    containerColor: Color,
    borderColor: Color,
    labelColor: Color,
    setsColor: Color,
    valueColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Surface(
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$setsCount Sets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = setsColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showFinancialValue) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", financialValue)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Monthly Balance Summary Card (Grid Card 3)
 */
@Composable
fun DashboardMonthlyBalanceCard(
    monthlyIncome: Double,
    monthlyExpense: Double,
    openingBalance: Double = DashboardViewModel.STARTING_CAPITAL_BALANCE,
    totalNetBalance: Double = openingBalance,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    onIncomeClick: () -> Unit = {},
    onExpenseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_monthly_balance_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("monthly_balance_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly Balance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_monthly_balance_header")
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Income Row
                    Surface(
                        onClick = onIncomeClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_balance_subcard_income")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Cash In:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyIncome)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("monthly_balance_income_value")
                            )
                        }
                    }

                    // Expense Row
                    Surface(
                        onClick = onExpenseClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2D161B) else Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFE11D48).copy(alpha = 0.5f) else Color(0xFFFECDD3)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_balance_subcard_expense")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expense:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyExpense)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("monthly_balance_expense_value")
                            )
                        }
                    }

                    // Net Balance Row
                    val isPositive = totalNetBalance >= 0
                    val netContainerColor = if (isPositive) {
                        if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4)
                    } else {
                        if (isDark) Color(0xFF2D161B) else Color(0xFFFFF1F2)
                    }
                    val netBorderColor = if (isPositive) {
                        if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)
                    } else {
                        if (isDark) Color(0xFFE11D48).copy(alpha = 0.5f) else Color(0xFFFECDD3)
                    }
                    val netTextColor = if (isPositive) {
                        if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    } else {
                        if (isDark) Color(0xFFFB7185) else Color(0xFFBE123C)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = netContainerColor,
                        border = BorderStroke(1.dp, netBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Balance:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = netTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val formattedNetBalance = if (isPositive) {
                                "৳ ${String.format(java.util.Locale.US, "%,.0f", totalNetBalance)}"
                            } else {
                                "- ৳ ${String.format(java.util.Locale.US, "%,.0f", kotlin.math.abs(totalNetBalance))}"
                            }
                            Text(
                                text = formattedNetBalance,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = netTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("monthly_balance_total_net_value")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Monthly Performance Card (Grid Card 4)
 */
@Composable
fun DashboardMonthlyPerformanceCard(
    isMonthlyExpanded: Boolean,
    onToggle: () -> Unit,
    selectedPerformanceMonth: String,
    showMonthDropdown: Boolean,
    onShowMonthDropdownChange: (Boolean) -> Unit,
    onSelectMonth: (String) -> Unit,
    availableMonths: List<String>,
    performanceMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    isAdmin: Boolean,
    currentYearMonth: String,
    isBengali: Boolean,
    onNavigateToSales: (String) -> Unit,
    onNavigateToStock: () -> Unit,
    monthlyNetProfit: Double = performanceMonthSummary.netProfit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("accordion_monthly_performance")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("monthly_performance_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBengali) "পারফরম্যান্স" else "Performance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("monthly_performance_header_title")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        Surface(
                            onClick = {
                                onShowMonthDropdownChange(true)
                                if (!isMonthlyExpanded) onToggle()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF262E3D) else Color(0xFFEFF6FF),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFFBFDBFE)
                            ),
                            modifier = Modifier.testTag("monthly_performance_month_selector_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = formatCompactMonth(selectedPerformanceMonth),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Month",
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { onShowMonthDropdownChange(false) },
                            modifier = Modifier
                                .background(if (isDark) Color(0xFF1E2430) else Color.White)
                                .testTag("monthly_performance_dropdown_menu")
                        ) {
                            availableMonths.forEach { ym ->
                                val isSelected = ym == selectedPerformanceMonth
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = MonthFilterHelper.formatMonthDisplayName(ym),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) {
                                                    if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                                } else {
                                                    if (isDark) Color.White else Slate900
                                                }
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectMonth(ym)
                                        onShowMonthDropdownChange(false)
                                    },
                                    modifier = Modifier.testTag("month_item_$ym")
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onToggle() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMonthlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isMonthlyExpanded) "Collapse" else "Expand",
                                tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isMonthlyExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sold
                    Surface(
                        onClick = { onNavigateToSales(selectedPerformanceMonth) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF13233D) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("metric_sold_this_month")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sold:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${performanceMonthSummary.soldPhonesCount} Units",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳ ${String.format(java.util.Locale.US, "%,.0f", performanceMonthSummary.totalRevenue)} rev",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1E40AF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Net Profit (Admin)
                    if (isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF0F291E) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("metric_profit_this_month")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Profit:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyNetProfit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Stock Added
                    Surface(
                        onClick = onNavigateToStock,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2E200E) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("metric_stock_added_this_month")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stock Added:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${performanceMonthSummary.newStockAddedCount} Units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inventory Summary Card (Grid Card 5)
 */
@Composable
fun DashboardInventorySummaryCard(
    inStockCount: Int,
    inTransitCount: Int,
    soldCount: Int,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    onNavigateToStock: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    isBengali: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("accordion_overall_inventory")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("inventory_summary_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBengali) "ইনভেন্টরি সামারি" else "Inventory Summary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_summary_header_title")
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // In Stock
                    Surface(
                        onClick = onNavigateToStock,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In Stock:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$inStockCount Units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StockGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // In Transit
                    Surface(
                        onClick = onNavigateToStock,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2E200E) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In Transit:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$inTransitCount Units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TransitAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Sold Out
                    Surface(
                        onClick = onNavigateToSales,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF13233D) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sold Out:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$soldCount Units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Financial Overview Card (Grid Card 6)
 */
@Composable
fun DashboardFinancialOverviewCard(
    thisMonthProfit: Double,
    grossProfit: Double,
    totalRevenue: Double,
    isAdmin: Boolean,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    isBengali: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else CardBg
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("accordion_financial_overview")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle() }
                    .padding(vertical = 2.dp)
                    .testTag("financial_overview_accordion_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBengali) "আর্থিক ওভারভিউ" else "Financial Overview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("financial_overview_header_title")
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggle() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isDark) Color(0xFFE2E8F0) else Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Profit Metrics (Admin Only - completely unmounted for non-admins)
                    if (isAdmin) {
                        // This Month Profit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF0F291E) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Month Profit:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", thisMonthProfit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Gross Profit
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF0F291E) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gross Profit:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", grossProfit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ProfitGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Total Revenue
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF13233D) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Revenue:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "৳ ${String.format(java.util.Locale.US, "%,.0f", totalRevenue)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format Year-Month (e.g., "2026-09") into compact display string (e.g., "Sep, 26").
 */
fun formatCompactMonth(yearMonth: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val date = parser.parse(yearMonth) ?: return yearMonth
        java.text.SimpleDateFormat("MMM, yy", java.util.Locale.US).format(date)
    } catch (e: Exception) {
        yearMonth
    }
}

/**
 * Types of Quick Action popup dialogs on the Dashboard.
 */
enum class DashboardModalType {
    MONTHLY_SELLS,
    STOCK_OVERVIEW,
    MONTHLY_BALANCE,
    MONTHLY_PERFORMANCE,
    INVENTORY_SUMMARY,
    FINANCIAL_OVERVIEW
}

/**
 * Format numbers compactly (e.g. 25.9K, 1.2M).
 */
fun formatCompactNumber(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", amount / 1_000_000)
        absAmount >= 1_000 -> String.format(java.util.Locale.US, "%.0fK", amount / 1_000)
        else -> String.format(java.util.Locale.US, "%.0f", amount)
    }
}

/**
 * Equal-sized Highlighted Quick Action Button.
 * Features a subtle light border stroke (12dp radius), delicate box-shadow,
 * dark surface fill with subtle amber/gold highlights, clear active press scale feedback,
 * and high-contrast single-line typography with ellipsis overflow prevention.
 */
@Composable
fun QuickActionGoldenButton(
    label: String,
    icon: ImageVector,
    subtext: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val amberAccent = Color(0xFFFB8500)
    val amberHighlight = Color(0xFFFFB703)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "quick_btn_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF151D2C) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDark) amberAccent.copy(alpha = 0.22f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isPressed) 1.dp else if (isDark) 3.dp else 2.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            amberAccent.copy(alpha = if (isDark) 0.08f else 0.04f),
                            amberAccent.copy(alpha = if (isDark) 0.015f else 0.00f)
                        )
                    )
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = amberAccent.copy(alpha = if (isDark) 0.15f else 0.10f),
                    border = BorderStroke(0.8.dp, amberAccent.copy(alpha = 0.32f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isDark) amberHighlight else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        fontSize = if (subtext != null) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF8FAFC) else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtext != null) {
                        Text(
                            text = subtext,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) amberHighlight else Color(0xFFD97706),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Universal Dialog Popup for Dashboard Quick Actions.
 * Displays the corresponding detailed data, calculations, and tables for that category.
 */
@Composable
fun DashboardOverviewModalDialog(
    activeModal: DashboardModalType,
    onDismiss: () -> Unit,
    runningMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    lastMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    currentMonthDisplayName: String,
    lastYearMonth: String,
    inStockPhones: List<PhoneItem>,
    inTransitPhones: List<PhoneItem>,
    soldPhones: List<PhoneItem>,
    phones: List<PhoneItem>,
    totalStockLandingCost: Double,
    totalInTransitCost: Double,
    totalRevenue: Double,
    totalSoldCost: Double,
    grossProfit: Double,
    openingBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    dynamicTotalNetBalance: Double,
    allTimeExpenses: Double = 0.0,
    monthlyNetProfit: Double = 0.0,
    selectedPerformanceMonth: String,
    showMonthDropdown: Boolean,
    onShowMonthDropdownChange: (Boolean) -> Unit,
    onSelectMonth: (String) -> Unit,
    availableMonths: List<String>,
    performanceMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    performanceMonthlyNetProfit: Double = performanceMonthSummary.netProfit,
    isAdmin: Boolean,
    currentYearMonth: String,
    isBengali: Boolean,
    onOpeningBalanceClick: () -> Unit = {},
    onNavigateToSales: (String?) -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddExpenseClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val goldenColor = Color(0xFFD4AF37)

    val modalTitle = when (activeModal) {
        DashboardModalType.MONTHLY_SELLS -> "Monthly Sells Overview"
        DashboardModalType.STOCK_OVERVIEW -> "Stock Overview"
        DashboardModalType.MONTHLY_BALANCE -> "Monthly Balance"
        DashboardModalType.MONTHLY_PERFORMANCE -> "Monthly Performance"
        DashboardModalType.INVENTORY_SUMMARY -> "Inventory Summary"
        DashboardModalType.FINANCIAL_OVERVIEW -> "Financial Overview"
    }

    val modalIcon = when (activeModal) {
        DashboardModalType.MONTHLY_SELLS -> Icons.Default.TrendingUp
        DashboardModalType.STOCK_OVERVIEW -> Icons.Default.Inventory2
        DashboardModalType.MONTHLY_BALANCE -> Icons.Default.ReceiptLong
        DashboardModalType.MONTHLY_PERFORMANCE -> Icons.Default.CalendarMonth
        DashboardModalType.INVENTORY_SUMMARY -> Icons.Default.PhoneAndroid
        DashboardModalType.FINANCIAL_OVERVIEW -> Icons.Default.ShoppingCartCheckout
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E2638) else Color.White,
                border = BorderStroke(1.5.dp, goldenColor),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss when clicking content */ }
                    .testTag("dashboard_modal_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = goldenColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, goldenColor.copy(alpha = 0.6f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = modalIcon,
                                        contentDescription = null,
                                        tint = goldenColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = modalTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onDismiss() }
                                .testTag("close_dashboard_modal_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modal Body Content based on active category
                    when (activeModal) {
                        DashboardModalType.MONTHLY_SELLS -> {
                            MonthlySellsModalBody(
                                runningMonthSummary = runningMonthSummary,
                                lastMonthSummary = lastMonthSummary,
                                currentMonthDisplayName = currentMonthDisplayName,
                                lastYearMonth = lastYearMonth,
                                currentYearMonth = currentYearMonth,
                                isDark = isDark,
                                onNavigateToSales = { ym ->
                                    onDismiss()
                                    onNavigateToSales(ym)
                                }
                            )
                        }
                        DashboardModalType.STOCK_OVERVIEW -> {
                            StockOverviewModalBody(
                                inStockCount = inStockPhones.size,
                                inTransitCount = inTransitPhones.size,
                                stockValue = totalStockLandingCost,
                                transitValue = totalInTransitCost,
                                isAdmin = isAdmin,
                                isDark = isDark,
                                onNavigateToStock = {
                                    onDismiss()
                                    onNavigateToStock()
                                }
                            )
                        }
                        DashboardModalType.MONTHLY_BALANCE -> {
                            MonthlyBalanceModalBody(
                                openingBalance = openingBalance,
                                monthlyIncome = monthlyIncome,
                                monthlyExpense = monthlyExpense,
                                totalNetBalance = dynamicTotalNetBalance,
                                allTimeSalesRevenue = totalRevenue,
                                allTimeExpenses = allTimeExpenses,
                                currentYearMonth = currentYearMonth,
                                isDark = isDark,
                                isAdmin = isAdmin,
                                onOpeningBalanceClick = onOpeningBalanceClick,
                                onNavigateToSales = { ym ->
                                    onDismiss()
                                    onNavigateToSales(ym)
                                },
                                onAddExpenseClick = {
                                    onDismiss()
                                    onAddExpenseClick()
                                }
                            )
                        }
                        DashboardModalType.MONTHLY_PERFORMANCE -> {
                            MonthlyPerformanceModalBody(
                                selectedPerformanceMonth = selectedPerformanceMonth,
                                showMonthDropdown = showMonthDropdown,
                                onShowMonthDropdownChange = onShowMonthDropdownChange,
                                onSelectMonth = onSelectMonth,
                                availableMonths = availableMonths,
                                performanceMonthSummary = performanceMonthSummary,
                                isAdmin = isAdmin,
                                isDark = isDark,
                                onNavigateToSales = { ym ->
                                    onDismiss()
                                    onNavigateToSales(ym)
                                },
                                onNavigateToStock = {
                                    onDismiss()
                                    onNavigateToStock()
                                },
                                monthlyNetProfit = performanceMonthlyNetProfit
                            )
                        }
                        DashboardModalType.INVENTORY_SUMMARY -> {
                            InventorySummaryModalBody(
                                totalUnits = phones.size,
                                inStockCount = inStockPhones.size,
                                inTransitCount = inTransitPhones.size,
                                soldCount = soldPhones.size,
                                isDark = isDark,
                                onNavigateToStock = {
                                    onDismiss()
                                    onNavigateToStock()
                                },
                                onNavigateToSales = {
                                    onDismiss()
                                    onNavigateToSales(null)
                                }
                            )
                        }
                        DashboardModalType.FINANCIAL_OVERVIEW -> {
                            FinancialOverviewModalBody(
                                thisMonthProfit = if (monthlyNetProfit != 0.0) monthlyNetProfit else runningMonthSummary.netProfit,
                                grossProfit = grossProfit,
                                totalRevenue = totalRevenue,
                                totalSoldCost = totalSoldCost,
                                totalUnitsSold = soldPhones.size,
                                inStockUnits = inStockPhones.size,
                                inTransitUnits = inTransitPhones.size,
                                isAdmin = isAdmin,
                                isDark = isDark,
                                onNavigateToReports = {
                                    onDismiss()
                                    onNavigateToReports()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySellsModalBody(
    runningMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    lastMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    currentMonthDisplayName: String,
    lastYearMonth: String,
    currentYearMonth: String,
    isDark: Boolean,
    onNavigateToSales: (String) -> Unit
) {
    val lastMonthDisplayName = remember(lastYearMonth) {
        MonthFilterHelper.formatMonthDisplayName(lastYearMonth)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // This Month Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF13233D) else Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Month ($currentMonthDisplayName)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = "${runningMonthSummary.soldPhonesCount} Units Sold",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Gross Sales Revenue",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", runningMonthSummary.totalRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Slate900
                        )
                    }

                    Surface(
                        onClick = { onNavigateToSales(currentYearMonth) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "View Sales →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Last Month Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF241A3E) else Color(0xFFFAF5FF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF7C3AED).copy(alpha = 0.5f) else Color(0xFFDDD6FE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Month ($lastMonthDisplayName)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6D28D9)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                    ) {
                        Text(
                            text = "${lastMonthSummary.soldPhonesCount} Units Sold",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6B21A8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Gross Sales Revenue",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", lastMonthSummary.totalRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Slate900
                        )
                    }

                    Surface(
                        onClick = { onNavigateToSales(lastYearMonth) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "View Sales →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockOverviewModalBody(
    inStockCount: Int,
    inTransitCount: Int,
    stockValue: Double,
    transitValue: Double,
    isAdmin: Boolean,
    isDark: Boolean,
    onNavigateToStock: () -> Unit
) {
    val totalUnits = inStockCount + inTransitCount
    val totalCombinedValue = stockValue + transitValue

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // In Stock Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "In Stock Units",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    )
                    Text(
                        text = "$inStockCount Sets",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Slate900
                    )
                }

                if (isAdmin) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Stock Valuation",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", stockValue)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        )
                    }
                }
            }
        }

        // In Transit Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF2C2011) else Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "In Transit Units",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                    )
                    Text(
                        text = "$inTransitCount Sets",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Slate900
                    )
                }

                if (isAdmin) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Transit Valuation",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", transitValue)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // Total Stock Value Summary Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Combined Portfolio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                    Text(
                        text = "$totalUnits Total Sets",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Slate900
                    )
                }

                if (isAdmin) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Investment Value",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", totalCombinedValue)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            onClick = onNavigateToStock,
            shape = RoundedCornerShape(10.dp),
            color = PrimaryNavy,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Open Stock Inventory →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MonthlyBalanceModalBody(
    openingBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    totalNetBalance: Double,
    allTimeSalesRevenue: Double = 0.0,
    allTimeExpenses: Double = 0.0,
    currentYearMonth: String,
    isDark: Boolean,
    isAdmin: Boolean = false,
    onOpeningBalanceClick: () -> Unit = {},
    onNavigateToSales: (String) -> Unit,
    onAddExpenseClick: () -> Unit
) {
    val isPositive = totalNetBalance >= 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High-Contrast Net Balance Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isPositive) {
                if (isDark) Color(0xFF0C2719) else Color(0xFFDCFCE7)
            } else {
                if (isDark) Color(0xFF33141C) else Color(0xFFFFE4E6)
            },
            border = BorderStroke(
                1.5.dp,
                if (isPositive) Color(0xFF22C55E) else Color(0xFFF43F5E)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DYNAMIC TOTAL NET BALANCE",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = if (isPositive) {
                        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    } else {
                        if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedNet = if (isPositive) {
                    "৳ ${String.format(java.util.Locale.US, "%,.0f", totalNetBalance)}"
                } else {
                    "- ৳ ${String.format(java.util.Locale.US, "%,.0f", kotlin.math.abs(totalNetBalance))}"
                }
                Text(
                    text = formattedNet,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPositive) {
                        if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    } else {
                        if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                    }
                )
            }
        }

        // Opening Balance & Due Collection (Editable by Admin - Restricted to Admin Only)
        if (isAdmin) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF221E12) else Color(0xFFFEFCE8),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpeningBalanceClick() }
                    .testTag("opening_balance_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Opening Balance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Opening Balance",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "& Due Collection",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Slate500
                        )
                    }
                    Text(
                        text = "৳ ${String.format(java.util.Locale.US, "%,.0f", openingBalance)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }

        // Monthly Cash In Card (Clickable to Sales)
        Surface(
            onClick = { onNavigateToSales(currentYearMonth) },
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Cash In",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    )
                    Text(
                        text = "Sales revenue →",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    )
                }
                Text(
                    text = "+ ৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyIncome)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                )
            }
        }

        // Monthly Expense Card (Clickable to Add Expense)
        Surface(
            onClick = onAddExpenseClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF2D161B) else Color(0xFFFFF1F2),
            border = BorderStroke(1.dp, if (isDark) Color(0xFFE11D48).copy(alpha = 0.5f) else Color(0xFFFECDD3)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Expenses",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                    )
                    Text(
                        text = "Operating costs →",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                    )
                }
                Text(
                    text = "- ৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyExpense)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                )
            }
        }

        // All Time Cash Breakdown Card (Restricted to Admin Only)
        if (isAdmin) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("modal_all_time_cash_breakdown")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "All Time Cash Breakdown",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else PrimaryNavy
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "All-Time Sales Revenue:",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", allTimeSalesRevenue)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "All-Time Total Expenses:",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700
                        )
                        Text(
                            text = "৳ ${String.format(java.util.Locale.US, "%,.0f", allTimeExpenses)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                        )
                    }
                }
            }
        }
    }
}

/**
 * All Time Cash Breakdown summary card displayed at the bottom of the Dashboard for Admin users.
 */
@Composable
fun AllTimeCashBreakdownCard(
    allTimeRevenue: Double,
    allTimeCashReceived: Double = allTimeRevenue,
    allTimeOtherIncome: Double = 0.0,
    globalDueCollections: Double = 0.0,
    totalOutstandingCustomerDue: Double = 0.0,
    allTimeExpenses: Double,
    openingBalance: Double,
    dynamicTotalNetBalance: Double,
    isDark: Boolean,
    onNavigateToDue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_all_time_cash_breakdown_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Time Cash Breakdown",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF93C5FD) else PrimaryNavy
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF2563EB).copy(alpha = 0.2f) else Color(0xFFDBEAFE)
                ) {
                    Text(
                        text = "Admin Only",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opening Baseline Capital:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
                Text(
                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", openingBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All-Time Cash Received (Sales):",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
                Text(
                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", allTimeCashReceived)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                )
            }

            if (allTimeOtherIncome > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All-Time Other Income:",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700
                    )
                    Text(
                        text = "+ ৳ ${String.format(java.util.Locale.US, "%,.0f", allTimeOtherIncome)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Due Collections Added:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
                Text(
                    text = "+ ৳ ${String.format(java.util.Locale.US, "%,.0f", globalDueCollections)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All-Time Total Expenses:",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )
                Text(
                    text = "- ৳ ${String.format(java.util.Locale.US, "%,.0f", allTimeExpenses)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                )
            }

            Surface(
                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {}

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dynamic Total Net Balance:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900
                )
                Text(
                    text = "৳ ${String.format(java.util.Locale.US, "%,.0f", dynamicTotalNetBalance)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (dynamicTotalNetBalance >= 0) {
                        if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    } else {
                        if (isDark) Color(0xFFFB7185) else Color(0xFF9F1239)
                    }
                )
            }
        }
    }
}

/**
 * Dialog for Admin to edit baseline Starting Balance / Opening Balance & Due Collection.
 */
@Composable
fun EditOpeningBalanceDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val isDark = isAppInDarkTheme()
    var amountText by remember {
        mutableStateOf(if (currentAmount > 0) String.format(java.util.Locale.US, "%.0f", currentAmount) else "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD4AF37).copy(alpha = 0.18f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Update Opening Balance",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter baseline starting balance and collected dues amount (৳):",
                    fontSize = 12.5.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Starting Balance & Due (৳)") },
                    placeholder = { Text("e.g. 16845") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_opening_balance_input")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.trim().toDoubleOrNull()
                    if (parsed == null || parsed < 0) {
                        errorMessage = "Please enter a valid amount (≥ 0)"
                    } else {
                        onSave(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("edit_opening_balance_save_btn")
            ) {
                Text("Save Balance", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_opening_balance_cancel_btn")
            ) {
                Text("Cancel", color = if (isDark) Color(0xFF94A3B8) else Slate700)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}

@Composable
fun MonthlyPerformanceModalBody(
    selectedPerformanceMonth: String,
    showMonthDropdown: Boolean,
    onShowMonthDropdownChange: (Boolean) -> Unit,
    onSelectMonth: (String) -> Unit,
    availableMonths: List<String>,
    performanceMonthSummary: MonthFilterHelper.MonthFinancialSummary,
    isAdmin: Boolean,
    isDark: Boolean,
    onNavigateToSales: (String) -> Unit,
    onNavigateToStock: () -> Unit,
    monthlyNetProfit: Double = performanceMonthSummary.netProfit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month Selector Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Month:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700
                )

                Box {
                    Surface(
                        onClick = { onShowMonthDropdownChange(true) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD4AF37).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = MonthFilterHelper.formatMonthDisplayName(selectedPerformanceMonth),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Month",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { onShowMonthDropdownChange(false) },
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF1E2430) else Color.White)
                            .border(1.dp, if (isDark) Color(0xFF334155) else CardBorder)
                    ) {
                        availableMonths.forEach { ym ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = MonthFilterHelper.formatMonthDisplayName(ym),
                                        fontSize = 12.sp,
                                        fontWeight = if (ym == selectedPerformanceMonth) FontWeight.Bold else FontWeight.Normal,
                                        color = if (ym == selectedPerformanceMonth) Color(0xFFD4AF37) else if (isDark) Color.White else Slate900
                                    )
                                },
                                onClick = {
                                    onSelectMonth(ym)
                                    onShowMonthDropdownChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBox(
                title = "Sets Sold",
                value = "${performanceMonthSummary.soldPhonesCount} Units",
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
            MetricBox(
                title = "Revenue",
                value = "৳ ${String.format(java.util.Locale.US, "%,.0f", performanceMonthSummary.totalRevenue)}",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
        }

        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    title = "Expenses",
                    value = "৳ ${String.format(java.util.Locale.US, "%,.0f", performanceMonthSummary.totalExpenses)}",
                    color = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
                MetricBox(
                    title = "Net Profit",
                    value = "৳ ${String.format(java.util.Locale.US, "%,.0f", monthlyNetProfit)}",
                    color = Color(0xFFD4AF37),
                    modifier = Modifier.weight(1f),
                    isDark = isDark
                )
            }
        } else {
            MetricBox(
                title = "Expenses",
                value = "৳ ${String.format(java.util.Locale.US, "%,.0f", performanceMonthSummary.totalExpenses)}",
                color = Color(0xFFF43F5E),
                modifier = Modifier.fillMaxWidth(),
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onNavigateToSales(selectedPerformanceMonth) },
                shape = RoundedCornerShape(10.dp),
                color = PrimaryNavy,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "View Sales →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                onClick = onNavigateToStock,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0D9488),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Check Stock →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InventorySummaryModalBody(
    totalUnits: Int,
    inStockCount: Int,
    inTransitCount: Int,
    soldCount: Int,
    isDark: Boolean,
    onNavigateToStock: () -> Unit,
    onNavigateToSales: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status Row Breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventoryStatusMiniCard(
                title = "In Stock",
                count = inStockCount,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
            InventoryStatusMiniCard(
                title = "In Transit",
                count = inTransitCount,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
            InventoryStatusMiniCard(
                title = "Sold",
                count = soldCount,
                color = Color(0xFF6366F1),
                modifier = Modifier.weight(1f),
                isDark = isDark
            )
        }

        // Total registered card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF5F3FF),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFDDD6FE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total System Registered",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                    )
                    Text(
                        text = "Across all lifecycles",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate500
                    )
                }
                Text(
                    text = "$totalUnits Units",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Slate900
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onNavigateToStock,
                shape = RoundedCornerShape(10.dp),
                color = PrimaryNavy,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Stock Inventory →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                onClick = onNavigateToSales,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF4338CA),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sales History →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialOverviewModalBody(
    thisMonthProfit: Double,
    grossProfit: Double,
    totalRevenue: Double,
    totalSoldCost: Double,
    totalUnitsSold: Int,
    inStockUnits: Int,
    inTransitUnits: Int,
    isAdmin: Boolean,
    isDark: Boolean,
    onNavigateToReports: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isAdmin) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF0F291E) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "This Month's Net Profit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                    )
                    Text(
                        text = "৳ ${String.format(java.util.Locale.US, "%,.0f", thisMonthProfit)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialRow(
                        label = "All-Time Total Revenue",
                        value = "৳ ${String.format(java.util.Locale.US, "%,.0f", totalRevenue)}",
                        isDark = isDark
                    )
                    FinancialRow(
                        label = "All-Time Cost of Sold Items",
                        value = "৳ ${String.format(java.util.Locale.US, "%,.0f", totalSoldCost)}",
                        isDark = isDark
                    )
                    FinancialRow(
                        label = "All-Time Gross Profit",
                        value = "৳ ${String.format(java.util.Locale.US, "%,.0f", grossProfit)}",
                        valueColor = Color(0xFFD4AF37),
                        isDark = isDark
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Operational Overview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Lifetime Units Sold: $totalUnitsSold sets\n• Current Stock Available: $inStockUnits sets\n• Shipping in Transit: $inTransitUnits sets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Slate900,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            onClick = onNavigateToReports,
            shape = RoundedCornerShape(10.dp),
            color = PrimaryNavy,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Open Full Reports & Analytics →",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Slate500
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InventoryStatusMiniCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Slate900
            )
        }
    }
}

@Composable
private fun FinancialRow(
    label: String,
    value: String,
    isDark: Boolean,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = if (isDark) Color(0xFF94A3B8) else Slate700
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: (if (isDark) Color.White else Slate900)
        )
    }
}

/**
 * Detects whether an item represents a "Due Collection Payment from Razu vai" transaction
 * or any static due collection transaction alert box, ensuring it is hidden from the Dashboard
 * layout while strictly preserving all underlying ViewModel calculations, database logic, and Admin authorization checks.
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


