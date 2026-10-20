package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.SectionBadgeTitle
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppStrings
import com.example.data.model.BRAND_OPTIONS
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.sortedByEntryDateDescending
import com.example.data.model.sortedBySaleDateDescending
import com.example.ui.components.AdminSecurityConfirmDialog
import com.example.ui.components.AppActionButton
import com.example.ui.components.AppOutlinedActionButton
import com.example.ui.components.AppSearchBar
import com.example.ui.components.CustomInputPopupDialog
import com.example.ui.components.ImeiCameraScannerDialog
import com.example.ui.components.PhoneCard
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private sealed class StockSecuredAction {
    object AddPhone : StockSecuredAction()
    data class EditCosts(val phone: PhoneItem) : StockSecuredAction()
    data class DeletePhone(val phone: PhoneItem) : StockSecuredAction()
    data class MarkInStock(val phone: PhoneItem) : StockSecuredAction()
    data class ChangeStatus(val phone: PhoneItem, val newStatus: String) : StockSecuredAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    phones: List<PhoneItem>,
    currentRole: UserRole,
    rmbRate: Double,
    isBengali: Boolean = false,
    adminPin: String = "1111",
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusFilterChange: (String) -> Unit,
    selectedBrand: String,
    onBrandFilterChange: (String) -> Unit,
    onAddNewPhoneClick: () -> Unit,
    onSellPhoneClick: (PhoneItem) -> Unit,
    onViewMemoClick: (PhoneItem) -> Unit,
    onEditCostsClick: (PhoneItem) -> Unit,
    onMarkInStockClick: (PhoneItem) -> Unit,
    onDeletePhoneClick: (PhoneItem) -> Unit,
    onStatusChange: ((PhoneItem, String) -> Unit)? = null,
    onRequireAdminAuth: ((title: String, actionTitle: String, actionDesc: String, icon: ImageVector, confirmText: String, isDanger: Boolean, onAuth: () -> Unit) -> Unit)? = null,
    isScreenActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentRole == UserRole.ADMIN
    var brandDropdownExpanded by remember { mutableStateOf(false) }
    var showCustomBrandDialog by remember { mutableStateOf(false) }
    val todayDate = remember { DateUtils.getTodayStorageDate() }
    var selectedDateFilter by remember { mutableStateOf("") } // Default to "All" dates
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showStockImeiScanner by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showStockImeiScanner = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan IMEI", Toast.LENGTH_SHORT).show()
        }
    }

    val handleScanImeiClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showStockImeiScanner = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-reset to "All" Dates whenever user navigates to or enters the Stock screen
    LaunchedEffect(isScreenActive) {
        if (isScreenActive) {
            selectedDateFilter = ""
        }
    }

    // Auto-reset to "All" Dates when app resumes from background or user leaves/returns
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                selectedDateFilter = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            selectedDateFilter = ""
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Admin PIN Security State (fallback when global auth callback not provided)
    var pendingSecuredAction by remember { mutableStateOf<StockSecuredAction?>(null) }

    fun handleAddPhone() {
        onAddNewPhoneClick()
    }

    fun handleEditCosts(phone: PhoneItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Edit Phone Details & Costs",
                "Modifying ${phone.brand} ${phone.model} (${phone.ramRom})",
                Icons.Default.Edit,
                "Authorize Edit",
                false
            ) { onEditCostsClick(phone) }
        } else {
            pendingSecuredAction = StockSecuredAction.EditCosts(phone)
        }
    }

    fun handleDeletePhone(phone: PhoneItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Delete Phone Entry",
                "Permanently delete ${phone.brand} ${phone.model} (IMEI: ${phone.imei.ifBlank { "N/A" }})",
                Icons.Default.Delete,
                "Delete Item",
                true
            ) { onDeletePhoneClick(phone) }
        } else {
            pendingSecuredAction = StockSecuredAction.DeletePhone(phone)
        }
    }

    fun handleMarkInStock(phone: PhoneItem) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin PIN Verification",
                "Transfer to Active Stock",
                "Enter Admin PIN to transfer ${phone.brand} ${phone.model} (IMEI: ${phone.imei.ifBlank { "N/A" }}) to active stock",
                Icons.Default.Inventory2,
                "Transfer to Stock",
                false
            ) { onMarkInStockClick(phone) }
        } else {
            pendingSecuredAction = StockSecuredAction.MarkInStock(phone)
        }
    }

    fun handleStatusChange(phone: PhoneItem, newStatus: String) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                "Change Stock Status",
                "Update ${phone.brand} ${phone.model} status to $newStatus",
                Icons.Default.Inventory2,
                "Authorize Status Change",
                false
            ) { onStatusChange?.invoke(phone, newStatus) }
        } else {
            pendingSecuredAction = StockSecuredAction.ChangeStatus(phone, newStatus)
        }
    }

    val yesterdayDate = remember { DateUtils.getYesterdayStorageDate() }

    // Visible inventory on Stock Screen: completely hides any Due Collection Payment / Razu vai transaction card
    // or static due collection alert box from the Sold items section/tab and stock layout
    // while strictly preserving all underlying ViewModel calculations, database bindings, and Admin checks
    val visibleStockPhones = remember(phones) {
        phones.filterNot { isDueCollectionTransaction(it) }
    }

    val todayStockCount = remember(visibleStockPhones, todayDate) {
        visibleStockPhones.count { it.entryDate.trim().startsWith(todayDate) }
    }
    val yesterdayStockCount = remember(visibleStockPhones, yesterdayDate) {
        visibleStockPhones.count { it.entryDate.trim().startsWith(yesterdayDate) }
    }

    // Filter phones by date, strictly sorted:
    // - In-Stock & In-Transit: sorted by entryDate descending (newest entry first)
    // - Sold: sorted by saleDate descending (newest sale first)
    val dateFilteredPhones = remember(visibleStockPhones, selectedDateFilter, selectedStatus) {
        val filtered = if (selectedDateFilter.isBlank() || selectedDateFilter == "ALL") {
            visibleStockPhones
        } else {
            visibleStockPhones.filter { phone ->
                val targetDate = if (selectedStatus == PhoneStatus.SOLD.code || selectedStatus == "SOLD") {
                    phone.saleDate.ifBlank { phone.entryDate }
                } else {
                    phone.entryDate
                }
                targetDate.trim().startsWith(selectedDateFilter)
            }
        }

        if (selectedStatus == PhoneStatus.SOLD.code || selectedStatus == "SOLD") {
            filtered.sortedBySaleDateDescending()
        } else {
            filtered.sortedByEntryDateDescending()
        }
    }

    var dynamicBrands by remember(visibleStockPhones) {
        val customInPhones = visibleStockPhones.map { it.brand }.filter { it.isNotBlank() && it !in BRAND_OPTIONS }
        val baseList = BRAND_OPTIONS.filter { !it.startsWith("Custom") }
        val combined = (baseList + customInPhones).distinct()
        mutableStateOf(combined + listOf("Custom..."))
    }

    val isDark = isAppInDarkTheme()

    // Exclude "All": ensure a valid status choice (Stock, Transit, or Sold) is active
    // All users (Admin and Non-Admin) can view Stock, Transit, and Sold items
    LaunchedEffect(selectedStatus) {
        if (selectedStatus == "ALL" || selectedStatus.isBlank()) {
            onStatusFilterChange(PhoneStatus.IN_STOCK.code)
        }
    }

    val screenBgColor by animateColorAsState(
        targetValue = if (isDark) CardBg else when (selectedStatus) {
            PhoneStatus.IN_STOCK.code, "STOCK" -> Color(0xFFF0FDF4) // Soft light emerald tint
            PhoneStatus.IN_TRANSIT.code, "TRANSIT" -> Color(0xFFFFF7ED) // Soft warm light orange tint
            PhoneStatus.SOLD.code, "SOLD" -> Color(0xFFEFF6FF) // Light cool blue tint
            else -> Color(0xFFF8FAFC) // Standard soft off-white background
        },
        animationSpec = tween(durationMillis = 350),
        label = "stock_screen_bg"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
            .padding(horizontal = 14.dp)
            .testTag("stock_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Top Search Bar with Integrated QR / Barcode Scanner on the right
            AppSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search brand, model, IMEI, or date...",
                testTag = "stock_search_input",
                onScanImeiClick = handleScanImeiClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Status Segment Control Toggle Bar: "In Stock" | "Transit" | "Sold"
            Surface(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_status_filter_dropdown")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: In Stock
                    val isInStockActive = selectedStatus == PhoneStatus.IN_STOCK.code || selectedStatus == "STOCK" || selectedStatus.isBlank() || selectedStatus == "ALL"
                    val inStockBgColor by animateColorAsState(
                        targetValue = if (isInStockActive) Color(0xFF059669) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        animationSpec = tween(200),
                        label = "stock_tab_bg"
                    )
                    val inStockTextColor by animateColorAsState(
                        targetValue = if (isInStockActive) Color.White else (if (isDark) Color(0xFFCBD5E1) else Slate700),
                        animationSpec = tween(200),
                        label = "stock_tab_text"
                    )

                    Surface(
                        onClick = { onStatusFilterChange(PhoneStatus.IN_STOCK.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = inStockBgColor,
                        shadowElevation = if (isInStockActive) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("status_option_stock")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = inStockTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Stock",
                                fontWeight = if (isInStockActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = inStockTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tab 2: Transit (Accessible to all users: Admin and Non-Admin)
                    val isTransitActive = selectedStatus == PhoneStatus.IN_TRANSIT.code || selectedStatus == "TRANSIT"
                    val transitBgColor by animateColorAsState(
                        targetValue = if (isTransitActive) Color(0xFFD97706) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        animationSpec = tween(200),
                        label = "transit_tab_bg"
                    )
                    val transitTextColor by animateColorAsState(
                        targetValue = if (isTransitActive) {
                            Color.White
                        } else {
                            if (isDark) Color(0xFFCBD5E1) else Slate700
                        },
                        animationSpec = tween(200),
                        label = "transit_tab_text"
                    )

                    Surface(
                        onClick = {
                            onStatusFilterChange(PhoneStatus.IN_TRANSIT.code)
                        },
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        color = transitBgColor,
                        shadowElevation = if (isTransitActive) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("status_option_transit")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = transitTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Transit",
                                fontWeight = if (isTransitActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = transitTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tab 3: Sold
                    val isSoldActive = selectedStatus == PhoneStatus.SOLD.code || selectedStatus == "SOLD"
                    val soldBgColor by animateColorAsState(
                        targetValue = if (isSoldActive) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        animationSpec = tween(200),
                        label = "sold_tab_bg"
                    )
                    val soldTextColor by animateColorAsState(
                        targetValue = if (isSoldActive) Color.White else (if (isDark) Color(0xFFCBD5E1) else Slate700),
                        animationSpec = tween(200),
                        label = "sold_tab_text"
                    )

                    Surface(
                        onClick = { onStatusFilterChange(PhoneStatus.SOLD.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = soldBgColor,
                        shadowElevation = if (isSoldActive) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("status_option_sold")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = soldTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Sold",
                                fontWeight = if (isSoldActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = soldTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control & Filter Bar: Brands (Left) | Select Date (Middle) | Sets Badge (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_control_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Brands Dropdown Button (Left of Date Picker) - strictly "Brands", no leading icon
                ExposedDropdownMenuBox(
                    expanded = brandDropdownExpanded,
                    onExpandedChange = { brandDropdownExpanded = !brandDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("stock_brand_dropdown_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedBrand == "ALL" || selectedBrand.isBlank()) "Brands" else selectedBrand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = if (brandDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Brand",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = brandDropdownExpanded,
                        onDismissRequest = { brandDropdownExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF2D3748) else Color.White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Brands",
                                    color = if (isDark) Color.White else Slate900,
                                    fontWeight = if (selectedBrand == "ALL") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (isDark) Color.White else Slate900
                            ),
                            onClick = {
                                onBrandFilterChange("ALL")
                                brandDropdownExpanded = false
                            }
                        )
                        dynamicBrands.forEach { brand ->
                            val isCustom = brand.startsWith("Custom") || brand == "Other"
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = brand,
                                        color = if (isCustom) (if (isDark) Color(0xFF60A5FA) else PrimaryNavy) else (if (isDark) Color.White else Slate900),
                                        fontWeight = if (isCustom || brand == selectedBrand) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isDark) Color.White else Slate900
                                ),
                                onClick = {
                                    if (isCustom) {
                                        showCustomBrandDialog = true
                                    } else {
                                        onBrandFilterChange(brand)
                                    }
                                    brandDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Select Date Button (Left of Sets Badge) with Auto-Reset to All Dates
                val isDateFilterActive = selectedDateFilter.isNotBlank() && selectedDateFilter != "ALL"
                val displayDateText = if (isDateFilterActive) {
                    if (selectedDateFilter == todayDate) "Today" else DateUtils.formatDisplayDate(selectedDateFilter)
                } else {
                    "Select Date"
                }

                Surface(
                    onClick = { showDatePickerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDateFilterActive) PrimaryNavy.copy(alpha = 0.5f) else (if (isDark) Color(0xFF334155) else CardBorder)
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(44.dp)
                        .testTag("stock_date_picker_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = displayDateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        if (isDateFilterActive) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Date Filter",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { selectedDateFilter = "" }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 3. Rename "Inventory Devices" to "Sets" with Unit Count into single styled container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0C2A40) else Color(0xFFE0F2FE),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFFBAE6FD)
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(0.95f)
                        .height(44.dp)
                        .testTag("stock_sets_badge_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sets (${dateFilteredPhones.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (dateFilteredPhones.isEmpty()) {
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
                        Text(
                            text = if (selectedDateFilter.isNotBlank() && selectedDateFilter != "ALL") {
                                "No phones entered into stock on ${if (selectedDateFilter == todayDate) "Today" else DateUtils.formatDisplayDate(selectedDateFilter)}"
                            } else {
                                "No phones found matching your filter!"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            fontSize = 14.sp
                        )
                        val isCustomDateFilterActive = selectedDateFilter.isNotBlank() && selectedDateFilter != "ALL"
                        if (isCustomDateFilterActive) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AppOutlinedActionButton(
                                text = "Show All Dates",
                                onClick = { selectedDateFilter = "" }
                            )
                        }
                    }
                }
            }
        } else {
            items(dateFilteredPhones, key = { it.id }) { phone ->
                PhoneCard(
                    phone = phone,
                    currentRole = currentRole,
                    rmbRate = rmbRate,
                    isBengali = false,
                    onSellClick = { onSellPhoneClick(phone) },
                    onViewMemoClick = { onViewMemoClick(phone) },
                    onEditCostsClick = {
                        handleEditCosts(phone)
                    },
                    onMarkInStockClick = {
                        handleMarkInStock(phone)
                    },
                    onDeleteClick = {
                        handleDeletePhone(phone)
                    },
                    onStatusChange = { newStatus ->
                        handleStatusChange(phone, newStatus)
                    },
                    onQuickFilter = { filterType, query ->
                        when (filterType) {
                            "MODEL" -> onSearchQueryChange(query)
                            "STOCK_DATE" -> {
                                selectedDateFilter = query
                            }
                            "CUSTOMER" -> onSearchQueryChange(query)
                            "SOLD_DATE" -> onSearchQueryChange(query)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Calendar Date Picker Dialog
    if (showDatePickerDialog) {
        val initialMillis = remember(selectedDateFilter) {
            val target = if (selectedDateFilter.isNotBlank() && selectedDateFilter != "ALL") selectedDateFilter else todayDate
            DateUtils.parseDateToMillis(target)
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat(DateUtils.STORAGE_DATE_PATTERN, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            selectedDateFilter = sdf.format(Date(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Filter By Date", fontWeight = FontWeight.Bold, color = PrimaryNavy)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        selectedDateFilter = ""
                        showDatePickerDialog = false
                    }) {
                        Text("All Dates", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                    TextButton(onClick = {
                        selectedDateFilter = todayDate
                        showDatePickerDialog = false
                    }) {
                        Text("Today", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
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

    if (showCustomBrandDialog) {
        CustomInputPopupDialog(
            title = "Enter Custom Brand Name",
            label = "Brand Name",
            placeholder = "e.g. Nothing, Sony, ASUS, Tecno",
            initialValue = if (selectedBrand == "ALL" || selectedBrand in BRAND_OPTIONS) "" else selectedBrand,
            onDismiss = { showCustomBrandDialog = false },
            onConfirm = { customValue ->
                if (!dynamicBrands.contains(customValue)) {
                    val customIdx = dynamicBrands.indexOfFirst { it.startsWith("Custom") || it == "Other" }
                    dynamicBrands = if (customIdx >= 0) {
                        dynamicBrands.toMutableList().apply { add(customIdx, customValue) }
                    } else {
                        dynamicBrands + customValue
                    }
                }
                onBrandFilterChange(customValue)
            }
        )
    }

    // Admin Security Verification Dialog for Protected Stock Actions (fallback)
    if (onRequireAdminAuth == null) {
        pendingSecuredAction?.let { action ->
        val (actionTitle, actionDesc, isDanger, icon) = when (action) {
            is StockSecuredAction.AddPhone -> StockActionSecurityInfo(
                "Add New Phone to Stock",
                "Authorize adding a new phone entry to inventory",
                false,
                Icons.Default.Add
            )
            is StockSecuredAction.EditCosts -> StockActionSecurityInfo(
                "Edit Phone Details & Costs",
                "Modifying ${action.phone.brand} ${action.phone.model} (${action.phone.ramRom})",
                false,
                Icons.Default.Edit
            )
            is StockSecuredAction.DeletePhone -> StockActionSecurityInfo(
                "Delete Phone Entry",
                "Permanently delete ${action.phone.brand} ${action.phone.model} (IMEI: ${action.phone.imei.ifBlank { "N/A" }})",
                true,
                Icons.Default.Delete
            )
            is StockSecuredAction.MarkInStock -> StockActionSecurityInfo(
                "Transfer to Active Stock",
                "Enter Admin PIN to transfer ${action.phone.brand} ${action.phone.model} (IMEI: ${action.phone.imei.ifBlank { "N/A" }}) to active stock",
                false,
                Icons.Default.Inventory2
            )
            is StockSecuredAction.ChangeStatus -> StockActionSecurityInfo(
                "Change Stock Status",
                "Update ${action.phone.brand} ${action.phone.model} status to ${action.newStatus}",
                false,
                Icons.Default.Inventory2
            )
        }

        AdminSecurityConfirmDialog(
            title = "Admin Authentication",
            actionTitle = actionTitle,
            actionDescription = actionDesc,
            actionIcon = icon,
            confirmButtonText = if (isDanger) "Delete Item" else "Authorize Action",
            isDangerAction = isDanger,
            onDismiss = { pendingSecuredAction = null },
            onVerifyPin = { inputPin -> inputPin == adminPin },
            onAuthorized = {
                val act = pendingSecuredAction
                pendingSecuredAction = null
                when (act) {
                    is StockSecuredAction.AddPhone -> onAddNewPhoneClick()
                    is StockSecuredAction.EditCosts -> onEditCostsClick(act.phone)
                    is StockSecuredAction.DeletePhone -> onDeletePhoneClick(act.phone)
                    is StockSecuredAction.MarkInStock -> onMarkInStockClick(act.phone)
                    is StockSecuredAction.ChangeStatus -> onStatusChange?.invoke(act.phone, act.newStatus)
                    null -> {}
                }
            }
        )
    }
    }

    // Camera Barcode & IMEI Scanner Dialog
    if (showStockImeiScanner) {
        ImeiCameraScannerDialog(
            onDismiss = { showStockImeiScanner = false },
            onImeiScanned = { scannedImei, _ ->
                onSearchQueryChange(scannedImei.trim())
                showStockImeiScanner = false
            },
            initialHelperTitle = "Scan IMEI"
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StockDateFilterChip(
    label: String,
    count: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) PrimaryNavy else (if (isDark) Color(0xFF1E293B) else Color.White),
        animationSpec = tween(durationMillis = 200),
        label = "date_chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Slate800),
        animationSpec = tween(durationMillis = 200),
        label = "date_chip_content"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) PrimaryNavy else CardBorder,
        animationSpec = tween(durationMillis = 200),
        label = "date_chip_border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = if (selected) Color.White.copy(alpha = 0.25f) else (if (isDark) Color(0xFF2D3748) else Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$count",
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StockFilterPill(
    label: String,
    selected: Boolean,
    selectedColor: Color = Color(0xFF334155),
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val effectiveSelectedColor = if (isDark && selectedColor == Color(0xFF334155)) Color(0xFF475569) else selectedColor
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) effectiveSelectedColor else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
        animationSpec = tween(durationMillis = 250),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
        animationSpec = tween(durationMillis = 250),
        label = "pill_content"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) effectiveSelectedColor else CardBorder,
        animationSpec = tween(durationMillis = 250),
        label = "pill_border"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "pill_elev"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "pill_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = elevation,
        modifier = modifier
            .scale(scale)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class StockActionSecurityInfo(
    val title: String,
    val desc: String,
    val isDanger: Boolean,
    val icon: ImageVector
)

/**
 * Detects whether an item represents a "Due Collection Payment from Razu vai" transaction
 * or any static due collection transaction alert box, ensuring it is hidden from the Stock screen
 * Sold section and inventory list layout while strictly preserving all underlying ViewModel calculations,
 * database bindings, and Admin authorization checks.
 */
private fun isDueCollectionTransaction(phone: PhoneItem): Boolean {
    val searchBlob = "${phone.brand} ${phone.model} ${phone.customerName} ${phone.notes} ${phone.customerPhone} ${phone.paymentMethod} ${phone.soldByStaff} ${phone.variant} ${phone.imei}".lowercase()
    return searchBlob.contains("razu") ||
           searchBlob.contains("due collection") ||
           searchBlob.contains("due payment") ||
           phone.brand.trim().equals("Due Collection", ignoreCase = true) ||
           phone.model.contains("Due Collection", ignoreCase = true) ||
           phone.notes.contains("Due Collection", ignoreCase = true)
}


