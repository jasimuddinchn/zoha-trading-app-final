package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
import com.example.data.repository.SalesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel for Customer Due Management & Due Collections Ledger.
 * Provides deduplicated customer profiles, live synchronization, search/filter,
 * and admin controls without duplicate listener stacking or entry duplication.
 */
class DueViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val firebaseSyncManager = FirebaseSyncManager.getInstance(application, database, viewModelScope)
    val salesRepository = SalesRepository(database, firebaseSyncManager, viewModelScope)

    // Customer Due Profiles uniquely mapped by customerKey
    val customerDueProfiles: StateFlow<List<CustomerDueProfile>> = firebaseSyncManager.customerDueProfiles
        .combine(MutableStateFlow(Unit)) { list, _ -> list.distinctBy { it.customerKey } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Total aggregate due amount at top banner
    val totalOutstandingDue: StateFlow<Double> = firebaseSyncManager.totalOutstandingDue

    // All collected payments uniquely mapped by id
    val allDuePayments: StateFlow<List<DuePaymentRecord>> = firebaseSyncManager.allDuePayments
        .combine(MutableStateFlow(Unit)) { list, _ -> list.distinctBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totalDueCollectionsAmount: StateFlow<Double> = firebaseSyncManager.totalDueCollectionsAmount

    // Search and filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyDueCustomers = MutableStateFlow(true)
    val filterOnlyDueCustomers: StateFlow<Boolean> = _filterOnlyDueCustomers.asStateFlow()

    val filteredCustomerDues: StateFlow<List<CustomerDueProfile>> = combine(
        customerDueProfiles,
        _searchQuery,
        _filterOnlyDueCustomers
    ) { profiles, query, onlyDue ->
        val distinct = profiles.distinctBy { it.customerKey }
        distinct.filter { profile ->
            val matchesFilter = if (onlyDue) profile.remainingDue > 0.0 else true
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOnlyDue(onlyDue: Boolean) {
        _filterOnlyDueCustomers.value = onlyDue
    }

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
                customerKey = customerKey,
                dueId = dueId,
                newDueAmount = newDueAmount,
                newTotalSalePrice = newTotalSalePrice,
                newCashReceived = newCashReceived,
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
