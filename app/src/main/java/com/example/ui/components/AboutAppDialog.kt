package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.InAppUpdateManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val scope = rememberCoroutineScope()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var downloadStatusText by remember { mutableStateOf<String?>(null) }
    var updateErrorText by remember { mutableStateOf<String?>(null) }
    var readyApkFile by remember { mutableStateOf<File?>(InAppUpdateManager.getExistingApk(context)) }
    var lastCheckedTime by remember { mutableStateOf<String?>(null) }
    var showNoNewUpdateDialog by remember { mutableStateOf(false) }
    var noNewUpdateInfo by remember { mutableStateOf<InAppUpdateManager.RemoteUpdateInfo?>(null) }

    val appVersionName = InAppUpdateManager.getInstalledVersionName(context)
    val appBuildNumber = InAppUpdateManager.CURRENT_VERSION_CODE.toString()

    fun checkForUpdates() {
        if (isUpdating || isCheckingUpdates) return
        isCheckingUpdates = true
        updateErrorText = null
        downloadProgress = null
        downloadStatusText = "Checking Google Drive for new updates..."

        scope.launch {
            val checkResult = InAppUpdateManager.checkRemoteVersion(context)
            val timeStr = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.US).format(Date())
            lastCheckedTime = timeStr

            checkResult.onSuccess { info ->
                isCheckingUpdates = false
                if (info.isDriveVersionHigher) {
                    // Google Drive version is GREATER than installed version:
                    // Download and proceed to install
                    isUpdating = true
                    downloadStatusText = "New version v${info.driveVersionName} found! Downloading APK..."
                    val result = InAppUpdateManager.downloadApk(context, targetVersionName = info.driveVersionName) { downloadedBytes, totalBytes ->
                        if (totalBytes > 0) {
                            val pct = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            downloadProgress = pct
                            val mbDownloaded = downloadedBytes / (1024f * 1024f)
                            val mbTotal = totalBytes / (1024f * 1024f)
                            downloadStatusText = "Downloading update v${info.driveVersionName}: ${String.format(Locale.US, "%.1f", mbDownloaded)} MB / ${String.format(Locale.US, "%.1f", mbTotal)} MB (${(pct * 100).toInt()}%)"
                        } else {
                            val mbDownloaded = downloadedBytes / (1024f * 1024f)
                            downloadProgress = null
                            downloadStatusText = "Downloading update v${info.driveVersionName}: ${String.format(Locale.US, "%.1f", mbDownloaded)} MB"
                        }
                    }

                    isUpdating = false
                    result.onSuccess { apkFile ->
                        readyApkFile = apkFile
                        downloadProgress = 1f
                        downloadStatusText = "✓ Download complete! Opening Android Package Installer..."

                        val installResult = InAppUpdateManager.triggerInstall(context, apkFile)
                        installResult.onFailure { err ->
                            updateErrorText = err.message ?: "Installer required permission or encountered an issue."
                        }
                    }.onFailure { error ->
                        updateErrorText = "Download failed: ${error.localizedMessage ?: "Unknown network error"}."
                        downloadStatusText = null
                    }
                } else {
                    // Installed version and Google Drive version are the same (or drive is not higher):
                    // DO NOT DOWNLOAD!
                    // Show popup: "No New Update"
                    isUpdating = false
                    downloadProgress = null
                    downloadStatusText = "✓ App is up to date (v${info.installedVersionName}). Google Drive version: v${info.driveVersionName}."
                    InAppUpdateManager.clearExistingApks(context)
                    readyApkFile = null
                    noNewUpdateInfo = info
                    showNoNewUpdateDialog = true
                }
            }.onFailure { error ->
                isCheckingUpdates = false
                isUpdating = false
                updateErrorText = "Failed to check update from Google Drive: ${error.localizedMessage ?: "Network error"}"
                downloadStatusText = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEA580C).copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "About Zoha Trading",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Version Badge Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Zoha Trading",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA580C),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Trading Worldwide Since 2012",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Slate700
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Version $appVersionName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ProfitGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Build $appBuildNumber • Stable",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Interactive In-App Updates Card (Connected to direct Google Drive APK Link)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Updates",
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "In-App Updates & Releases",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else PrimaryNavy
                            )
                        }

                        // Status message (shown only when downloading or ready to install)
                        val displayStatus = when {
                            downloadStatusText != null -> downloadStatusText
                            readyApkFile != null -> "✓ Update APK v$appVersionName is downloaded and ready to install."
                            else -> null
                        }

                        if (displayStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayStatus,
                                fontSize = 12.sp,
                                fontWeight = if (readyApkFile != null) FontWeight.Medium else FontWeight.Normal,
                                color = if (readyApkFile != null) ProfitGreen else (if (isDark) Color(0xFFCBD5E1) else Slate700)
                            )
                        }

                        // Progress Bar if downloading
                        if (isUpdating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (downloadProgress != null) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress!! },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = Color(0xFF2563EB),
                                    trackColor = if (isDark) Color(0xFF334155) else Color(0xFFDBEAFE)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = Color(0xFF2563EB),
                                    trackColor = if (isDark) Color(0xFF334155) else Color(0xFFDBEAFE)
                                )
                            }
                        }

                        // Error Banner if occurred
                        if (updateErrorText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ExpenseRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = updateErrorText!!,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFFCA5A5) else ExpenseRed
                                    )
                                }
                            }
                        }

                        if (lastCheckedTime != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Last updated: $lastCheckedTime",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons:
                        // If APK is downloaded and ready: Primary button is "Install Update Now"
                        if (readyApkFile != null && !isUpdating) {
                            Button(
                                onClick = {
                                    val installResult = InAppUpdateManager.triggerInstall(context, readyApkFile!!)
                                    installResult.onFailure { err ->
                                        updateErrorText = err.message ?: "Could not open installer."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProfitGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("btn_install_update_now")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InstallMobile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Install Update Now",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Primary "Check for Updates" button
                            Button(
                                onClick = { checkForUpdates() },
                                enabled = !isUpdating && !isCheckingUpdates,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("btn_check_for_updates")
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Downloading Update...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Checking Version...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check for Updates",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Cloud & Security Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cloud Sync Active",
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Slate700
                    )
                }

                // 5. Developer Credit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Design & Develop By ")
                            withStyle(
                                style = SpanStyle(
                                    color = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Jasim Uddin")
                            }
                        },
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF2563EB) else PrimaryNavy
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("about_dialog_close_btn")
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )

    // Popup: No New Update (shown when installed version matches or exceeds Google Drive version)
    if (showNoNewUpdateDialog) {
        val info = noNewUpdateInfo
        val instVer = info?.installedVersionName ?: appVersionName
        val driveVer = info?.driveVersionName ?: "1.0.1"

        AlertDialog(
            onDismissRequest = { showNoNewUpdateDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = ProfitGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "No New Update",
                            tint = ProfitGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "No New Update",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "You are currently running the latest version of Zoha Trading. No new update is required at this time.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Installed Version:",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "v$instVer",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Google Drive Version:",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "v$driveVer",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNoNewUpdateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProfitGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_no_new_update_ok")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        )
    }
}
