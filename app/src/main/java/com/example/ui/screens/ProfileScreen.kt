package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.UserRole

/**
 * ProfileScreen acts as the user-facing Profile & Settings destination for Zoha Trading.
 * Completely unmounts the visual cloud sync status card while preserving automatic
 * background sync and Room SQLite offline-first local persistence.
 */
@Composable
fun ProfileScreen(
    settings: AppSettings,
    currentRole: UserRole,
    isBengali: Boolean = false,
    phones: List<PhoneItem> = emptyList(),
    expenses: List<ExpenseItem> = emptyList(),
    isCloudConnected: Boolean = false,
    isPermissionDenied: Boolean = false,
    onRetryCloudSync: () -> Unit = {},
    onSaveSettings: (AppSettings) -> Unit = {},
    onSwitchRole: (UserRole, String?) -> Unit = { _, _ -> },
    onUnlockAdminClick: () -> Unit = {},
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
    SettingsScreen(
        settings = settings,
        currentRole = currentRole,
        isBengali = isBengali,
        phones = phones,
        expenses = expenses,
        isCloudConnected = isCloudConnected,
        isPermissionDenied = isPermissionDenied,
        onRetryCloudSync = onRetryCloudSync,
        onSaveSettings = onSaveSettings,
        onSwitchRole = onSwitchRole,
        onUnlockAdminClick = onUnlockAdminClick,
        onLockAdminSession = onLockAdminSession,
        onLogout = onLogout,
        onResetDemoData = onResetDemoData,
        onSetThemeMode = onSetThemeMode,
        onSetDynamicColor = onSetDynamicColor,
        onRestoreFromCloud = onRestoreFromCloud,
        onBackupToCloud = onBackupToCloud,
        onRestoreBackupData = onRestoreBackupData,
        onRequireAdminAuth = onRequireAdminAuth,
        modifier = modifier
    )
}
