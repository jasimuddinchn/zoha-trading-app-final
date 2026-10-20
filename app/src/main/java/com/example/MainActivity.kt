package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppStrings
import com.example.data.model.ExpenseItem
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.UserRole
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddPhoneDialog
import com.example.ui.components.AdminSecurityConfirmDialog
import com.example.ui.components.AppNavigationDrawer
import com.example.ui.components.CashMemoDialog
import com.example.ui.components.CurrencyConverterDialog
import com.example.ui.components.EditPhoneDialog
import com.example.ui.components.FirstLaunchAdminSetupDialog
import com.example.ui.components.IncomeDialog
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.OwnerCostUpdateDialog
import com.example.ui.components.PinDialog
import com.example.ui.components.RoleHeaderBar
import com.example.ui.components.SellPhoneDialog
import com.example.ui.components.WholesalePriceListDialog
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.NotificationDetailDialog
import com.example.data.model.AppNotificationItem
import com.example.util.NotificationHelper
import com.example.util.AppBadgeHelper
import com.example.util.InAppUpdateManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DueManagementScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.LandingLoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.util.MonthFilterHelper
import com.example.viewmodel.ExpenseViewModel
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.InvoiceEmailHelper
import com.example.util.SessionManager
import com.example.viewmodel.PhoneHisabViewModel

sealed class NavTab(
    val index: Int,
    val icon: ImageVector,
    val tag: String,
    val accentColor: Color,
    val shortLabel: String,
    val titleAccentColor: Color = accentColor
) {
    object Dashboard : NavTab(0, Icons.Default.Dashboard, "tab_dashboard", Color(0xFF2563EB), "Dashboard", Color(0xFF38BDF8))
    object Stock : NavTab(1, Icons.Default.PhoneAndroid, "tab_stock", Color(0xFF059669), "Stock", Color(0xFF34D399))
    object Sales : NavTab(2, Icons.Default.PointOfSale, "tab_sales", Color(0xFF7C3AED), "Sales", Color(0xFFA78BFA))
    object Due : NavTab(3, Icons.Default.ReceiptLong, "tab_due", Color(0xFFEA580C), "Due", Color(0xFFFB923C))
    object Expense : NavTab(4, Icons.Default.Payments, "tab_expense", Color(0xFFE11D48), "Expense", Color(0xFFFB7185))
    object Settings : NavTab(5, Icons.Default.Settings, "tab_settings", Color(0xFF4F46E5), "Settings", Color(0xFF818CF8))

    companion object {
        val Reports: NavTab get() = Expense
    }

    fun getTitle(isBn: Boolean = false): String {
        return when (this) {
            Dashboard -> "Dashboard"
            Stock -> "Stock"
            Sales -> "Sales"
            Due -> "Due"
            Expense -> "Expense"
            Settings -> "Settings"
        }
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PhoneHisabViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isDarkTheme = when (settings.themeMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settings.dynamicColor
            ) {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SessionManager.clearSession(this)
    }
}

@Composable
fun MainAppRoot(viewModel: PhoneHisabViewModel = viewModel()) {
    val context = LocalContext.current
    val expenseViewModel: ExpenseViewModel = viewModel()

    val phones by viewModel.allPhones.collectAsStateWithLifecycle()
    val filteredPhones by viewModel.filteredPhones.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val vmExpenses by expenseViewModel.allExpenses.collectAsStateWithLifecycle()
    val resolvedExpenses = (if (vmExpenses.isNotEmpty()) vmExpenses else expenses).distinctBy { it.id }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val activeUserId by viewModel.activeUserId.collectAsStateWithLifecycle()
    val activeStaffName by viewModel.activeStaffName.collectAsStateWithLifecycle()
    val isBengali by viewModel.isBengali.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val isCloudConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val isPermissionDenied by viewModel.isPermissionDenied.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrandFilter.collectAsStateWithLifecycle()
    val customerDueProfiles by viewModel.customerDueProfiles.collectAsStateWithLifecycle()
    val allOtherIncomes by viewModel.allOtherIncomes.collectAsStateWithLifecycle()
    val allDuePayments by viewModel.allDuePayments.collectAsStateWithLifecycle()

    var notificationRefreshKey by remember { mutableIntStateOf(0) }
    val notifications = remember(phones, resolvedExpenses, allDuePayments, notificationRefreshKey) {
        NotificationHelper.buildNotifications(context, phones, resolvedExpenses, allDuePayments)
    }
    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    // Notification Permission for Android 13+ (API 33+) to enable system badge & status bar notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AppBadgeHelper.updateBadgeCount(context, unreadNotificationsCount)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        AppBadgeHelper.initNotificationChannel(context)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val result = InAppUpdateManager.checkRemoteVersion(context)
            if (result.isSuccess && result.getOrNull()?.isDriveVersionHigher == true) {
                notificationRefreshKey++
            }
        }
    }

    LaunchedEffect(unreadNotificationsCount) {
        AppBadgeHelper.updateBadgeCount(context, unreadNotificationsCount)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var salesInitialDateFilter by remember { mutableStateOf(MonthFilterHelper.getCurrentYearMonth()) }

    // Ensure staff never remains on Settings tab if switching from Admin
    LaunchedEffect(currentRole) {
        if (currentRole != UserRole.ADMIN && selectedTabIndex == 4) {
            selectedTabIndex = 0
        }
    }

    // Dialog controllers
    var showAddPhoneDialog by remember { mutableStateOf(false) }
    var showCurrencyCalcDialog by remember { mutableStateOf(false) }
    var userToUnlock by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddOtherIncomeDialog by remember { mutableStateOf(false) }
    var otherIncomeToEdit by remember { mutableStateOf<OtherIncomeItem?>(null) }
    var phoneToSell by remember { mutableStateOf<PhoneItem?>(null) }
    var phoneToEditCosts by remember { mutableStateOf<PhoneItem?>(null) }
    var phoneForCashMemo by remember { mutableStateOf<PhoneItem?>(null) }
    var phoneForInvoice by remember { mutableStateOf<PhoneItem?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showWholesaleDialogFromDrawer by remember { mutableStateOf(false) }
    var showNotificationCenterDialog by remember { mutableStateOf(false) }
    var selectedNotificationForDetail by remember { mutableStateOf<AppNotificationItem?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Global Admin Security PIN Verification Flow
    var pendingGlobalAction by remember {
        mutableStateOf<Pair<Triple<String, String, String>, Pair<Pair<ImageVector, String>, Pair<Boolean, () -> Unit>>>?>(null)
    }

    fun requestAdminAuth(
        title: String = "Admin Authentication",
        actionTitle: String,
        actionDesc: String,
        icon: ImageVector,
        confirmText: String = "Authorize Action",
        isDanger: Boolean = false,
        onAuth: () -> Unit
    ) {
        pendingGlobalAction = Pair(
            Triple(title, actionTitle, actionDesc),
            Pair(Pair(icon, confirmText), Pair(isDanger, onAuth))
        )
    }

    // Unified Back-Button & Back-Gesture Handler (Pop Back Stack)
    BackHandler(enabled = true) {
        when {
            pendingGlobalAction != null -> {
                pendingGlobalAction = null
            }
            selectedNotificationForDetail != null -> {
                selectedNotificationForDetail = null
            }
            showNotificationCenterDialog -> {
                showNotificationCenterDialog = false
            }
            showExitConfirmDialog -> {
                showExitConfirmDialog = false
            }
            userToUnlock != null -> {
                userToUnlock = null
            }
            showAddPhoneDialog -> {
                showAddPhoneDialog = false
            }
            showAddExpenseDialog -> {
                showAddExpenseDialog = false
            }
            showAddOtherIncomeDialog -> {
                showAddOtherIncomeDialog = false
            }
            otherIncomeToEdit != null -> {
                otherIncomeToEdit = null
            }
            phoneToSell != null -> {
                phoneToSell = null
            }
            phoneToEditCosts != null -> {
                phoneToEditCosts = null
            }
            phoneForCashMemo != null -> {
                phoneForCashMemo = null
            }
            phoneForInvoice != null -> {
                phoneForInvoice = null
            }
            showAboutDialog -> {
                showAboutDialog = false
            }
            showWholesaleDialogFromDrawer -> {
                showWholesaleDialogFromDrawer = false
            }
            showCurrencyCalcDialog -> {
                showCurrencyCalcDialog = false
            }
            !isUserLoggedIn -> {
                // On Landing / Login Screen
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    viewModel.logoutToLanding()
                    SessionManager.clearSession(context)
                    (context as? Activity)?.finishAffinity()
                } else {
                    lastBackPressTime = now
                    showExitConfirmDialog = true
                }
            }
            selectedTabIndex != 0 -> {
                // Return to Dashboard screen first from any other tab (Stock, Sales, Reports, Settings)
                viewModel.searchQuery.value = ""
                selectedTabIndex = 0
            }
            else -> {
                // Already on Dashboard screen root
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    viewModel.logoutToLanding()
                    SessionManager.clearSession(context)
                    (context as? Activity)?.finishAffinity()
                } else {
                    lastBackPressTime = now
                    showExitConfirmDialog = true
                }
            }
        }
    }

    // Biometric Authentication Handler (Fingerprint / Face Unlock)
    // Strictly maps to targetProfile to prevent cross-account auto-login
    var isBiometricPromptInFlight by remember { mutableStateOf(false) }

    fun triggerBiometricAuth(targetProfile: String) {
        val fragActivity = context as? FragmentActivity ?: return
        if (isBiometricPromptInFlight || BiometricAuthManager.isPromptShowing()) {
            return
        }
        isBiometricPromptInFlight = true
        val profileName = when (targetProfile) {
            "ADMIN" -> settings.getAdminName(false)
            "STAFF_1" -> settings.getStaff1Name(false)
            else -> settings.getStaff2Name(false)
        }

        BiometricAuthManager.showBiometricPrompt(
            activity = fragActivity,
            title = "Fingerprint Verification",
            subtitle = null,
            description = null,
            negativeButtonText = "Use PIN Instead",
            onSuccess = {
                isBiometricPromptInFlight = false
                BiometricAuthManager.setLastProfile(context, targetProfile)
                viewModel.unlockUserWithBiometrics(targetProfile)
                Toast.makeText(context, "Welcome back, $profileName! 🔓", Toast.LENGTH_SHORT).show()
            },
            onFallbackToPin = {
                isBiometricPromptInFlight = false
                userToUnlock = targetProfile
            },
            onError = { errMsg ->
                isBiometricPromptInFlight = false
                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // If no user is logged in, show the Clean Landing/Login Screen
    if (!isUserLoggedIn) {
        val isBioAvail = BiometricAuthManager.isBiometricAvailable(context)
        val isBioEnabled = BiometricAuthManager.isBiometricEnabled(context)
        LandingLoginScreen(
            settings = settings,
            isCloudConnected = isCloudConnected,
            isInitialLoading = isInitialLoading,
            isPermissionDenied = isPermissionDenied,
            isDarkTheme = isAppInDarkTheme(),
            isBiometricAvailable = isBioAvail,
            isBiometricEnabled = isBioEnabled,
            onTriggerBiometric = { targetProfile -> triggerBiometricAuth(targetProfile) },
            onToggleTheme = { viewModel.toggleThemeMode() },
            onSelectProfile = { targetUser ->
                userToUnlock = targetUser
            }
        )
    } else {
        val visibleTabs = remember(currentRole) {
            if (currentRole == UserRole.ADMIN) {
                listOf(NavTab.Dashboard, NavTab.Stock, NavTab.Sales, NavTab.Due, NavTab.Expense, NavTab.Settings)
            } else {
                listOf(NavTab.Dashboard, NavTab.Stock, NavTab.Sales, NavTab.Due, NavTab.Expense)
            }
        }

        val pagerState = rememberPagerState(
            initialPage = selectedTabIndex.coerceIn(0, (visibleTabs.size - 1).coerceAtLeast(0)),
            pageCount = { visibleTabs.size }
        )
        val coroutineScope = rememberCoroutineScope()

        // Sync swipe gesture from HorizontalPager to bottom navigation active tab
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage in visibleTabs.indices) {
                val currentTab = visibleTabs[pagerState.currentPage]
                if (selectedTabIndex != currentTab.index) {
                    selectedTabIndex = currentTab.index
                }
            }
        }

        // Sync programmatic selectedTabIndex changes to HorizontalPager
        LaunchedEffect(selectedTabIndex, visibleTabs) {
            val targetPage = visibleTabs.indexOfFirst { it.index == selectedTabIndex }
            if (targetPage in 0 until visibleTabs.size && targetPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }

        // Compute dynamic active page title and accent theme color based on active tab / pager page
        val currentTab = visibleTabs.getOrNull(pagerState.currentPage)
            ?: visibleTabs.find { it.index == selectedTabIndex }
            ?: NavTab.Dashboard

        val activePageTitle = when (currentTab) {
            NavTab.Dashboard -> "Dashboard"
            NavTab.Stock -> "Stock"
            NavTab.Sales -> "Sales"
            NavTab.Due -> "Due Ledger"
            NavTab.Expense -> "Expense"
            NavTab.Settings -> "Profile"
        }

        val activePageColor = currentTab.titleAccentColor

        val animatedPageColor by animateColorAsState(
            targetValue = activePageColor,
            animationSpec = tween(durationMillis = 250),
            label = "top_bar_title_color"
        )

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        BackHandler(enabled = drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppNavigationDrawer(
                    currentRole = currentRole,
                    activeUserId = activeUserId,
                    adminName = settings.adminName,
                    staff1Name = settings.staff1Name,
                    staff2Name = settings.staff2Name,
                    onNavigateToDashboard = {
                        coroutineScope.launch { drawerState.close() }
                        val targetPage = visibleTabs.indexOf(NavTab.Dashboard)
                        if (targetPage >= 0) {
                            selectedTabIndex = NavTab.Dashboard.index
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        }
                    },
                    onNavigateToDueLedger = {
                        coroutineScope.launch { drawerState.close() }
                        val targetPage = visibleTabs.indexOf(NavTab.Due)
                        if (targetPage >= 0) {
                            selectedTabIndex = NavTab.Due.index
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        }
                    },
                    onOpenWholesaleGenerator = {
                        coroutineScope.launch { drawerState.close() }
                        showWholesaleDialogFromDrawer = true
                    },
                    onOpenAbout = {
                        coroutineScope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    onOpenSettings = {
                        coroutineScope.launch { drawerState.close() }
                        if (currentRole == UserRole.ADMIN && activeUserId == "ADMIN") {
                            val targetPage = visibleTabs.indexOf(NavTab.Settings)
                            if (targetPage >= 0) {
                                selectedTabIndex = NavTab.Settings.index
                                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        } else {
                            requestAdminAuth(
                                "Settings Access",
                                "Admin Verification Required",
                                "Admin credentials required to open application settings",
                                Icons.Default.Settings,
                                "Verify & Open",
                                false
                            ) {
                                val targetPage = visibleTabs.indexOf(NavTab.Settings)
                                if (targetPage >= 0) {
                                    selectedTabIndex = NavTab.Settings.index
                                    coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                                }
                            }
                        }
                    },
                    onLogout = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logoutToLanding()
                        Toast.makeText(context, "Logged out to Landing Screen", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    RoleHeaderBar(
                        currentRole = currentRole,
                        activeUserId = activeUserId,
                        adminName = settings.adminName,
                        staff1Name = settings.staff1Name,
                        staff2Name = settings.staff2Name,
                        isBengali = isBengali,
                        isInitialLoading = isInitialLoading,
                        isCloudConnected = isCloudConnected,
                        isDarkTheme = isAppInDarkTheme(),
                        pageTitle = activePageTitle,
                        pageTitleColor = animatedPageColor,
                        unreadNotificationCount = unreadNotificationsCount,
                        onOpenNotifications = { showNotificationCenterDialog = true },
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onToggleTheme = { viewModel.toggleThemeMode() },
                        onSelectUser = { targetUser ->
                            val isActive = when (targetUser) {
                                "ADMIN" -> currentRole == UserRole.ADMIN && activeUserId == "ADMIN"
                                "STAFF_1" -> currentRole == UserRole.TEAM_MEMBER && activeUserId == "STAFF_1"
                                "STAFF_2" -> currentRole == UserRole.TEAM_MEMBER && activeUserId == "STAFF_2"
                                else -> false
                            }
                            val targetName = when (targetUser) {
                                "ADMIN" -> settings.adminName
                                "STAFF_1" -> settings.staff1Name
                                else -> settings.staff2Name
                            }
                            if (isActive) {
                                Toast.makeText(context, "$targetName profile is currently active.", Toast.LENGTH_SHORT).show()
                            } else {
                                userToUnlock = targetUser
                            }
                        },
                        onLockAdmin = {
                            viewModel.lockAdminSession()
                            Toast.makeText(context, "Admin session locked 🔒", Toast.LENGTH_SHORT).show()
                        },
                        onLogout = {
                            viewModel.logoutToLanding()
                            Toast.makeText(context, "Logged out to Landing Screen", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
            bottomBar = {
                Surface(
                    color = CardBg,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleTabs.forEach { tab ->
                            val isSelected = selectedTabIndex == tab.index
                            val tabTitle = tab.getTitle(isBengali)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) tab.accentColor else if (isAppInDarkTheme()) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                    )
                                    .clickable {
                                        val targetPage = visibleTabs.indexOf(tab)
                                        if (targetPage >= 0) {
                                            selectedTabIndex = tab.index
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(targetPage)
                                            }
                                        }
                                    }
                                    .testTag(tab.tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tabTitle,
                                        tint = if (isSelected) Color.White else tab.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.shortLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Slate700,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("main_horizontal_pager"),
                key = { page -> visibleTabs.getOrNull(page)?.tag ?: page }
            ) { page ->
                val currentTab = visibleTabs.getOrNull(page)
                when (currentTab) {
                    NavTab.Dashboard -> DashboardScreen(
                        phones = phones,
                        expenses = resolvedExpenses,
                        allOtherIncomes = allOtherIncomes,
                        currentRole = currentRole,
                        settings = settings,
                        isBengali = isBengali,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onAddNewPhoneClick = {
                            showAddPhoneDialog = true
                        },
                        onAddOtherIncomeClick = {
                            showAddOtherIncomeDialog = true
                        },
                        onSellPhoneClick = { phone -> phoneToSell = phone },
                        onViewMemoClick = { phone -> phoneForCashMemo = phone },
                        onEditCostsClick = { phone ->
                            requestAdminAuth(
                                actionTitle = "Edit Phone Details & Costs",
                                actionDesc = "Modifying ${phone.brand} ${phone.model} (${phone.ramRom})",
                                icon = Icons.Default.Edit,
                                confirmText = "Authorize Edit"
                            ) {
                                phoneToEditCosts = phone
                            }
                        },
                        onMarkInStockClick = { phone ->
                            requestAdminAuth(
                                actionTitle = "Receive Into Stock",
                                actionDesc = "Confirm stock arrival for ${phone.brand} ${phone.model}",
                                icon = Icons.Default.Inventory2,
                                confirmText = "Receive Item"
                            ) {
                                viewModel.changePhoneStatus(phone, com.example.data.model.PhoneStatus.IN_STOCK.code)
                                NotificationHelper.logTransitReceived(context, phone)
                                notificationRefreshKey++
                                Toast.makeText(context, "Phone received into stock!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeletePhoneClick = { phone ->
                            viewModel.deletePhone(phone)
                            Toast.makeText(context, "Phone removed.", Toast.LENGTH_SHORT).show()
                        },
                        onAddExpenseClick = {
                            showAddExpenseDialog = true
                        },
                        onNavigateToStock = {
                            selectedTabIndex = 1
                            coroutineScope.launch {
                                val target = visibleTabs.indexOfFirst { it.index == 1 }
                                if (target >= 0) pagerState.animateScrollToPage(target)
                            }
                        },
                        onNavigateToSales = { filter ->
                            salesInitialDateFilter = filter ?: MonthFilterHelper.getCurrentYearMonth()
                            selectedTabIndex = 2
                            coroutineScope.launch {
                                val target = visibleTabs.indexOfFirst { it.index == 2 }
                                if (target >= 0) pagerState.animateScrollToPage(target)
                            }
                        },
                        onNavigateToReports = {
                            selectedTabIndex = 4
                            coroutineScope.launch {
                                val target = visibleTabs.indexOfFirst { it.index == 4 }
                                if (target >= 0) pagerState.animateScrollToPage(target)
                            }
                        },
                        onNavigateToDue = {
                            selectedTabIndex = 3
                            coroutineScope.launch {
                                val target = visibleTabs.indexOfFirst { it.index == 3 }
                                if (target >= 0) pagerState.animateScrollToPage(target)
                            }
                        },
                        onStatusChange = { phone, newStatus ->
                            requestAdminAuth(
                                actionTitle = "Update Stock Status",
                                actionDesc = "Change ${phone.brand} ${phone.model} status to $newStatus",
                                icon = Icons.Default.Inventory2,
                                confirmText = "Authorize Status"
                            ) {
                                viewModel.changePhoneStatus(phone, newStatus)
                                val statusLabel = when (newStatus) {
                                    com.example.data.model.PhoneStatus.IN_STOCK.code -> "In Stock"
                                    com.example.data.model.PhoneStatus.IN_TRANSIT.code -> "In Transit"
                                    com.example.data.model.PhoneStatus.SOLD.code -> "Sold"
                                    com.example.data.model.PhoneStatus.RETURNED.code, "RETURN" -> "Returned"
                                    else -> newStatus
                                }
                                Toast.makeText(context, "Status updated to $statusLabel", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    NavTab.Stock -> StockScreen(
                        phones = filteredPhones,
                        currentRole = currentRole,
                        rmbRate = settings.rmbToBdtRate,
                        isBengali = isBengali,
                        adminPin = settings.ownerPin.ifBlank { "1111" },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        selectedStatus = selectedStatus,
                        onStatusFilterChange = { newStatus ->
                            viewModel.selectedStatusFilter.value = newStatus
                        },
                        selectedBrand = selectedBrand,
                        onBrandFilterChange = { viewModel.selectedBrandFilter.value = it },
                        onAddNewPhoneClick = { showAddPhoneDialog = true },
                        onSellPhoneClick = { phone -> phoneToSell = phone },
                        onViewMemoClick = { phone -> phoneForCashMemo = phone },
                        onEditCostsClick = { phone ->
                            phoneToEditCosts = phone
                        },
                        onMarkInStockClick = { phone ->
                            viewModel.changePhoneStatus(phone, com.example.data.model.PhoneStatus.IN_STOCK.code)
                            NotificationHelper.logTransitReceived(context, phone)
                            notificationRefreshKey++
                            Toast.makeText(context, "Phone received into stock!", Toast.LENGTH_SHORT).show()
                        },
                        onDeletePhoneClick = { phone ->
                            viewModel.deletePhone(phone)
                            Toast.makeText(context, "Phone removed.", Toast.LENGTH_SHORT).show()
                        },
                        onStatusChange = { phone, newStatus ->
                            if (phone.status == com.example.data.model.PhoneStatus.IN_TRANSIT.code &&
                                newStatus == com.example.data.model.PhoneStatus.IN_STOCK.code) {
                                NotificationHelper.logTransitReceived(context, phone)
                                notificationRefreshKey++
                            }
                            viewModel.changePhoneStatus(phone, newStatus)
                            val statusLabel = when (newStatus) {
                                com.example.data.model.PhoneStatus.IN_STOCK.code -> "In Stock"
                                com.example.data.model.PhoneStatus.IN_TRANSIT.code -> "In Transit"
                                com.example.data.model.PhoneStatus.SOLD.code -> "Sold"
                                com.example.data.model.PhoneStatus.RETURNED.code, "RETURN" -> "Returned"
                                else -> newStatus
                            }
                            Toast.makeText(context, "Status updated to $statusLabel", Toast.LENGTH_SHORT).show()
                        },
                        onRequireAdminAuth = { title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth ->
                            requestAdminAuth(title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth)
                        },
                        isScreenActive = (pagerState.currentPage == page)
                    )

                    NavTab.Sales -> SalesScreen(
                        phones = phones,
                        otherIncomes = allOtherIncomes,
                        currentRole = currentRole,
                        isBengali = isBengali,
                        initialDateFilter = salesInitialDateFilter,
                        onDateFilterChange = { salesInitialDateFilter = it },
                        onViewMemoClick = { phone -> phoneForCashMemo = phone },
                        onViewInvoiceClick = { phone -> phoneForInvoice = phone },
                        onEditSaleRecordClick = { phone ->
                            phoneToEditCosts = phone
                        },
                        onDeleteSaleRecordClick = { phone ->
                            viewModel.revertSaleToStock(phone)
                            Toast.makeText(context, "Sale record reverted back to stock!", Toast.LENGTH_LONG).show()
                        },
                        onAddOtherIncomeClick = { showAddOtherIncomeDialog = true },
                        onEditOtherIncomeClick = { income -> otherIncomeToEdit = income },
                        onDeleteOtherIncomeClick = { income ->
                            viewModel.deleteOtherIncome(income.id)
                            Toast.makeText(context, "Other income record removed.", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToDue = {
                            selectedTabIndex = 3
                            coroutineScope.launch {
                                val target = visibleTabs.indexOfFirst { it.index == 3 }
                                if (target >= 0) pagerState.animateScrollToPage(target)
                            }
                        },
                        onRequireAdminAuth = { title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth ->
                            requestAdminAuth(title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth)
                        }
                    )

                    NavTab.Due -> {
                        val totalOutstandingDue by viewModel.totalOutstandingDue.collectAsStateWithLifecycle()
                        val totalDueCollectionsAmount by viewModel.totalDueCollectionsAmount.collectAsStateWithLifecycle()

                        DueManagementScreen(
                            customerProfiles = customerDueProfiles,
                            totalOutstandingDue = totalOutstandingDue,
                            totalDueCollected = totalDueCollectionsAmount,
                            currentRole = currentRole,
                            staffOptions = listOf(settings.staff1Name, settings.staff2Name, settings.adminName).filter { it.isNotBlank() },
                            isBengali = isBengali,
                            phones = phones,
                            onCollectDue = { custKey, custName, custPhone, amount, paymentMethod, collectedBy, date, notes ->
                                viewModel.collectCustomerDue(
                                    customerKey = custKey,
                                    customerName = custName,
                                    customerPhone = custPhone,
                                    amount = amount,
                                    paymentMethod = paymentMethod,
                                    collectedBy = collectedBy,
                                    date = date,
                                    notes = notes
                                )
                                Toast.makeText(context, "Collected ৳ ${String.format(java.util.Locale.US, "%,.0f", amount)} due from $custName! Reconciled to Cash Drawer.", Toast.LENGTH_LONG).show()
                            },
                            onAddExistingDue = { name, phone, amount, date, notes ->
                                viewModel.addExistingDue(name, phone, amount, date, notes)
                            },
                            onDeleteCustomerDue = { recordId, custKey ->
                                viewModel.deleteCustomerDue(custKey, recordId)
                            },
                            onDeleteDuePayment = { paymentId, custKey ->
                                viewModel.deleteDuePayment(custKey, paymentId)
                            },
                            onUpdateCustomerDetails = { custKey, newName, newPhone ->
                                viewModel.updateCustomerDetails(custKey, newName, newPhone)
                                Toast.makeText(context, "Customer details updated: $newName", Toast.LENGTH_SHORT).show()
                            },
                            onUpdateCustomerDueRecord = { recordId, custKey, newDueAmount, newTotalSalePrice, newCashReceived, newDate, newNotes ->
                                viewModel.updateCustomerDueRecord(custKey, recordId, newDueAmount, newTotalSalePrice, newCashReceived, newDate, newNotes)
                                Toast.makeText(context, "Due entry updated successfully", Toast.LENGTH_SHORT).show()
                            },
                            onUpdateDuePayment = { paymentId, custKey, newAmount, newMethod, newStaff, newDate, newNotes ->
                                viewModel.updateDuePaymentRecord(custKey, paymentId, newAmount, newMethod, newStaff, newDate, newNotes)
                                Toast.makeText(context, "Payment entry updated successfully", Toast.LENGTH_SHORT).show()
                            },
                            onRequireAdminAuth = { title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth ->
                                requestAdminAuth(title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth)
                            }
                        )
                    }

                    NavTab.Expense -> ExpenseScreen(
                        expenses = resolvedExpenses,
                        currentRole = currentRole,
                        isBengali = isBengali,
                        adminPin = settings.ownerPin.ifBlank { "1111" },
                        onAddExpenseClick = { showAddExpenseDialog = true },
                        onDeleteExpenseClick = { exp ->
                            expenseViewModel.deleteExpense(exp)
                            viewModel.deleteExpense(exp)
                            Toast.makeText(context, "Expense removed.", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateExpenseClick = { exp ->
                            expenseViewModel.updateExpense(exp)
                            viewModel.updateExpense(exp)
                            Toast.makeText(context, "Expense updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onUnlockAdminClick = { userToUnlock = "ADMIN" },
                        onRequireAdminAuth = { title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth ->
                            requestAdminAuth(title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth)
                        }
                    )

                    NavTab.Settings -> SettingsScreen(
                        settings = settings,
                        currentRole = currentRole,
                        isBengali = isBengali,
                        phones = phones,
                        expenses = resolvedExpenses,
                        isCloudConnected = isCloudConnected,
                        isPermissionDenied = isPermissionDenied,
                        onRetryCloudSync = {
                            viewModel.retryCloudSync()
                            Toast.makeText(context, "Retrying cloud connection...", Toast.LENGTH_SHORT).show()
                        },
                        onSaveSettings = { updated ->
                            viewModel.updateSettings(updated)
                        },
                        onSetThemeMode = { mode ->
                            viewModel.setThemeMode(mode)
                        },
                        onSetDynamicColor = { dynamic ->
                            viewModel.setDynamicColor(dynamic)
                        },
                        onSwitchRole = { role, name ->
                            if (role == UserRole.ADMIN) {
                                userToUnlock = "ADMIN"
                            } else {
                                viewModel.switchToTeamMember(name)
                                Toast.makeText(context, "Switched to $name profile.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUnlockAdminClick = { userToUnlock = "ADMIN" },
                        onLockAdminSession = {
                            viewModel.lockAdminSession()
                            Toast.makeText(context, "Admin session locked 🔒", Toast.LENGTH_SHORT).show()
                        },
                        onLogout = {
                            viewModel.logoutToLanding()
                            Toast.makeText(context, "Logged out to Landing Screen", Toast.LENGTH_SHORT).show()
                        },
                        onResetDemoData = {
                            viewModel.resetDemoData()
                            Toast.makeText(context, "Database reset completed.", Toast.LENGTH_LONG).show()
                        },
                        onRestoreFromCloud = { onResult ->
                            viewModel.forceRestoreFromCloud(onResult)
                        },
                        onBackupToCloud = { onResult ->
                            viewModel.forceBackupToCloud(onResult)
                        },
                        onRestoreBackupData = { backupPhones, backupExpenses, overwrite, backupSettings, onResult ->
                            viewModel.restoreBackupData(backupPhones, backupExpenses, overwrite, backupSettings, onResult)
                        },
                        onRequireAdminAuth = { title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth ->
                            requestAdminAuth(title, actionTitle, actionDesc, icon, confirmText, isDanger, onAuth)
                        }
                    )

                    null -> {}
                }
            }
        }
    }
}

    // ==========================================
    // DIALOGS
    // ==========================================

    // About App & In-App Updates Dialog (Opened via Navigation Drawer or Settings)
    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Wholesale Price List PDF Generator Dialog (Opened via Navigation Drawer)
    if (showWholesaleDialogFromDrawer) {
        WholesalePriceListDialog(
            phones = phones,
            settings = settings,
            onDismiss = { showWholesaleDialogFromDrawer = false }
        )
    }

    // First Launch Admin PIN Mandatory Setup
    if (!settings.isAdminPinSetupCompleted) {
        FirstLaunchAdminSetupDialog(
            adminName = settings.adminName,
            onSetupComplete = { pin ->
                viewModel.setupInitialAdminPin(pin)
                Toast.makeText(context, "Admin PIN successfully configured! Unlocked 👑", Toast.LENGTH_LONG).show()
            },
            onRestoreCloudBackup = {
                viewModel.forceRestoreFromCloud { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Dynamic Multi-User PIN Dialog (Admin: Jasim, Staff 1: Sumon, Staff 2: Deedat)
    userToUnlock?.let { targetUser ->
        val targetName = when (targetUser) {
            "ADMIN" -> settings.adminName
            "STAFF_1" -> settings.staff1Name
            else -> settings.staff2Name
        }
        val defaultHint = when (targetUser) {
            "ADMIN" -> settings.ownerPin.ifBlank { "1111" }
            "STAFF_1" -> settings.staff1Pin.ifBlank { "2222" }
            else -> settings.staff2Pin.ifBlank { "3333" }
        }
        val isBioAvail = BiometricAuthManager.isBiometricAvailable(context)
        val isBioEnabled = BiometricAuthManager.isBiometricEnabled(context)
        PinDialog(
            userName = targetName,
            userRoleType = targetUser,
            defaultPinHint = defaultHint,
            isBengali = isBengali,
            onBiometricClick = if (isBioAvail && isBioEnabled) {
                {
                    userToUnlock = null
                    triggerBiometricAuth(targetUser)
                }
            } else null,
            onDismiss = { userToUnlock = null },
            onPinSuccess = {
                userToUnlock = null
                Toast.makeText(context, "Switched to $targetName profile! 🎉", Toast.LENGTH_SHORT).show()
            },
            onVerifyPin = { pin ->
                viewModel.verifyAndUnlockUser(targetUser, pin)
            }
        )
    }

    // Currency Calculator Dialog (¥ ⇄ ৳)
    if (showCurrencyCalcDialog) {
        CurrencyConverterDialog(
            currentRate = settings.rmbToBdtRate,
            isBengali = isBengali,
            onDismiss = { showCurrencyCalcDialog = false }
        )
    }

    // Add Phone Dialog
    if (showAddPhoneDialog) {
        AddPhoneDialog(
            currentRole = currentRole,
            rmbRate = settings.rmbToBdtRate,
            isBengali = isBengali,
            existingPhones = phones,
            onDismiss = { showAddPhoneDialog = false },
            onAddPhone = { brand, model, imei, imei2, color, ramRom, variant, batteryHealth, conditionGrade, itemCondition, packaging, entryDate, status, notes, rmb, buyingBdt, shipping, other, desiredProfit ->
                viewModel.addPhone(
                    brand = brand,
                    model = model,
                    imei = imei,
                    imei2 = imei2,
                    color = color,
                    ramRom = ramRom,
                    variant = variant,
                    batteryHealth = batteryHealth,
                    conditionGrade = conditionGrade,
                    itemCondition = itemCondition,
                    packaging = packaging,
                    entryDate = entryDate,
                    status = status,
                    notes = notes,
                    buyingPriceRmb = rmb,
                    buyingPriceBdt = buyingBdt,
                    shippingCostBdt = shipping,
                    otherCostBdt = other,
                    desiredProfitBdt = desiredProfit
                )
                Toast.makeText(context, "Phone added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Sell Phone Dialog
    phoneToSell?.let { phone ->
        SellPhoneDialog(
            phone = phone,
            defaultStaffName = activeStaffName,
            staff1Name = settings.staff1Name,
            staff2Name = settings.staff2Name,
            isBengali = isBengali,
            customerProfiles = customerDueProfiles,
            phones = phones,
            onDismiss = { phoneToSell = null },
            onConfirmSaleWithDue = { totalSalePrice, cashReceived, dueAmount, saleDate, customerName, customerPhone, customerEmail, soldByStaff, paymentMethod, warrantyDays, notes ->
                viewModel.sellPhone(
                    phoneId = phone.id,
                    sellingPriceBdt = totalSalePrice,
                    cashReceivedBdt = cashReceived,
                    dueAmountBdt = dueAmount,
                    saleDate = saleDate,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    soldByStaff = soldByStaff,
                    paymentMethod = paymentMethod,
                    warrantyDays = warrantyDays,
                    notes = notes
                )
                val soldPhone = phone.copy(
                    sellingPriceBdt = totalSalePrice,
                    cashReceivedBdt = cashReceived,
                    dueAmountBdt = dueAmount,
                    saleDate = saleDate,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    soldByStaff = soldByStaff,
                    paymentMethod = paymentMethod,
                    warrantyDays = warrantyDays,
                    notes = if (notes.isNotBlank()) "${phone.notes} | $notes".trim() else phone.notes,
                    status = com.example.data.model.PhoneStatus.SOLD.code
                )
                phoneToSell = null
                phoneForCashMemo = soldPhone

                // SILENT AUTOMATIC BACKGROUND EMAIL (Admin jasimuddin.chn@gmail.com + Customer)
                InvoiceEmailHelper.sendInvoiceEmailSilently(soldPhone, settings)
                val toastMsg = if (dueAmount > 0.0) {
                    "Sale recorded! ৳ ${String.format(java.util.Locale.US, "%,.0f", dueAmount)} posted to Customer Due Ledger."
                } else {
                    "Sale recorded successfully!"
                }
                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            },
            onConfirmSale = { sellingPrice, saleDate, customerName, customerPhone, customerEmail, soldByStaff, paymentMethod, warrantyDays ->
                viewModel.sellPhone(
                    phoneId = phone.id,
                    sellingPriceBdt = sellingPrice,
                    cashReceivedBdt = sellingPrice,
                    dueAmountBdt = 0.0,
                    saleDate = saleDate,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    soldByStaff = soldByStaff,
                    paymentMethod = paymentMethod,
                    warrantyDays = warrantyDays
                )
                val soldPhone = phone.copy(
                    sellingPriceBdt = sellingPrice,
                    cashReceivedBdt = sellingPrice,
                    dueAmountBdt = 0.0,
                    saleDate = saleDate,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    soldByStaff = soldByStaff,
                    paymentMethod = paymentMethod,
                    warrantyDays = warrantyDays,
                    status = com.example.data.model.PhoneStatus.SOLD.code
                )
                phoneToSell = null
                phoneForCashMemo = soldPhone

                // SILENT AUTOMATIC BACKGROUND EMAIL (Admin jasimuddin.chn@gmail.com + Customer)
                InvoiceEmailHelper.sendInvoiceEmailSilently(soldPhone, settings)
                Toast.makeText(context, "Sale recorded successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Admin Full Field Phone Edit Dialog
    phoneToEditCosts?.let { phone ->
        EditPhoneDialog(
            phone = phone,
            rmbRate = settings.rmbToBdtRate,
            isBengali = isBengali,
            existingPhones = phones,
            onDismiss = { phoneToEditCosts = null },
            onSaveEdit = { updatedPhone ->
                viewModel.updatePhoneDetails(updatedPhone)
                phoneToEditCosts = null
                Toast.makeText(context, "Entry updated successfully by Admin", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Cash Memo Dialog
    phoneForCashMemo?.let { phone ->
        CashMemoDialog(
            phone = phone,
            settings = settings,
            isBengali = isBengali,
            onDismiss = { phoneForCashMemo = null }
        )
    }

    // Official Sales Invoice Dialog
    phoneForInvoice?.let { phone ->
        InvoiceDialog(
            phone = phone,
            settings = settings,
            isBengali = isBengali,
            onDismiss = { phoneForInvoice = null }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            isBengali = isBengali,
            onDismiss = { showAddExpenseDialog = false },
            onAddExpense = { title, category, amount, date, notes, receiptUrl ->
                expenseViewModel.addExpense(
                    title = title,
                    category = category,
                    amountBdt = amount,
                    date = date,
                    notes = notes,
                    receiptUrl = receiptUrl
                )
                showAddExpenseDialog = false
                Toast.makeText(context, "Expense added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add / Edit Other Income Dialog
    if (showAddOtherIncomeDialog || otherIncomeToEdit != null) {
        IncomeDialog(
            initialIncome = otherIncomeToEdit,
            onDismiss = {
                showAddOtherIncomeDialog = false
                otherIncomeToEdit = null
            },
            onConfirm = { category, amount, date, notes ->
                val toEdit = otherIncomeToEdit
                if (toEdit != null) {
                    viewModel.updateOtherIncome(
                        toEdit.copy(
                            category = category,
                            amountBdt = amount,
                            date = date,
                            notes = notes
                        )
                    )
                    Toast.makeText(context, "Other income updated!", Toast.LENGTH_SHORT).show()
                } else {
                    val staffName = if (currentRole == UserRole.ADMIN) settings.getAdminName(false) else activeStaffName.ifBlank { "Staff" }
                    viewModel.addOtherIncome(
                        category = category,
                        amount = amount,
                        date = date,
                        notes = notes,
                        addedBy = staffName
                    )
                    Toast.makeText(context, "Other income of ৳ ${String.format(java.util.Locale.US, "%,.0f", amount)} added!", Toast.LENGTH_SHORT).show()
                }
                showAddOtherIncomeDialog = false
                otherIncomeToEdit = null
            }
        )
    }

    // App Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Exit App",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isBengali) "অ্যাপ থেকে বের হতে চান?" else "Exit Phone Hisab?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "আপনি কি নিশ্চিত যে আপনি Phone Hisab অ্যাপ থেকে বের হতে চান?"
                    else
                        "Are you sure you want to exit the application? Tap Exit to close the app or Cancel to continue.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        viewModel.logoutToLanding()
                        SessionManager.clearSession(context)
                        (context as? Activity)?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_exit_app_btn")
                ) {
                    Text(
                        text = if (isBengali) "বের হন" else "Exit App",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_exit_app_btn")
                ) {
                    Text(
                        text = if (isBengali) "বাতিল" else "Cancel",
                        color = if (isAppInDarkTheme()) Color(0xFF94A3B8) else Slate700,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = CardBg
        )
    }

    // Notification Center List Dialog
    if (showNotificationCenterDialog) {
        NotificationCenterDialog(
            notifications = notifications,
            onDismiss = { showNotificationCenterDialog = false },
            onSelectNotification = { notif ->
                NotificationHelper.markNotificationAsRead(context, notif.id)
                notificationRefreshKey++
                selectedNotificationForDetail = notif
            },
            onMarkAllAsRead = {
                NotificationHelper.markAllAsRead(context, notifications.map { it.id })
                notificationRefreshKey++
            },
            isBengali = isBengali
        )
    }

    // Notification Detail Dialog
    selectedNotificationForDetail?.let { notif ->
        NotificationDetailDialog(
            item = notif,
            onDismiss = { selectedNotificationForDetail = null },
            onViewCashMemo = { phone ->
                phoneForCashMemo = phone
            },
            onViewInvoice = { phone ->
                phoneForInvoice = phone
            },
            onOpenUpdateDialog = {
                showAboutDialog = true
            },
            isBengali = isBengali
        )
    }

    // Global Admin Security PIN Verification Dialog
    pendingGlobalAction?.let { actionData ->
        val (title, actionTitle, actionDesc) = actionData.first
        val (iconAndBtn, dangerAndCallback) = actionData.second
        val (icon, confirmBtnText) = iconAndBtn
        val (isDanger, callback) = dangerAndCallback

        AdminSecurityConfirmDialog(
            title = title,
            actionTitle = actionTitle,
            actionDescription = actionDesc,
            actionIcon = icon,
            confirmButtonText = confirmBtnText,
            isDangerAction = isDanger,
            onDismiss = { pendingGlobalAction = null },
            onVerifyPin = { inputPin ->
                viewModel.verifyAdminPin(inputPin)
            },
            onAuthorized = {
                pendingGlobalAction = null
                callback()
            }
        )
    }
}
