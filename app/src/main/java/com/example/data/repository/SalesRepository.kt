package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.PhoneDao
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
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
 * Dedicated Repository for Sales operations with DIRECT Firebase Realtime Database write-first flow.
 * Every sale record, status change, or due collection is written DIRECTLY to Firebase Realtime Database first (`/sales`, `/phones`),
 * ensuring instant real-time synchronization across all connected devices.
 */
class SalesRepository(
    private val database: AppDatabase,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
    private val scope: CoroutineScope
) {
    private val phoneDao: PhoneDao = database.phoneDao()

    private val DATABASE_URL = "https://zoha-trading-default-rtdb.firebaseio.com/"
    private val TAG = "SalesRepository"

    private val firebaseDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL).apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    private val salesRef: DatabaseReference by lazy { firebaseDb.getReference("sales") }
    private val salesRecordsRef: DatabaseReference by lazy { firebaseDb.getReference("sales_records") }
    private val stockRef: DatabaseReference by lazy { firebaseDb.getReference("stock") }
    private val phonesRef: DatabaseReference by lazy { firebaseDb.getReference("phones") }
    private val financialsRef: DatabaseReference by lazy { firebaseDb.getReference("financials") }

    private val _salesPhones = MutableStateFlow<List<PhoneItem>>(emptyList())
    val salesPhones: StateFlow<List<PhoneItem>> = _salesPhones.asStateFlow()

    private var salesListener: ValueEventListener? = null

    init {
        setupPersistentSalesListener()
    }

    /**
     * Attaches persistent ValueEventListener to `/sales` node.
     * Any change on any device instantly pushes updates to UI State (StateFlow)
     * and local Room DB.
     * Deduplicates by unique phone ID to prevent duplicate entry issues.
     */
    private fun setupPersistentSalesListener() {
        try {
            salesRef.keepSynced(true)
            salesRecordsRef.keepSynced(true)
        } catch (_: Exception) {}

        salesListener?.let {
            try { salesRef.removeEventListener(it) } catch (_: Exception) {}
            salesListener = null
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phoneMap = linkedMapOf<Long, PhoneItem>()
                for (child in snapshot.children) {
                    val phone = FirebaseSyncManager.parsePhoneFromSnapshot(child, PhoneStatus.SOLD.code) ?: continue
                    if (phone.id == 0L) continue
                    val existing = phoneMap[phone.id]
                    if (existing == null || phone.updatedAt >= existing.updatedAt) {
                        phoneMap[phone.id] = phone
                    }
                }
                val list = phoneMap.values.toList().distinctBy { it.id }.sortedWith(
                    compareByDescending<PhoneItem> {
                        val d = it.saleDate.ifBlank { it.entryDate }.trim()
                        com.example.util.DateUtils.parseDateToMillis(d)
                    }.thenByDescending { if (it.updatedAt > 0L) it.updatedAt else it.createdAt }
                    .thenByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }
                    .thenByDescending { it.id }
                )
                _salesPhones.value = list
                scope.launch(Dispatchers.IO) {
                    syncSalesToLocalRoom(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "salesRef listener cancelled: ${error.message}")
            }
        }
        salesListener = listener
        salesRef.addValueEventListener(listener)
    }

    /**
     * Disposes of active ValueEventListener to prevent listener stacking and memory leaks.
     */
    fun cleanUp() {
        salesListener?.let {
            try { salesRef.removeEventListener(it) } catch (_: Exception) {}
            salesListener = null
        }
    }

    private suspend fun syncSalesToLocalRoom(remoteSales: List<PhoneItem>) = withContext(Dispatchers.IO) {
        try {
            if (remoteSales.isEmpty()) return@withContext
            val localPhones = phoneDao.getAllPhonesDirect().associateBy { it.id }
            val toUpsert = mutableListOf<PhoneItem>()
            for (remote in remoteSales) {
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
            Log.w(TAG, "Error syncing sales to Room: ${e.message}")
        }
    }

    /**
     * Direct Write/Push to Firebase First for Sale Recording:
     * Immediately executes .setValue() to /sales/{id} and /phones/{id},
     * removes from /stock/{id}, then stores in Room SQLite.
     */
    suspend fun recordSale(
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
    ) = withContext(Dispatchers.IO) {
        val phone = phoneDao.getPhoneById(phoneId) ?: return@withContext
        val combinedNotes = when {
            notes.isNotBlank() && phone.notes.isNotBlank() && !phone.notes.contains(notes) -> "${phone.notes} | $notes"
            notes.isNotBlank() && phone.notes.isBlank() -> notes
            else -> phone.notes
        }
        val updated = phone.copy(
            status = PhoneStatus.SOLD.code,
            sellingPriceBdt = sellingPriceBdt,
            cashReceivedBdt = cashReceivedBdt,
            dueAmountBdt = dueAmountBdt,
            saleDate = saleDate,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            customerEmail = customerEmail.trim(),
            soldByStaff = soldByStaff,
            paymentMethod = paymentMethod,
            warrantyDays = warrantyDays,
            notes = combinedNotes,
            updatedAt = System.currentTimeMillis()
        )

        val key = updated.id.toString()
        val map = FirebaseSyncManager.phoneToMap(updated, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            salesRef.child(key).setValue(map)
            salesRecordsRef.child(key).setValue(map)
            phonesRef.child(key).setValue(map)
            stockRef.child(key).removeValue()
            Log.d(TAG, "Direct Firebase sale write succeeded for phone $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase sale write note for phone $key: ${e.message}")
        }

        // 2. If dueAmount > 0, post automatically under the specific Customer profile in Firebase (/customer_dues)
        if (dueAmountBdt > 0.0) {
            val dueRecord = CustomerDueRecord(
                id = key,
                phoneId = updated.id,
                phoneModel = "${updated.brand} ${updated.model}".trim(),
                imei = updated.imei,
                totalSalePrice = sellingPriceBdt,
                cashReceived = cashReceivedBdt,
                dueAmount = dueAmountBdt,
                saleDate = saleDate,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                soldByStaff = soldByStaff,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
            firebaseSyncManager?.postCustomerDue(dueRecord)
            Log.d(TAG, "Posted customer due record under /customer_dues: ৳ $dueAmountBdt for ${customerName.trim()}")
        }

        firebaseSyncManager?.pushPhone(updated)

        // 3. Persist to Room SQLite
        phoneDao.updatePhone(updated)
    }

    /**
     * Direct Write/Push to Firebase First for Customer Due Collection:
     * - Deducts amount from customer's due balance under /customer_dues/{customerKey}/payments
     * - Pushes collection record to /financials/due_collections
     * - Adds directly to Dynamic Total Net Balance while EXCLUDING from Monthly Cash In (preventing false sales inflation).
     */
    suspend fun collectCustomerDue(
        customerKey: String,
        customerName: String,
        customerPhone: String,
        amount: Double,
        paymentMethod: String = "Cash",
        collectedBy: String = "Staff",
        date: String = CustomerDueUtils.getTodayDate(),
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val paymentRecord = DuePaymentRecord(
            id = System.currentTimeMillis().toString(),
            customerKey = customerKey,
            customerName = customerName,
            customerPhone = customerPhone,
            amount = amount,
            paymentMethod = paymentMethod,
            collectedBy = collectedBy,
            date = date,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )

        // Post to Firebase Realtime Database
        firebaseSyncManager?.recordDueCollectionPayment(paymentRecord)
        Log.d(TAG, "Collected due ৳ $amount from $customerName via $paymentMethod by $collectedBy")
    }

    /**
     * Direct Write/Push to Firebase First for Due Collection on a specific phone sale item:
     * Immediately updates /sales/{id} with collected amount and logs collection under /financials/due_collections.
     */
    suspend fun collectDue(
        phoneId: Long,
        collectedAmount: Double,
        paymentMethod: String,
        collectedBy: String,
        date: String
    ) = withContext(Dispatchers.IO) {
        val phone = phoneDao.getPhoneById(phoneId) ?: return@withContext
        val now = System.currentTimeMillis()
        val remainingDue = (phone.dueAmountBdt - collectedAmount).coerceAtLeast(0.0)
        val updatedCashReceived = phone.cashReceivedBdt + collectedAmount
        val noteSuffix = " [Due Collected: ৳ $collectedAmount via $paymentMethod by $collectedBy on $date]"
        val updated = phone.copy(
            cashReceivedBdt = updatedCashReceived,
            dueAmountBdt = remainingDue,
            notes = if (phone.notes.isNotBlank()) "${phone.notes}$noteSuffix" else noteSuffix.trim(),
            updatedAt = now
        )

        val key = updated.id.toString()
        val map = FirebaseSyncManager.phoneToMap(updated, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            salesRef.child(key).setValue(map)
            salesRecordsRef.child(key).setValue(map)
            phonesRef.child(key).setValue(map)

            // Log collection transaction under /financials/due_collections
            val collectionId = now.toString()
            val collectionData = mapOf(
                "id" to collectionId,
                "phoneId" to phoneId,
                "customerKey" to CustomerDueUtils.getCustomerKey(phone.customerPhone, phone.customerName),
                "customerName" to phone.customerName,
                "customerPhone" to phone.customerPhone,
                "amount" to collectedAmount,
                "paymentMethod" to paymentMethod,
                "collectedBy" to collectedBy,
                "date" to date,
                "timestamp" to now
            )
            financialsRef.child("due_collections").child(collectionId).setValue(collectionData)

            // Also record in customer profile payments
            val paymentRecord = DuePaymentRecord(
                id = collectionId,
                customerKey = CustomerDueUtils.getCustomerKey(phone.customerPhone, phone.customerName),
                customerName = phone.customerName,
                customerPhone = phone.customerPhone,
                amount = collectedAmount,
                paymentMethod = paymentMethod,
                collectedBy = collectedBy,
                date = date,
                notes = "Collected against IMEI ${phone.imei}",
                timestamp = now
            )
            firebaseSyncManager?.recordDueCollectionPayment(paymentRecord)

            Log.d(TAG, "Direct Firebase due collection write succeeded for phone $key: ৳ $collectedAmount")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase due collection note for phone $key: ${e.message}")
        }

        firebaseSyncManager?.pushPhone(updated)

        // 2. Persist to Room SQLite
        phoneDao.updatePhone(updated)
    }

    /**
     * Reverts a sale back to stock:
     * Immediately writes to /stock/{id} and removes from /sales/{id} in Firebase first.
     */
    suspend fun revertSale(phone: PhoneItem) = withContext(Dispatchers.IO) {
        val reverted = phone.copy(
            status = PhoneStatus.IN_STOCK.code,
            sellingPriceBdt = 0.0,
            cashReceivedBdt = 0.0,
            dueAmountBdt = 0.0,
            saleDate = "",
            customerName = "",
            customerPhone = "",
            customerEmail = "",
            soldByStaff = "",
            warrantyDays = 0,
            notes = if (phone.notes.isNotBlank()) "${phone.notes} (Sale Reverted)" else "Sale Reverted",
            updatedAt = System.currentTimeMillis()
        )

        val key = reverted.id.toString()
        val map = FirebaseSyncManager.phoneToMap(reverted, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            stockRef.child(key).setValue(map)
            phonesRef.child(key).setValue(map)
            salesRef.child(key).removeValue()
            salesRecordsRef.child(key).removeValue()

            // If there was a due, remove it from /customer_dues
            if (phone.dueAmountBdt > 0.0) {
                firebaseSyncManager?.revertCustomerDue(phone.id, phone.customerPhone, phone.customerName)
            }
            Log.d(TAG, "Direct Firebase revert sale write succeeded for phone $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase revert sale note: ${e.message}")
        }

        firebaseSyncManager?.pushPhone(reverted)

        // 2. Persist to Room SQLite
        phoneDao.updatePhone(reverted)
    }

    /**
     * Direct Due Entry / Historical Adjustment without creating a phone sale:
     * - Captures Name, Phone, Due Amount, Date, and Note.
     * - Posts due record automatically under the Customer's ledger profile in Firebase (/customer_dues).
     * - Excludes from Monthly Cash In and physical drawer since no cash was received.
     */
    suspend fun addExistingDue(
        customerName: String,
        customerPhone: String,
        amount: Double,
        date: String = CustomerDueUtils.getTodayDate(),
        notes: String = "",
        addedByStaff: String = "Admin"
    ) = withContext(Dispatchers.IO) {
        val dueKey = "manual_${System.currentTimeMillis()}"
        val record = CustomerDueRecord(
            id = dueKey,
            phoneId = 0L,
            phoneModel = "Existing / Manual Due Entry",
            imei = "",
            totalSalePrice = amount,
            cashReceived = 0.0,
            dueAmount = amount,
            saleDate = date,
            customerName = customerName.trim().uppercase(),
            customerPhone = customerPhone.trim(),
            soldByStaff = addedByStaff,
            notes = notes.trim(),
            timestamp = System.currentTimeMillis()
        )
        firebaseSyncManager?.postCustomerDue(record)
        Log.d(TAG, "Added existing due of ৳ $amount for $customerName")
    }

    suspend fun deleteCustomerDue(customerKey: String, dueId: String) = withContext(Dispatchers.IO) {
        firebaseSyncManager?.deleteCustomerDue(customerKey, dueId)
        Log.d(TAG, "Deleted customer due $dueId under $customerKey")
    }

    suspend fun deleteDuePayment(customerKey: String, paymentId: String) = withContext(Dispatchers.IO) {
        firebaseSyncManager?.deleteDuePayment(customerKey, paymentId)
        Log.d(TAG, "Deleted due payment $paymentId under $customerKey")
    }

    suspend fun updateCustomerDueRecord(
        customerKey: String,
        dueId: String,
        newDueAmount: Double,
        newTotalSalePrice: Double,
        newCashReceived: Double,
        newDate: String,
        newNotes: String
    ) = withContext(Dispatchers.IO) {
        firebaseSyncManager?.updateCustomerDueRecord(
            customerKey,
            dueId,
            newDueAmount,
            newTotalSalePrice,
            newCashReceived,
            newDate,
            newNotes
        )
        Log.d(TAG, "Updated customer due $dueId under $customerKey")
    }

    suspend fun updateDuePaymentRecord(
        customerKey: String,
        paymentId: String,
        newAmount: Double,
        newPaymentMethod: String,
        newCollectedBy: String,
        newDate: String,
        newNotes: String
    ) = withContext(Dispatchers.IO) {
        firebaseSyncManager?.updateDuePaymentRecord(
            customerKey,
            paymentId,
            newAmount,
            newPaymentMethod,
            newCollectedBy,
            newDate,
            newNotes
        )
        Log.d(TAG, "Updated due payment $paymentId under $customerKey")
    }

    suspend fun updateCustomerDetails(customerKey: String, newName: String, newPhone: String) = withContext(Dispatchers.IO) {
        firebaseSyncManager?.updateCustomerDetails(customerKey, newName, newPhone)
        Log.d(TAG, "Updated customer details for $customerKey to $newName ($newPhone)")
    }

    suspend fun updateSale(phone: PhoneItem) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = phone.copy(
            customerName = phone.customerName.trim().uppercase(),
            updatedAt = now
        )
        val key = updated.id.toString()
        val map = FirebaseSyncManager.phoneToMap(updated, key)

        try {
            salesRef.child(key).setValue(map)
            salesRecordsRef.child(key).setValue(map)
            phonesRef.child(key).setValue(map)
            Log.d(TAG, "Direct Firebase updateSale write succeeded for phone $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase updateSale note: ${e.message}")
        }

        firebaseSyncManager?.pushPhone(updated)
        firebaseSyncManager?.syncSaleWithCustomerDue(updated)

        phoneDao.updatePhone(updated)
    }

    fun getSoldPhones(): Flow<List<PhoneItem>> = phoneDao.getPhonesByStatus(PhoneStatus.SOLD.code)
}
