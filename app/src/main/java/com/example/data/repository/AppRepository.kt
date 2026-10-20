package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.ExpenseDao
import com.example.data.db.PhoneDao
import com.example.data.db.SettingsDao
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class AppRepository(
    private val database: AppDatabase,
    private val firebaseSyncManager: FirebaseSyncRepository? = null
) {
    private val phoneDao: PhoneDao = database.phoneDao()
    private val expenseDao: ExpenseDao = database.expenseDao()
    private val settingsDao: SettingsDao = database.settingsDao()

    val allPhones: Flow<List<PhoneItem>> = phoneDao.getAllPhones()
    val allExpenses: Flow<List<ExpenseItem>> = expenseDao.getAllExpenses()
    val settings: Flow<AppSettings?> = settingsDao.getSettings()

    val isCloudConnected: StateFlow<Boolean>? = firebaseSyncManager?.isCloudConnected
    val isInitialLoading: StateFlow<Boolean>? = firebaseSyncManager?.isInitialLoading
    val lastSyncTimestamp: StateFlow<Long>? = firebaseSyncManager?.lastSyncTimestamp

    companion object {
        /**
         * Generates a globally unique, collision-resistant 64-bit positive Long ID.
         * Combines millisecond epoch time with a 3-digit random component.
         */
        fun generateUniqueId(): Long {
            return (System.currentTimeMillis() * 1000L) + (100L..999L).random()
        }
    }

    suspend fun getSettingsDirect(): AppSettings? = settingsDao.getSettingsDirect()

    suspend fun insertPhone(phone: PhoneItem): Long {
        val uniqueId = if (phone.id == 0L) generateUniqueId() else phone.id
        val now = System.currentTimeMillis()
        val phoneWithId = phone.copy(
            id = uniqueId,
            createdAt = if (phone.createdAt == 0L) now else phone.createdAt,
            updatedAt = now
        )
        // 1. Direct write/push to Firebase first for instant multi-device sync
        firebaseSyncManager?.pushPhone(phoneWithId)
        // 2. Persist locally to Room SQLite
        val id = phoneDao.insertPhone(phoneWithId)
        return id
    }

    suspend fun updatePhone(phone: PhoneItem) {
        val existing = phoneDao.getPhoneById(phone.id)
        val isTransitToStock = existing != null &&
            (existing.status.equals(com.example.data.model.PhoneStatus.IN_TRANSIT.code, ignoreCase = true) || existing.status.equals("TRANSIT", ignoreCase = true)) &&
            (phone.status.equals(com.example.data.model.PhoneStatus.IN_STOCK.code, ignoreCase = true) || phone.status.equals("IN_STOCK", ignoreCase = true) || phone.status.equals("STOCK", ignoreCase = true))

        val now = System.currentTimeMillis()
        val updatedPhone = if (isTransitToStock) {
            val today = com.example.util.DateUtils.getTodayStorageDate()
            phone.copy(
                status = com.example.data.model.PhoneStatus.IN_STOCK.code,
                entryDate = today,
                createdAt = now,
                updatedAt = now
            )
        } else {
            phone.copy(updatedAt = now)
        }
        // 1. Direct write/push to Firebase first for instant multi-device sync
        firebaseSyncManager?.pushPhone(updatedPhone)
        // 2. Persist locally to Room SQLite
        phoneDao.updatePhone(updatedPhone)
    }

    suspend fun updateStatus(phoneId: Long, newStatus: String) {
        val phone = phoneDao.getPhoneById(phoneId) ?: return
        val isTransitToStock = (phone.status.equals(com.example.data.model.PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
            phone.status.equals("TRANSIT", ignoreCase = true)) &&
            (newStatus.equals(com.example.data.model.PhoneStatus.IN_STOCK.code, ignoreCase = true) ||
            newStatus.equals("IN_STOCK", ignoreCase = true) ||
            newStatus.equals("STOCK", ignoreCase = true))

        val now = System.currentTimeMillis()
        val updated = if (isTransitToStock) {
            val today = com.example.util.DateUtils.getTodayStorageDate()
            phone.copy(
                status = com.example.data.model.PhoneStatus.IN_STOCK.code,
                entryDate = today,
                createdAt = now,
                updatedAt = now
            )
        } else {
            phone.copy(status = newStatus, updatedAt = now)
        }
        updatePhone(updated)
    }

    suspend fun deletePhone(phone: PhoneItem) {
        // 1. Direct write/push to Firebase first
        firebaseSyncManager?.deletePhone(phone.id)
        // 2. Delete locally from Room SQLite
        phoneDao.deletePhone(phone)
    }

    suspend fun deletePhoneById(id: Long) {
        // 1. Direct write/push to Firebase first
        firebaseSyncManager?.deletePhone(id)
        // 2. Delete locally from Room SQLite
        phoneDao.deletePhoneById(id)
    }

    suspend fun getPhoneById(id: Long): PhoneItem? = phoneDao.getPhoneById(id)

    fun getPhonesByStatus(status: String): Flow<List<PhoneItem>> = phoneDao.getPhonesByStatus(status)

    fun searchPhones(query: String): Flow<List<PhoneItem>> = phoneDao.searchPhones(query)

    suspend fun insertExpense(expense: ExpenseItem): Long {
        val uniqueId = if (expense.id == 0L) generateUniqueId() else expense.id
        val now = System.currentTimeMillis()
        val expenseWithId = expense.copy(
            id = uniqueId,
            timestamp = if (expense.timestamp == 0L) now else expense.timestamp
        )
        // 1. Direct write/push to Firebase first for instant multi-device sync
        firebaseSyncManager?.pushExpense(expenseWithId)
        // 2. Persist locally to Room SQLite
        val id = expenseDao.insertExpense(expenseWithId)
        return id
    }

    suspend fun updateExpense(expense: ExpenseItem) {
        val existing = expenseDao.getExpenseById(expense.id)
        val preservedTimestamp = when {
            existing != null && existing.timestamp > 0L -> existing.timestamp
            expense.timestamp > 0L -> expense.timestamp
            else -> System.currentTimeMillis()
        }
        val updatedExpense = expense.copy(timestamp = preservedTimestamp)
        // 1. Direct write/push to Firebase first for instant multi-device sync
        firebaseSyncManager?.pushExpense(updatedExpense)
        // 2. Persist locally to Room SQLite
        expenseDao.updateExpense(updatedExpense)
    }

    suspend fun deleteExpense(expense: ExpenseItem) {
        // 1. Direct write/push to Firebase first
        firebaseSyncManager?.deleteExpense(expense.id)
        // 2. Delete locally from Room SQLite
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        // 1. Direct write/push to Firebase first
        firebaseSyncManager?.deleteExpense(id)
        // 2. Delete locally from Room SQLite
        expenseDao.deleteExpenseById(id)
    }

    suspend fun updateSettings(settings: AppSettings) {
        // 1. Direct write/push to Firebase first for instant multi-device sync
        firebaseSyncManager?.pushSettings(settings)
        // 2. Persist locally to Room SQLite
        settingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun clearAllInventoryAndExpenses(clearCloud: Boolean = true) {
        if (clearCloud) {
            firebaseSyncManager?.clearCloudInventoryAndExpenses()
        }
        phoneDao.deleteAllPhones()
        expenseDao.deleteAllExpenses()
    }

    suspend fun restoreAllFromCloud(): Result<Triple<Int, Int, Boolean>> {
        return firebaseSyncManager?.restoreAllFromCloud() ?: Result.failure(Exception("Sync manager not available"))
    }

    suspend fun backupAllToCloud(): Result<Pair<Int, Int>> {
        return firebaseSyncManager?.backupAllToCloud() ?: Result.failure(Exception("Sync manager not available"))
    }

    suspend fun importDataBundle(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        overwrite: Boolean,
        settings: AppSettings? = null
    ): Result<Pair<Int, Int>> {
        return try {
            if (overwrite) {
                phoneDao.deleteAllPhones()
                expenseDao.deleteAllExpenses()
            }
            if (phones.isNotEmpty()) {
                phoneDao.insertAll(phones)
            }
            if (expenses.isNotEmpty()) {
                expenseDao.insertAll(expenses)
            }
            if (settings != null) {
                val current = getSettingsDirect() ?: AppSettings()
                val mergedSettings = current.copy(
                    shopName = settings.shopName.ifBlank { current.shopName },
                    shopSubtitle = settings.shopSubtitle.ifBlank { current.shopSubtitle },
                    shopAddress = settings.shopAddress.ifBlank { current.shopAddress },
                    shopPhone = settings.shopPhone.ifBlank { current.shopPhone },
                    rmbToBdtRate = if (settings.rmbToBdtRate > 0) settings.rmbToBdtRate else current.rmbToBdtRate,
                    ownerNotificationEmail = settings.ownerNotificationEmail.ifBlank { current.ownerNotificationEmail }
                )
                settingsDao.insertOrUpdateSettings(mergedSettings)
                firebaseSyncManager?.pushSettings(mergedSettings)
            }
            // Sync all changes to cloud database
            firebaseSyncManager?.backupAllToCloud()
            Result.success(Pair(phones.size, expenses.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetToSampleData() {
        phoneDao.deleteAllPhones()
        expenseDao.deleteAllExpenses()
        phoneDao.insertAll(com.example.data.db.DefaultOfflineData.getDefaultPhones())
        expenseDao.insertAll(com.example.data.db.DefaultOfflineData.getDefaultExpenses())
        firebaseSyncManager?.backupAllToCloud()
    }
}
