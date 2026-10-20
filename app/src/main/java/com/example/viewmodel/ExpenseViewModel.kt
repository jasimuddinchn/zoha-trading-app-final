package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.EXPENSE_CATEGORIES_EN
import com.example.data.model.ExpenseItem
import com.example.data.repository.AppRepository
import com.example.data.repository.ExpenseRepository
import com.example.util.MonthFilterHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated Expense ViewModel handling instant real-time multi-device expenses sync,
 * chronological sorting by original entryDate descending (newest first),
 * dynamic individual expense category total amount calculation,
 * and reliable atomic Firebase Realtime Database updates preventing edit reversion.
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ExpenseViewModel"
    private val DATABASE_URL = "https://zoha-trading-default-rtdb.firebaseio.com/"

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(application, database, viewModelScope)
    val expenseRepository = ExpenseRepository(database, firebaseSyncManager, viewModelScope)
    private val appRepository = AppRepository(database, firebaseSyncManager)

    private val firebaseDb: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL).apply {
                try { setPersistenceEnabled(true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
    }
    private val expensesRef: DatabaseReference by lazy { firebaseDb.getReference("expenses") }

    private val _liveExpensesFlow = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val liveExpenses: StateFlow<List<ExpenseItem>> = _liveExpensesFlow.asStateFlow()
    val allExpenses: StateFlow<List<ExpenseItem>> = _liveExpensesFlow.asStateFlow()

    val isCloudConnected: StateFlow<Boolean> = firebaseSyncManager.isCloudConnected
    val lastSyncTimestamp: StateFlow<Long> = firebaseSyncManager.lastSyncTimestamp

    private val _selectedYearMonth = MutableStateFlow(MonthFilterHelper.getCurrentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        firebaseSyncManager.startRealtimeSync()
        setupRealtimeExpenseListener()
        loadInitialExpensesFromRoom()
    }

    private fun loadInitialExpensesFromRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val local = database.expenseDao().getAllExpensesDirect()
                if (local.isNotEmpty() && _liveExpensesFlow.value.isEmpty()) {
                    _liveExpensesFlow.value = local.distinctBy { it.id }.sortedByDescending { it.entryDate }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice loading initial expenses: ${e.message}")
            }
        }
    }

    // Tracks recent in-flight or completed local updates to protect StateFlow against stale snapshot overrides
    private val recentLocalUpdates = java.util.concurrent.ConcurrentHashMap<Long, Pair<ExpenseItem, Long>>()
    private var expenseListener: ValueEventListener? = null

    /**
     * Attaches persistent ValueEventListener directly to `/expenses`.
     * Cleanly updates StateFlow upon snapshot arrival without fetching stale cached data.
     * Deduplicates multiple nodes pointing to the same expense ID to avoid duplicate rendering or reversion.
     */
    private fun setupRealtimeExpenseListener() {
        try {
            expensesRef.keepSynced(true)
        } catch (_: Exception) {}

        // Remove any previous active listener to prevent stacking
        expenseListener?.let {
            try { expensesRef.removeEventListener(it) } catch (_: Exception) {}
            expenseListener = null
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val parsedMap = linkedMapOf<Long, Triple<ExpenseItem, Long, String>>() // id -> Triple(item, lastUpdated, childKey)

                        for (child in snapshot.children) {
                            val childKey = child.key ?: continue
                            val item = FirebaseSyncManager.parseExpenseFromSnapshot(child) ?: continue
                            if (item.id == 0L) continue
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

                        // Protect recently edited expenses from being overwritten by stale cached snapshots or non-updated nodes
                        val finalItems = parsedMap.values.map { (remoteItem, remoteLastUpdated, _) ->
                            val localRecent = recentLocalUpdates[remoteItem.id]
                            if (localRecent != null) {
                                val (localItem, localTime) = localRecent
                                val hasMatchingData = remoteItem.title == localItem.title &&
                                    Math.abs(remoteItem.amountBdt - localItem.amountBdt) < 0.01 &&
                                    remoteItem.category == localItem.category &&
                                    remoteItem.date == localItem.date
                                if (hasMatchingData && remoteLastUpdated >= localTime) {
                                    recentLocalUpdates.remove(remoteItem.id)
                                    remoteItem
                                } else {
                                    localItem
                                }
                            } else {
                                remoteItem
                            }
                        }.distinctBy { it.id }.sortedWith(
                            compareByDescending<ExpenseItem> {
                                if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                            }.thenByDescending { it.id }
                        )

                        withContext(Dispatchers.Main) {
                            _liveExpensesFlow.value = finalItems
                        }

                        // Asynchronously persist to local Room SQLite
                        withContext(Dispatchers.IO) {
                            try {
                                if (finalItems.isNotEmpty()) {
                                    database.expenseDao().insertAll(finalItems)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error upserting expenses to Room: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice parsing /expenses snapshot: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "expensesRef onCancelled: ${error.message}")
            }
        }

        expenseListener = listener
        expensesRef.addValueEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        expenseListener?.let {
            try { expensesRef.removeEventListener(it) } catch (_: Exception) {}
            expenseListener = null
        }
        expenseRepository.cleanUp()
    }

    /**
     * Expenses filtered by selected month, selected category ("ALL" or specific category), and search query,
     * strictly sorted chronologically by original entryDate descending (newest first).
     */
    val filteredExpenses: StateFlow<List<ExpenseItem>> = combine(
        allExpenses,
        _selectedYearMonth,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, yearMonth, category, query ->
        val distinctExpenses = expenses.distinctBy { it.id }
        val monthFiltered = MonthFilterHelper.filterExpenses(distinctExpenses, yearMonth)
        val categoryFiltered = filterByCategory(monthFiltered, category)
        val result = if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim().lowercase()
            categoryFiltered.filter { item ->
                item.title.lowercase().contains(q) ||
                    item.category.lowercase().contains(q) ||
                    item.notes.lowercase().contains(q) ||
                    item.date.lowercase().contains(q)
            }
        }
        result.distinctBy { it.id }.sortedWith(
            compareByDescending<ExpenseItem> {
                if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
            }.thenByDescending { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Dynamically calculates the total sum specifically for the selected expense category
     * (or all categories when "ALL" is selected).
     */
    val selectedCategoryTotalAmount: StateFlow<Double> = combine(
        allExpenses,
        _selectedYearMonth,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, yearMonth, category, query ->
        val distinctExpenses = expenses.distinctBy { it.id }
        val monthFiltered = MonthFilterHelper.filterExpenses(distinctExpenses, yearMonth)
        val categoryFiltered = filterByCategory(monthFiltered, category)
        val finalFiltered = if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim().lowercase()
            categoryFiltered.filter { item ->
                item.title.lowercase().contains(q) ||
                    item.category.lowercase().contains(q) ||
                    item.notes.lowercase().contains(q) ||
                    item.date.lowercase().contains(q)
            }
        }
        finalFiltered.distinctBy { it.id }.sumOf { it.amountBdt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun selectCategory(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun selectYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    companion object {
        val SUPPORTED_EXPENSE_CATEGORIES: List<String> = EXPENSE_CATEGORIES_EN

        fun sortChronologically(expenses: List<ExpenseItem>): List<ExpenseItem> {
            return expenses.sortedWith(
                compareByDescending<ExpenseItem> {
                    if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                }.thenByDescending { it.id }
            )
        }

        fun filterByCategory(expenses: List<ExpenseItem>, category: String): List<ExpenseItem> {
            val cleanCat = category.trim()
            val filtered = if (cleanCat.isEmpty() || cleanCat.equals("ALL", ignoreCase = true) || cleanCat.equals("All Categories", ignoreCase = true)) {
                expenses
            } else {
                expenses.filter { item ->
                    item.category.trim().equals(cleanCat, ignoreCase = true)
                }
            }
            return filtered.sortedWith(
                compareByDescending<ExpenseItem> {
                    if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                }.thenByDescending { it.id }
            )
        }

        fun calculateCategoryTotal(expenses: List<ExpenseItem>, category: String): Double {
            return filterByCategory(expenses, category).sumOf { it.amountBdt }
        }
    }

    /**
     * Direct write to Firebase Realtime Database first, then saves to Room SQLite.
     * Deduplicates by unique ID to prevent double entry appearance.
     */
    fun addExpense(expense: ExpenseItem) {
        viewModelScope.launch {
            val uniqueId = if (expense.id == 0L) AppRepository.generateUniqueId() else expense.id
            val now = System.currentTimeMillis()
            val expenseWithId = expense.copy(
                id = uniqueId,
                timestamp = if (expense.timestamp == 0L) now else expense.timestamp
            )
            recentLocalUpdates[uniqueId] = Pair(expenseWithId, now)
            withContext(Dispatchers.Main) {
                val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                currentMap[uniqueId] = expenseWithId
                _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
            }
            expenseRepository.insertExpense(expenseWithId)
        }
    }

    fun addExpense(
        title: String,
        category: String,
        amountBdt: Double,
        date: String,
        notes: String,
        addedBy: String = "Admin",
        receiptUrl: String? = null
    ) {
        viewModelScope.launch {
            val uniqueId = AppRepository.generateUniqueId()
            val now = System.currentTimeMillis()
            val expense = ExpenseItem(
                id = uniqueId,
                title = title.trim(),
                category = category,
                amountBdt = amountBdt,
                date = date,
                notes = notes.trim(),
                addedBy = addedBy,
                receiptUrl = receiptUrl,
                timestamp = now
            )
            recentLocalUpdates[uniqueId] = Pair(expense, now)
            withContext(Dispatchers.Main) {
                val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                currentMap[uniqueId] = expense
                _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
            }
            expenseRepository.insertExpense(expense)
        }
    }

    /**
     * FIXED EXPENSE EDIT/PERSISTENCE (Firebase Sync Fix):
     * Explicit atomic update on the direct Firebase node `/expenses/{expense_id}` using `updateChildren`
     * and immediate local StateFlow / Room synchronization.
     * Prevents edited expenses from reverting back to their original state.
     */
    fun updateExpense(expense: ExpenseItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.expenseDao().getExpenseById(expense.id)
            val preservedTimestamp = when {
                existing != null && existing.timestamp > 0L -> existing.timestamp
                expense.timestamp > 0L -> expense.timestamp
                else -> System.currentTimeMillis()
            }
            val updated = expense.copy(timestamp = preservedTimestamp)
            val expenseKey = updated.id.toString()
            val now = System.currentTimeMillis()

            // Register in local tracker so subsequent realtime snapshot triggers do not revert with stale data
            recentLocalUpdates[updated.id] = Pair(updated, now)

            val updateMap = hashMapOf<String, Any>(
                "id" to updated.id,
                "uniqueId" to expenseKey,
                "pushKey" to expenseKey,
                "title" to updated.title,
                "category" to updated.category,
                "amountBdt" to updated.amountBdt,
                "date" to updated.date,
                "notes" to updated.notes,
                "addedBy" to updated.addedBy,
                "receiptUrl" to (updated.receiptUrl ?: ""),
                "timestamp" to updated.timestamp,
                "entryDate" to updated.timestamp,
                "createdAt" to updated.timestamp,
                "lastUpdated" to now
            )

            // 1. Optimistically update local StateFlow immediately with unique mapping
            withContext(Dispatchers.Main) {
                val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                currentMap[updated.id] = updated
                _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
            }

            // 2. Persist to local Room SQLite
            database.expenseDao().updateExpense(updated)

            // 3. Explicit atomic update on the direct child reference /expenses/{expense_id}
            val directChildRef = expensesRef.child(expenseKey)
            directChildRef.updateChildren(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Explicit atomic updateChildren succeeded for /expenses/$expenseKey")
                    // Cleanly confirm StateFlow update on success response from Firebase without fetching stale cached state
                    viewModelScope.launch(Dispatchers.Main) {
                        val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                        currentMap[updated.id] = updated
                        _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "updateChildren failed for /expenses/$expenseKey: ${e.message}, falling back to setValue")
                    directChildRef.setValue(updateMap)
                        .addOnSuccessListener {
                            viewModelScope.launch(Dispatchers.Main) {
                                val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                                currentMap[updated.id] = updated
                                _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
                            }
                        }
                }

            // 4. Also scan and purge any legacy push-key nodes matching this same expense ID
            // to completely eliminate stale duplicate records that cause reversion
            expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val k = child.key ?: continue
                        if (k != expenseKey) {
                            val cid = (child.child("id").value as? Number)?.toLong()
                                ?: child.child("uniqueId").getValue(String::class.java)?.toLongOrNull()
                                ?: FirebaseSyncManager.parseIdFromString(k)
                            if (cid == updated.id) {
                                expensesRef.child(k).removeValue()
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Also keep expenseRepository and syncManager in sync
            expenseRepository.updateExpense(updated)
        }
    }

    /**
     * Deletes from Firebase Realtime Database directly first, then deletes from Room SQLite.
     */
    fun deleteExpense(expense: ExpenseItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseKey = expense.id.toString()
            try {
                expensesRef.child(expenseKey).removeValue()
            } catch (e: Exception) {
                Log.w(TAG, "Error removing expense $expenseKey: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                val currentMap = _liveExpensesFlow.value.associateBy { it.id }.toMutableMap()
                currentMap.remove(expense.id)
                _liveExpensesFlow.value = sortChronologically(currentMap.values.toList().distinctBy { it.id })
            }

            expenseRepository.deleteExpense(expense)
        }
    }
}
