package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.sortedByEntryDateDescending
import com.example.data.model.sortedBySaleDateDescending
import com.example.data.repository.AppRepository
import com.example.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dedicated Stock & Inventory ViewModel handling instant real-time multi-device phone sync.
 * Writes DIRECTLY to Firebase Realtime Database first on every operation.
 */
class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(application, database, viewModelScope)
    val stockRepository = StockRepository(database, firebaseSyncManager, viewModelScope)
    private val appRepository = AppRepository(database, firebaseSyncManager)

    init {
        firebaseSyncManager.startRealtimeSync()
    }

    val isCloudConnected: StateFlow<Boolean> = firebaseSyncManager.isCloudConnected
    val lastSyncTimestamp: StateFlow<Long> = firebaseSyncManager.lastSyncTimestamp

    val allPhones: StateFlow<List<PhoneItem>> = stockRepository.allPhones
        .map { list ->
            list.distinctBy { phone -> phone.id }.sortedByEntryDateDescending()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val liveStockPhones: StateFlow<List<PhoneItem>> = stockRepository.stockPhones
    val liveTransitPhones: StateFlow<List<PhoneItem>> = stockRepository.transitPhones

    val stockItems: StateFlow<List<PhoneItem>> = allPhones.map { phones ->
        phones.filter { it.status == PhoneStatus.IN_STOCK.code || it.status == "IN_STOCK" || it.status == PhoneStatus.RETURNED.code || it.status == "RETURN" }
            .sortedByEntryDateDescending()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transitItems: StateFlow<List<PhoneItem>> = allPhones.map { phones ->
        phones.filter { it.status == PhoneStatus.IN_TRANSIT.code || it.status == "IN_TRANSIT" || it.status == "TRANSIT" }
            .sortedByEntryDateDescending()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val salesRecords: StateFlow<List<PhoneItem>> = allPhones.map { phones ->
        phones.filter { it.status == PhoneStatus.SOLD.code || it.status == "SOLD" }
            .sortedBySaleDateDescending()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("ALL")
    val selectedBrandFilter = MutableStateFlow("ALL")

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
                phone.color.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                PhoneStatus.IN_STOCK.code, "STOCK" -> phone.status == PhoneStatus.IN_STOCK.code || phone.status == PhoneStatus.RETURNED.code || phone.status == "RETURN"
                PhoneStatus.IN_TRANSIT.code, "TRANSIT" -> phone.status == PhoneStatus.IN_TRANSIT.code || phone.status == "TRANSIT"
                PhoneStatus.SOLD.code, "SOLD" -> phone.status == PhoneStatus.SOLD.code || phone.status == "SOLD"
                else -> phone.status == status
            }

            val matchesBrand = when (brand) {
                "ALL" -> true
                else -> phone.brand.equals(brand, ignoreCase = true)
            }

            matchesQuery && matchesStatus && matchesBrand
        }

        if (status == PhoneStatus.SOLD.code || status == "SOLD") {
            filtered.sortedBySaleDateDescending()
        } else {
            filtered.sortedByEntryDateDescending()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Writes directly to Firebase Realtime Database first, then persists locally.
     */
    fun addPhone(phone: PhoneItem) {
        viewModelScope.launch {
            stockRepository.insertPhone(phone)
        }
    }

    /**
     * Updates Firebase Realtime Database directly first, then updates local Room.
     */
    fun updatePhone(phone: PhoneItem) {
        viewModelScope.launch {
            stockRepository.updatePhone(phone)
        }
    }

    /**
     * Deletes from Firebase Realtime Database directly first, then removes from local Room.
     */
    fun deletePhone(phone: PhoneItem) {
        viewModelScope.launch {
            stockRepository.deletePhone(phone)
        }
    }

    fun markStatus(phoneId: Long, newStatus: String) {
        viewModelScope.launch {
            stockRepository.updateStatus(phoneId, newStatus)
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

    override fun onCleared() {
        super.onCleared()
        stockRepository.cleanUp()
    }
}
