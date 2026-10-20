package com.example.data.repository

import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Firebase Realtime Multi-Device Synchronization Repository.
 * Provides live bi-directional sync contract between local Room SQLite and Firebase Realtime Database.
 */
interface FirebaseSyncRepository {
    val isCloudConnected: StateFlow<Boolean>
    val isInitialLoading: StateFlow<Boolean>
    val isPermissionDenied: StateFlow<Boolean>
    val syncErrorMessage: StateFlow<String?>
    val lastSyncTimestamp: StateFlow<Long>
    val databaseUrl: String

    fun startRealtimeSync()
    fun retrySync()
    fun ensureAuthenticated(onComplete: (Boolean) -> Unit = {})
    fun getCachedPin(userId: String): String?
    fun pushPhone(phone: PhoneItem)
    fun deletePhone(phoneId: Long)
    fun pushExpense(expense: ExpenseItem)
    fun deleteExpense(expenseId: Long)
    fun pushSettings(settings: AppSettings)
    fun pushSecurityPins(
        ownerPin: String,
        staff1Pin: String,
        staff2Pin: String,
        adminName: String = "Jasim",
        staff1Name: String = "Sumon",
        staff2Name: String = "Deedat"
    )
    suspend fun fetchAndSyncCloudPinsAndSettings()
    suspend fun pullAndPopulateAllFromCloud(): Result<Triple<Int, Int, Boolean>>
    suspend fun restoreAllFromCloud(): Result<Triple<Int, Int, Boolean>>
    suspend fun backupAllToCloud(): Result<Pair<Int, Int>>
    fun clearCloudInventoryAndExpenses()
    fun syncPendingOfflineChanges()
}
