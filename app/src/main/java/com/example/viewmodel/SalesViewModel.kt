package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing Sales, Customer Due Management Ledger, and Cash Reconciliation.
 */
class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val firebaseSyncManager = FirebaseSyncManager.getInstance(application, database, viewModelScope)
    val salesRepository = SalesRepository(database, firebaseSyncManager, viewModelScope)

    val soldPhones: StateFlow<List<PhoneItem>> = salesRepository.salesPhones
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.distinctBy { it.id }.sortedWith(
                compareByDescending<PhoneItem> {
                    val d = it.saleDate.ifBlank { it.entryDate }.trim()
                    com.example.util.DateUtils.parseDateToMillis(d)
                }.thenByDescending { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Live synced customer due profiles from Firebase Realtime Database (/customer_dues) uniquely mapped
    val customerDueProfiles: StateFlow<List<CustomerDueProfile>> = firebaseSyncManager.customerDueProfiles
        .combine(MutableStateFlow(Unit)) { list, _ -> list.distinctBy { it.customerKey } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Total aggregate due amount at the top
    val totalOutstandingDue: StateFlow<Double> = firebaseSyncManager.totalOutstandingDue

    // All collected payments from /financials/due_collections uniquely mapped
    val allDuePayments: StateFlow<List<DuePaymentRecord>> = firebaseSyncManager.allDuePayments
        .combine(MutableStateFlow(Unit)) { list, _ -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totalDueCollectionsAmount: StateFlow<Double> = firebaseSyncManager.totalDueCollectionsAmount

    // Other Income Live StateFlows
    val allOtherIncomes: StateFlow<List<OtherIncomeItem>> = firebaseSyncManager.allOtherIncomes
    val totalOtherIncomeAmount: StateFlow<Double> = firebaseSyncManager.totalOtherIncomeAmount

    fun addOtherIncome(
        category: String,
        amount: Double,
        date: String,
        notes: String = "",
        addedBy: String = "Admin"
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

    companion object {
        const val CASH_IN_ALL = "ALL"
        const val CASH_IN_SET_SALE = "SET_SALES"
        const val CASH_IN_DIRECT_CASH = "OTHER_INCOME"

        const val TIME_FLOW_ALL_DATES = "ALL_DATES"
        const val TIME_FLOW_THIS_MONTH = "THIS_MONTH"
        const val TIME_FLOW_LAST_MONTH = "LAST_MONTH"
        const val TIME_FLOW_SELECT_MONTH = "SELECT_MONTH"
        const val TIME_FLOW_TODAY = "TODAY"
        const val TIME_FLOW_SELECT_DATE = "SELECT_DATE"

        fun getCashInLabel(filter: String): String {
            return when (filter) {
                CASH_IN_SET_SALE -> "Set Sale"
                CASH_IN_DIRECT_CASH -> "Direct Cash"
                else -> "All"
            }
        }

        fun getTimeFlowLabel(
            filter: String,
            currentYm: String = "",
            lastYm: String = "",
            todayDate: String = ""
        ): String {
            return when {
                filter.isBlank() || filter == "ALL" || filter == TIME_FLOW_ALL_DATES -> "All dates"
                filter == currentYm || filter == TIME_FLOW_THIS_MONTH -> "This Month"
                filter == lastYm || filter == TIME_FLOW_LAST_MONTH -> "Last month"
                filter == todayDate || filter == TIME_FLOW_TODAY -> "Today"
                filter.length == 7 && filter.contains("-") -> com.example.util.MonthFilterHelper.formatMonthDisplayName(filter)
                else -> com.example.util.DateUtils.formatDisplayDate(filter)
            }
        }
    }

    /**
     * Dynamic Calculations:
     * Calculates the total amount for the active transaction filter ("SET_SALES", "OTHER_INCOME" / "DIRECT_CASH", or "ALL")
     * based on the active date/search filtered items.
     */
    fun calculateTabRevenue(
        tab: String,
        filteredSoldPhones: List<PhoneItem>,
        filteredIncomes: List<OtherIncomeItem>
    ): Double {
        return when (tab) {
            CASH_IN_SET_SALE -> filteredSoldPhones.sumOf { it.sellingPriceBdt }
            CASH_IN_DIRECT_CASH -> filteredIncomes.sumOf { it.amountBdt }
            else -> filteredSoldPhones.sumOf { it.sellingPriceBdt } + filteredIncomes.sumOf { it.amountBdt }
        }
    }

    fun calculateTabProfit(
        tab: String,
        filteredSoldPhones: List<PhoneItem>,
        filteredIncomes: List<OtherIncomeItem>
    ): Double {
        return when (tab) {
            CASH_IN_SET_SALE -> filteredSoldPhones.sumOf { it.netProfitBdt }
            CASH_IN_DIRECT_CASH -> filteredIncomes.sumOf { it.amountBdt }
            else -> filteredSoldPhones.sumOf { it.netProfitBdt } + filteredIncomes.sumOf { it.amountBdt }
        }
    }

    fun calculateCombinedRevenue(
        cashInFilter: String,
        soldPhones: List<PhoneItem>,
        incomes: List<OtherIncomeItem>
    ): Double = calculateTabRevenue(cashInFilter, soldPhones, incomes)

    fun calculateCombinedProfit(
        cashInFilter: String,
        soldPhones: List<PhoneItem>,
        incomes: List<OtherIncomeItem>
    ): Double = calculateTabProfit(cashInFilter, soldPhones, incomes)

    // Search and filter for Dues
    val dueSearchQuery = MutableStateFlow("")
    val dueFilterOnlyOutstanding = MutableStateFlow(true) // true: only remainingDue > 0

    val filteredCustomerDues: StateFlow<List<CustomerDueProfile>> = combine(
        customerDueProfiles,
        dueSearchQuery,
        dueFilterOnlyOutstanding
    ) { profiles, query, onlyOutstanding ->
        val distinct = profiles.distinctBy { it.customerKey }
        distinct.filter { profile ->
            val matchesFilter = if (onlyOutstanding) profile.remainingDue > 0.0 else true
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                profile.customerName.lowercase().contains(q) ||
                profile.customerPhone.contains(q) ||
                profile.creditSales.any {
                    it.phoneModel.lowercase().contains(q) || it.imei.contains(q)
                }
            }
            matchesFilter && matchesQuery
        }.distinctBy { it.customerKey }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Record a new mobile sale with complete Partial/Credit Breakdown:
     * - Captures Total Sale Price and Cash Received.
     * - Due Amount = Total Sale Price - Cash Received.
     * - Adds ONLY Cash Received to Monthly Cash In and Total Net Balance.
     * - If Due Amount > 0, posts due record automatically under customer profile in Firebase (/customer_dues).
     */
    fun recordSale(
        phoneId: Long,
        totalSalePrice: Double,
        cashReceived: Double,
        dueAmount: Double,
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
                sellingPriceBdt = totalSalePrice,
                cashReceivedBdt = cashReceived,
                dueAmountBdt = dueAmount,
                saleDate = saleDate,
                customerName = customerName.trim().uppercase(),
                customerPhone = customerPhone.trim(),
                customerEmail = customerEmail.trim(),
                soldByStaff = soldByStaff,
                paymentMethod = paymentMethod,
                warrantyDays = warrantyDays,
                notes = notes
            )
        }
    }

    /**
     * Collect Due Payment from customer:
     * - Deducts amount from customer's outstanding due balance.
     * - EXCLUDES collected amount from Monthly Cash In (preventing false inflation of current sales/revenue).
     * - ADDS collected amount directly to Dynamic Total Net Balance.
     */
    fun collectDuePayment(
        customerKey: String,
        customerName: String,
        customerPhone: String,
        amount: Double,
        paymentMethod: String = "Cash",
        collectedBy: String = "Staff",
        date: String = CustomerDueUtils.getTodayDate(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            salesRepository.collectCustomerDue(
                customerKey = customerKey,
                customerName = customerName.trim().uppercase(),
                customerPhone = customerPhone.trim(),
                amount = amount,
                paymentMethod = paymentMethod,
                collectedBy = collectedBy,
                date = date,
                notes = notes
            )
        }
    }

    fun revertSaleToStock(phone: PhoneItem) {
        viewModelScope.launch {
            salesRepository.revertSale(phone)
        }
    }

    /**
     * Direct Due Entry / Adjustment of historical dues without a phone sale:
     * - Captures Name, Phone, Due Amount, Date, and Note.
     * - Posts due record automatically under customer profile in Firebase (/customer_dues).
     */
    fun addExistingDue(
        customerName: String,
        customerPhone: String,
        amount: Double,
        date: String = CustomerDueUtils.getTodayDate(),
        notes: String = "",
        addedByStaff: String = "Admin"
    ) {
        viewModelScope.launch {
            salesRepository.addExistingDue(
                customerName = customerName.trim().uppercase(),
                customerPhone = customerPhone.trim(),
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

    fun updateSale(phone: PhoneItem) {
        viewModelScope.launch {
            salesRepository.updateSale(phone)
        }
    }

    fun updateCustomerDetails(customerKey: String, newName: String, newPhone: String) {
        viewModelScope.launch {
            salesRepository.updateCustomerDetails(customerKey, newName.trim().uppercase(), newPhone.trim())
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
                customerKey = customerKey,
                paymentId = paymentId,
                newAmount = newAmount,
                newPaymentMethod = newPaymentMethod,
                newCollectedBy = newCollectedBy,
                newDate = newDate,
                newNotes = newNotes
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        salesRepository.cleanUp()
    }
}
