package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.ImportDataDialog
import com.example.ui.components.WholesalePriceListDialog
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.BiometricAuthManager
import com.example.util.ExportImportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Types of operations that require Admin PIN authorization before execution
 */
private enum class AdminSecuredAction(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    EXPORT_FULL_JSON(
        "Full App Backup (.json)",
        "Exports complete database state for full recovery",
        Icons.Default.CloudDownload
    ),
    EXPORT_FULL_CSV(
        "Full Database Spreadsheet (.csv)",
        "Exports comprehensive inventory, expenses & settings sheet",
        Icons.Default.Storage
    ),
    EXPORT_STOCK_CSV(
        "Stock & Inventory Backup (.csv)",
        "Exports all phones, IMEIs, China yuan & landing costs",
        Icons.Default.PhoneIphone
    ),
    EXPORT_SALES_CSV(
        "Sales History Backup (.csv)",
        "Exports sold invoices, customer names & net profit",
        Icons.Default.ReceiptLong
    ),
    EXPORT_FINANCIAL_CSV(
        "Financial & Profit Reports (.csv)",
        "Exports executive revenue, COGS, expenses & margins",
        Icons.Default.Assessment
    ),
    RESTORE_JSON(
        "Restore Database (.json)",
        "Select and import previously saved JSON backup from storage",
        Icons.Default.FileUpload
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    currentRole: UserRole,
    isBengali: Boolean = false,
    phones: List<PhoneItem> = emptyList(),
    expenses: List<ExpenseItem> = emptyList(),
    isCloudConnected: Boolean = false,
    isPermissionDenied: Boolean = false,
    onRetryCloudSync: () -> Unit = {},
    onSaveSettings: (AppSettings) -> Unit,
    onSwitchRole: (UserRole, String?) -> Unit,
    onUnlockAdminClick: () -> Unit,
    onLockAdminSession: () -> Unit = {},
    onLogout: () -> Unit = {},
    onResetDemoData: () -> Unit = {},
    onSetThemeMode: ((String) -> Unit)? = null,
    onSetDynamicColor: ((Boolean) -> Unit)? = null,
    onRestoreFromCloud: (((Boolean, String) -> Unit) -> Unit)? = null,
    onBackupToCloud: (((Boolean, String) -> Unit) -> Unit)? = null,
    onRestoreBackupData: ((List<PhoneItem>, List<ExpenseItem>, Boolean, AppSettings?, (Boolean, String) -> Unit) -> Unit)? = null,
    onRequireAdminAuth: ((title: String, actionTitle: String, actionDesc: String, icon: ImageVector, confirmText: String, isDanger: Boolean, onAuth: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    // Appearance & Dynamic Theme states
    var themeModeState by remember(settings.themeMode) { mutableStateOf(settings.themeMode) }
    var dynamicColorState by remember(settings.dynamicColor) { mutableStateOf(settings.dynamicColor) }

    // Biometric Authentication state (Fingerprint / Face Unlock)
    var biometricEnabledState by remember {
        mutableStateOf(BiometricAuthManager.isBiometricEnabled(context))
    }
    val isBiometricAvailable = remember {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    // Store & Exchange states
    var rmbRateText by remember(settings.rmbToBdtRate) { mutableStateOf(settings.rmbToBdtRate.toString()) }
    var shopNameText by remember(settings.shopName) { mutableStateOf(settings.shopName) }
    var shopSubtitleText by remember(settings.shopSubtitle) { mutableStateOf(settings.shopSubtitle) }
    var shopAddressText by remember(settings.shopAddress) { mutableStateOf(settings.shopAddress) }
    var shopPhoneText by remember(settings.shopPhone) { mutableStateOf(settings.shopPhone) }
    var ownerEmailText by remember(settings.ownerNotificationEmail) { mutableStateOf(settings.ownerNotificationEmail) }
    var warrantyText by remember(settings.defaultWarrantyText) { mutableStateOf(settings.defaultWarrantyText) }

    // PIN states
    var ownerPinText by remember(settings.ownerPin) { mutableStateOf(settings.ownerPin) }
    var staff1PinText by remember(settings.staff1Pin) { mutableStateOf(settings.staff1Pin) }
    var staff2PinText by remember(settings.staff2Pin) { mutableStateOf(settings.staff2Pin) }
    var showAdminPin by remember { mutableStateOf(false) }
    var showStaff1Pin by remember { mutableStateOf(false) }
    var showStaff2Pin by remember { mutableStateOf(false) }

    // User Names in English
    var adminNameEnText by remember(settings.adminNameEn) { mutableStateOf(settings.adminNameEn) }
    var staff1NameEnText by remember(settings.staff1NameEn) { mutableStateOf(settings.staff1NameEn) }
    var staff2NameEnText by remember(settings.staff2NameEn) { mutableStateOf(settings.staff2NameEn) }

    // Security & Dialog States
    var pendingSecuredAction by remember { mutableStateOf<AdminSecuredAction?>(null) }
    var showAdminPinPrompt by remember { mutableStateOf(false) }
    var showImportRestoreDialog by remember { mutableStateOf(false) }
    var showWholesalePriceListDialog by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }

    // Execution helper after PIN verification
    fun executeSecuredAction(action: AdminSecuredAction) {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        when (action) {
            AdminSecuredAction.EXPORT_FULL_JSON -> {
                val json = ExportImportHelper.generateBackupJson(phones, expenses, settings)
                ExportImportHelper.shareExportFile(
                    context = context,
                    filename = "Zoha_Trading_Full_Backup_$dateStr.json",
                    content = json,
                    mimeType = "application/json",
                    title = "Full Database JSON Backup"
                )
            }
            AdminSecuredAction.EXPORT_FULL_CSV -> {
                val csv = ExportImportHelper.generateFullDatabaseCsv(phones, expenses, settings)
                ExportImportHelper.shareExportFile(
                    context = context,
                    filename = "Zoha_Trading_Full_Database_$dateStr.csv",
                    content = csv,
                    mimeType = "text/csv",
                    title = "Full Database Excel Sheet"
                )
            }
            AdminSecuredAction.EXPORT_STOCK_CSV -> {
                val csv = ExportImportHelper.generateStockCsv(phones, settings.rmbToBdtRate)
                ExportImportHelper.shareExportFile(
                    context = context,
                    filename = "Zoha_Trading_Stock_$dateStr.csv",
                    content = csv,
                    mimeType = "text/csv",
                    title = "Stock & Inventory Excel CSV"
                )
            }
            AdminSecuredAction.EXPORT_SALES_CSV -> {
                val csv = ExportImportHelper.generateSalesCsv(phones)
                ExportImportHelper.shareExportFile(
                    context = context,
                    filename = "Zoha_Trading_Sales_$dateStr.csv",
                    content = csv,
                    mimeType = "text/csv",
                    title = "Sales History Excel CSV"
                )
            }
            AdminSecuredAction.EXPORT_FINANCIAL_CSV -> {
                val csv = ExportImportHelper.generateFinancialReportCsv(phones, expenses, settings)
                ExportImportHelper.shareExportFile(
                    context = context,
                    filename = "Zoha_Trading_Financial_Report_$dateStr.csv",
                    content = csv,
                    mimeType = "text/csv",
                    title = "Profit & Financial Reports Excel CSV"
                )
            }
            AdminSecuredAction.RESTORE_JSON -> {
                showImportRestoreDialog = true
            }
        }
    }

    fun triggerSecuredAction(action: AdminSecuredAction) {
        if (onRequireAdminAuth != null) {
            onRequireAdminAuth(
                "Admin Authentication",
                action.title,
                action.description,
                action.icon,
                "Authorize Action",
                action == AdminSecuredAction.RESTORE_JSON
            ) {
                executeSecuredAction(action)
            }
        } else {
            pendingSecuredAction = action
            showAdminPinPrompt = true
        }
    }

    // 1. Admin PIN Security Dialog
    if (showAdminPinPrompt && pendingSecuredAction != null) {
        val action = pendingSecuredAction!!
        AdminPinSecurityPopup(
            action = action,
            onDismiss = {
                showAdminPinPrompt = false
                pendingSecuredAction = null
            },
            onVerifyPin = { enteredPin ->
                val adminPin = settings.ownerPin.ifBlank { "1111" }
                enteredPin.trim() == adminPin.trim()
            },
            onAuthorized = {
                showAdminPinPrompt = false
                executeSecuredAction(action)
                pendingSecuredAction = null
            }
        )
    }

    // 2. Data Import & Restore Dialog
    if (showImportRestoreDialog) {
        ImportDataDialog(
            onDismiss = { showImportRestoreDialog = false },
            onConfirmRestore = { bundle, overwrite ->
                if (onRestoreBackupData != null) {
                    onRestoreBackupData(bundle.phones, bundle.expenses, overwrite, bundle.settings) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Restore handler not available.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 3. Wholesale Price List PDF Generator Dialog
    if (showWholesalePriceListDialog) {
        WholesalePriceListDialog(
            phones = phones,
            settings = settings,
            onDismiss = { showWholesalePriceListDialog = false }
        )
    }

    // 4. About & In-App Updates Dialog
    if (showAboutAppDialog) {
        AboutAppDialog(
            onDismiss = { showAboutAppDialog = false }
        )
    }

    if (currentRole != UserRole.ADMIN) {
        // Non-admin guard
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CardBg)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Admin Protected",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Admin Access Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Settings, data exports, and backups are strictly restricted to Admin.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onUnlockAdminClick,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Unlock Admin (Enter PIN)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CardBg)
            .padding(horizontal = 14.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ==========================================
        // CARD 1: DATA MANAGEMENT & EXPORTS (OFFLINE)
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF0F766E).copy(alpha = 0.35f) else Color(0xFF0F766E).copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Data Management",
                                    tint = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Data Management & Exports",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Offline Excel (.csv) sheets & universal (.json) recovery",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Database Stats Pills
                    Surface(
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatPill(label = "In Stock", value = "${phones.count { it.status != PhoneStatus.SOLD.code }}", color = if (isDark) Color(0xFF60A5FA) else PrimaryNavy)
                            StatPill(label = "Sold Phones", value = "${phones.count { it.status == PhoneStatus.SOLD.code }}", color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                            StatPill(label = "Expenses", value = "${expenses.size}", color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "OFFLINE DATA EXPORT & BACKUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF94A3B8) else Slate500,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Full Database Options (.json & .csv)
                    DataActionItemCard(
                        title = "Full Application Database",
                        subtitle = "Complete offline bundle for full system recovery and audit",
                        icon = Icons.Default.CloudDownload,
                        iconTint = Color(0xFF7C3AED)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StandardizedActionButton(
                                text = "Full Backup (.json)",
                                icon = Icons.Default.CloudDownload,
                                backgroundColor = Color(0xFF7C3AED),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                testTag = "export_full_json_btn",
                                onClick = {
                                    triggerSecuredAction(AdminSecuredAction.EXPORT_FULL_JSON)
                                }
                            )
                            StandardizedActionButton(
                                text = "Full Sheet (.csv)",
                                icon = Icons.Default.Storage,
                                backgroundColor = Color(0xFF0F766E),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                testTag = "export_full_csv_btn",
                                onClick = {
                                    triggerSecuredAction(AdminSecuredAction.EXPORT_FULL_CSV)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Stock / Inventory Backup (.csv)
                    DataActionItemCard(
                        title = "Stock & Inventory (.csv)",
                        subtitle = "All phones, IMEI 1/2, China RMB cost, landing price & status",
                        icon = Icons.Default.PhoneIphone,
                        iconTint = Color(0xFF2563EB)
                    ) {
                        StandardizedActionButton(
                            text = "Export Stock Backup (.csv)",
                            icon = Icons.Default.FileDownload,
                            backgroundColor = Color(0xFF2563EB),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "export_stock_csv_btn",
                            onClick = {
                                triggerSecuredAction(AdminSecuredAction.EXPORT_STOCK_CSV)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Sales History Backup (.csv)
                    DataActionItemCard(
                        title = "Sales History & Invoices (.csv)",
                        subtitle = "Sold units, invoice numbers, customer details & net profits",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = Color(0xFF059669)
                    ) {
                        StandardizedActionButton(
                            text = "Export Sales History (.csv)",
                            icon = Icons.Default.FileDownload,
                            backgroundColor = Color(0xFF059669),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "export_sales_csv_btn",
                            onClick = {
                                triggerSecuredAction(AdminSecuredAction.EXPORT_SALES_CSV)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Profit & Financial Reports (.csv)
                    DataActionItemCard(
                        title = "Profit & Financial Reports (.csv)",
                        subtitle = "Executive financial overview, revenue, COGS & operating margin",
                        icon = Icons.Default.Assessment,
                        iconTint = Color(0xFFD97706)
                    ) {
                        StandardizedActionButton(
                            text = "Export Financial Report (.csv)",
                            icon = Icons.Default.FileDownload,
                            backgroundColor = Color(0xFFD97706),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "export_profit_csv_btn",
                            onClick = {
                                triggerSecuredAction(AdminSecuredAction.EXPORT_FINANCIAL_CSV)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Wholesale Price List (PDF)
                    DataActionItemCard(
                        title = "Wholesale Price List (PDF)",
                        subtitle = "Interactive device selection, brand categorization, box status & PDF export",
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = Color(0xFF0284C7)
                    ) {
                        StandardizedActionButton(
                            text = "Wholesale Price List",
                            icon = Icons.Default.PictureAsPdf,
                            backgroundColor = Color(0xFF0284C7),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "open_wholesale_price_list_btn",
                            onClick = {
                                showWholesalePriceListDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DATA IMPORT & RESTORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate500,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Restore from File (.json)
                    DataActionItemCard(
                        title = "Restore Database from File (.json)",
                        subtitle = "Import previously exported .json file to restore all records",
                        icon = Icons.Default.FileUpload,
                        iconTint = Color(0xFF4F46E5)
                    ) {
                        StandardizedActionButton(
                            text = "Select Backup & Restore (.json)",
                            icon = Icons.Default.FileUpload,
                            backgroundColor = Color(0xFF4F46E5),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "open_import_restore_btn",
                            onClick = {
                                triggerSecuredAction(AdminSecuredAction.RESTORE_JSON)
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 2: SYSTEM SECURITY & PIN MANAGEMENT
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else PrimaryNavy.copy(alpha = 0.1f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security PINs",
                                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Security & PIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Confidential PIN access controls for all profiles",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Admin PIN
                    OutlinedTextField(
                        value = ownerPinText,
                        onValueChange = { ownerPinText = it },
                        label = { Text("Admin PIN", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Enter 4-digit PIN") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showAdminPin) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showAdminPin = !showAdminPin }) {
                                Icon(
                                    imageVector = if (showAdminPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle PIN Visibility",
                                    tint = if (isDark) Color(0xFF94A3B8) else Slate700
                                )
                            }
                        },
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_owner_pin_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Staff 1 PIN
                        OutlinedTextField(
                            value = staff1PinText,
                            onValueChange = { staff1PinText = it },
                            label = { Text("Staff 1 PIN", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("PIN") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showStaff1Pin) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showStaff1Pin = !showStaff1Pin }) {
                                    Icon(
                                        imageVector = if (showStaff1Pin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle PIN Visibility",
                                        tint = if (isDark) Color(0xFF94A3B8) else Slate700
                                    )
                                }
                            },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setting_staff1_pin_input")
                        )

                        // Staff 2 PIN
                        OutlinedTextField(
                            value = staff2PinText,
                            onValueChange = { staff2PinText = it },
                            label = { Text("Staff 2 PIN", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("PIN") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showStaff2Pin) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showStaff2Pin = !showStaff2Pin }) {
                                    Icon(
                                        imageVector = if (showStaff2Pin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle PIN Visibility",
                                        tint = if (isDark) Color(0xFF94A3B8) else Slate700
                                    )
                                }
                            },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setting_staff2_pin_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Standardized PIN Update Button
                    StandardizedActionButton(
                        text = "Update Security PINs",
                        icon = Icons.Default.Lock,
                        backgroundColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "update_pins_btn",
                        onClick = {
                            val updated = settings.copy(
                                ownerPin = ownerPinText.ifBlank { "1111" },
                                staff1Pin = staff1PinText.ifBlank { "2222" },
                                staff2Pin = staff2PinText.ifBlank { "3333" }
                            )
                            onSaveSettings(updated)
                            Toast.makeText(context, "Security PINs updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Authentication (Fingerprint / Face Unlock) Toggle Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometric Authentication",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Biometric Authentication",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White else Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBiometricAvailable) "Fingerprint & Face Unlock active with PIN fallback" else "Device biometric security active with PIN fallback",
                                        fontSize = 11.sp,
                                        color = if (isBiometricAvailable) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else Slate500),
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Switch(
                                checked = biometricEnabledState,
                                onCheckedChange = { isChecked ->
                                    biometricEnabledState = isChecked
                                    BiometricAuthManager.setBiometricEnabled(context, isChecked)
                                    Toast.makeText(
                                        context,
                                        if (isChecked) "Biometric authentication enabled! 🔒" else "Biometric authentication disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("setting_biometric_switch")
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // CARD 3: ADMIN & STAFF PROFILES
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else PrimaryBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "User Names",
                                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin & Staff Profiles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Display names shown on invoices and audit logs",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = adminNameEnText,
                        onValueChange = { adminNameEnText = it },
                        label = { Text("Admin Display Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Jasim") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = staff1NameEnText,
                            onValueChange = { staff1NameEnText = it },
                            label = { Text("Staff 1 Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("Sumon") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = staff2NameEnText,
                            onValueChange = { staff2NameEnText = it },
                            label = { Text("Staff 2 Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("Deedat") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ==========================================
        // CARD 4: SHOP PROFILE & BUSINESS DETAILS
        // ==========================================
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else PrimaryNavy.copy(alpha = 0.1f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = "Shop Info",
                                    tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shop Profile & Business Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Branding, invoice headers, and exchange rates",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shop Name
                    OutlinedTextField(
                        value = shopNameText,
                        onValueChange = { shopNameText = it },
                        label = { Text("Shop Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Zoha Trading") },
                        leadingIcon = {
                            Icon(Icons.Default.Store, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Slate500, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_shop_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slogan / Subtitle
                    OutlinedTextField(
                        value = shopSubtitleText,
                        onValueChange = { shopSubtitleText = it },
                        label = { Text("Banner Subtitle / Slogan", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Trading Worldwide Since 2012") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_shop_subtitle_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // RMB Exchange Rate
                    OutlinedTextField(
                        value = rmbRateText,
                        onValueChange = { rmbRateText = it },
                        label = { Text("1 RMB Exchange Rate (¥ ⇄ ৳ BDT)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("17.50") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_rmb_rate_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shop Address
                    OutlinedTextField(
                        value = shopAddressText,
                        onValueChange = { shopAddressText = it },
                        label = { Text("Shop Address", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Dhaka, Bangladesh") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Slate500, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_shop_address_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contact Phone / Hotline
                    OutlinedTextField(
                        value = shopPhoneText,
                        onValueChange = { shopPhoneText = it },
                        label = { Text("Contact Phone / Hotline", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("+880 1700-000000") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Slate500, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_shop_phone_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Owner Email
                    OutlinedTextField(
                        value = ownerEmailText,
                        onValueChange = { ownerEmailText = it },
                        label = { Text("Owner Email (Sales Memos & Invoices)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("jasimuddin.nsu@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Slate500, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_owner_email_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cash Memo Warranty Policy
                    OutlinedTextField(
                        value = warrantyText,
                        onValueChange = { warrantyText = it },
                        label = { Text("Cash Memo Warranty Policy", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("e.g. 7 Days Replacement, 1 Year Service Warranty") },
                        leadingIcon = {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = if (isDark) Color(0xFF94A3B8) else Slate500, modifier = Modifier.size(18.dp))
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_warranty_input")
                    )
                }
            }
        }

        // CARD 6: SAVE ALL SETTINGS BUTTON
        // ==========================================
        item {
            Button(
                onClick = {
                    val rate = rmbRateText.toDoubleOrNull() ?: settings.rmbToBdtRate
                    val newSettings = settings.copy(
                        rmbToBdtRate = rate,
                        ownerPin = ownerPinText.ifBlank { "1111" },
                        staff1Pin = staff1PinText.ifBlank { "2222" },
                        staff2Pin = staff2PinText.ifBlank { "3333" },
                        isAdminPinSetupCompleted = true,
                        adminNameBn = adminNameEnText.ifBlank { "Jasim" },
                        adminNameEn = adminNameEnText.ifBlank { "Jasim" },
                        staff1NameBn = staff1NameEnText.ifBlank { "Sumon" },
                        staff1NameEn = staff1NameEnText.ifBlank { "Sumon" },
                        staff2NameBn = staff2NameEnText.ifBlank { "Deedat" },
                        staff2NameEn = staff2NameEnText.ifBlank { "Deedat" },
                        shopName = shopNameText.ifBlank { "Zoha Trading" },
                        shopSubtitle = shopSubtitleText.ifBlank { "Trading Worldwide Since 2012" },
                        shopAddress = shopAddressText,
                        shopPhone = shopPhoneText,
                        staff1Name = staff1NameEnText.ifBlank { "Sumon" },
                        staff2Name = staff2NameEnText.ifBlank { "Deedat" },
                        defaultWarrantyText = warrantyText,
                        ownerNotificationEmail = ownerEmailText.ifBlank { "jasimuddin.nsu@gmail.com" },
                        themeMode = themeModeState,
                        dynamicColor = dynamicColorState,
                        appLanguage = "en"
                    )
                    BiometricAuthManager.setBiometricEnabled(context, biometricEnabledState)
                    onSaveSettings(newSettings)
                    Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_all_settings_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save All Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // CARD 7: ABOUT & IN-APP UPDATES
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About App",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About & In-App Updates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else PrimaryNavy
                            )
                            Text(
                                text = "Version 1.0.3 • Check for online APK releases & updates",
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StandardizedActionButton(
                        text = "About & Check for Updates",
                        icon = Icons.Default.SystemUpdate,
                        backgroundColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_about_updates_btn",
                        onClick = { showAboutAppDialog = true }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Standardized Action Button re-skinned with 12dp rounded corners, subtle 1dp border,
 * ambient shadow, press feedback, and single-line typography with ellipsis.
 */
@Composable
private fun StandardizedActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "std_action_btn_scale"
    )

    val gradientBrush = Brush.horizontalGradient(
        listOf(backgroundColor, backgroundColor.copy(alpha = 0.85f))
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
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
                .background(gradientBrush)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Clean Card container for each data action item to guarantee zero text overflow
 */
@Composable
private fun DataActionItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = if (isDark) iconTint.copy(alpha = 0.25f) else iconTint.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

/**
 * Database stat pill for clean, responsive overview
 */
@Composable
private fun StatPill(label: String, value: String, color: Color) {
    val isDark = isAppInDarkTheme()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isDark) Color(0xFF94A3B8) else Slate500,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Secure Admin PIN Dialog required before any data export or restore operation
 */
@Composable
private fun AdminPinSecurityPopup(
    action: AdminSecuredAction,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onAuthorized: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isAppInDarkTheme()
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else PrimaryNavy.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Security",
                            tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Admin Authentication",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Security Verification Required",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Operation badge
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else PrimaryNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = action.description,
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Enter confidential Admin PIN to authorize:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE2E8F0) else Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Admin PIN") },
                    placeholder = { Text("Enter 4-digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PIN visibility",
                                tint = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                    },
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_confirm_input")
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
                    if (onVerifyPin(pinText)) {
                        onAuthorized()
                    } else {
                        errorMessage = "Incorrect Admin PIN! Authorization denied."
                        Toast.makeText(context, "Incorrect Admin PIN", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_pin_confirm_btn")
            ) {
                Text(
                    text = "Authorize & Proceed",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("admin_pin_cancel_btn")
            ) {
                Text(
                    text = "Cancel",
                    color = if (isDark) Color(0xFF94A3B8) else Slate700,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = if (isDark) Color(0xFF1E293B) else CardBg
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    val sampleSettings = AppSettings(
        shopName = "Zoha Trading",
        shopSubtitle = "Trading Worldwide Since 2012",
        rmbToBdtRate = 17.50,
        ownerPin = "1111",
        staff1Pin = "2222",
        staff2Pin = "3333",
        adminNameEn = "Jasim",
        staff1NameEn = "Sumon",
        staff2NameEn = "Deedat"
    )
    SettingsScreen(
        settings = sampleSettings,
        currentRole = UserRole.ADMIN,
        phones = emptyList(),
        expenses = emptyList(),
        onSaveSettings = {},
        onSwitchRole = { _, _ -> },
        onUnlockAdminClick = {}
    )
}
