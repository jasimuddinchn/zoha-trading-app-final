package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.PhoneDao
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Repository for Stock & Inventory operations with DIRECT Firebase Realtime Database write-first flow.
 * Every new entry, update, or deletion is pushed DIRECTLY to Firebase Realtime Database first (`/stock`, `/transit`, `/phones`),
 * ensuring instant real-time synchronization across all connected devices.
 */
class StockRepository(
    private val database: AppDatabase,
    val firebaseSyncManager: FirebaseSyncManager? = null,
    private val scope: CoroutineScope
) {
    private val phoneDao: PhoneDao = database.phoneDao()

    private val DATABASE_URL = "https://zoha-trading-default-rtdb.firebaseio.com/"
    private val TAG = "StockRepository"

    private val firebaseDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL).apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    private val stockRef: DatabaseReference by lazy { firebaseDb.getReference("stock") }
    private val transitRef: DatabaseReference by lazy { firebaseDb.getReference("transit") }
    private val salesRef: DatabaseReference by lazy { firebaseDb.getReference("sales") }
    private val salesRecordsRef: DatabaseReference by lazy { firebaseDb.getReference("sales_records") }
    private val phonesRef: DatabaseReference by lazy { firebaseDb.getReference("phones") }
    private val deletedItemsRef: DatabaseReference by lazy { firebaseDb.getReference("deleted_items") }

    private val _stockPhones = MutableStateFlow<List<PhoneItem>>(emptyList())
    val stockPhones: StateFlow<List<PhoneItem>> = _stockPhones.asStateFlow()

    private val _transitPhones = MutableStateFlow<List<PhoneItem>>(emptyList())
    val transitPhones: StateFlow<List<PhoneItem>> = _transitPhones.asStateFlow()

    val allPhones: Flow<List<PhoneItem>> = phoneDao.getAllPhones()

    private var stockListener: ValueEventListener? = null
    private var transitListener: ValueEventListener? = null

    init {
        setupPersistentStockListeners()
    }

    /**
     * Attaches persistent ValueEventListeners to `/stock` and `/transit` nodes.
     * Any remote or local change in Firebase instantly pushes updates to UI State (StateFlow)
     * and differentially updates local Room DB.
     * Deduplicates items by unique phone ID.
     */
    private fun setupPersistentStockListeners() {
        try {
            stockRef.keepSynced(true)
            transitRef.keepSynced(true)
            phonesRef.keepSynced(true)
        } catch (_: Exception) {}

        stockListener?.let {
            try { stockRef.removeEventListener(it) } catch (_: Exception) {}
            stockListener = null
        }
        transitListener?.let {
            try { transitRef.removeEventListener(it) } catch (_: Exception) {}
            transitListener = null
        }

        // 1. Persistent Realtime Listener for `/stock`
        val sListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = linkedMapOf<Long, PhoneItem>()
                for (child in snapshot.children) {
                    val phone = FirebaseSyncManager.parsePhoneFromSnapshot(child, PhoneStatus.IN_STOCK.code) ?: continue
                    if (phone.id == 0L) continue
                    val existing = map[phone.id]
                    if (existing == null || phone.updatedAt >= existing.updatedAt) {
                        map[phone.id] = phone
                    }
                }
                val list = map.values.toList().distinctBy { it.id }.sortedWith(
                    compareByDescending<PhoneItem> {
                        com.example.util.DateUtils.parseDateToMillis(it.entryDate.trim())
                    }.thenByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }
                    .thenByDescending { it.updatedAt }
                    .thenByDescending { it.id }
                )
                _stockPhones.value = list
                scope.launch(Dispatchers.IO) {
                    syncPhonesToLocalRoom(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "stockRef listener cancelled: ${error.message}")
            }
        }
        stockListener = sListener
        stockRef.addValueEventListener(sListener)

        // 2. Persistent Realtime Listener for `/transit`
        val tListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = linkedMapOf<Long, PhoneItem>()
                for (child in snapshot.children) {
                    val phone = FirebaseSyncManager.parsePhoneFromSnapshot(child, PhoneStatus.IN_TRANSIT.code) ?: continue
                    if (phone.id == 0L) continue
                    val existing = map[phone.id]
                    if (existing == null || phone.updatedAt >= existing.updatedAt) {
                        map[phone.id] = phone
                    }
                }
                val list = map.values.toList().distinctBy { it.id }.sortedWith(
                    compareByDescending<PhoneItem> {
                        com.example.util.DateUtils.parseDateToMillis(it.entryDate.trim())
                    }.thenByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }
                    .thenByDescending { it.updatedAt }
                    .thenByDescending { it.id }
                )
                _transitPhones.value = list
                scope.launch(Dispatchers.IO) {
                    syncPhonesToLocalRoom(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "transitRef listener cancelled: ${error.message}")
            }
        }
        transitListener = tListener
        transitRef.addValueEventListener(tListener)
    }

    /**
     * Disposes of active ValueEventListeners to prevent listener stacking and memory leaks.
     */
    fun cleanUp() {
        stockListener?.let {
            try { stockRef.removeEventListener(it) } catch (_: Exception) {}
            stockListener = null
        }
        transitListener?.let {
            try { transitRef.removeEventListener(it) } catch (_: Exception) {}
            transitListener = null
        }
    }

    private suspend fun syncPhonesToLocalRoom(remotePhones: List<PhoneItem>) = withContext(Dispatchers.IO) {
        try {
            if (remotePhones.isEmpty()) return@withContext
            val localPhones = phoneDao.getAllPhonesDirect().associateBy { it.id }
            val toUpsert = mutableListOf<PhoneItem>()
            for (remote in remotePhones) {
                val local = localPhones[remote.id]
                if (local == null || remote.updatedAt >= local.updatedAt) {
                    if (remote != local) {
                        toUpsert.add(remote)
                    }
                }
            }
            if (toUpsert.isNotEmpty()) {
                phoneDao.insertAll(toUpsert)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing stock to Room: ${e.message}")
        }
    }

    /**
     * Direct Write/Push to Firebase First for Stock Addition:
     * Immediately executes .setValue() to Firebase paths (/stock/{id} and /phones/{id}),
     * then stores in Room SQLite.
     */
    suspend fun insertPhone(phone: PhoneItem): Long = withContext(Dispatchers.IO) {
        val uniqueId = if (phone.id == 0L) AppRepository.generateUniqueId() else phone.id
        val now = System.currentTimeMillis()
        val phoneWithId = phone.copy(
            id = uniqueId,
            createdAt = if (phone.createdAt == 0L) now else phone.createdAt,
            updatedAt = now
        )

        val key = uniqueId.toString()
        val map = FirebaseSyncManager.phoneToMap(phoneWithId, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            phonesRef.child(key).setValue(map)
            val isTransit = phoneWithId.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
                phoneWithId.status.equals("TRANSIT", ignoreCase = true)
            val isSold = phoneWithId.status.equals(PhoneStatus.SOLD.code, ignoreCase = true) ||
                phoneWithId.status.equals("SOLD", ignoreCase = true)

            if (isSold) {
                salesRef.child(key).setValue(map)
                salesRecordsRef.child(key).setValue(map)
                stockRef.child(key).removeValue()
                transitRef.child(key).removeValue()
            } else if (isTransit) {
                transitRef.child(key).setValue(map)
                stockRef.child(key).removeValue()
                salesRef.child(key).removeValue()
                salesRecordsRef.child(key).removeValue()
            } else {
                stockRef.child(key).setValue(map)
                transitRef.child(key).removeValue()
                salesRef.child(key).removeValue()
                salesRecordsRef.child(key).removeValue()
            }
            Log.d(TAG, "Direct Firebase write succeeded for stock item $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase write note for phone $key: ${e.message}")
        }

        // Also notify sync manager
        firebaseSyncManager?.pushPhone(phoneWithId)

        // 2. Persist to Room SQLite
        val rowId = phoneDao.insertPhone(phoneWithId)
        return@withContext rowId
    }

    /**
     * Direct Write/Push to Firebase First for Stock Edit/Update.
     */
    suspend fun updatePhone(phone: PhoneItem) = withContext(Dispatchers.IO) {
        val existing = phoneDao.getPhoneById(phone.id)
        val isTransitToStock = (phone.status.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) ||
            phone.status.equals("IN_STOCK", ignoreCase = true) ||
            phone.status.equals("STOCK", ignoreCase = true)) &&
            existing != null &&
            (existing.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) || existing.status.equals("TRANSIT", ignoreCase = true))

        val now = System.currentTimeMillis()
        val updated = if (isTransitToStock) {
            val today = com.example.util.DateUtils.getTodayStorageDate()
            phone.copy(
                status = PhoneStatus.IN_STOCK.code,
                entryDate = today,
                createdAt = now,
                updatedAt = now
            )
        } else {
            phone.copy(updatedAt = now)
        }
        val key = updated.id.toString()
        val map = FirebaseSyncManager.phoneToMap(updated, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            phonesRef.child(key).setValue(map)
            val isTransit = updated.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
                updated.status.equals("TRANSIT", ignoreCase = true)
            val isSold = updated.status.equals(PhoneStatus.SOLD.code, ignoreCase = true) ||
                updated.status.equals("SOLD", ignoreCase = true)

            if (isSold) {
                salesRef.child(key).setValue(map)
                salesRecordsRef.child(key).setValue(map)
                stockRef.child(key).removeValue()
                transitRef.child(key).removeValue()
            } else if (isTransit) {
                transitRef.child(key).setValue(map)
                stockRef.child(key).removeValue()
                salesRef.child(key).removeValue()
                salesRecordsRef.child(key).removeValue()
            } else {
                stockRef.child(key).setValue(map)
                transitRef.child(key).removeValue()
                salesRef.child(key).removeValue()
                salesRecordsRef.child(key).removeValue()
            }
            Log.d(TAG, "Direct Firebase update succeeded for item $key (status: ${updated.status})")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase update note for phone $key: ${e.message}")
        }

        firebaseSyncManager?.pushPhone(updated)
        if (updated.status.equals(PhoneStatus.SOLD.code, ignoreCase = true) ||
            updated.status.equals("SOLD", ignoreCase = true)) {
            firebaseSyncManager?.syncSaleWithCustomerDue(updated)
        }

        // 2. Persist to Room SQLite
        phoneDao.updatePhone(updated)
    }

    /**
     * Direct Delete from Firebase Realtime Database first.
     */
    suspend fun deletePhone(phone: PhoneItem) = withContext(Dispatchers.IO) {
        val key = phone.id.toString()

        // 1. Direct delete in Firebase Realtime Database FIRST
        try {
            phonesRef.child(key).removeValue()
            stockRef.child(key).removeValue()
            transitRef.child(key).removeValue()
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "phone", "id" to phone.id, "timestamp" to System.currentTimeMillis())
            )
            Log.d(TAG, "Direct Firebase remove succeeded for phone $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase delete note for phone $key: ${e.message}")
        }

        firebaseSyncManager?.deletePhone(phone.id)

        // 2. Delete from Room SQLite
        phoneDao.deletePhone(phone)
    }

    suspend fun deletePhoneById(id: Long) = withContext(Dispatchers.IO) {
        val key = id.toString()
        try {
            phonesRef.child(key).removeValue()
            stockRef.child(key).removeValue()
            transitRef.child(key).removeValue()
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "phone", "id" to id, "timestamp" to System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase deleteById note: ${e.message}")
        }
        firebaseSyncManager?.deletePhone(id)
        phoneDao.deletePhoneById(id)
    }

    suspend fun updateOwnerCosts(
        phoneId: Long,
        buyingPriceRmb: Double,
        buyingPriceBdt: Double,
        shippingCostBdt: Double,
        otherCostBdt: Double,
        desiredProfitBdt: Double = 0.0
    ) = withContext(Dispatchers.IO) {
        val phone = phoneDao.getPhoneById(phoneId) ?: return@withContext
        val totalCost = buyingPriceBdt + shippingCostBdt + otherCostBdt
        val updated = phone.copy(
            buyingPriceRmb = buyingPriceRmb,
            buyingPriceBdt = buyingPriceBdt,
            shippingCostBdt = shippingCostBdt,
            otherCostBdt = otherCostBdt,
            totalCostBdt = totalCost,
            desiredProfitBdt = desiredProfitBdt,
            updatedAt = System.currentTimeMillis()
        )
        updatePhone(updated)
    }

    suspend fun updateStatus(phoneId: Long, newStatus: String) = withContext(Dispatchers.IO) {
        val phone = phoneDao.getPhoneById(phoneId) ?: return@withContext
        val isTransitToStock = (phone.status.equals(PhoneStatus.IN_TRANSIT.code, ignoreCase = true) ||
            phone.status.equals("TRANSIT", ignoreCase = true)) &&
            (newStatus.equals(PhoneStatus.IN_STOCK.code, ignoreCase = true) ||
            newStatus.equals("IN_STOCK", ignoreCase = true) ||
            newStatus.equals("STOCK", ignoreCase = true))

        val now = System.currentTimeMillis()
        val updated = if (isTransitToStock) {
            val today = com.example.util.DateUtils.getTodayStorageDate()
            phone.copy(
                status = PhoneStatus.IN_STOCK.code,
                entryDate = today,
                createdAt = now,
                updatedAt = now
            )
        } else {
            phone.copy(status = newStatus, updatedAt = now)
        }
        updatePhone(updated)
    }

    suspend fun getPhoneById(id: Long): PhoneItem? = phoneDao.getPhoneById(id)
}
