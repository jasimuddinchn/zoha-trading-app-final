package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.AppSettings
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
import com.example.data.model.ExpenseItem
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.sortedByEntryDateDescending
import com.example.data.model.sortedBySaleDateDescending
import com.example.data.repository.AppRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.StockRepository
import com.example.util.DateUtils
import com.example.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StaffStats(
    val staffName: String,
    val unitsSold: Int,
    val totalSalesBdt: Double
)

data class MonthlyStat(
    val monthYear: String, // e.g. "2026-08"
    val unitsSold: Int,
    val totalSalesBdt: Double,
    val totalCostBdt: Double,
    val grossProfitBdt: Double,
    val expensesBdt: Double,
    val netProfitBdt: Double
)

class PhoneHisabViewModel(application: Application) : AndroidViewModel(application) {
    val repository: AppRepository
    val firebaseSyncManager: FirebaseSyncManager
    val stockRepository: StockRepository
    val salesRepository: SalesRepository
    val expenseRepository: ExpenseRepository

    init {
        // Enforce clean session state on startup: user must log in afresh with profile PIN
        SessionManager.clearSession(application)

        val database = AppDatabase.getDatabase(application, viewModelScope)
        firebaseSyncManager = FirebaseSyncManager.getInstance(application, database, viewModelScope)
        firebaseSyncManager.startRealtimeSync()
        stockRepository = StockRepository(database, firebaseSyncManager, viewModelScope)
        salesRepository = SalesRepository(database, firebaseSyncManager, viewModelScope)
        expenseRepository = ExpenseRepository(database, firebaseSyncManager, viewModelScope)
        repository = AppRepository(database, firebaseSyncManager)
        
        // Proactively fetch latest cloud PINs & settings, and populate local Room DB from Firebase upon launch or reinstall
        viewModelScope.launch(Dispatchers.IO) {
            firebaseSyncManager.fetchAndSyncCloudPinsAndSettings()
            val phoneCount = database.phoneDao().getPhoneCount()
            val expenseCount = database.expenseDao().getExpenseCount()
            if (phoneCount == 0 || expenseCount == 0) {
                firebaseSyncManager.pullAndPopulateAllFromCloud()
            }
        }
    }

    // Live streams from DB & Firebase Sync
    val isCloudConnected: StateFlow<Boolean> = firebaseSyncManager.isCloudConnected
    val isInitialLoading: StateFlow<Boolean> = firebaseSyncManager.isInitialLoading
    val isPermissionDenied: StateFlow<Boolean> = firebaseSyncManager.isPermissionDenied
    val syncErrorMessage: StateFlow<String?> = firebaseSyncManager.syncErrorMessage
    val lastSyncTimestamp: StateFlow<Long> = firebaseSyncManager.lastSyncTimestamp
    val cloudDatabaseUrl: String = firebaseSyncManager.databaseUrl

    fun retryCloudSync() {
        firebaseSyncManager.retrySync()
    }

    val allPhones: StateFlow<List<PhoneItem>> = repository.allPhones
        .map { list ->
            list.distinctBy { phone -> phone.id }.sortedByEntryDateDescending()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExpenses: StateFlow<List<ExpenseItem>> = repository.allExpenses
        .map { list -> list.distinctBy { it.id }.sortedByDescending { it.entryDate } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .combine(MutableStateFlow(AppSettings())) { set, default -> set ?: default }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // Customer Dues & Due Collections Live Streams
    val customerDueProfiles: StateFlow<List<CustomerDueProfile>> = firebaseSyncManager.customerDueProfiles
    val totalOutstandingDue: StateFlow<Double> = firebaseSyncManager.totalOutstandingDue
    val allDuePayments: StateFlow<List<DuePaymentRecord>> = firebaseSyncManager.allDuePayments
    val totalDueCollectionsAmount: StateFlow<Double> = firebaseSyncManager.totalDueCollectionsAmount

    // Other Income Live StateFlows & CRUD
    val allOtherIncomes: StateFlow<List<OtherIncomeItem>> = firebaseSyncManager.allOtherIncomes
    val totalOtherIncomeAmount: StateFlow<Double> = firebaseSyncManager.totalOtherIncomeAmount

    fun addOtherIncome(
        category: String,
        amount: Double,
        date: String,
        notes: String = "",
        addedBy: String = "Staff"
    ) {
        viewModelScope.launch {
            val item = OtherIncomeItem(
                id = System.currentTimeMillis().toString(),
                category = category,
                amountBdt = amount,
                date = date,
                notes = notes,
                addedBy = addedBy,
                timestamp = System.currentTimeMillis()
            )
            firebaseSyncManager.postOtherIncome(item)
        }
    }

    fun updateOtherIncome(item: OtherIncomeItem) {
        viewModelScope.launch {
            firebaseSyncManager.updateOtherIncome(item)
        }
    }

    fun deleteOtherIncome(incomeId: String) {
        viewModelScope.launch {
            firebaseSyncManager.deleteOtherIncome(incomeId)
        }
    }

    // Multi-User Login & Session State
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Role & User state - Defaults to LOCKED on app start/restart to ensure security
    private val _currentRole = MutableStateFlow(UserRole.TEAM_MEMBER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _activeUserId = MutableStateFlow("") // "ADMIN", "STAFF_1", "STAFF_2"
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _activeStaffName = MutableStateFlow("Sumon")
    val activeStaffName: StateFlow<String> = _activeStaffName.asStateFlow()

    // Always false for Bengali - app is 100% English
    val isBengali: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    // Filters and search
    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("ALL") // ALL, IN_STOCK, IN_TRANSIT, SOLD
    val selectedBrandFilter = MutableStateFlow("ALL")

    // Filtered Phones list - strictly sorted:
    // In-Stock / In-Transit / All: newest entryDate first
    // Sold: newest saleDate first
    val filteredPhones: StateFlow<List<PhoneItem>> = combine(
        allPhones,
        searchQuery,
        selectedStatusFilter,
        selectedBrandFilter
    ) { phones, query, status, brand ->
        val filtered = phones.filter { phone ->
            val matchesQuery = query.isBlank() ||
                phone.brand.contains(query, ignoreCase = true) ||
                phone.model.contains(query, ignoreCase = true) ||
                phone.imei.contains(query, ignoreCase = true) ||
                phone.imei2.contains(query, ignoreCase = true) ||
                phone.color.contains(query, ignoreCase = true) ||
                phone.customerName.contains(query, ignoreCase = true) ||
                phone.customerPhone.contains(query, ignoreCase = true) ||
                phone.entryDate.contains(query, ignoreCase = true) ||
                DateUtils.formatDisplayDate(phone.entryDate).contains(query, ignoreCase = true) ||
                phone.saleDate.contains(query, ignoreCase = true) ||
                DateUtils.formatDisplayDate(phone.saleDate).contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                PhoneStatus.IN_STOCK.code, "STOCK" -> phone.status == PhoneStatus.IN_STOCK.code || phone.status == PhoneStatus.RETURNED.code || phone.status == "RETURN"
                PhoneStatus.IN_TRANSIT.code, "TRANSIT" -> phone.status == PhoneStatus.IN_TRANSIT.code || phone.status == "TRANSIT"
                PhoneStatus.SOLD.code, "SOLD" -> phone.status == PhoneStatus.SOLD.code || phone.status == "SOLD"
                else -> phone.status == status
            }

            val matchesBrand = when (brand) {
                "ALL" -> true
                else -> phone.brand.equals(brand, ignoreCase = true) || phone.brand.contains(brand, ignoreCase = true)
            }

            matchesQuery && matchesStatus && matchesBrand
        }

        if (status == PhoneStatus.SOLD.code || status == "SOLD") {
            filtered.sortedBySaleDateDescending()
        } else {
            filtered.sortedByEntryDateDescending()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setup Admin PIN on First Launch
    fun setupInitialAdminPin(newPin: String) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                ownerPin = newPin,
                isAdminPinSetupCompleted = true,
                appLanguage = "en"
            )
            repository.updateSettings(updated)
            firebaseSyncManager.pushSecurityPins(
                ownerPin = newPin,
                staff1Pin = updated.staff1Pin,
                staff2Pin = updated.staff2Pin,
                adminName = updated.adminName,
                staff1Name = updated.staff1Name,
                staff2Name = updated.staff2Name
            )
            _currentRole.value = UserRole.ADMIN
            _isAdminUnlocked.value = true
            _activeUserId.value = "ADMIN"
            _isUserLoggedIn.value = true
        }
    }

    // Explicitly logout / return to landing screen
    fun logoutToLanding() {
        SessionManager.clearSession(getApplication())
        _isUserLoggedIn.value = false
        _currentRole.value = UserRole.TEAM_MEMBER
        _isAdminUnlocked.value = false
        _activeUserId.value = ""
    }

    // Explicitly lock Admin Session (switch to staff or stay on current)
    fun lockAdminSession() {
        _currentRole.value = UserRole.TEAM_MEMBER
        _isAdminUnlocked.value = false
        _activeUserId.value = "STAFF_1"
        _activeStaffName.value = settings.value.getStaff1Name(false)
    }

    // Role & User Switching
    fun verifyAndUnlockUser(userId: String, enteredPin: String): Boolean {
        val currentSettings = settings.value
        val cloudPin = firebaseSyncManager.getCachedPin(userId)
        val isMatched = when (userId) {
            "ADMIN" -> {
                val correctPin = if (!cloudPin.isNullOrBlank()) cloudPin else currentSettings.ownerPin.ifBlank { "1111" }
                enteredPin == correctPin || enteredPin == currentSettings.ownerPin
            }
            "STAFF_1" -> {
                val correctPin = if (!cloudPin.isNullOrBlank()) cloudPin else currentSettings.staff1Pin.ifBlank { "2222" }
                enteredPin == correctPin || enteredPin == currentSettings.staff1Pin
            }
            "STAFF_2" -> {
                val correctPin = if (!cloudPin.isNullOrBlank()) cloudPin else currentSettings.staff2Pin.ifBlank { "3333" }
                enteredPin == correctPin || enteredPin == currentSettings.staff2Pin
            }
            else -> false
        }

        if (isMatched) {
            when (userId) {
                "ADMIN" -> {
                    _currentRole.value = UserRole.ADMIN
                    _isAdminUnlocked.value = true
                    _activeUserId.value = "ADMIN"
                    _isUserLoggedIn.value = true
                    SessionManager.saveTransientSession(getApplication(), "ADMIN")
                }
                "STAFF_1" -> {
                    _currentRole.value = UserRole.TEAM_MEMBER
                    _isAdminUnlocked.value = false
                    _activeUserId.value = "STAFF_1"
                    _activeStaffName.value = currentSettings.getStaff1Name(false)
                    _isUserLoggedIn.value = true
                    SessionManager.saveTransientSession(getApplication(), "STAFF_1")
                }
                "STAFF_2" -> {
                    _currentRole.value = UserRole.TEAM_MEMBER
                    _isAdminUnlocked.value = false
                    _activeUserId.value = "STAFF_2"
                    _activeStaffName.value = currentSettings.getStaff2Name(false)
                    _isUserLoggedIn.value = true
                    SessionManager.saveTransientSession(getApplication(), "STAFF_2")
                }
            }

            // Ensure local DB settings are synchronized with cloud PIN after logging in
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val syncedOwnerPin = firebaseSyncManager.getCachedPin("ADMIN") ?: currentSettings.ownerPin
                    val syncedStaff1Pin = firebaseSyncManager.getCachedPin("STAFF_1") ?: currentSettings.staff1Pin
                    val syncedStaff2Pin = firebaseSyncManager.getCachedPin("STAFF_2") ?: currentSettings.staff2Pin
                    if (syncedOwnerPin != currentSettings.ownerPin || syncedStaff1Pin != currentSettings.staff1Pin || syncedStaff2Pin != currentSettings.staff2Pin) {
                        val synced = currentSettings.copy(
                            ownerPin = syncedOwnerPin,
                            staff1Pin = syncedStaff1Pin,
                            staff2Pin = syncedStaff2Pin
                        )
                        repository.updateSettings(synced)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PhoneHisabViewModel", "Error synchronizing PIN after login: ${e.message}")
                }
            }
            return true
        }
        return false
    }

    fun updateSecurityPins(adminPin: String, staff1Pin: String, staff2Pin: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val current = settings.value
            val newAdmin = adminPin.ifBlank { "1111" }
            val newStaff1 = staff1Pin.ifBlank { "2222" }
            val newStaff2 = staff2Pin.ifBlank { "3333" }
            val updated = current.copy(
                ownerPin = newAdmin,
                staff1Pin = newStaff1,
                staff2Pin = newStaff2
            )
            repository.updateSettings(updated)
            firebaseSyncManager.pushSecurityPins(
                ownerPin = newAdmin,
                staff1Pin = newStaff1,
                staff2Pin = newStaff2,
                adminName = updated.adminName,
                staff1Name = updated.staff1Name,
                staff2Name = updated.staff2Name
            )
            onComplete()
        }
    }

    fun switchToTeamMember(name: String? = null) {
        _currentRole.value = UserRole.TEAM_MEMBER
        _isAdminUnlocked.value = false
        name?.let { _activeStaffName.value = it }
    }

    fun unlockUserWithBiometrics(userId: String) {
        val currentSettings = settings.value
        when (userId) {
            "ADMIN" -> {
                _currentRole.value = UserRole.ADMIN
                _isAdminUnlocked.value = true
                _activeUserId.value = "ADMIN"
                _isUserLoggedIn.value = true
                SessionManager.saveTransientSession(getApplication(), "ADMIN")
            }
            "STAFF_1" -> {
                _currentRole.value = UserRole.TEAM_MEMBER
                _isAdminUnlocked.value = false
                _activeUserId.value = "STAFF_1"
                _activeStaffName.value = currentSettings.getStaff1Name(false)
                _isUserLoggedIn.value = true
                SessionManager.saveTransientSession(getApplication(), "STAFF_1")
            }
            "STAFF_2" -> {
                _currentRole.value = UserRole.TEAM_MEMBER
                _isAdminUnlocked.value = false
                _activeUserId.value = "STAFF_2"
                _activeStaffName.value = currentSettings.getStaff2Name(false)
                _isUserLoggedIn.value = true
                SessionManager.saveTransientSession(getApplication(), "STAFF_2")
            }
            else -> {
                _currentRole.value = UserRole.ADMIN
                _isAdminUnlocked.value = true
                _activeUserId.value = "ADMIN"
                _isUserLoggedIn.value = true
                SessionManager.saveTransientSession(getApplication(), "ADMIN")
            }
        }
    }

    fun verifyAndUnlockAdmin(enteredPin: String): Boolean {
        return verifyAndUnlockUser("ADMIN", enteredPin)
    }

    /**
     * Verifies if the entered PIN matches the Admin PIN without altering the current user role/session.
     * Used globally for protected Edit, Delete, Expense, and System operations.
     */
    fun verifyAdminPin(enteredPin: String): Boolean {
        val currentSettings = settings.value
        val cloudPin = firebaseSyncManager.getCachedPin("ADMIN")
        val correctPin = if (!cloudPin.isNullOrBlank()) cloudPin else currentSettings.ownerPin.ifBlank { "1111" }
        val trimmed = enteredPin.trim()
        return trimmed == correctPin.trim() || trimmed == currentSettings.ownerPin.trim()
    }

    fun lockToTeamMember() {
        _currentRole.value = UserRole.TEAM_MEMBER
        _isAdminUnlocked.value = false
        _activeUserId.value = "STAFF_1"
        _activeStaffName.value = settings.value.getStaff1Name(false)
    }

    fun setActiveTeamMember(name: String) {
        _activeStaffName.value = name
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            val updated = settings.value.copy(appLanguage = "en")
            repository.updateSettings(updated)
        }
    }

    // Phone CRUD operations
    fun addPhone(
        brand: String,
        model: String,
        imei: String,
        imei2: String,
        color: String,
        ramRom: String,
        variant: String = "Global",
        batteryHealth: String = "100%",
        conditionGrade: String,
        itemCondition: String = "Used",
        packaging: String = "Full Box",
        entryDate: String,
        status: String,
        notes: String,
        buyingPriceRmb: Double,
        buyingPriceBdt: Double,
        shippingCostBdt: Double,
        otherCostBdt: Double,
        desiredProfitBdt: Double = 0.0
    ) {
        viewModelScope.launch {
            val cleanImei = imei.trim()
            if (cleanImei.isNotBlank()) {
                val currentList = allPhones.value
                val exists = currentList.any {
                    it.imei.equals(cleanImei, ignoreCase = true) ||
                    (!it.imei2.isNullOrBlank() && it.imei2.equals(cleanImei, ignoreCase = true))
                }
                if (exists) {
                    android.util.Log.w("PhoneHisabViewModel", "Rejecting duplicate IMEI: $cleanImei")
                    return@launch
                }
            }

            val totalCost = buyingPriceBdt + shippingCostBdt + otherCostBdt
            val newPhone = PhoneItem(
                brand = brand.trim(),
                model = model.trim(),
                imei = imei.trim(),
                imei2 = imei2.trim(),
                color = color.trim(),
                ramRom = ramRom.trim(),
                variant = variant.trim(),
                batteryHealth = batteryHealth.trim(),
                conditionGrade = conditionGrade,
                itemCondition = itemCondition.trim().ifBlank { "Used" },
                packaging = packaging.trim().ifBlank { "Full Box" },
                entryDate = if (entryDate.isBlank()) getTodayDate() else entryDate,
                status = status,
                notes = notes.trim(),
                buyingPriceRmb = buyingPriceRmb,
                buyingPriceBdt = buyingPriceBdt,
                shippingCostBdt = shippingCostBdt,
                otherCostBdt = otherCostBdt,
                totalCostBdt = totalCost,
                desiredProfitBdt = desiredProfitBdt,
                addedByRole = currentRole.value.name
            )
            stockRepository.insertPhone(newPhone)
        }
    }

    fun updatePhoneDetails(phone: PhoneItem) {
        viewModelScope.launch {
            val cleanImei = phone.imei.trim()
            if (cleanImei.isNotBlank()) {
                val currentList = allPhones.value
                val conflict = currentList.any {
                    it.id != phone.id && (
                        it.imei.equals(cleanImei, ignoreCase = true) ||
                        (!it.imei2.isNullOrBlank() && it.imei2.equals(cleanImei, ignoreCase = true))
                    )
                }
                if (conflict) {
                    android.util.Log.w("PhoneHisabViewModel", "Rejecting duplicate IMEI update: $cleanImei for id ${phone.id}")
                    return@launch
                }
            }
            val existing = repository.getPhoneById(phone.id) ?: stockRepository.getPhoneById(phone.id)
            val isTransitToStock = existing != null &&
                (existing.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) || existing.status.equals("TRANSIT", ignoreCase = true)) &&
                (phone.status.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) || phone.status.equals("IN_STOCK", ignoreCase = true) || phone.status.equals("STOCK", ignoreCase = true))

            val now = System.currentTimeMillis()
            val totalCost = phone.buyingPriceBdt + phone.shippingCostBdt + phone.otherCostBdt
            val updated = if (isTransitToStock) {
                val today = DateUtils.getTodayStorageDate()
                phone.copy(
                    totalCostBdt = totalCost,
                    customerName = phone.customerName.trim().uppercase(),
                    status = PhoneStatus.IN_STOCK.code,
                    entryDate = today,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                phone.copy(
                    totalCostBdt = totalCost,
                    customerName = phone.customerName.trim().uppercase(),
                    updatedAt = now
                )
            }
            stockRepository.updatePhone(updated)
            if (updated.status.equals(PhoneStatus.SOLD.code, ignoreCase = true) ||
                updated.status.equals("SOLD", ignoreCase = true)) {
                stockRepository.firebaseSyncManager?.syncSaleWithCustomerDue(updated)
            }
        }
    }

    fun updateOwnerCosts(
        phoneId: Long,
        buyingPriceRmb: Double,
        buyingPriceBdt: Double,
        shippingCostBdt: Double,
        otherCostBdt: Double,
        desiredProfitBdt: Double = 0.0
    ) {
        viewModelScope.launch {
            stockRepository.updateOwnerCosts(
                phoneId = phoneId,
                buyingPriceRmb = buyingPriceRmb,
                buyingPriceBdt = buyingPriceBdt,
                shippingCostBdt = shippingCostBdt,
                otherCostBdt = otherCostBdt,
                desiredProfitBdt = desiredProfitBdt
            )
        }
    }

    fun sellPhone(
        phoneId: Long,
        sellingPriceBdt: Double,
        cashReceivedBdt: Double = sellingPriceBdt,
        dueAmountBdt: Double = (sellingPriceBdt - cashReceivedBdt).coerceAtLeast(0.0),
        saleDate: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        soldByStaff: String,
        paymentMethod: String,
        warrantyDays: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            salesRepository.recordSale(
                phoneId = phoneId,
                sellingPriceBdt = sellingPriceBdt,
                cashReceivedBdt = cashReceivedBdt,
                dueAmountBdt = dueAmountBdt,
                saleDate = if (saleDate.isBlank()) getTodayDate() else saleDate,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                soldByStaff = if (soldByStaff.isBlank()) activeStaffName.value else soldByStaff,
                paymentMethod = paymentMethod,
                warrantyDays = warrantyDays,
                notes = notes
            )
        }
    }

    fun collectCustomerDue(
        customerKey: String,
        customerName: String,
        customerPhone: String,
        amount: Double,
        paymentMethod: String = "Cash",
        collectedBy: String = activeStaffName.value,
        date: String = getTodayDate(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            salesRepository.collectCustomerDue(
                customerKey = customerKey,
                customerName = customerName,
                customerPhone = customerPhone,
                amount = amount,
                paymentMethod = paymentMethod,
                collectedBy = collectedBy,
                date = date,
                notes = notes
            )
        }
    }

    fun addExistingDue(
        customerName: String,
        customerPhone: String,
        amount: Double,
        date: String = getTodayDate(),
        notes: String = "",
        addedByStaff: String = activeStaffName.value
    ) {
        viewModelScope.launch {
            salesRepository.addExistingDue(
                customerName = customerName,
                customerPhone = customerPhone,
                amount = amount,
                date = date,
                notes = notes,
                addedByStaff = addedByStaff
            )
        }
    }

    fun deleteCustomerDue(customerKey: String, dueId: String) {
        viewModelScope.launch {
            salesRepository.deleteCustomerDue(customerKey, dueId)
        }
    }

    fun deleteDuePayment(customerKey: String, paymentId: String) {
        viewModelScope.launch {
            salesRepository.deleteDuePayment(customerKey, paymentId)
        }
    }

    fun updateCustomerDueRecord(
        customerKey: String,
        dueId: String,
        newDueAmount: Double,
        newTotalSalePrice: Double,
        newCashReceived: Double,
        newDate: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            salesRepository.updateCustomerDueRecord(
                customerKey,
                dueId,
                newDueAmount,
                newTotalSalePrice,
                newCashReceived,
                newDate,
                newNotes
            )
        }
    }

    fun updateDuePaymentRecord(
        customerKey: String,
        paymentId: String,
        newAmount: Double,
        newPaymentMethod: String,
        newCollectedBy: String,
        newDate: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            salesRepository.updateDuePaymentRecord(
                customerKey,
                paymentId,
                newAmount,
                newPaymentMethod,
                newCollectedBy,
                newDate,
                newNotes
            )
        }
    }

    fun updateCustomerDetails(customerKey: String, newName: String, newPhone: String) {
        viewModelScope.launch {
            salesRepository.updateCustomerDetails(customerKey, newName, newPhone)
        }
    }

    fun collectDue(
        phoneId: Long,
        collectedAmount: Double,
        paymentMethod: String,
        collectedBy: String = activeStaffName.value,
        date: String = getTodayDate()
    ) {
        viewModelScope.launch {
            salesRepository.collectDue(
                phoneId = phoneId,
                collectedAmount = collectedAmount,
                paymentMethod = paymentMethod,
                collectedBy = collectedBy,
                date = date
            )
        }
    }

    fun markStatus(phoneId: Long, newStatus: String) {
        viewModelScope.launch {
            val phone = repository.getPhoneById(phoneId) ?: stockRepository.getPhoneById(phoneId) ?: return@launch
            val isTransitToStock = (phone.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
                phone.status.equals("TRANSIT", ignoreCase = true)) &&
                (newStatus.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) ||
                newStatus.equals("IN_STOCK", ignoreCase = true) ||
                newStatus.equals("STOCK", ignoreCase = true))

            val now = System.currentTimeMillis()
            val finalStatus = if (phone.status == PhoneStatus.SOLD.code && (newStatus == PhoneStatus.IN_STOCK.code || newStatus == PhoneStatus.RETURNED.code)) {
                PhoneStatus.RETURNED.code
            } else {
                newStatus
            }

            val updatedPhone = if (isTransitToStock) {
                val today = DateUtils.getTodayStorageDate()
                phone.copy(
                    status = PhoneStatus.IN_STOCK.code,
                    entryDate = today,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                phone.copy(status = finalStatus, updatedAt = now)
            }
            stockRepository.updatePhone(updatedPhone)
        }
    }

    fun changePhoneStatus(phone: PhoneItem, targetStatus: String) {
        viewModelScope.launch {
            val isTransitToStock = (phone.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
                phone.status.equals("TRANSIT", ignoreCase = true)) &&
                (targetStatus.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) ||
                targetStatus.equals("IN_STOCK", ignoreCase = true) ||
                targetStatus.equals("STOCK", ignoreCase = true))

            val now = System.currentTimeMillis()
            val finalStatus = if (phone.status == PhoneStatus.SOLD.code && (targetStatus == PhoneStatus.IN_STOCK.code || targetStatus == PhoneStatus.RETURNED.code)) {
                PhoneStatus.RETURNED.code
            } else {
                targetStatus
            }

            val updatedPhone = if (isTransitToStock) {
                val today = DateUtils.getTodayStorageDate()
                phone.copy(
                    status = PhoneStatus.IN_STOCK.code,
                    entryDate = today,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                phone.copy(status = finalStatus, updatedAt = now)
            }
            stockRepository.updatePhone(updatedPhone)
        }
    }

    fun deletePhone(phone: PhoneItem) {
        viewModelScope.launch {
            stockRepository.deletePhone(phone)
        }
    }

    // Expense operations
    fun addExpense(
        title: String,
        category: String,
        amountBdt: Double,
        date: String,
        notes: String,
        receiptUrl: String? = null
    ) {
        viewModelScope.launch {
            val expense = ExpenseItem(
                title = title.trim(),
                category = category,
                amountBdt = amountBdt,
                date = if (date.isBlank()) getTodayDate() else date,
                notes = notes.trim(),
                addedBy = if (currentRole.value == UserRole.ADMIN) "Admin" else activeStaffName.value,
                receiptUrl = receiptUrl
            )
            expenseRepository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    fun updateExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
        }
    }

    fun revertSaleToStock(phone: PhoneItem) {
        viewModelScope.launch {
            salesRepository.revertSale(phone)
        }
    }

    fun updateOpeningBalance(amount: Double) {
        viewModelScope.launch {
            DashboardViewModel.saveStartingBalance(getApplication(), amount)
        }
    }

    // Settings
    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings.copy(appLanguage = "en"))
        }
    }

    fun toggleThemeMode() {
        val current = settings.value
        val isCurrentDark = when (current.themeMode.uppercase()) {
            "DARK" -> true
            "LIGHT" -> false
            else -> false
        }
        val nextMode = if (isCurrentDark) "LIGHT" else "DARK"
        updateSettings(current.copy(themeMode = nextMode))
    }

    fun setThemeMode(mode: String) {
        val current = settings.value
        updateSettings(current.copy(themeMode = mode))
    }

    fun setDynamicColor(enabled: Boolean) {
        val current = settings.value
        updateSettings(current.copy(dynamicColor = enabled))
    }

    fun clearAllData(clearCloud: Boolean = true) {
        viewModelScope.launch {
            repository.clearAllInventoryAndExpenses(clearCloud)
        }
    }

    fun resetDemoData() {
        clearAllData(clearCloud = true)
    }

    fun forceRestoreFromCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreAllFromCloud()
            result.onSuccess { triple ->
                val (phones, expenses, settingsUpdated) = triple
                val msg = "Cloud sync successful! Restored $phones phones, $expenses expenses" + if (settingsUpdated) " & updated shop settings." else "."
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, "Restore failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun forceBackupToCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.backupAllToCloud()
            result.onSuccess { pair ->
                val (phones, expenses) = pair
                val msg = "Cloud backup complete! Saved $phones phones, $expenses expenses & settings."
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, "Backup failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreBackupData(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        overwrite: Boolean,
        settings: AppSettings? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.importDataBundle(phones, expenses, overwrite, settings)
            result.onSuccess { pair ->
                val (pCount, eCount) = pair
                val mode = if (overwrite) "Replaced & Restored" else "Merged & Imported"
                onResult(true, "Successfully $mode $pCount phone records & $eCount expenses.")
            }.onFailure { err ->
                onResult(false, "Import failed: ${err.localizedMessage ?: "Invalid data format"}")
            }
        }
    }

    // Currency helper
    fun convertRmbToBdt(rmb: Double): Double {
        return rmb * settings.value.rmbToBdtRate
    }

    fun convertBdtToRmb(bdt: Double): Double {
        val rate = settings.value.rmbToBdtRate
        return if (rate > 0) bdt / rate else 0.0
    }

    override fun onCleared() {
        super.onCleared()
        SessionManager.clearSession(getApplication())
        stockRepository.cleanUp()
        salesRepository.cleanUp()
        expenseRepository.cleanUp()
    }

    companion object {
        fun getTodayDate(): String {
            return com.example.util.DateUtils.getTodayStorageDate()
        }
    }
}
