package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.appTextFieldColors
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.BackupBundle
import com.example.util.ExportImportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportDataDialog(
    phones: List<PhoneItem>,
    expenses: List<ExpenseItem>,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }
    val isDark = isAppInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("export_data_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Data to Excel / CSV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDark) Color.White else Slate900
                            )
                            Text(
                                text = "Download inventory, sales & expenses",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFA0AEC0) else Slate700
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color(0xFF94A3B8) else Slate700)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats summary banner
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${phones.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                            Text(text = "Total Stock", fontSize = 11.sp, color = Slate700)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${phones.count { it.status == "SOLD" }}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                            Text(text = "Sales Memos", fontSize = 11.sp, color = Slate700)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${expenses.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF59E0B))
                            Text(text = "Expenses", fontSize = 11.sp, color = Slate700)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SELECT EXPORT FORMAT:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 1: Complete Stock CSV (Excel)
                ExportOptionCard(
                    title = "Phone Stock & Inventory (.csv)",
                    subtitle = "All items, IMEI, China Yuan (¥) & landing costs, profits & notes",
                    icon = Icons.Default.PhoneIphone,
                    color = Color(0xFF2563EB),
                    buttonText = "Export Stock CSV",
                    tag = "export_stock_csv_btn",
                    onClick = {
                        val csv = ExportImportHelper.generateStockCsv(phones, settings.rmbToBdtRate)
                        ExportImportHelper.shareExportFile(
                            context = context,
                            filename = "Zoha_Trading_Stock_$dateStr.csv",
                            content = csv,
                            mimeType = "text/csv",
                            title = "Share Stock CSV (Excel)"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Sales History CSV (Excel)
                ExportOptionCard(
                    title = "Sales History & Customer Memos (.csv)",
                    subtitle = "Sold items, invoices, buyer contact info, payments & profits",
                    icon = Icons.Default.ReceiptLong,
                    color = Color(0xFF059669),
                    buttonText = "Export Sales CSV",
                    tag = "export_sales_csv_btn",
                    onClick = {
                        val csv = ExportImportHelper.generateSalesCsv(phones)
                        ExportImportHelper.shareExportFile(
                            context = context,
                            filename = "Zoha_Trading_Sales_$dateStr.csv",
                            content = csv,
                            mimeType = "text/csv",
                            title = "Share Sales CSV (Excel)"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Monthly Expenses CSV (Excel)
                ExportOptionCard(
                    title = "Monthly Shop Expenses (.csv)",
                    subtitle = "Rent, cargo, courier, transport & utilities expense records",
                    icon = Icons.Default.Assessment,
                    color = Color(0xFFD97706),
                    buttonText = "Export Expenses CSV",
                    tag = "export_expenses_csv_btn",
                    onClick = {
                        val csv = ExportImportHelper.generateExpensesCsv(expenses)
                        ExportImportHelper.shareExportFile(
                            context = context,
                            filename = "Zoha_Trading_Expenses_$dateStr.csv",
                            content = csv,
                            mimeType = "text/csv",
                            title = "Share Expenses CSV (Excel)"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 4: Full Database JSON Backup (Universal Backup for 1-click restore)
                ExportOptionCard(
                    title = "Complete Database Backup (.json)",
                    subtitle = "Full data bundle containing inventory, sales, expenses & settings",
                    icon = Icons.Default.CloudDownload,
                    color = Color(0xFF7C3AED),
                    buttonText = "Export Full Backup JSON",
                    tag = "export_backup_json_btn",
                    onClick = {
                        val json = ExportImportHelper.generateBackupJson(phones, expenses, settings)
                        ExportImportHelper.shareExportFile(
                            context = context,
                            filename = "Zoha_Trading_Full_Backup_$dateStr.json",
                            content = json,
                            mimeType = "application/json",
                            title = "Share Full Database Backup"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close", fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                }
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    buttonText: String,
    tag: String,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDark) Color.White else Slate900)
                    Text(text = subtitle, fontSize = 11.sp, color = if (isDark) Color(0xFFA0AEC0) else Slate700)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag(tag)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ImportDataDialog(
    onDismiss: () -> Unit,
    onConfirmRestore: (BackupBundle, Boolean) -> Unit
) {
    val context = LocalContext.current
    var loadedBundle by remember { mutableStateOf<BackupBundle?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isMergeMode by remember { mutableStateOf(true) } // true = Merge/Append, false = Overwrite
    var rawTextBackup by remember { mutableStateOf("") }
    var showRawTextInput by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDark = isAppInDarkTheme()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            errorMessage = null
            try {
                val jsonString = ExportImportHelper.readTextFromUri(context, uri)
                if (jsonString != null) {
                    val bundle = ExportImportHelper.parseBackupJson(jsonString)
                    if (bundle != null && (bundle.phones.isNotEmpty() || bundle.expenses.isNotEmpty())) {
                        loadedBundle = bundle
                    } else {
                        errorMessage = "No valid phone or expense records found in this file."
                    }
                } else {
                    errorMessage = "Unable to read selected file."
                }
            } catch (e: Exception) {
                errorMessage = "Error parsing file: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("import_data_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import & Restore Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDark) Color.White else Slate900
                            )
                            Text(
                                text = "Restore inventory & history from backup",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFA0AEC0) else Slate700
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color(0xFF94A3B8) else Slate700)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (loadedBundle == null) {
                    // Step 1: Select File
                    Text(
                        text = "SELECT BACKUP FILE TO RESTORE:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                filePickerLauncher.launch("*/*")
                            }
                            .testTag("select_backup_file_btn")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF2563EB))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Reading backup file...", fontSize = 12.sp, color = if (isDark) Color(0xFFA0AEC0) else Slate700)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Tap to Browse Backup File (.json)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White else Slate900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select previously exported JSON backup from storage",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA0AEC0) else Slate700
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = errorMessage ?: "", fontSize = 12.sp, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option to paste raw JSON text
                    if (!showRawTextInput) {
                        OutlinedButton(
                            onClick = { showRawTextInput = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Or Paste Backup JSON Code", fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                        }
                    } else {
                        OutlinedTextField(
                            value = rawTextBackup,
                            onValueChange = { rawTextBackup = it },
                            label = { Text("Paste JSON Backup Text") },
                            minLines = 3,
                            maxLines = 6,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (rawTextBackup.isNotBlank()) {
                                    val bundle = ExportImportHelper.parseBackupJson(rawTextBackup)
                                    if (bundle != null && (bundle.phones.isNotEmpty() || bundle.expenses.isNotEmpty())) {
                                        loadedBundle = bundle
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Invalid JSON format or empty data."
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2563EB) else Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Parse Pasted JSON", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Step 2: Confirmation & Mode Selection
                    val bundle = loadedBundle!!
                    Surface(
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Backup File Verified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Date of Backup: ${bundle.exportDate}\n• Phones / Stock Items: ${bundle.totalPhones}\n• Expense Records: ${bundle.totalExpenses}",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF047857),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CHOOSE RESTORE MODE:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Merge / Append
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMergeMode) {
                            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMergeMode) Color(0xFF3B82F6) else if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMergeMode = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            RadioButton(
                                selected = isMergeMode,
                                onClick = { isMergeMode = true },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Merge & Append (Recommended)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else Slate900
                                )
                                Text(
                                    text = "Keeps current data and imports new records from backup",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFA0AEC0) else Slate700
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Overwrite All
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isMergeMode) {
                            if (isDark) Color(0xFF450A0A).copy(alpha = 0.4f) else Color(0xFFFEF2F2)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isMergeMode) Color(0xFFEF4444) else if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMergeMode = false }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            RadioButton(
                                selected = !isMergeMode,
                                onClick = { isMergeMode = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Replace & Overwrite Everything",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                                Text(
                                    text = "⚠️ Clears current database and replaces it completely with backup",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { loadedBundle = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Choose Another", fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                        }

                        Button(
                            onClick = {
                                onConfirmRestore(bundle, !isMergeMode) // overwrite = !isMergeMode
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMergeMode) Color(0xFF2563EB) else Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("confirm_restore_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMergeMode) "Start Merge" else "Overwrite & Restore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancel", fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                }
            }
        }
    }
}
