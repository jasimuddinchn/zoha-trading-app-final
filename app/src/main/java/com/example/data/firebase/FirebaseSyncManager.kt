package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.CustomerDueProfile
import com.example.data.model.CustomerDueRecord
import com.example.data.model.CustomerDueUtils
import com.example.data.model.DuePaymentRecord
import com.example.data.model.ExpenseItem
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.repository.AppRepository
import com.example.data.repository.FirebaseSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real-Time Multi-Device Cloud Sync Manager & Repository for Zoha Trading.
 *
 * Implements an Offline-First Live Multi-Device Synchronization Architecture:
 * 1. Persistent ValueEventListeners are registered across all inventory nodes:
 *    - `/stock`: In-stock units
 *    - `/transit`: Sourced units in transit from China
 *    - `/sales`: Units sold to customers
 *    - `/expenses`: Business expense ledger
 *    - `/deleted_items`: Global tombstone register for multi-device deletions
 *    - `/phones`, `/stock_items`, `/transit_items`, `/sales_records`: Mirrors
 *    - `/settings` & `/auth_pins`: Shop configuration & profile PINs
 * 2. Remote updates are merged into local Room DB differentially (without wiping Room SQLite)
 *    on the background IO dispatcher so Jetpack Compose flows automatically re-collect state
 *    and instantly reflect changes on screen across all connected devices.
 * 3. Bidirectional write: local CRUD immediately writes to Room, then pushes with a unique key
 *    and lastUpdated timestamp to Firebase Realtime Database.
 */
class FirebaseSyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope
) : FirebaseSyncRepository {

    override val databaseUrl = "https://zoha-trading-default-rtdb.firebaseio.com/"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Notice initializing FirebaseAuth: ${e.message}")
            null
        }
    }

    private val firebaseDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(databaseUrl).apply {
                try {
                    setPersistenceEnabled(true)
                } catch (e: Exception) {
                    Log.d(TAG, "Firebase disk persistence already active: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice initializing Firebase Database with custom URL: ${e.message}")
            FirebaseDatabase.getInstance().apply {
                try {
                    setPersistenceEnabled(true)
                } catch (ex: Exception) {
                    // ignore
                }
            }
        }
    }

    // Standard Realtime Database Nodes requested
    val stockRef: DatabaseReference by lazy { firebaseDb.getReference("stock") }
    val transitRef: DatabaseReference by lazy { firebaseDb.getReference("transit") }
    val salesRef: DatabaseReference by lazy { firebaseDb.getReference("sales") }
    val expensesRef: DatabaseReference by lazy { firebaseDb.getReference("expenses") }
    val deletedItemsRef: DatabaseReference by lazy { firebaseDb.getReference("deleted_items") }

    // Alternative & Mirror Database Nodes for comprehensive compatibility
    val phonesRef: DatabaseReference by lazy { firebaseDb.getReference("phones") }
    val stockItemsRef: DatabaseReference by lazy { firebaseDb.getReference("stock_items") }
    val transitItemsRef: DatabaseReference by lazy { firebaseDb.getReference("transit_items") }
    val salesRecordsRef: DatabaseReference by lazy { firebaseDb.getReference("sales_records") }

    // Customer Due Management & Cash Reconciliation Nodes
    val customerDuesRef: DatabaseReference by lazy { firebaseDb.getReference("customer_dues") }
    val dueCollectionsRef: DatabaseReference by lazy { firebaseDb.getReference("financials").child("due_collections") }
    val otherIncomeRef: DatabaseReference by lazy { firebaseDb.getReference("other_income") }

    // Configuration & Auth Security Nodes
    val settingsRef: DatabaseReference by lazy { firebaseDb.getReference("settings") }
    val authPinsRef: DatabaseReference by lazy { firebaseDb.getReference("auth_pins") }
    val userProfilesRef: DatabaseReference by lazy { firebaseDb.getReference("user_profiles") }
    val connectedRef: DatabaseReference by lazy { firebaseDb.getReference(".info/connected") }

    @Volatile
    private var cachedOwnerPin: String? = null
    @Volatile
    private var cachedStaff1Pin: String? = null
    @Volatile
    private var cachedStaff2Pin: String? = null

    override fun getCachedPin(userId: String): String? {
        return when (userId) {
            "ADMIN" -> cachedOwnerPin
            "STAFF_1" -> cachedStaff1Pin
            "STAFF_2" -> cachedStaff2Pin
            else -> null
        }
    }

    private val _isCloudConnected = MutableStateFlow(false)
    override val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    override val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isPermissionDenied = MutableStateFlow(false)
    override val isPermissionDenied: StateFlow<Boolean> = _isPermissionDenied.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    override val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    override val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    // Customer Dues Live StateFlows
    private val _customerDueProfiles = MutableStateFlow<List<CustomerDueProfile>>(emptyList())
    val customerDueProfiles: StateFlow<List<CustomerDueProfile>> = _customerDueProfiles.asStateFlow()

    private val _totalOutstandingDue = MutableStateFlow(0.0)
    val totalOutstandingDue: StateFlow<Double> = _totalOutstandingDue.asStateFlow()

    private val _totalDueCollectionsAmount = MutableStateFlow(0.0)
    val totalDueCollectionsAmount: StateFlow<Double> = _totalDueCollectionsAmount.asStateFlow()

    private val _allDuePayments = MutableStateFlow<List<DuePaymentRecord>>(emptyList())
    val allDuePayments: StateFlow<List<DuePaymentRecord>> = _allDuePayments.asStateFlow()

    // Other Income Live StateFlows
    private val _allOtherIncomes = MutableStateFlow<List<OtherIncomeItem>>(emptyList())
    val allOtherIncomes: StateFlow<List<OtherIncomeItem>> = _allOtherIncomes.asStateFlow()

    private val _totalOtherIncomeAmount = MutableStateFlow(0.0)
    val totalOtherIncomeAmount: StateFlow<Double> = _totalOtherIncomeAmount.asStateFlow()

    private var isListenersSetup = false

    private val activeListeners = java.util.concurrent.ConcurrentHashMap<DatabaseReference, ValueEventListener>()

    private fun registerListener(ref: DatabaseReference, listener: ValueEventListener) {
        activeListeners[ref]?.let { old ->
            try { ref.removeEventListener(old) } catch (_: Exception) {}
        }
        activeListeners[ref] = listener
        ref.addValueEventListener(listener)
    }

    private fun removeAllListeners() {
        activeListeners.forEach { (ref, listener) ->
            try { ref.removeEventListener(listener) } catch (_: Exception) {}
        }
        activeListeners.clear()
    }

    private fun handleListenerCancelled(nodeName: String, error: DatabaseError) {
        if (error.code == DatabaseError.PERMISSION_DENIED) {
            Log.w(TAG, "Firebase '$nodeName' listener access restricted: ${error.message}. Re-authenticating...")
            ensureAuthenticated { success ->
                if (success) {
                    _isPermissionDenied.value = false
                    _syncErrorMessage.value = null
                } else {
                    _isPermissionDenied.value = true
                    _syncErrorMessage.value = "Firebase Realtime Database access restricted. Check security rules."
                }
            }
        } else {
            Log.w(TAG, "Firebase '$nodeName' listener cancelled: ${error.message} (code: ${error.code})")
        }
    }

    private fun handleWriteFailure(nodeName: String, e: Exception) {
        if (e.message?.contains("permission_denied", ignoreCase = true) == true ||
            e.message?.contains("Permission denied", ignoreCase = true) == true
        ) {
            Log.w(TAG, "Firebase write notice on node '$nodeName': ${e.message}. Re-authenticating...")
            ensureAuthenticated { success ->
                if (success) {
                    _isPermissionDenied.value = false
                }
            }
        } else {
            Log.w(TAG, "Notice pushing to Firebase node '$nodeName': ${e.message}")
        }
    }

    override fun ensureAuthenticated(onComplete: (Boolean) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(true)
            return
        }
        if (auth.currentUser != null) {
            _isPermissionDenied.value = false
            onComplete(true)
            return
        }
        try {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Firebase Anonymous Auth sign-in successful: uid=${result.user?.uid}")
                    _isPermissionDenied.value = false
                    _syncErrorMessage.value = null
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Firebase Anonymous Auth sign-in note: ${e.message}")
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice initiating anonymous auth: ${e.message}")
            onComplete(false)
        }
    }

    override fun retrySync() {
        _isPermissionDenied.value = false
        _syncErrorMessage.value = null
        _isInitialLoading.value = true
        startRealtimeSync()
        coroutineScope.launch(Dispatchers.IO) {
            fetchAndSyncCloudPinsAndSettings()
            syncPendingOfflineChanges()
        }
    }

    override fun startRealtimeSync() {
        if (!isListenersSetup) {
            setupRealtimeSyncListeners()
            isListenersSetup = true
        }
        ensureAuthenticated { success ->
            coroutineScope.launch(Dispatchers.IO) {
                val localPhoneCount = database.phoneDao().getPhoneCount()
                val localExpenseCount = database.expenseDao().getExpenseCount()
                if (localPhoneCount == 0 || localExpenseCount == 0) {
                    pullAndPopulateAllFromCloud()
                }
            }
        }
    }

    private fun setupRealtimeSyncListeners() {
        removeAllListeners()
        // Fallback timeout to stop showing initial spinner after 1.5 seconds
        coroutineScope.launch {
            kotlinx.coroutines.delay(1500)
            if (_isInitialLoading.value) {
                _isInitialLoading.value = false
                Log.d(TAG, "Initial sync window elapsed; local Room DB active")
            }
        }

        // 1. Connection status listener (.info/connected)
        registerListener(connectedRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _isCloudConnected.value = connected
                if (connected) {
                    _isPermissionDenied.value = false
                    _isInitialLoading.value = false
                    _syncErrorMessage.value = null
                    Log.d(TAG, "Firebase Realtime DB Connection Status: ONLINE (Connected)")
                    ensureAuthenticated {
                        coroutineScope.launch(Dispatchers.IO) {
                            val localPhoneCount = database.phoneDao().getPhoneCount()
                            val localExpenseCount = database.expenseDao().getExpenseCount()
                            if (localPhoneCount == 0 && localExpenseCount == 0) {
                                pullAndPopulateAllFromCloud()
                            } else {
                                syncPendingOfflineChanges()
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Firebase Realtime DB Connection Status: OFFLINE (Operating via Room SQLite)")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("connected", error)
            }
        })

        // Keep core nodes cached for offline speed & instant responsiveness
        try {
            stockRef.keepSynced(true)
            transitRef.keepSynced(true)
            salesRef.keepSynced(true)
            expensesRef.keepSynced(true)
            customerDuesRef.keepSynced(true)
            dueCollectionsRef.keepSynced(true)
            deletedItemsRef.keepSynced(true)
            phonesRef.keepSynced(true)
            stockItemsRef.keepSynced(true)
            transitItemsRef.keepSynced(true)
            salesRecordsRef.keepSynced(true)
            settingsRef.keepSynced(true)
            authPinsRef.keepSynced(true)
        } catch (e: Exception) {
            Log.w(TAG, "Notice keeping nodes synced: ${e.message}")
        }

        // 2. Persistent Realtime ValueEventListener for `/stock`
        registerListener(stockRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val stockMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_STOCK.code)?.let {
                                val existing = stockMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    stockMap[it.id] = it
                                }
                            }
                        }
                        if (stockMap.isNotEmpty()) {
                            syncRemotePhonesToRoom(stockMap.values.toList())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /stock from Firebase: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("stock", error)
            }
        })

        // 3. Persistent Realtime ValueEventListener for `/transit`
        registerListener(transitRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val transitMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_TRANSIT.code)?.let {
                                val existing = transitMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    transitMap[it.id] = it
                                }
                            }
                        }
                        if (transitMap.isNotEmpty()) {
                            syncRemotePhonesToRoom(transitMap.values.toList())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /transit from Firebase: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("transit", error)
            }
        })

        // 4. Persistent Realtime ValueEventListener for `/sales`
        registerListener(salesRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val salesMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.SOLD.code)?.let {
                                val existing = salesMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    salesMap[it.id] = it
                                }
                            }
                        }
                        if (salesMap.isNotEmpty()) {
                            syncRemotePhonesToRoom(salesMap.values.toList())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /sales from Firebase: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("sales", error)
            }
        })

        // 5. Persistent Realtime ValueEventListener for `/expenses`
        registerListener(expensesRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val parsedMap = linkedMapOf<Long, Triple<ExpenseItem, Long, String>>() // id -> Triple(item, lastUpdated, childKey)
                        for (child in snapshot.children) {
                            val childKey = child.key ?: continue
                            val item = parseExpenseFromSnapshot(child) ?: continue
                            val lastUpdated = (child.child("lastUpdated").value as? Number)?.toLong()
                                ?: (child.child("updatedAt").value as? Number)?.toLong()
                                ?: 0L

                            val isCanonical = childKey == item.id.toString()
                            val existing = parsedMap[item.id]

                            if (existing == null) {
                                parsedMap[item.id] = Triple(item, lastUpdated, childKey)
                            } else {
                                val existingIsCanonical = existing.third == item.id.toString()
                                val shouldReplace = when {
                                    isCanonical && !existingIsCanonical -> true
                                    !isCanonical && existingIsCanonical -> false
                                    else -> lastUpdated >= existing.second
                                }
                                if (shouldReplace) {
                                    parsedMap[item.id] = Triple(item, lastUpdated, childKey)
                                }
                            }
                        }
                        val cloudExpenses = parsedMap.values.map { it.first }
                        syncRemoteExpensesToRoom(cloudExpenses)
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /expenses from Firebase: ${e.message}")
                    } finally {
                        _isInitialLoading.value = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("expenses", error)
                _isInitialLoading.value = false
            }
        })

        // 6. Persistent Realtime ValueEventListener for `/deleted_items`
        registerListener(deletedItemsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        for (child in snapshot.children) {
                            val key = child.key ?: continue
                            val id = parseIdFromString(key)
                            database.phoneDao().deletePhoneById(id)
                            database.expenseDao().deleteExpenseById(id)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice processing /deleted_items from Firebase: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("deleted_items", error)
            }
        })

        // 7. Persistent Realtime ValueEventListener for `/phones` (Master Directory)
        registerListener(phonesRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val cloudPhonesMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child)?.let {
                                val existing = cloudPhonesMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    cloudPhonesMap[it.id] = it
                                }
                            }
                        }
                        syncRemotePhonesToRoom(cloudPhonesMap.values.toList())
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /phones from Firebase: ${e.message}")
                    } finally {
                        _isInitialLoading.value = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("phones", error)
                _isInitialLoading.value = false
            }
        })

        // 8. Mirrors: `stock_items`, `transit_items`, `sales_records`
        registerListener(stockItemsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val stockPhonesMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_STOCK.code)?.let {
                                val existing = stockPhonesMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    stockPhonesMap[it.id] = it
                                }
                            }
                        }
                        if (stockPhonesMap.isNotEmpty()) syncRemotePhonesToRoom(stockPhonesMap.values.toList())
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing stock_items from Firebase: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("stock_items", error) }
        })

        registerListener(transitItemsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val transitPhonesMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_TRANSIT.code)?.let {
                                val existing = transitPhonesMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    transitPhonesMap[it.id] = it
                                }
                            }
                        }
                        if (transitPhonesMap.isNotEmpty()) syncRemotePhonesToRoom(transitPhonesMap.values.toList())
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing transit_items from Firebase: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("transit_items", error) }
        })

        registerListener(salesRecordsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val salesPhonesMap = linkedMapOf<Long, PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.SOLD.code)?.let {
                                val existing = salesPhonesMap[it.id]
                                if (existing == null || it.updatedAt >= existing.updatedAt) {
                                    salesPhonesMap[it.id] = it
                                }
                            }
                        }
                        if (salesPhonesMap.isNotEmpty()) syncRemotePhonesToRoom(salesPhonesMap.values.toList())
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing sales_records from Firebase: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("sales_records", error) }
        })

        // 9. Persistent Realtime ValueEventListener for `settings`
        registerListener(settingsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        if (snapshot.exists()) {
                            val remoteOpening = (snapshot.child("opening_balance").value as? Number)?.toDouble()
                                ?: (snapshot.child("openingBalance").value as? Number)?.toDouble()
                                ?: snapshot.child("opening_balance").getValue(String::class.java)?.toDoubleOrNull()
                            if (remoteOpening != null && remoteOpening >= 0.0) {
                                com.example.viewmodel.DashboardViewModel.updateStartingBalanceFromRemote(context, remoteOpening)
                            }

                            parseSettingsFromSnapshot(snapshot)?.let { cloudSettings ->
                                if (cloudSettings.ownerPin.isNotBlank()) cachedOwnerPin = cloudSettings.ownerPin
                                if (cloudSettings.staff1Pin.isNotBlank()) cachedStaff1Pin = cloudSettings.staff1Pin
                                if (cloudSettings.staff2Pin.isNotBlank()) cachedStaff2Pin = cloudSettings.staff2Pin

                                database.settingsDao().insertOrUpdateSettings(cloudSettings)
                                Log.d(TAG, "Live synced settings from Firebase: shopName=${cloudSettings.shopName}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing settings from Firebase: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("settings", error) }
        })

        // 9b. Persistent Realtime ValueEventListener for `financials/opening_balance`
        registerListener(firebaseDb.getReference("financials").child("opening_balance"), object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val amount = when (val raw = snapshot.value) {
                    is Number -> raw.toDouble()
                    is String -> raw.toDoubleOrNull()
                    else -> null
                }
                if (amount != null && amount >= 0.0) {
                    com.example.viewmodel.DashboardViewModel.updateStartingBalanceFromRemote(context, amount)
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("financials/opening_balance", error) }
        })

        // 10. Persistent Realtime ValueEventListener for `auth_pins`
        registerListener(authPinsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        if (snapshot.exists()) {
                            val ownerPin = snapshot.child("ownerPin").getValue(String::class.java)
                                ?: snapshot.child("ADMIN").child("pin").getValue(String::class.java)
                            val staff1Pin = snapshot.child("staff1Pin").getValue(String::class.java)
                                ?: snapshot.child("STAFF_1").child("pin").getValue(String::class.java)
                            val staff2Pin = snapshot.child("staff2Pin").getValue(String::class.java)
                                ?: snapshot.child("STAFF_2").child("pin").getValue(String::class.java)

                            if (!ownerPin.isNullOrBlank()) cachedOwnerPin = ownerPin
                            if (!staff1Pin.isNullOrBlank()) cachedStaff1Pin = staff1Pin
                            if (!staff2Pin.isNullOrBlank()) cachedStaff2Pin = staff2Pin

                            val currentLocal = database.settingsDao().getSettingsDirect() ?: AppSettings()
                            val updatedLocal = currentLocal.copy(
                                ownerPin = ownerPin ?: currentLocal.ownerPin,
                                staff1Pin = staff1Pin ?: currentLocal.staff1Pin,
                                staff2Pin = staff2Pin ?: currentLocal.staff2Pin
                            )
                            database.settingsDao().insertOrUpdateSettings(updatedLocal)
                            Log.d(TAG, "Live synced auth PINs from Firebase Cloud")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing auth PINs from Firebase: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { handleListenerCancelled("auth_pins", error) }
        })

        // 11. Persistent Realtime ValueEventListener for `/customer_dues`
        registerListener(customerDuesRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        // CustomerKey -> (Name, Phone, (LinkedHashMap<String, CustomerDueRecord>, LinkedHashMap<String, DuePaymentRecord>))
                        val customerMap = linkedMapOf<String, Triple<String, String, Pair<LinkedHashMap<String, CustomerDueRecord>, LinkedHashMap<String, DuePaymentRecord>>>>()

                        fun getOrCreateCustomer(key: String, defaultName: String, defaultPhone: String): Pair<LinkedHashMap<String, CustomerDueRecord>, LinkedHashMap<String, DuePaymentRecord>> {
                            val existing = customerMap[key]
                            if (existing != null) {
                                return existing.third
                            }
                            val salesMap = linkedMapOf<String, CustomerDueRecord>()
                            val paymentsMap = linkedMapOf<String, DuePaymentRecord>()
                            customerMap[key] = Triple(defaultName, defaultPhone, Pair(salesMap, paymentsMap))
                            return Pair(salesMap, paymentsMap)
                        }

                        fun parseSnapDouble(snap: DataSnapshot, vararg keys: String): Double {
                            for (key in keys) {
                                val child = snap.child(key)
                                val num = (child.value as? Number)?.toDouble()
                                if (num != null) return num
                                val str = child.getValue(String::class.java)
                                val parsed = str?.trim()?.toDoubleOrNull()
                                if (parsed != null) return parsed
                            }
                            return 0.0
                        }

                        for (custChild in snapshot.children) {
                            val custKey = custChild.key ?: continue
                            val rawName = custChild.child("customerName").getValue(String::class.java)
                                ?: custChild.child("name").getValue(String::class.java)
                            val rawPhone = custChild.child("customerPhone").getValue(String::class.java)
                                ?: custChild.child("phone").getValue(String::class.java)

                            val hasNestedDues = custChild.hasChild("dues") || custChild.hasChild("records") || custChild.hasChild("sales")
                            val hasNestedPayments = custChild.hasChild("payments") || custChild.hasChild("collections")
                            val directDueAmount = parseSnapDouble(custChild, "dueAmount", "amount", "remainingDue", "totalDue", "due", "dueAmountBdt", "balance")
                            val isDirectDue = directDueAmount > 0.0 || custChild.hasChild("dueAmount") || custChild.hasChild("amount")
                            val hasDirectChildrenDues = !hasNestedDues && !hasNestedPayments && !isDirectDue && custChild.children.any {
                                it.hasChild("dueAmount") || it.hasChild("amount") || it.hasChild("totalSalePrice")
                            }

                            if (hasNestedDues || hasNestedPayments || hasDirectChildrenDues) {
                                val custName = rawName ?: "Walk-in Customer"
                                val custPhone = rawPhone ?: custKey
                                val (salesMap, paymentsMap) = getOrCreateCustomer(custKey, custName, custPhone)

                                val duesNodes = listOfNotNull(
                                    if (custChild.hasChild("dues")) custChild.child("dues") else null,
                                    if (custChild.hasChild("records")) custChild.child("records") else null,
                                    if (custChild.hasChild("sales")) custChild.child("sales") else null,
                                    if (hasDirectChildrenDues) custChild else null
                                )

                                for (dNode in duesNodes) {
                                    for (dSnap in dNode.children) {
                                        if (dSnap.key == "payments" || dSnap.key == "collections") continue
                                        val dueAmt = parseSnapDouble(dSnap, "dueAmount", "amount", "remainingDue", "due", "dueAmountBdt")
                                        val totalSale = parseSnapDouble(dSnap, "totalSalePrice", "sellingPriceBdt", "totalPrice", "amount", "dueAmount")
                                        val cashRec = parseSnapDouble(dSnap, "cashReceived", "cashReceivedBdt")
                                        val finalDue = if (dueAmt > 0.0) dueAmt else (totalSale - cashRec).coerceAtLeast(0.0)
                                        if (finalDue <= 0.0 && totalSale <= 0.0) continue

                                        val dId = dSnap.key ?: ""
                                        val pId = (dSnap.child("phoneId").value as? Number)?.toLong() ?: 0L
                                        val uniqueKey = if (dId.isNotBlank()) dId else (if (pId > 0) pId.toString() else System.currentTimeMillis().toString())

                                        val dueRec = CustomerDueRecord(
                                            id = uniqueKey,
                                            phoneId = pId,
                                            phoneModel = dSnap.child("phoneModel").getValue(String::class.java)
                                                ?: dSnap.child("model").getValue(String::class.java)
                                                ?: "Historical Due Record",
                                            imei = dSnap.child("imei").getValue(String::class.java) ?: "",
                                            totalSalePrice = if (totalSale > 0.0) totalSale else finalDue,
                                            cashReceived = cashRec,
                                            dueAmount = finalDue,
                                            saleDate = dSnap.child("saleDate").getValue(String::class.java)
                                                ?: dSnap.child("date").getValue(String::class.java)
                                                ?: "",
                                            customerName = dSnap.child("customerName").getValue(String::class.java) ?: custName,
                                            customerPhone = dSnap.child("customerPhone").getValue(String::class.java) ?: custPhone,
                                            soldByStaff = dSnap.child("soldByStaff").getValue(String::class.java) ?: "",
                                            notes = dSnap.child("notes").getValue(String::class.java) ?: "",
                                            timestamp = (dSnap.child("timestamp").value as? Number)?.toLong() ?: System.currentTimeMillis()
                                        )
                                        val existing = salesMap[uniqueKey]
                                        if (existing == null || dueRec.timestamp >= existing.timestamp) {
                                            salesMap[uniqueKey] = dueRec
                                        }
                                    }
                                }

                                val payNodes = listOfNotNull(
                                    if (custChild.hasChild("payments")) custChild.child("payments") else null,
                                    if (custChild.hasChild("collections")) custChild.child("collections") else null
                                )

                                for (pNode in payNodes) {
                                    for (pSnap in pNode.children) {
                                        val payAmt = parseSnapDouble(pSnap, "amount", "amountBdt", "collectedAmount")
                                        if (payAmt <= 0.0) continue
                                        val payId = pSnap.key ?: System.currentTimeMillis().toString()
                                        val payRec = DuePaymentRecord(
                                            id = payId,
                                            customerKey = custKey,
                                            customerName = pSnap.child("customerName").getValue(String::class.java) ?: custName,
                                            customerPhone = pSnap.child("customerPhone").getValue(String::class.java) ?: custPhone,
                                            amount = payAmt,
                                            paymentMethod = pSnap.child("paymentMethod").getValue(String::class.java) ?: "Cash",
                                            collectedBy = pSnap.child("collectedBy").getValue(String::class.java) ?: "",
                                            date = pSnap.child("date").getValue(String::class.java) ?: "",
                                            notes = pSnap.child("notes").getValue(String::class.java) ?: "",
                                            timestamp = (pSnap.child("timestamp").value as? Number)?.toLong() ?: System.currentTimeMillis()
                                        )
                                        val existing = paymentsMap[payId]
                                        if (existing == null || payRec.timestamp >= existing.timestamp) {
                                            paymentsMap[payId] = payRec
                                        }
                                    }
                                }
                            } else if (isDirectDue) {
                                // Direct/flat historical due record stored at /customer_dues/{dueId}
                                val custName = rawName ?: "Walk-in Customer"
                                val custPhone = rawPhone ?: ""
                                val targetKey = CustomerDueUtils.getCustomerKey(custPhone, custName)
                                val (salesMap, _) = getOrCreateCustomer(targetKey, custName, custPhone)

                                val totalSale = parseSnapDouble(custChild, "totalSalePrice", "totalPrice", "sellingPriceBdt", "amount", "dueAmount")
                                val cashRec = parseSnapDouble(custChild, "cashReceived", "cashReceivedBdt")
                                val finalDue = if (directDueAmount > 0.0) directDueAmount else (totalSale - cashRec).coerceAtLeast(0.0)

                                val dueRec = CustomerDueRecord(
                                    id = custKey,
                                    phoneId = (custChild.child("phoneId").value as? Number)?.toLong() ?: 0L,
                                    phoneModel = custChild.child("phoneModel").getValue(String::class.java)
                                        ?: custChild.child("model").getValue(String::class.java)
                                        ?: "Direct Due Adjustment",
                                    imei = custChild.child("imei").getValue(String::class.java) ?: "",
                                    totalSalePrice = if (totalSale > 0.0) totalSale else finalDue,
                                    cashReceived = cashRec,
                                    dueAmount = finalDue,
                                    saleDate = custChild.child("saleDate").getValue(String::class.java)
                                        ?: custChild.child("date").getValue(String::class.java)
                                        ?: "",
                                    customerName = custName,
                                    customerPhone = custPhone,
                                    soldByStaff = custChild.child("soldByStaff").getValue(String::class.java) ?: "",
                                    notes = custChild.child("notes").getValue(String::class.java) ?: "",
                                    timestamp = (custChild.child("timestamp").value as? Number)?.toLong() ?: System.currentTimeMillis()
                                )
                                val existing = salesMap[custKey]
                                if (existing == null || dueRec.timestamp >= existing.timestamp) {
                                    salesMap[custKey] = dueRec
                                }
                            }
                        }

                        // Also merge local sold phones with outstanding due balance if not already present
                        try {
                            val soldPhones = database.phoneDao().getAllPhonesDirect().filter {
                                it.status == PhoneStatus.SOLD.code && it.effectiveDueAmount > 0.0 && it.customerName.isNotBlank()
                            }
                            for (sp in soldPhones) {
                                val cKey = CustomerDueUtils.getCustomerKey(sp.customerPhone, sp.customerName)
                                val (sMap, _) = getOrCreateCustomer(cKey, sp.customerName, sp.customerPhone)
                                val spKey = sp.id.toString()
                                val alreadyExists = sMap.containsKey(spKey) || sMap.values.any { it.phoneId == sp.id || (it.imei.isNotBlank() && it.imei == sp.imei) }
                                if (!alreadyExists) {
                                    sMap[spKey] = CustomerDueRecord(
                                        id = spKey,
                                        phoneId = sp.id,
                                        phoneModel = "${sp.brand} ${sp.model}".trim(),
                                        imei = sp.imei,
                                        totalSalePrice = sp.sellingPriceBdt,
                                        cashReceived = sp.effectiveCashReceived,
                                        dueAmount = sp.effectiveDueAmount,
                                        saleDate = sp.saleDate,
                                        customerName = sp.customerName,
                                        customerPhone = sp.customerPhone,
                                        soldByStaff = sp.soldByStaff,
                                        notes = sp.notes,
                                        timestamp = sp.createdAt
                                    )
                                }
                            }
                        } catch (_: Exception) {}

                        val profiles = mutableListOf<CustomerDueProfile>()
                        var totalOutstanding = 0.0

                        for ((cKey, data) in customerMap) {
                            val (cName, cPhone, maps) = data
                            val salesList = maps.first.values.toList()
                            val paymentsList = maps.second.values.toList()

                            val uniqueSales = salesList.distinctBy {
                                when {
                                    it.phoneId > 0L -> "phone_${it.phoneId}"
                                    it.imei.isNotBlank() -> "imei_${it.imei.trim()}"
                                    it.id.isNotBlank() -> "id_${it.id}"
                                    else -> "sale_${it.saleDate}_${it.dueAmount}_${it.timestamp}"
                                }
                            }
                            val uniquePayments = paymentsList.distinctBy {
                                if (it.id.isNotBlank()) "pay_${it.id}"
                                else "pay_${it.customerKey}_${it.date}_${it.amount}_${it.timestamp}"
                            }

                            val totalCredit = uniqueSales.sumOf { it.dueAmount }
                            val totalPaid = uniquePayments.sumOf { it.amount }
                            val remaining = (totalCredit - totalPaid).coerceAtLeast(0.0)
                            totalOutstanding += remaining

                            val sortedSales = uniqueSales.sortedWith(
                                compareByDescending<CustomerDueRecord> {
                                    val p = com.example.util.DateUtils.parseDateToMillis(it.saleDate)
                                    if (p > 0L) p else it.timestamp
                                }.thenByDescending { it.id }
                            )
                            val sortedPayments = uniquePayments.sortedWith(
                                compareByDescending<DuePaymentRecord> {
                                    val p = com.example.util.DateUtils.parseDateToMillis(it.date)
                                    if (p > 0L) p else it.timestamp
                                }.thenByDescending { it.id }
                            )
                            val lastDate = when {
                                sortedPayments.isNotEmpty() && (sortedSales.isEmpty() || sortedPayments.first().timestamp >= sortedSales.first().timestamp) -> sortedPayments.first().date
                                sortedSales.isNotEmpty() -> sortedSales.first().saleDate
                                else -> ""
                            }

                            profiles.add(
                                CustomerDueProfile(
                                    customerKey = cKey,
                                    customerName = cName.trim().uppercase(),
                                    customerPhone = cPhone.trim(),
                                    totalCreditSales = uniqueSales.sumOf { it.totalSalePrice },
                                    totalCashReceivedAtSale = uniqueSales.sumOf { it.cashReceived },
                                    totalDueCollected = totalPaid,
                                    remainingDue = remaining,
                                    creditSales = sortedSales,
                                    paymentCollections = sortedPayments,
                                    lastActivityDate = lastDate
                                )
                            )
                        }

                        _customerDueProfiles.value = profiles.distinctBy { it.customerKey }.sortedByDescending { it.remainingDue }
                        _totalOutstandingDue.value = totalOutstanding
                        com.example.viewmodel.DashboardViewModel.updateGlobalTotalOutstandingDue(totalOutstanding)
                        Log.d(TAG, "Live synced ${profiles.size} customer due profiles; total due: ৳ $totalOutstanding")
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /customer_dues: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("customer_dues", error)
            }
        })

        // 12. Persistent Realtime ValueEventListener for `/financials/due_collections`
        registerListener(dueCollectionsRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val paymentMap = linkedMapOf<String, DuePaymentRecord>()
                        for (child in snapshot.children) {
                            val key = child.key ?: continue
                            val amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0
                            val record = DuePaymentRecord(
                                id = key,
                                customerKey = child.child("customerKey").getValue(String::class.java) ?: "",
                                customerName = child.child("customerName").getValue(String::class.java) ?: "",
                                customerPhone = child.child("customerPhone").getValue(String::class.java) ?: "",
                                amount = amount,
                                paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "Cash",
                                collectedBy = child.child("collectedBy").getValue(String::class.java) ?: "",
                                date = child.child("date").getValue(String::class.java) ?: "",
                                notes = child.child("notes").getValue(String::class.java) ?: "",
                                timestamp = (child.child("timestamp").value as? Number)?.toLong() ?: 0L
                            )
                            paymentMap[key] = record
                        }
                        val distinctList = paymentMap.values.toList().distinctBy { it.id }.sortedWith(
                            compareByDescending<DuePaymentRecord> {
                                if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                            }.thenByDescending { it.id }
                        )
                        val totalAmount = distinctList.sumOf { it.amount }
                        _allDuePayments.value = distinctList
                        _totalDueCollectionsAmount.value = totalAmount
                        com.example.viewmodel.DashboardViewModel.updateGlobalDueCollections(totalAmount)
                        Log.d(TAG, "Live synced ${distinctList.size} due collections; total collected: ৳ $totalAmount")
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /financials/due_collections: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("financials/due_collections", error)
            }
        })

        // 13. Persistent Realtime ValueEventListener for `/other_income`
        registerListener(otherIncomeRef, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val incomeMap = linkedMapOf<String, OtherIncomeItem>()
                        for (child in snapshot.children) {
                            val item = parseOtherIncomeFromSnapshot(child) ?: continue
                            incomeMap[item.id] = item
                        }
                        val distinctList = incomeMap.values.toList().distinctBy { it.id }.sortedWith(
                            compareByDescending<OtherIncomeItem> {
                                if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                            }.thenByDescending { it.id }
                        )
                        val total = distinctList.sumOf { it.amountBdt }
                        _allOtherIncomes.value = distinctList
                        _totalOtherIncomeAmount.value = total
                        com.example.viewmodel.DashboardViewModel.updateGlobalOtherIncomes(distinctList)
                        Log.d(TAG, "Live synced ${distinctList.size} other income entries; total: ৳ $total")
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice syncing /other_income: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleListenerCancelled("other_income", error)
            }
        })
    }

    fun postOtherIncome(item: OtherIncomeItem, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val key = if (item.id.isNotBlank()) item.id else (otherIncomeRef.push().key ?: System.currentTimeMillis().toString())
            val finalItem = item.copy(id = key)
            val map = otherIncomeToMap(finalItem)
            otherIncomeRef.child(key).setValue(map).addOnCompleteListener { task ->
                val success = task.isSuccessful
                if (success) {
                    Log.d(TAG, "Pushed other income to /other_income/$key: ৳ ${finalItem.amountBdt}")
                } else {
                    Log.w(TAG, "Failed pushing other income: ${task.exception?.message}")
                }
                onComplete?.invoke(success)
            }
        } catch (e: Exception) {
            handleWriteFailure("post_other_income", e)
            onComplete?.invoke(false)
        }
    }

    fun updateOtherIncome(item: OtherIncomeItem, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val key = item.id
            if (key.isBlank()) {
                postOtherIncome(item, onComplete)
                return
            }
            val map = otherIncomeToMap(item)
            otherIncomeRef.child(key).updateChildren(map).addOnCompleteListener { task ->
                val success = task.isSuccessful
                if (success) {
                    Log.d(TAG, "Updated other income /other_income/$key: ৳ ${item.amountBdt}")
                } else {
                    Log.w(TAG, "Failed updating other income: ${task.exception?.message}")
                }
                onComplete?.invoke(success)
            }
        } catch (e: Exception) {
            handleWriteFailure("update_other_income", e)
            onComplete?.invoke(false)
        }
    }

    fun deleteOtherIncome(incomeId: String, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            otherIncomeRef.child(incomeId).removeValue().addOnCompleteListener { task ->
                val success = task.isSuccessful
                if (success) {
                    Log.d(TAG, "Deleted other income /other_income/$incomeId")
                } else {
                    Log.w(TAG, "Failed deleting other income: ${task.exception?.message}")
                }
                onComplete?.invoke(success)
            }
        } catch (e: Exception) {
            handleWriteFailure("delete_other_income", e)
            onComplete?.invoke(false)
        }
    }

    fun postCustomerDue(due: CustomerDueRecord) {
        try {
            val custKey = CustomerDueUtils.getCustomerKey(due.customerPhone, due.customerName)
            val dueKey = if (due.id.isNotBlank()) due.id else (if (due.phoneId > 0) due.phoneId.toString() else System.currentTimeMillis().toString())

            val map = mapOf(
                "id" to dueKey,
                "phoneId" to due.phoneId,
                "phoneModel" to due.phoneModel,
                "imei" to due.imei,
                "totalSalePrice" to due.totalSalePrice,
                "cashReceived" to due.cashReceived,
                "dueAmount" to due.dueAmount,
                "saleDate" to due.saleDate,
                "customerName" to due.customerName,
                "customerPhone" to due.customerPhone,
                "soldByStaff" to due.soldByStaff,
                "notes" to due.notes,
                "timestamp" to due.timestamp
            )

            customerDuesRef.child(custKey).child("dues").child(dueKey).setValue(map)
            customerDuesRef.child(custKey).child("customerName").setValue(due.customerName)
            customerDuesRef.child(custKey).child("customerPhone").setValue(due.customerPhone)
            customerDuesRef.child(custKey).child("lastUpdated").setValue(System.currentTimeMillis())
            Log.d(TAG, "Pushed customer due to /customer_dues/$custKey/dues/$dueKey: ৳ ${due.dueAmount}")
        } catch (e: Exception) {
            handleWriteFailure("post_customer_due", e)
        }
    }

    fun recordDueCollectionPayment(payment: DuePaymentRecord) {
        try {
            val custKey = if (payment.customerKey.isNotBlank()) payment.customerKey else CustomerDueUtils.getCustomerKey(payment.customerPhone, payment.customerName)
            val paymentKey = if (payment.id.isNotBlank()) payment.id else System.currentTimeMillis().toString()

            val map = mapOf(
                "id" to paymentKey,
                "customerKey" to custKey,
                "customerName" to payment.customerName,
                "customerPhone" to payment.customerPhone,
                "amount" to payment.amount,
                "paymentMethod" to payment.paymentMethod,
                "collectedBy" to payment.collectedBy,
                "date" to payment.date,
                "notes" to payment.notes,
                "timestamp" to payment.timestamp
            )

            // 1. Post to customer profile payments
            customerDuesRef.child(custKey).child("payments").child(paymentKey).setValue(map)
            customerDuesRef.child(custKey).child("lastUpdated").setValue(System.currentTimeMillis())

            // 2. Post to global financials/due_collections for cash reconciliation
            dueCollectionsRef.child(paymentKey).setValue(map)
            Log.d(TAG, "Recorded due collection of ৳ ${payment.amount} for customer $custKey")
        } catch (e: Exception) {
            handleWriteFailure("record_due_collection_payment", e)
        }
    }

    fun syncSaleWithCustomerDue(phone: PhoneItem) {
        try {
            val key = phone.id.toString()
            val custName = phone.customerName.trim().uppercase()
            val custPhone = phone.customerPhone.trim()
            val custKey = CustomerDueUtils.getCustomerKey(custPhone, custName)
            val now = System.currentTimeMillis()

            // Remove this phone's due record from any other customer nodes if customer was changed
            customerDuesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (cSnap in snapshot.children) {
                        val cKey = cSnap.key ?: continue
                        if (cKey != custKey && cSnap.child("dues").hasChild(key)) {
                            cSnap.ref.child("dues").child(key).removeValue()
                            cSnap.ref.child("lastUpdated").setValue(now)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            if (phone.dueAmountBdt > 0.0 && custName.isNotBlank()) {
                val dueMap = mapOf(
                    "id" to key,
                    "phoneId" to phone.id,
                    "phoneModel" to "${phone.brand} ${phone.model}".trim(),
                    "imei" to phone.imei,
                    "totalSalePrice" to phone.sellingPriceBdt,
                    "cashReceived" to phone.cashReceivedBdt,
                    "dueAmount" to phone.dueAmountBdt,
                    "saleDate" to phone.saleDate,
                    "customerName" to custName,
                    "customerPhone" to custPhone,
                    "soldByStaff" to phone.soldByStaff,
                    "notes" to phone.notes,
                    "timestamp" to if (phone.createdAt > 0L) phone.createdAt else now,
                    "lastUpdated" to now
                )
                customerDuesRef.child(custKey).child("dues").child(key).setValue(dueMap)
                customerDuesRef.child(custKey).child("customerName").setValue(custName)
                customerDuesRef.child(custKey).child("customerPhone").setValue(custPhone)
                customerDuesRef.child(custKey).child("lastUpdated").setValue(now)
                Log.d(TAG, "Synced sale due record under /customer_dues/$custKey/dues/$key: ৳${phone.dueAmountBdt}")
            } else {
                if (custKey.isNotBlank()) {
                    customerDuesRef.child(custKey).child("dues").child(key).removeValue()
                    customerDuesRef.child(custKey).child("lastUpdated").setValue(now)
                }
            }
        } catch (e: Exception) {
            handleWriteFailure("sync_sale_due", e)
        }
    }

    fun revertCustomerDue(phoneId: Long, customerPhone: String, customerName: String) {
        try {
            val custKey = CustomerDueUtils.getCustomerKey(customerPhone, customerName)
            val dueKey = phoneId.toString()
            customerDuesRef.child(custKey).child("dues").child(dueKey).removeValue()
            customerDuesRef.child(custKey).child("lastUpdated").setValue(System.currentTimeMillis())
            Log.d(TAG, "Reverted customer due for phone $phoneId under $custKey")
        } catch (e: Exception) {
            handleWriteFailure("revert_customer_due", e)
        }
    }

    fun deleteCustomerDue(customerKey: String, dueId: String) {
        try {
            customerDuesRef.child(customerKey).child("dues").child(dueId).removeValue()
            customerDuesRef.child(customerKey).child("lastUpdated").setValue(System.currentTimeMillis())
            Log.d(TAG, "Deleted customer due $dueId under $customerKey")
        } catch (e: Exception) {
            handleWriteFailure("delete_customer_due", e)
        }
    }

    fun deleteDuePayment(customerKey: String, paymentId: String) {
        try {
            val now = System.currentTimeMillis()
            if (customerKey.isNotBlank()) {
                customerDuesRef.child(customerKey).child("payments").child(paymentId).removeValue()
                customerDuesRef.child(customerKey).child("lastUpdated").setValue(now)
            }
            dueCollectionsRef.child(paymentId).removeValue()

            // Remove from any customer node containing paymentId
            customerDuesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    for (cSnap in snap.children) {
                        if (cSnap.child("payments").hasChild(paymentId)) {
                            cSnap.ref.child("payments").child(paymentId).removeValue()
                            cSnap.ref.child("lastUpdated").setValue(now)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Immediately remove from in-memory StateFlow
            val currentList = _allDuePayments.value.toMutableList()
            currentList.removeAll { it.id == paymentId }
            _allDuePayments.value = currentList
            Log.d(TAG, "Deleted due payment $paymentId under $customerKey")
        } catch (e: Exception) {
            handleWriteFailure("delete_due_payment", e)
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
        try {
            val now = System.currentTimeMillis()
            val parsedTimestamp = com.example.util.DateUtils.parseDateToMillis(newDate)
            val updatedTimestamp = if (parsedTimestamp > 0L) parsedTimestamp else now
            val updates = mapOf<String, Any>(
                "dueAmount" to newDueAmount,
                "totalSalePrice" to newTotalSalePrice,
                "cashReceived" to newCashReceived,
                "saleDate" to newDate,
                "date" to newDate,
                "notes" to newNotes,
                "timestamp" to updatedTimestamp,
                "lastUpdated" to now
            )

            // 1. Direct atomic update on nested child references
            if (customerKey.isNotBlank()) {
                customerDuesRef.child(customerKey).child("dues").child(dueId).updateChildren(updates)
                customerDuesRef.child(customerKey).child("lastUpdated").setValue(now)
            }

            // 2. Direct check across flat direct due nodes or other customer nodes
            customerDuesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (snap.hasChild(dueId)) {
                        customerDuesRef.child(dueId).updateChildren(updates)
                        customerDuesRef.child(dueId).child("lastUpdated").setValue(now)
                    }
                    for (cSnap in snap.children) {
                        if (cSnap.child("dues").hasChild(dueId)) {
                            cSnap.ref.child("dues").child(dueId).updateChildren(updates)
                            cSnap.ref.child("lastUpdated").setValue(now)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // 3. Immediately update in-memory StateFlow to ensure instant reactive UI update
            val currentProfiles = _customerDueProfiles.value.toMutableList()
            val pIdx = currentProfiles.indexOfFirst { it.customerKey == customerKey || it.creditSales.any { s -> s.id == dueId } }
            if (pIdx >= 0) {
                val prof = currentProfiles[pIdx]
                val updatedSales = prof.creditSales.map { s ->
                    if (s.id == dueId) {
                        s.copy(
                            dueAmount = newDueAmount,
                            totalSalePrice = newTotalSalePrice,
                            cashReceived = newCashReceived,
                            saleDate = newDate,
                            notes = newNotes,
                            timestamp = updatedTimestamp
                        )
                    } else s
                }
                val totalCredit = updatedSales.sumOf { it.dueAmount }
                val totalPaid = prof.paymentCollections.sumOf { it.amount }
                val remaining = (totalCredit - totalPaid).coerceAtLeast(0.0)
                currentProfiles[pIdx] = prof.copy(
                    creditSales = updatedSales.sortedWith(
                        compareByDescending<CustomerDueRecord> {
                            val p = com.example.util.DateUtils.parseDateToMillis(it.saleDate)
                            if (p > 0L) p else it.timestamp
                        }.thenByDescending { it.id }
                    ),
                    totalCreditSales = updatedSales.sumOf { it.totalSalePrice },
                    totalCashReceivedAtSale = updatedSales.sumOf { it.cashReceived },
                    remainingDue = remaining
                )
                _customerDueProfiles.value = currentProfiles
            }

            val phoneId = dueId.toLongOrNull()
            if (phoneId != null && phoneId > 0) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val phone = database.phoneDao().getPhoneById(phoneId)
                        if (phone != null) {
                            val updatedPhone = phone.copy(
                                dueAmountBdt = newDueAmount,
                                cashReceivedBdt = newCashReceived,
                                sellingPriceBdt = newTotalSalePrice,
                                saleDate = newDate,
                                notes = newNotes,
                                updatedAt = now
                            )
                            database.phoneDao().updatePhone(updatedPhone)
                            pushPhone(updatedPhone)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice updating phone for due $dueId: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "Updated customer due $dueId under $customerKey: due=৳$newDueAmount, total=৳$newTotalSalePrice, date=$newDate, timestamp=$updatedTimestamp")
        } catch (e: Exception) {
            handleWriteFailure("update_customer_due_record", e)
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
        try {
            val now = System.currentTimeMillis()
            val parsedTimestamp = com.example.util.DateUtils.parseDateToMillis(newDate)
            val updatedTimestamp = if (parsedTimestamp > 0L) parsedTimestamp else now
            val updates = mapOf<String, Any>(
                "amount" to newAmount,
                "paymentMethod" to newPaymentMethod,
                "collectedBy" to newCollectedBy,
                "date" to newDate,
                "notes" to newNotes,
                "timestamp" to updatedTimestamp,
                "lastUpdated" to now
            )

            // Direct atomic update on child references in Firebase Realtime DB
            if (customerKey.isNotBlank()) {
                customerDuesRef.child(customerKey).child("payments").child(paymentId).updateChildren(updates)
                customerDuesRef.child(customerKey).child("lastUpdated").setValue(now)
            }
            dueCollectionsRef.child(paymentId).updateChildren(updates)

            // Direct check across all customer due nodes to guarantee persistence on the matched customer
            customerDuesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    for (cSnap in snap.children) {
                        if (cSnap.child("payments").hasChild(paymentId)) {
                            cSnap.ref.child("payments").child(paymentId).updateChildren(updates)
                            cSnap.ref.child("lastUpdated").setValue(now)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Immediately update in-memory StateFlow to prevent reverting to stale cache
            val currentList = _allDuePayments.value.toMutableList()
            val idx = currentList.indexOfFirst { it.id == paymentId }
            if (idx >= 0) {
                val old = currentList[idx]
                currentList[idx] = old.copy(
                    amount = newAmount,
                    paymentMethod = newPaymentMethod,
                    collectedBy = newCollectedBy,
                    date = newDate,
                    notes = newNotes,
                    timestamp = updatedTimestamp
                )
                _allDuePayments.value = currentList
            }

            // Immediately update _customerDueProfiles in-memory state
            val currentProfiles = _customerDueProfiles.value.toMutableList()
            val pIdx = currentProfiles.indexOfFirst { it.customerKey == customerKey || it.paymentCollections.any { p -> p.id == paymentId } }
            if (pIdx >= 0) {
                val prof = currentProfiles[pIdx]
                val updatedPays = prof.paymentCollections.map { p ->
                    if (p.id == paymentId) {
                        p.copy(
                            amount = newAmount,
                            paymentMethod = newPaymentMethod,
                            collectedBy = newCollectedBy,
                            date = newDate,
                            notes = newNotes,
                            timestamp = updatedTimestamp
                        )
                    } else p
                }
                val totalCredit = prof.creditSales.sumOf { it.dueAmount }
                val totalPaid = updatedPays.sumOf { it.amount }
                val remaining = (totalCredit - totalPaid).coerceAtLeast(0.0)
                currentProfiles[pIdx] = prof.copy(
                    paymentCollections = updatedPays.sortedWith(
                        compareByDescending<DuePaymentRecord> {
                            val p = com.example.util.DateUtils.parseDateToMillis(it.date)
                            if (p > 0L) p else it.timestamp
                        }.thenByDescending { it.id }
                    ),
                    totalDueCollected = totalPaid,
                    remainingDue = remaining
                )
                _customerDueProfiles.value = currentProfiles
            }

            Log.d(TAG, "Updated due payment $paymentId under $customerKey: amount=৳$newAmount, date=$newDate, timestamp=$updatedTimestamp")
        } catch (e: Exception) {
            handleWriteFailure("update_due_payment_record", e)
        }
    }

    /**
     * Admin capability to edit Customer Name and Phone Number across all linked records in Firebase Realtime DB.
     */
    fun updateCustomerDetails(oldCustomerKey: String, newName: String, newPhone: String) {
        try {
            val trimmedName = newName.trim()
            val trimmedPhone = newPhone.trim()
            val newKey = CustomerDueUtils.getCustomerKey(trimmedPhone, trimmedName)
            val now = System.currentTimeMillis()
            val targetKey = if (newKey.isNotBlank()) newKey else oldCustomerKey

            customerDuesRef.child(oldCustomerKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawMap = (snapshot.value as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                    rawMap["customerName"] = trimmedName
                    rawMap["customerPhone"] = trimmedPhone
                    rawMap["name"] = trimmedName
                    rawMap["phone"] = trimmedPhone
                    rawMap["lastUpdated"] = now

                    // Write updated profile
                    customerDuesRef.child(targetKey).setValue(rawMap)

                    if (targetKey != oldCustomerKey) {
                        customerDuesRef.child(oldCustomerKey).removeValue()
                    }

                    // Update linked dues
                    customerDuesRef.child(targetKey).child("dues").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(duesSnap: DataSnapshot) {
                            for (dSnap in duesSnap.children) {
                                dSnap.ref.child("customerName").setValue(trimmedName)
                                dSnap.ref.child("customerPhone").setValue(trimmedPhone)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })

                    // Update linked payments and global financials/due_collections
                    customerDuesRef.child(targetKey).child("payments").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(paySnap: DataSnapshot) {
                            for (pSnap in paySnap.children) {
                                pSnap.ref.child("customerName").setValue(trimmedName)
                                pSnap.ref.child("customerPhone").setValue(trimmedPhone)
                                pSnap.ref.child("customerKey").setValue(targetKey)
                                val payId = pSnap.key
                                if (!payId.isNullOrBlank()) {
                                    dueCollectionsRef.child(payId).child("customerName").setValue(trimmedName)
                                    dueCollectionsRef.child(payId).child("customerPhone").setValue(trimmedPhone)
                                    dueCollectionsRef.child(payId).child("customerKey").setValue(targetKey)
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Update matching sold phones in Room & Firebase
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val allPhones = database.phoneDao().getAllPhonesDirect()
                    val matchingPhones = allPhones.filter { phone ->
                        val phoneCustKey = CustomerDueUtils.getCustomerKey(phone.customerPhone, phone.customerName)
                        phoneCustKey == oldCustomerKey || phoneCustKey == targetKey
                    }
                    for (phone in matchingPhones) {
                        val updated = phone.copy(
                            customerName = trimmedName,
                            customerPhone = trimmedPhone,
                            updatedAt = now
                        )
                        pushPhone(updated)
                        database.phoneDao().updatePhone(updated)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Notice updating phone customer details: ${e.message}")
                }
            }
            Log.d(TAG, "Updated customer details: $oldCustomerKey -> $targetKey: $trimmedName ($trimmedPhone)")
        } catch (e: Exception) {
            handleWriteFailure("update_customer_details", e)
        }
    }

    /**
     * Differential, non-destructive synchronization of remote phone items into local Room DB.
     * Prevents wiping local SQLite data and eliminates UI flicker.
     */
    private suspend fun syncRemotePhonesToRoom(remotePhones: List<PhoneItem>) = withContext(Dispatchers.IO) {
        try {
            if (remotePhones.isEmpty()) return@withContext

            val localPhones = database.phoneDao().getAllPhonesDirect()
            val localMap = localPhones.associateBy { it.id }

            val toUpsert = mutableListOf<PhoneItem>()
            for (remote in remotePhones) {
                val local = localMap[remote.id]
                if (local == null) {
                    toUpsert.add(remote)
                } else if (remote.updatedAt >= local.updatedAt) {
                    if (remote != local) {
                        toUpsert.add(remote)
                    }
                } else if (local.updatedAt > remote.updatedAt) {
                    // Local change is newer; push to cloud
                    pushPhone(local)
                }
            }

            if (toUpsert.isNotEmpty()) {
                database.phoneDao().insertAll(toUpsert)
                Log.d(TAG, "Live synced and upserted ${toUpsert.size} phones into local Room DB")
            }

            _lastSyncTimestamp.value = System.currentTimeMillis()
            _isCloudConnected.value = true
            _isPermissionDenied.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Notice in syncRemotePhonesToRoom: ${e.message}")
        }
    }

    /**
     * Differential synchronization of remote expenses into local Room DB.
     */
    private suspend fun syncRemoteExpensesToRoom(remoteExpenses: List<ExpenseItem>) = withContext(Dispatchers.IO) {
        try {
            if (remoteExpenses.isEmpty()) return@withContext

            val localExpenses = database.expenseDao().getAllExpensesDirect()
            val localMap = localExpenses.associateBy { it.id }

            val toUpsert = mutableListOf<ExpenseItem>()
            for (remote in remoteExpenses) {
                val local = localMap[remote.id]
                if (local == null) {
                    toUpsert.add(remote)
                } else if (remote != local) {
                    toUpsert.add(remote)
                }
            }

            if (toUpsert.isNotEmpty()) {
                database.expenseDao().insertAll(toUpsert)
                Log.d(TAG, "Live synced and upserted ${toUpsert.size} expenses into local Room DB")
            }

            _lastSyncTimestamp.value = System.currentTimeMillis()
            _isCloudConnected.value = true
            _isPermissionDenied.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Notice in syncRemoteExpensesToRoom: ${e.message}")
        }
    }

    /**
     * Automatically flushes and syncs any changes created while offline as soon as connection is re-established.
     */
    override fun syncPendingOfflineChanges() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val localPhones = database.phoneDao().getAllPhonesDirect()
                val localExpenses = database.expenseDao().getAllExpensesDirect()
                val localSettings = database.settingsDao().getSettingsDirect()

                if (localPhones.isNotEmpty()) {
                    localPhones.forEach { pushPhone(it) }
                }
                if (localExpenses.isNotEmpty()) {
                    localExpenses.forEach { pushExpense(it) }
                }
                if (localSettings != null) {
                    pushSettings(localSettings)
                }
                _lastSyncTimestamp.value = System.currentTimeMillis()
                Log.d(TAG, "Synced offline pending records to Firebase Cloud (${localPhones.size} phones, ${localExpenses.size} expenses)")
            } catch (e: Exception) {
                Log.w(TAG, "Notice syncing pending offline changes: ${e.message}")
            }
        }
    }

    // =========================================================
    // CRUD WRITE OPERATIONS (PUSHED IMMEDIATELY TO FIREBASE)
    // =========================================================

    override fun pushPhone(phone: PhoneItem) {
        try {
            val key = if (phone.id != 0L) phone.id.toString() else (stockRef.push().key ?: AppRepository.generateUniqueId().toString())
            val map = phoneToMap(phone, key)

            // 1. Push to master phones path
            phonesRef.child(key).setValue(map)
                .addOnSuccessListener {
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _isCloudConnected.value = true
                }
                .addOnFailureListener { e ->
                    handleWriteFailure("phones/$key", e)
                }

            // 2. Synchronize with partitioned status paths
            val status = phone.status.uppercase()
            when {
                status == PhoneStatus.IN_STOCK.code || status == "IN_STOCK" || status == "STOCK" ||
                status == PhoneStatus.RETURNED.code || status == "RETURNED" || status == "RETURN" -> {
                    stockRef.child(key).setValue(map)
                    stockItemsRef.child(key).setValue(map)
                    transitRef.child(key).removeValue()
                    transitItemsRef.child(key).removeValue()
                    salesRef.child(key).removeValue()
                    salesRecordsRef.child(key).removeValue()
                }
                status == PhoneStatus.IN_TRANSIT.code || status == "IN_TRANSIT" || status == "TRANSIT" -> {
                    transitRef.child(key).setValue(map)
                    transitItemsRef.child(key).setValue(map)
                    stockRef.child(key).removeValue()
                    stockItemsRef.child(key).removeValue()
                    salesRef.child(key).removeValue()
                    salesRecordsRef.child(key).removeValue()
                }
                status == PhoneStatus.SOLD.code || status == "SOLD" -> {
                    salesRef.child(key).setValue(map)
                    salesRecordsRef.child(key).setValue(map)
                    stockRef.child(key).removeValue()
                    stockItemsRef.child(key).removeValue()
                    transitRef.child(key).removeValue()
                    transitItemsRef.child(key).removeValue()
                    syncSaleWithCustomerDue(phone)
                }
                else -> {
                    stockRef.child(key).setValue(map)
                    stockItemsRef.child(key).setValue(map)
                }
            }
        } catch (e: Exception) {
            handleWriteFailure("push_phone", e)
        }
    }

    override fun deletePhone(phoneId: Long) {
        try {
            val key = phoneId.toString()
            phonesRef.child(key).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Deleted phone $phoneId from Firebase Cloud")
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                }
                .addOnFailureListener { e ->
                    handleWriteFailure("delete_phone_$phoneId", e)
                }
            stockRef.child(key).removeValue()
            stockItemsRef.child(key).removeValue()
            transitRef.child(key).removeValue()
            transitItemsRef.child(key).removeValue()
            salesRef.child(key).removeValue()
            salesRecordsRef.child(key).removeValue()

            // Record tombstone so peer devices immediately delete from local Room
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "phone", "id" to phoneId, "timestamp" to System.currentTimeMillis())
            )
        } catch (e: Exception) {
            handleWriteFailure("delete_phone", e)
        }
    }

    override fun pushExpense(expense: ExpenseItem) {
        try {
            val key = if (expense.id != 0L) expense.id.toString() else (expensesRef.push().key ?: AppRepository.generateUniqueId().toString())
            val map = expenseToMap(expense, key)
            expensesRef.child(key).updateChildren(map)
                .addOnSuccessListener {
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _isCloudConnected.value = true
                }
                .addOnFailureListener {
                    expensesRef.child(key).setValue(map)
                        .addOnSuccessListener {
                            _lastSyncTimestamp.value = System.currentTimeMillis()
                            _isCloudConnected.value = true
                        }
                        .addOnFailureListener { e ->
                            handleWriteFailure("expense_$key", e)
                        }
                }
        } catch (e: Exception) {
            handleWriteFailure("push_expense", e)
        }
    }

    override fun deleteExpense(expenseId: Long) {
        try {
            val key = expenseId.toString()
            expensesRef.child(key).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "Deleted expense $expenseId from Firebase Cloud")
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                }
                .addOnFailureListener { e ->
                    handleWriteFailure("delete_expense_$expenseId", e)
                }
            // Record tombstone
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "expense", "id" to expenseId, "timestamp" to System.currentTimeMillis())
            )
        } catch (e: Exception) {
            handleWriteFailure("delete_expense", e)
        }
    }

    override fun pushSecurityPins(
        ownerPin: String,
        staff1Pin: String,
        staff2Pin: String,
        adminName: String,
        staff1Name: String,
        staff2Name: String
    ) {
        cachedOwnerPin = ownerPin
        cachedStaff1Pin = staff1Pin
        cachedStaff2Pin = staff2Pin
        val authMap = mapOf(
            "ownerPin" to ownerPin,
            "staff1Pin" to staff1Pin,
            "staff2Pin" to staff2Pin,
            "adminPin" to ownerPin,
            "adminName" to adminName,
            "staff1Name" to staff1Name,
            "staff2Name" to staff2Name,
            "ADMIN" to mapOf("userId" to "ADMIN", "name" to adminName, "pin" to ownerPin, "updatedAt" to System.currentTimeMillis()),
            "STAFF_1" to mapOf("userId" to "STAFF_1", "name" to staff1Name, "pin" to staff1Pin, "updatedAt" to System.currentTimeMillis()),
            "STAFF_2" to mapOf("userId" to "STAFF_2", "name" to staff2Name, "pin" to staff2Pin, "updatedAt" to System.currentTimeMillis())
        )
        try {
            authPinsRef.setValue(authMap)
                .addOnSuccessListener { Log.d(TAG, "Pushed auth PINs to Firebase auth_pins") }
                .addOnFailureListener { e -> handleWriteFailure("auth_pins", e) }
            userProfilesRef.setValue(authMap)
                .addOnSuccessListener { Log.d(TAG, "Pushed user profiles security to Firebase") }
                .addOnFailureListener { e -> handleWriteFailure("user_profiles", e) }
            settingsRef.child("ownerPin").setValue(ownerPin)
            settingsRef.child("staff1Pin").setValue(staff1Pin)
            settingsRef.child("staff2Pin").setValue(staff2Pin)
        } catch (e: Exception) {
            handleWriteFailure("push_security_pins", e)
        }
    }

    override fun pushSettings(settings: AppSettings) {
        cachedOwnerPin = settings.ownerPin
        cachedStaff1Pin = settings.staff1Pin
        cachedStaff2Pin = settings.staff2Pin
        try {
            settingsRef.setValue(settingsToMap(settings))
                .addOnSuccessListener {
                    Log.d(TAG, "Pushed app settings to Firebase Cloud")
                }
                .addOnFailureListener { e -> handleWriteFailure("settings", e) }
            pushSecurityPins(
                ownerPin = settings.ownerPin,
                staff1Pin = settings.staff1Pin,
                staff2Pin = settings.staff2Pin,
                adminName = settings.adminName,
                staff1Name = settings.staff1Name,
                staff2Name = settings.staff2Name
            )
        } catch (e: Exception) {
            handleWriteFailure("push_settings", e)
        }
    }

    override fun clearCloudInventoryAndExpenses() {
        try {
            stockRef.removeValue()
            transitRef.removeValue()
            salesRef.removeValue()
            expensesRef.removeValue()
            deletedItemsRef.removeValue()
            phonesRef.removeValue()
            stockItemsRef.removeValue()
            transitItemsRef.removeValue()
            salesRecordsRef.removeValue()
            Log.d(TAG, "Cleared cloud inventory and expenses")
        } catch (e: Exception) {
            handleWriteFailure("clear_inventory", e)
        }
    }

    override suspend fun fetchAndSyncCloudPinsAndSettings(): Unit = withContext(Dispatchers.IO) {
        try {
            // Fetch auth pins
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                authPinsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val ownerPin = snapshot.child("ownerPin").getValue(String::class.java)
                                ?: snapshot.child("ADMIN").child("pin").getValue(String::class.java)
                            val staff1Pin = snapshot.child("staff1Pin").getValue(String::class.java)
                                ?: snapshot.child("STAFF_1").child("pin").getValue(String::class.java)
                            val staff2Pin = snapshot.child("staff2Pin").getValue(String::class.java)
                                ?: snapshot.child("STAFF_2").child("pin").getValue(String::class.java)

                            if (!ownerPin.isNullOrBlank()) cachedOwnerPin = ownerPin
                            if (!staff1Pin.isNullOrBlank()) cachedStaff1Pin = staff1Pin
                            if (!staff2Pin.isNullOrBlank()) cachedStaff2Pin = staff2Pin

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val current = database.settingsDao().getSettingsDirect() ?: AppSettings()
                                    val updated = current.copy(
                                        ownerPin = ownerPin ?: current.ownerPin,
                                        staff1Pin = staff1Pin ?: current.staff1Pin,
                                        staff2Pin = staff2Pin ?: current.staff2Pin
                                    )
                                    database.settingsDao().insertOrUpdateSettings(updated)
                                    Log.d(TAG, "Fetched auth PINs on launch: ownerPin=$ownerPin")
                                } catch (e: Exception) {
                                    Log.w(TAG, "Notice saving fetched auth PINs: ${e.message}")
                                } finally {
                                    if (continuation.isActive) continuation.resume(Unit) {}
                                }
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        handleListenerCancelled("fetch_auth_pins", error)
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            // Fetch settings
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            parseSettingsFromSnapshot(snapshot)?.let { cloudSettings ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val current = database.settingsDao().getSettingsDirect() ?: AppSettings()
                                        val merged = current.copy(
                                            shopName = cloudSettings.shopName.ifBlank { current.shopName },
                                            shopSubtitle = cloudSettings.shopSubtitle.ifBlank { current.shopSubtitle },
                                            shopAddress = cloudSettings.shopAddress.ifBlank { current.shopAddress },
                                            shopPhone = cloudSettings.shopPhone.ifBlank { current.shopPhone },
                                            rmbToBdtRate = if (cloudSettings.rmbToBdtRate > 0) cloudSettings.rmbToBdtRate else current.rmbToBdtRate,
                                            ownerPin = cloudSettings.ownerPin.ifBlank { current.ownerPin },
                                            staff1Pin = cloudSettings.staff1Pin.ifBlank { current.staff1Pin },
                                            staff2Pin = cloudSettings.staff2Pin.ifBlank { current.staff2Pin }
                                        )
                                        database.settingsDao().insertOrUpdateSettings(merged)
                                        Log.d(TAG, "Fetched cloud settings on launch: shopName=${merged.shopName}")
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Notice saving fetched settings: ${e.message}")
                                    } finally {
                                        if (continuation.isActive) continuation.resume(Unit) {}
                                    }
                                }
                            } ?: run {
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        handleListenerCancelled("fetch_settings", error)
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice in fetchAndSyncCloudPinsAndSettings: ${e.message}")
        }
    }

    override suspend fun pullAndPopulateAllFromCloud(): Result<Triple<Int, Int, Boolean>> = withContext(Dispatchers.IO) {
        try {
            var restoredPhones = 0
            var restoredExpenses = 0
            var settingsRestored = false

            // Fetch remote stock
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                stockRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cloudPhones = mutableListOf<PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_STOCK.code)?.let { cloudPhones.add(it) }
                        }
                        if (cloudPhones.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                syncRemotePhonesToRoom(cloudPhones)
                                restoredPhones += cloudPhones.size
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            // Fetch remote transit
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                transitRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cloudPhones = mutableListOf<PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.IN_TRANSIT.code)?.let { cloudPhones.add(it) }
                        }
                        if (cloudPhones.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                syncRemotePhonesToRoom(cloudPhones)
                                restoredPhones += cloudPhones.size
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            // Fetch remote sales
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                salesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cloudPhones = mutableListOf<PhoneItem>()
                        for (child in snapshot.children) {
                            parsePhoneFromSnapshot(child, PhoneStatus.SOLD.code)?.let { cloudPhones.add(it) }
                        }
                        if (cloudPhones.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                syncRemotePhonesToRoom(cloudPhones)
                                restoredPhones += cloudPhones.size
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            // Fetch remote expenses
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val parsedMap = linkedMapOf<Long, Pair<ExpenseItem, Long>>()
                        for (child in snapshot.children) {
                            val item = parseExpenseFromSnapshot(child) ?: continue
                            val lastUpdated = (child.child("lastUpdated").value as? Number)?.toLong()
                                ?: (child.child("updatedAt").value as? Number)?.toLong()
                                ?: 0L

                            val isCanonicalKey = child.key == item.id.toString()
                            val existing = parsedMap[item.id]

                            if (existing == null) {
                                parsedMap[item.id] = Pair(item, lastUpdated)
                            } else {
                                val isExistingCanonical = snapshot.hasChild(item.id.toString()) && child.key != item.id.toString()
                                if (isCanonicalKey || (!isExistingCanonical && lastUpdated > existing.second)) {
                                    parsedMap[item.id] = Pair(item, lastUpdated)
                                }
                            }
                        }
                        val cloudExpenses = parsedMap.values.map { it.first }
                        if (cloudExpenses.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                syncRemoteExpensesToRoom(cloudExpenses)
                                restoredExpenses = cloudExpenses.size
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            // Fetch settings
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            parseSettingsFromSnapshot(snapshot)?.let { cloudSettings ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.settingsDao().insertOrUpdateSettings(cloudSettings)
                                    settingsRestored = true
                                    if (continuation.isActive) continuation.resume(Unit) {}
                                }
                            } ?: run {
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(Unit) {}
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        if (continuation.isActive) continuation.resume(Unit) {}
                    }
                })
            }

            fetchAndSyncCloudPinsAndSettings()

            _isInitialLoading.value = false
            _isCloudConnected.value = true
            Result.success(Triple(restoredPhones, restoredExpenses, settingsRestored))
        } catch (e: Exception) {
            Log.w(TAG, "Notice in pullAndPopulateAllFromCloud: ${e.message}")
            _isInitialLoading.value = false
            Result.failure(e)
        }
    }

    override suspend fun restoreAllFromCloud(): Result<Triple<Int, Int, Boolean>> = pullAndPopulateAllFromCloud()

    override suspend fun backupAllToCloud(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val localPhones = database.phoneDao().getAllPhonesDirect()
            val localExpenses = database.expenseDao().getAllExpensesDirect()

            localPhones.forEach { phone -> pushPhone(phone) }
            localExpenses.forEach { exp -> pushExpense(exp) }
            database.settingsDao().getSettingsDirect()?.let { settings -> pushSettings(settings) }
            com.example.viewmodel.DashboardViewModel.pushStartingBalanceToFirebase(com.example.viewmodel.DashboardViewModel.globalStartingBalance.value)

            _lastSyncTimestamp.value = System.currentTimeMillis()
            Result.success(Pair(localPhones.size, localExpenses.size))
        } catch (e: Exception) {
            Log.w(TAG, "Notice backing up all to cloud: ${e.message}")
            Result.failure(e)
        }
    }

    // =========================================================
    companion object {
        private const val TAG = "FirebaseSyncManager"

        @Volatile
        private var INSTANCE: FirebaseSyncManager? = null

        fun getInstance(
            context: Context,
            database: AppDatabase,
            scope: CoroutineScope
        ): FirebaseSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSyncManager(context.applicationContext, database, scope).also {
                    INSTANCE = it
                }
            }
        }

        fun phoneToMap(phone: PhoneItem, key: String): Map<String, Any> {
            val lastUpdated = if (phone.updatedAt > 0) phone.updatedAt else System.currentTimeMillis()
            return mapOf(
                "id" to phone.id,
                "uniqueId" to key,
                "pushKey" to key,
                "brand" to phone.brand,
                "model" to phone.model,
                "imei" to phone.imei,
                "imei2" to phone.imei2,
                "color" to phone.color,
                "ramRom" to phone.ramRom,
                "variant" to phone.variant,
                "batteryHealth" to phone.batteryHealth,
                "conditionGrade" to phone.conditionGrade,
                "itemCondition" to phone.itemCondition,
                "packaging" to phone.packaging,
                "entryDate" to phone.entryDate,
                "status" to phone.status,
                "notes" to phone.notes,
                "buyingPriceRmb" to phone.buyingPriceRmb,
                "buyingPriceBdt" to phone.buyingPriceBdt,
                "shippingCostBdt" to phone.shippingCostBdt,
                "otherCostBdt" to phone.otherCostBdt,
                "totalCostBdt" to phone.totalCostBdt,
                "desiredProfitBdt" to phone.desiredProfitBdt,
                "sellingPriceBdt" to phone.sellingPriceBdt,
                "saleDate" to phone.saleDate,
                "customerName" to phone.customerName,
                "customerPhone" to phone.customerPhone,
                "customerEmail" to phone.customerEmail,
                "soldByStaff" to phone.soldByStaff,
                "paymentMethod" to phone.paymentMethod,
                "warrantyDays" to phone.warrantyDays,
                "cashReceivedBdt" to phone.cashReceivedBdt,
                "cashReceived" to phone.cashReceivedBdt,
                "dueAmountBdt" to phone.dueAmountBdt,
                "dueAmount" to phone.dueAmountBdt,
                "addedByRole" to phone.addedByRole,
                "createdAt" to phone.createdAt,
                "updatedAt" to lastUpdated,
                "lastUpdated" to lastUpdated
            )
        }

        fun parsePhoneFromSnapshot(child: DataSnapshot, defaultStatus: String? = null): PhoneItem? {
            return try {
                val id = parseId(child)

                val lastUpdated = (child.child("lastUpdated").value as? Number)?.toLong()
                    ?: (child.child("updatedAt").value as? Number)?.toLong()
                    ?: System.currentTimeMillis()

                val created = (child.child("createdAt").value as? Number)?.toLong()
                    ?: lastUpdated

                val statusRaw = child.child("status").getValue(String::class.java)
                val finalStatus = if (!statusRaw.isNullOrBlank()) {
                    statusRaw
                } else if (defaultStatus != null) {
                    defaultStatus
                } else {
                    PhoneStatus.IN_STOCK.code
                }

                PhoneItem(
                    id = id,
                    brand = child.child("brand").getValue(String::class.java) ?: "",
                    model = child.child("model").getValue(String::class.java) ?: "",
                    imei = child.child("imei").getValue(String::class.java) ?: "",
                    imei2 = child.child("imei2").getValue(String::class.java) ?: "",
                    color = child.child("color").getValue(String::class.java) ?: "",
                    ramRom = child.child("ramRom").getValue(String::class.java) ?: "",
                    variant = child.child("variant").getValue(String::class.java) ?: "Global",
                    batteryHealth = child.child("batteryHealth").getValue(String::class.java) ?: "100%",
                    conditionGrade = child.child("conditionGrade").getValue(String::class.java) ?: "Grade A",
                    itemCondition = child.child("itemCondition").getValue(String::class.java) ?: "Used",
                    packaging = child.child("packaging").getValue(String::class.java) ?: "Full Box",
                    entryDate = child.child("entryDate").getValue(String::class.java) ?: "",
                    status = finalStatus,
                    notes = child.child("notes").getValue(String::class.java) ?: "",
                    buyingPriceRmb = (child.child("buyingPriceRmb").value as? Number)?.toDouble() ?: 0.0,
                    buyingPriceBdt = (child.child("buyingPriceBdt").value as? Number)?.toDouble() ?: 0.0,
                    shippingCostBdt = (child.child("shippingCostBdt").value as? Number)?.toDouble() ?: 0.0,
                    otherCostBdt = (child.child("otherCostBdt").value as? Number)?.toDouble() ?: 0.0,
                    totalCostBdt = (child.child("totalCostBdt").value as? Number)?.toDouble() ?: 0.0,
                    desiredProfitBdt = (child.child("desiredProfitBdt").value as? Number)?.toDouble() ?: 0.0,
                    sellingPriceBdt = (child.child("sellingPriceBdt").value as? Number)?.toDouble() ?: 0.0,
                    saleDate = child.child("saleDate").getValue(String::class.java) ?: "",
                    customerName = child.child("customerName").getValue(String::class.java) ?: "",
                    customerPhone = child.child("customerPhone").getValue(String::class.java) ?: "",
                    customerEmail = child.child("customerEmail").getValue(String::class.java) ?: "",
                    soldByStaff = child.child("soldByStaff").getValue(String::class.java) ?: "",
                    paymentMethod = child.child("paymentMethod").getValue(String::class.java) ?: "Cash",
                    warrantyDays = (child.child("warrantyDays").value as? Number)?.toInt() ?: 10,
                    cashReceivedBdt = (child.child("cashReceivedBdt").value as? Number)?.toDouble()
                        ?: (child.child("cashReceived").value as? Number)?.toDouble()
                        ?: 0.0,
                    dueAmountBdt = (child.child("dueAmountBdt").value as? Number)?.toDouble()
                        ?: (child.child("dueAmount").value as? Number)?.toDouble()
                        ?: 0.0,
                    addedByRole = child.child("addedByRole").getValue(String::class.java) ?: "STAFF",
                    createdAt = created,
                    updatedAt = lastUpdated
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice parsing phone snapshot: ${e.message}")
                null
            }
        }

        fun expenseToMap(expense: ExpenseItem, key: String): Map<String, Any> {
            val originalEntryTimestamp = if (expense.timestamp > 0L) expense.timestamp else System.currentTimeMillis()
            val lastUpdated = System.currentTimeMillis()
            return mapOf(
                "id" to expense.id,
                "uniqueId" to key,
                "pushKey" to key,
                "title" to expense.title,
                "category" to expense.category,
                "amountBdt" to expense.amountBdt,
                "date" to expense.date,
                "notes" to expense.notes,
                "addedBy" to expense.addedBy,
                "receiptUrl" to (expense.receiptUrl ?: ""),
                "timestamp" to originalEntryTimestamp,
                "entryDate" to originalEntryTimestamp,
                "createdAt" to originalEntryTimestamp,
                "lastUpdated" to lastUpdated
            )
        }

        fun parseExpenseFromSnapshot(child: DataSnapshot): ExpenseItem? {
            return try {
                val id = parseId(child)

                val timestamp = (child.child("entryDate").value as? Number)?.toLong()
                    ?: (child.child("createdAt").value as? Number)?.toLong()
                    ?: (child.child("timestamp").value as? Number)?.toLong()
                    ?: (child.child("lastUpdated").value as? Number)?.toLong()
                    ?: System.currentTimeMillis()

                val rawReceipt = child.child("receiptUrl").getValue(String::class.java)
                val receiptUrl = if (!rawReceipt.isNullOrBlank()) rawReceipt else null

                ExpenseItem(
                    id = id,
                    title = child.child("title").getValue(String::class.java) ?: "",
                    category = child.child("category").getValue(String::class.java) ?: "CARGO",
                    amountBdt = (child.child("amountBdt").value as? Number)?.toDouble() ?: 0.0,
                    date = child.child("date").getValue(String::class.java) ?: "",
                    notes = child.child("notes").getValue(String::class.java) ?: "",
                    addedBy = child.child("addedBy").getValue(String::class.java) ?: "Jasim",
                    receiptUrl = receiptUrl,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice parsing expense snapshot: ${e.message}")
                null
            }
        }

        fun settingsToMap(settings: AppSettings): Map<String, Any> {
            return mapOf(
                "id" to settings.id,
                "rmbToBdtRate" to settings.rmbToBdtRate,
                "ownerPin" to settings.ownerPin,
                "staff1Pin" to settings.staff1Pin,
                "staff2Pin" to settings.staff2Pin,
                "adminName" to settings.adminName,
                "adminNameEn" to settings.adminNameEn,
                "staff1Name" to settings.staff1Name,
                "staff1NameEn" to settings.staff1NameEn,
                "staff2Name" to settings.staff2Name,
                "staff2NameEn" to settings.staff2NameEn,
                "shopName" to settings.shopName,
                "shopSubtitle" to settings.shopSubtitle,
                "shopAddress" to settings.shopAddress,
                "shopPhone" to settings.shopPhone,
                "defaultWarrantyText" to settings.defaultWarrantyText,
                "ownerNotificationEmail" to settings.ownerNotificationEmail,
                "appLanguage" to settings.appLanguage,
                "themeMode" to settings.themeMode,
                "dynamicColor" to settings.dynamicColor,
                "opening_balance" to com.example.viewmodel.DashboardViewModel.globalStartingBalance.value
            )
        }

        fun parseSettingsFromSnapshot(child: DataSnapshot): AppSettings? {
            return try {
                AppSettings(
                    id = 1,
                    rmbToBdtRate = (child.child("rmbToBdtRate").value as? Number)?.toDouble() ?: 17.50,
                    ownerPin = child.child("ownerPin").getValue(String::class.java) ?: "1111",
                    staff1Pin = child.child("staff1Pin").getValue(String::class.java) ?: "2222",
                    staff2Pin = child.child("staff2Pin").getValue(String::class.java) ?: "3333",
                    adminName = child.child("adminName").getValue(String::class.java) ?: "Jasim",
                    adminNameEn = child.child("adminNameEn").getValue(String::class.java) ?: "Jasim",
                    staff1Name = child.child("staff1Name").getValue(String::class.java) ?: "Sumon",
                    staff1NameEn = child.child("staff1NameEn").getValue(String::class.java) ?: "Sumon",
                    staff2Name = child.child("staff2Name").getValue(String::class.java) ?: "Deedat",
                    staff2NameEn = child.child("staff2NameEn").getValue(String::class.java) ?: "Deedat",
                    shopName = child.child("shopName").getValue(String::class.java) ?: "Zoha Trading",
                    shopSubtitle = child.child("shopSubtitle").getValue(String::class.java) ?: "Trading Worldwide Since 2012",
                    shopAddress = child.child("shopAddress").getValue(String::class.java) ?: "Shop #204, Level 3, Eastern Plaza, Dhaka",
                    shopPhone = child.child("shopPhone").getValue(String::class.java) ?: "01755-987654",
                    defaultWarrantyText = child.child("defaultWarrantyText").getValue(String::class.java) ?: "10 days replacement warranty and 2 years free service warranty.",
                    ownerNotificationEmail = child.child("ownerNotificationEmail").getValue(String::class.java) ?: "jasimuddin.nsu@gmail.com",
                    appLanguage = "en",
                    themeMode = child.child("themeMode").getValue(String::class.java) ?: "LIGHT",
                    dynamicColor = child.child("dynamicColor").getValue(Boolean::class.java) ?: true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice parsing settings snapshot: ${e.message}")
                null
            }
        }

        fun parseId(child: DataSnapshot): Long {
            val rawId = (child.child("id").value as? Number)?.toLong()
                ?: child.child("uniqueId").getValue(String::class.java)?.toLongOrNull()
                ?: child.child("pushKey").getValue(String::class.java)?.toLongOrNull()
                ?: child.key?.toLongOrNull()
            if (rawId != null && rawId > 0L) return rawId

            val key = child.key ?: child.child("uniqueId").getValue(String::class.java) ?: ""
            return if (key.isNotBlank()) {
                parseIdFromString(key)
            } else {
                AppRepository.generateUniqueId()
            }
        }

        fun parseIdFromString(key: String): Long {
            key.toLongOrNull()?.let { if (it > 0L) return it }
            val hash = Math.abs(key.hashCode().toLong())
            val secondary = Math.abs(key.take(6).hashCode().toLong()) % 10_000L
            return (hash * 10_000L) + secondary
        }

        fun otherIncomeToMap(item: OtherIncomeItem): Map<String, Any> {
            val originalEntryTimestamp = if (item.timestamp > 0L) item.timestamp else System.currentTimeMillis()
            return mapOf(
                "id" to item.id,
                "category" to item.category,
                "amountBdt" to item.amountBdt,
                "amount" to item.amountBdt,
                "date" to item.date,
                "notes" to item.notes,
                "addedBy" to item.addedBy,
                "timestamp" to originalEntryTimestamp,
                "createdAt" to originalEntryTimestamp,
                "lastUpdated" to System.currentTimeMillis()
            )
        }

        fun parseOtherIncomeFromSnapshot(child: DataSnapshot): OtherIncomeItem? {
            return try {
                val key = child.key ?: return null
                val id = child.child("id").getValue(String::class.java)
                    ?: (child.child("id").value as? Number)?.toString()
                    ?: key
                val category = child.child("category").getValue(String::class.java) ?: "Accessory Sale"
                val amount = (child.child("amountBdt").value as? Number)?.toDouble()
                    ?: (child.child("amount").value as? Number)?.toDouble()
                    ?: child.child("amountBdt").getValue(String::class.java)?.toDoubleOrNull()
                    ?: 0.0
                val date = child.child("date").getValue(String::class.java) ?: ""
                val notes = child.child("notes").getValue(String::class.java) ?: ""
                val addedBy = child.child("addedBy").getValue(String::class.java) ?: "Staff"
                val timestamp = (child.child("timestamp").value as? Number)?.toLong()
                    ?: (child.child("createdAt").value as? Number)?.toLong()
                    ?: 0L
                OtherIncomeItem(id, category, amount, date, notes, addedBy, timestamp)
            } catch (e: Exception) {
                Log.w(TAG, "Notice parsing other income snapshot: ${e.message}")
                null
            }
        }
    }
}
