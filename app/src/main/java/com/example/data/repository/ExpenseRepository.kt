package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.ExpenseDao
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.ExpenseItem
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
 * Dedicated Repository for Expense operations with DIRECT Firebase Realtime Database write-first flow.
 * Every expense addition, edit, or deletion is pushed DIRECTLY to Firebase Realtime Database first (`/expenses`),
 * ensuring instant real-time synchronization across all connected devices.
 */
class ExpenseRepository(
    private val database: AppDatabase,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
    private val scope: CoroutineScope
) {
    private val expenseDao: ExpenseDao = database.expenseDao()

    private val DATABASE_URL = "https://zoha-trading-default-rtdb.firebaseio.com/"
    private val TAG = "ExpenseRepository"

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
    private val deletedItemsRef: DatabaseReference by lazy { firebaseDb.getReference("deleted_items") }

    private val _liveExpenses = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val liveExpenses: StateFlow<List<ExpenseItem>> = _liveExpenses.asStateFlow()

    val allExpenses: Flow<List<ExpenseItem>> = expenseDao.getAllExpenses()

    private var expenseListener: ValueEventListener? = null

    init {
        setupPersistentExpenseListener()
    }

    /**
     * Attaches persistent ValueEventListener to `/expenses` node.
     * Any change on any device instantly pushes updates to UI State (StateFlow)
     * and local Room DB.
     */
    private fun setupPersistentExpenseListener() {
        try {
            expensesRef.keepSynced(true)
        } catch (_: Exception) {}

        expenseListener?.let {
            try { expensesRef.removeEventListener(it) } catch (_: Exception) {}
            expenseListener = null
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                val sortedList = parsedMap.values.map { it.first }.distinctBy { it.id }.sortedWith(
                    compareByDescending<ExpenseItem> {
                        if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                    }.thenByDescending { it.id }
                )
                _liveExpenses.value = sortedList
                scope.launch(Dispatchers.IO) {
                    syncExpensesToLocalRoom(sortedList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "expensesRef listener cancelled: ${error.message}")
            }
        }
        expenseListener = listener
        expensesRef.addValueEventListener(listener)
    }

    /**
     * Disposes of the active ValueEventListener to prevent listener stacking and memory leaks.
     */
    fun cleanUp() {
        expenseListener?.let {
            try { expensesRef.removeEventListener(it) } catch (_: Exception) {}
            expenseListener = null
        }
    }

    private suspend fun syncExpensesToLocalRoom(remoteExpenses: List<ExpenseItem>) = withContext(Dispatchers.IO) {
        try {
            if (remoteExpenses.isEmpty()) return@withContext
            val localExpenses = expenseDao.getAllExpensesDirect().associateBy { it.id }
            val toUpsert = mutableListOf<ExpenseItem>()
            for (remote in remoteExpenses) {
                val local = localExpenses[remote.id]
                if (local == null || remote != local) {
                    toUpsert.add(remote)
                }
            }
            if (toUpsert.isNotEmpty()) {
                expenseDao.insertAll(toUpsert)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing expenses to Room: ${e.message}")
        }
    }

    /**
     * Direct Write/Push to Firebase First for Expense Entry:
     * Immediately executes .setValue() to /expenses/{id}, then stores in Room SQLite.
     */
    suspend fun insertExpense(expense: ExpenseItem): Long = withContext(Dispatchers.IO) {
        val uniqueId = if (expense.id == 0L) AppRepository.generateUniqueId() else expense.id
        val now = System.currentTimeMillis()
        val expenseWithId = expense.copy(
            id = uniqueId,
            timestamp = if (expense.timestamp == 0L) now else expense.timestamp
        )

        val key = uniqueId.toString()
        val map = FirebaseSyncManager.expenseToMap(expenseWithId, key)

        // 1. Direct write to Firebase Realtime Database FIRST
        try {
            expensesRef.child(key).setValue(map)
            Log.d(TAG, "Direct Firebase write succeeded for expense $key: ${expense.title}")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase write note for expense $key: ${e.message}")
        }

        // 2. Persist to Room SQLite
        val rowId = expenseDao.insertExpense(expenseWithId)
        return@withContext rowId
    }

    /**
     * Direct Write/Push to Firebase First for Expense Update.
     * Preserves the original entryDate / timestamp so the expense's chronological position remains intact.
     */
    suspend fun updateExpense(expense: ExpenseItem) = withContext(Dispatchers.IO) {
        val existing = expenseDao.getExpenseById(expense.id)
        val preservedTimestamp = when {
            existing != null && existing.timestamp > 0L -> existing.timestamp
            expense.timestamp > 0L -> expense.timestamp
            else -> System.currentTimeMillis()
        }
        val updated = expense.copy(timestamp = preservedTimestamp)
        val key = updated.id.toString()
        val map = FirebaseSyncManager.expenseToMap(updated, key)

        // 1. Direct atomic update to Firebase Realtime Database FIRST
        try {
            expensesRef.child(key).updateChildren(map)
                .addOnSuccessListener {
                    Log.d(TAG, "Direct Firebase atomic update succeeded for expense $key")
                }
                .addOnFailureListener {
                    expensesRef.child(key).setValue(map)
                }
            
            // Clean up / update any legacy keys with the same id
            expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (c in snapshot.children) {
                        val ck = c.key ?: continue
                        if (ck != key) {
                            val cid = (c.child("id").value as? Number)?.toLong()
                                ?: c.child("uniqueId").getValue(String::class.java)?.toLongOrNull()
                                ?: FirebaseSyncManager.parseIdFromString(ck)
                            if (cid == updated.id) {
                                expensesRef.child(ck).removeValue()
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase update note for expense $key: ${e.message}")
        }

        firebaseSyncManager?.pushExpense(updated)

        // 2. Persist to Room SQLite
        expenseDao.updateExpense(updated)
    }

    /**
     * Direct Delete from Firebase Realtime Database first.
     */
    suspend fun deleteExpense(expense: ExpenseItem) = withContext(Dispatchers.IO) {
        val key = expense.id.toString()

        // 1. Direct delete from Firebase Realtime Database FIRST
        try {
            expensesRef.child(key).removeValue()
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "expense", "id" to expense.id, "timestamp" to System.currentTimeMillis())
            )
            Log.d(TAG, "Direct Firebase delete succeeded for expense $key")
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase delete note for expense $key: ${e.message}")
        }

        firebaseSyncManager?.deleteExpense(expense.id)

        // 2. Delete from Room SQLite
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) = withContext(Dispatchers.IO) {
        val key = id.toString()
        try {
            expensesRef.child(key).removeValue()
            deletedItemsRef.child(key).setValue(
                mapOf("type" to "expense", "id" to id, "timestamp" to System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.w(TAG, "Direct Firebase deleteById note: ${e.message}")
        }
        firebaseSyncManager?.deleteExpense(id)
        expenseDao.deleteExpenseById(id)
    }
}
