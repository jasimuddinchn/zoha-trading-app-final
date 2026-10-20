package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.data.model.ExpenseItem
import com.example.data.model.OtherIncomeItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.util.MonthFilterHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing the dynamic financial balance state on the Dashboard.
 */
data class DashboardBalanceState(
    val startingBalance: Double = DashboardViewModel.STARTING_CAPITAL_BALANCE,
    val monthlyIncome: Double = 0.0, // Strictly Cash Received portion of current month sales + other income (excludes due collections)
    val monthlyExpense: Double = 0.0,
    val monthlyNetProfit: Double = 0.0,
    val allTimeSalesRevenue: Double = 0.0,
    val allTimeCashReceived: Double = 0.0,
    val allTimeOtherIncome: Double = 0.0, // All-time extra income included directly in total net balance
    val allTimeDueCollections: Double = 0.0, // Collected dues added to physical drawer cash
    val totalCustomerDueOutstanding: Double = 0.0, // Total remaining due across all customers
    val allTimeExpenses: Double = 0.0,
    val totalNetBalance: Double = DashboardViewModel.STARTING_CAPITAL_BALANCE,
    val currentYearMonth: String = MonthFilterHelper.getCurrentYearMonth()
)

/**
 * ViewModel managing dynamic calculations for the Dashboard screen:
 * 1. Starting Baseline Capital / Opening Balance & Due Collection (Default: ৳ 16,845, editable by Admin)
 * 2. Running Month Income (1st day of current month to today: ONLY Cash Received portion from sales)
 * 3. Running Month Expenses (from /expenses node)
 * 4. Overall Cumulative Total Net Balance:
 *    Dynamic Total Net Balance = Editable Opening Balance + (All-Time Cash Received from Sales) + (All-Time Due Collections) - (All-Time Expenses)
 *
 * Realtime Firebase Synchronization:
 * - Dedicated global nodes: `/financials/opening_balance`, `/financials/due_collections`, and `/settings/opening_balance`.
 * - Persistent ValueEventListeners update local StateFlow and SharedPreferences immediately when any device changes the amount.
 * - Pushes updated values immediately using `.setValue()` when edited by Admin.
 */
class DashboardViewModel : ViewModel() {

    companion object {
        /**
         * Initial starting balance constant representing baseline opening capital.
         */
        const val STARTING_CAPITAL_BALANCE: Double = 16845.0
        private const val PREFS_NAME = "dashboard_balance_prefs"
        private const val KEY_STARTING_BALANCE = "key_starting_balance_amount"
        private const val DATABASE_URL = "https://zoha-trading-default-rtdb.firebaseio.com/"
        private const val TAG = "DashboardViewModel"

        private val _globalStartingBalance = MutableStateFlow(STARTING_CAPITAL_BALANCE)
        val globalStartingBalance: StateFlow<Double> = _globalStartingBalance.asStateFlow()

        private val _globalDueCollections = MutableStateFlow(0.0)
        val globalDueCollections: StateFlow<Double> = _globalDueCollections.asStateFlow()

        private val _globalTotalCustomerDue = MutableStateFlow(0.0)
        val globalTotalCustomerDue: StateFlow<Double> = _globalTotalCustomerDue.asStateFlow()

        private val _globalOtherIncomes = MutableStateFlow<List<OtherIncomeItem>>(emptyList())
        val globalOtherIncomes: StateFlow<List<OtherIncomeItem>> = _globalOtherIncomes.asStateFlow()

        fun updateGlobalOtherIncomes(list: List<OtherIncomeItem>) {
            _globalOtherIncomes.value = list
            Log.d(TAG, "Live synced global other incomes: count=${list.size}, sum=৳ ${list.sumOf { it.amountBdt }}")
        }

        fun updateGlobalDueCollections(amount: Double) {
            if (amount >= 0.0 && _globalDueCollections.value != amount) {
                _globalDueCollections.value = amount
                Log.d(TAG, "Live synced global due collections across devices: ৳ $amount")
            }
        }

        fun updateGlobalTotalOutstandingDue(amount: Double) {
            if (amount >= 0.0 && _globalTotalCustomerDue.value != amount) {
                _globalTotalCustomerDue.value = amount
                Log.d(TAG, "Live synced global total customer due across devices: ৳ $amount")
            }
        }

        @Volatile
        private var isFirebaseListenerAttached = false
        @Volatile
        private var cachedAppContext: Context? = null
        @Volatile
        private var firebaseDatabaseInstance: FirebaseDatabase? = null

        fun getFirebaseDatabase(): FirebaseDatabase? {
            firebaseDatabaseInstance?.let { return it }
            return synchronized(this) {
                firebaseDatabaseInstance ?: try {
                    FirebaseDatabase.getInstance(DATABASE_URL).also {
                        try {
                            it.setPersistenceEnabled(true)
                        } catch (_: Exception) {}
                        firebaseDatabaseInstance = it
                    }
                } catch (_: Exception) {
                    try {
                        FirebaseDatabase.getInstance().also {
                            firebaseDatabaseInstance = it
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice initializing Firebase Database: ${e.message}")
                        null
                    }
                }
            }
        }

        fun getFinancialsRef(): DatabaseReference? {
            return getFirebaseDatabase()?.getReference("financials")?.child("opening_balance")
        }

        fun getSettingsRef(): DatabaseReference? {
            return getFirebaseDatabase()?.getReference("settings")?.child("opening_balance")
        }

        private fun ensureFirebaseAuth(onReady: () -> Unit = {}) {
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser != null) {
                    onReady()
                } else {
                    auth.signInAnonymously()
                        .addOnSuccessListener {
                            Log.d(TAG, "Firebase Anonymous Auth successful for Dashboard")
                            onReady()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Firebase Anonymous Auth note: ${e.message}")
                            onReady()
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth check notice: ${e.message}")
                onReady()
            }
        }

        /**
         * Attaches persistent ValueEventListeners to the Firebase Realtime Database opening balance nodes
         * (/financials/opening_balance and /settings/opening_balance) so any change on any connected
         * device is instantly received in real-time across all devices without requiring an app restart.
         */
        fun attachRealtimeFirebaseListener(context: Context) {
            cachedAppContext = context.applicationContext
            val localStored = getStoredStartingBalance(context)
            if (_globalStartingBalance.value == STARTING_CAPITAL_BALANCE && localStored != STARTING_CAPITAL_BALANCE) {
                _globalStartingBalance.value = localStored
            }

            if (isFirebaseListenerAttached) return
            isFirebaseListenerAttached = true

            ensureFirebaseAuth {
                try {
                    val finRef = getFinancialsRef()
                    val setRef = getSettingsRef()

                    try {
                        finRef?.keepSynced(true)
                        setRef?.keepSynced(true)
                    } catch (_: Exception) {}

                    val listener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) return
                            val amount = parseBalanceAmount(snapshot)
                            if (amount != null && amount >= 0.0) {
                                if (_globalStartingBalance.value != amount) {
                                    _globalStartingBalance.value = amount
                                    cachedAppContext?.let { persistLocally(it, amount) }
                                    Log.d(TAG, "Realtime Firebase sync: Opening Balance updated across devices to ৳ $amount from ${snapshot.ref.path}")
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w(TAG, "Opening balance realtime listener cancelled on ${error.message}")
                        }
                    }

                    finRef?.addValueEventListener(listener)
                    setRef?.addValueEventListener(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error attaching Firebase opening balance listener: ${e.message}")
                }
            }
        }

        fun parseBalanceAmount(snapshot: DataSnapshot): Double? {
            return when (val raw = snapshot.value) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                is Map<*, *> -> {
                    (raw["amount"] as? Number)?.toDouble()
                        ?: (raw["value"] as? Number)?.toDouble()
                        ?: (raw["opening_balance"] as? Number)?.toDouble()
                        ?: (raw["starting_balance"] as? Number)?.toDouble()
                }
                else -> null
            }
        }

        private fun persistLocally(context: Context, amount: Double) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_STARTING_BALANCE, amount.toString()).apply()
            } catch (_: Exception) {}
        }

        /**
         * Retrieves the stored starting balance from SharedPreferences, falling back to default 16,845.
         */
        fun getStoredStartingBalance(context: Context): Double {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val stored = prefs.getString(KEY_STARTING_BALANCE, null)
                stored?.toDoubleOrNull() ?: STARTING_CAPITAL_BALANCE
            } catch (_: Exception) {
                STARTING_CAPITAL_BALANCE
            }
        }

        /**
         * Persists the updated baseline starting balance into SharedPreferences and
         * immediately pushes the updated value using `.setValue()` to the Firebase Realtime
         * Database paths (/financials/opening_balance and /settings/opening_balance).
         */
        fun saveStartingBalance(context: Context, amount: Double) {
            _globalStartingBalance.value = amount
            cachedAppContext = context.applicationContext
            persistLocally(context, amount)
            pushStartingBalanceToFirebase(amount)
        }

        /**
         * Pushes the updated Opening Balance amount to Firebase Realtime Database.
         */
        fun pushStartingBalanceToFirebase(amount: Double) {
            ensureFirebaseAuth {
                try {
                    val db = getFirebaseDatabase()
                    if (db == null) {
                        Log.w(TAG, "FirebaseDatabase instance null when pushing opening balance")
                        return@ensureFirebaseAuth
                    }

                    // 1. Dedicated /financials/opening_balance path
                    db.getReference("financials").child("opening_balance").setValue(amount)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully pushed opening balance to /financials/opening_balance: ৳ $amount")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Notice pushing to /financials/opening_balance: ${e.message}")
                        }

                    // 2. Redundant /settings/opening_balance path for multi-device sync
                    db.getReference("settings").child("opening_balance").setValue(amount)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully pushed opening balance to /settings/opening_balance: ৳ $amount")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Notice pushing to /settings/opening_balance: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed pushing opening balance to Firebase: ${e.message}")
                }
            }
        }

        /**
         * Updates starting balance in local state and SharedPreferences when received from remote cloud sync.
         */
        fun updateStartingBalanceFromRemote(context: Context, amount: Double) {
            if (amount >= 0.0 && _globalStartingBalance.value != amount) {
                _globalStartingBalance.value = amount
                cachedAppContext = context.applicationContext
                persistLocally(context, amount)
                Log.d(TAG, "Updated starting balance from remote cloud to ৳ $amount")
            }
        }

        /**
         * Calculates running month cash income strictly for the current calendar month
         * (from 1st day of the current month to today).
         * STRICT RULE: Captures ONLY the Cash Received portion of current month sales,
         * and EXCLUDES collected due amounts to prevent false inflation of current monthly sales/revenue.
         * Automatically resets baseline display when a new calendar month starts.
         */
        /**
         * Calculates running month cash income strictly for the current calendar month
         * (from 1st day of the current month to today).
         * STRICT RULE: Captures Cash Received portion of current month sales + current month Other Income,
         * and EXCLUDES collected due amounts to prevent false inflation of current monthly sales/revenue.
         * Automatically resets baseline display when a new calendar month starts.
         */
        fun calculateRunningMonthIncome(
            phones: List<PhoneItem>,
            otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value,
            currentYearMonth: String = MonthFilterHelper.getCurrentYearMonth()
        ): Double {
            val soldThisMonth = MonthFilterHelper.filterSoldPhones(phones, currentYearMonth)
            val salesCashIn = soldThisMonth.sumOf { it.effectiveCashReceived }
            val otherIncomeCashIn = otherIncomes.filter {
                MonthFilterHelper.extractYearMonth(it.date, it.timestamp) == currentYearMonth
            }.sumOf { it.amountBdt }
            return salesCashIn + otherIncomeCashIn
        }

        /**
         * Calculates running month expenses strictly for the current calendar month
         * by summing all expense amounts in the /expenses node.
         * Automatically resets when a new calendar month starts.
         */
        fun calculateRunningMonthExpense(
            expenses: List<ExpenseItem>,
            currentYearMonth: String = MonthFilterHelper.getCurrentYearMonth()
        ): Double {
            val expensesThisMonth = MonthFilterHelper.filterExpenses(expenses, currentYearMonth)
            return expensesThisMonth.sumOf { it.amountBdt }
        }

        /**
         * Calculates total monthly net profit strictly from all mobile sales within the specified calendar month
         * (from 1st day of the current month to today) by summing:
         * Profit = (Selling Price) - (Purchase Cost)
         * for all sold items in the /sales node.
         * Automatically updates in real-time when new items are sold or edited,
         * and resets baseline tracking when a new calendar month starts.
         */
        fun calculateMonthlyNetProfit(
            phones: List<PhoneItem>,
            currentYearMonth: String = MonthFilterHelper.getCurrentYearMonth()
        ): Double {
            val soldThisMonth = MonthFilterHelper.filterSoldPhones(phones, currentYearMonth)
            return soldThisMonth.sumOf { phone ->
                val purchaseCost = if (phone.totalCostBdt > 0.0) phone.totalCostBdt else phone.buyingPriceBdt
                phone.sellingPriceBdt - purchaseCost
            }
        }

        /**
         * Calculates all-time cumulative total sales revenue by summing selling prices
         * of all sold devices across the entire database.
         */
        fun calculateAllTimeSalesRevenue(phones: List<PhoneItem>): Double {
            return phones
                .filter { it.status == PhoneStatus.SOLD.code || it.status.equals("SOLD", ignoreCase = true) }
                .sumOf { it.sellingPriceBdt }
        }

        /**
         * Calculates all-time cumulative cash received strictly from sales
         * (capturing only the cash received portion for partial/due sales).
         */
        fun calculateAllTimeCashReceivedFromSales(phones: List<PhoneItem>): Double {
            return phones
                .filter { it.status == PhoneStatus.SOLD.code || it.status.equals("SOLD", ignoreCase = true) }
                .sumOf { it.effectiveCashReceived }
        }

        /**
         * Calculates all-time cumulative other income by summing all entries
         * in the /other_income node.
         */
        fun calculateAllTimeOtherIncome(otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value): Double {
            return otherIncomes.sumOf { it.amountBdt }
        }

        /**
         * Calculates all-time cumulative total expenses by summing all entries
         * in the /expenses node.
         */
        fun calculateAllTimeTotalExpenses(expenses: List<ExpenseItem>): Double {
            return expenses.sumOf { it.amountBdt }
        }

        /**
         * Calculates overall cumulative net cash drawer balance across all time using the formula:
         * Dynamic Total Net Balance = Editable Opening Balance + (All-Time Cash Received from Sales) + (All-Time Other Income) + (All-Time Due Collections) - (All-Time Expenses)
         * Strictly matches physical cash drawer cash.
         */
        fun calculateTotalNetBalance(
            phones: List<PhoneItem>,
            expenses: List<ExpenseItem>,
            dueCollections: Double = _globalDueCollections.value,
            startingCapital: Double = _globalStartingBalance.value,
            otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value
        ): Double {
            val allTimeCashReceived = calculateAllTimeCashReceivedFromSales(phones)
            val allTimeOther = calculateAllTimeOtherIncome(otherIncomes)
            val allTimeExpenses = calculateAllTimeTotalExpenses(expenses)
            return startingCapital + allTimeCashReceived + allTimeOther + dueCollections - allTimeExpenses
        }

        /**
         * Computes full dashboard balance snapshot with complete Cash and Due Reconciliation.
         */
        fun computeBalanceState(
            phones: List<PhoneItem>,
            expenses: List<ExpenseItem>,
            startingCapital: Double = _globalStartingBalance.value,
            dueCollections: Double = _globalDueCollections.value,
            otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value,
            currentYearMonth: String = MonthFilterHelper.getCurrentYearMonth()
        ): DashboardBalanceState {
            val mIncome = calculateRunningMonthIncome(phones, otherIncomes, currentYearMonth)
            val mExpense = calculateRunningMonthExpense(expenses, currentYearMonth)
            val mNetProfit = calculateMonthlyNetProfit(phones, currentYearMonth)
            val allTimeRev = calculateAllTimeSalesRevenue(phones)
            val allTimeCash = calculateAllTimeCashReceivedFromSales(phones)
            val allTimeOther = calculateAllTimeOtherIncome(otherIncomes)
            val allTimeExp = calculateAllTimeTotalExpenses(expenses)
            val netBalance = startingCapital + allTimeCash + allTimeOther + dueCollections - allTimeExp
            val soldPhoneDue = phones
                .filter { it.status == PhoneStatus.SOLD.code || it.status.equals("SOLD", ignoreCase = true) }
                .sumOf { it.effectiveDueAmount }
            val outstandingDue = if (_globalTotalCustomerDue.value > 0.0) _globalTotalCustomerDue.value else soldPhoneDue

            return DashboardBalanceState(
                startingBalance = startingCapital,
                monthlyIncome = mIncome,
                monthlyExpense = mExpense,
                monthlyNetProfit = mNetProfit,
                allTimeSalesRevenue = allTimeRev,
                allTimeCashReceived = allTimeCash,
                allTimeOtherIncome = allTimeOther,
                allTimeDueCollections = dueCollections,
                totalCustomerDueOutstanding = outstandingDue,
                totalNetBalance = netBalance,
                currentYearMonth = currentYearMonth
            )
        }

        fun attachRealtimeFirebaseListenerDirect() {
            try {
                val finRef = getFinancialsRef()
                val setRef = getSettingsRef()
                val dueColRef = getFirebaseDatabase()?.getReference("financials")?.child("due_collections")
                val duesRef = getFirebaseDatabase()?.getReference("customer_dues")

                try {
                    finRef?.keepSynced(true)
                    setRef?.keepSynced(true)
                    dueColRef?.keepSynced(true)
                    duesRef?.keepSynced(true)
                } catch (_: Exception) {}

                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return
                        val amount = parseBalanceAmount(snapshot)
                        if (amount != null && amount >= 0.0) {
                            if (_globalStartingBalance.value != amount) {
                                _globalStartingBalance.value = amount
                                cachedAppContext?.let { persistLocally(it, amount) }
                                Log.d(TAG, "Live synced opening balance to ৳ $amount")
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Opening balance listener cancelled: ${error.message}")
                    }
                }

                finRef?.addValueEventListener(listener)
                setRef?.addValueEventListener(listener)

                dueColRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var sum = 0.0
                        for (child in snapshot.children) {
                            val amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0
                            sum += amount
                        }
                        updateGlobalDueCollections(sum)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

                duesRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalOutstanding = 0.0
                        for (custChild in snapshot.children) {
                            val duesNode = custChild.child("dues")
                            val payNode = custChild.child("payments")
                            var custCredit = 0.0
                            for (d in duesNode.children) {
                                custCredit += (d.child("dueAmount").value as? Number)?.toDouble() ?: 0.0
                            }
                            var custPaid = 0.0
                            for (p in payNode.children) {
                                custPaid += (p.child("amount").value as? Number)?.toDouble() ?: 0.0
                            }
                            val remaining = (custCredit - custPaid).coerceAtLeast(0.0)
                            totalOutstanding += remaining
                        }
                        updateGlobalTotalOutstandingDue(totalOutstanding)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

                val otherIncomeRef = getFirebaseDatabase()?.getReference("other_income")
                try { otherIncomeRef?.keepSynced(true) } catch (_: Exception) {}
                otherIncomeRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<OtherIncomeItem>()
                        for (child in snapshot.children) {
                            val key = child.key ?: continue
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
                            list.add(OtherIncomeItem(id, category, amount, date, notes, addedBy, timestamp))
                        }
                        val sorted = list.distinctBy { it.id }.sortedWith(
                            compareByDescending<OtherIncomeItem> {
                                if (it.timestamp > 0L) it.timestamp else com.example.util.DateUtils.parseDateToMillis(it.date)
                            }.thenByDescending { it.id }
                        )
                        updateGlobalOtherIncomes(sorted)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            } catch (e: Exception) {
                Log.w(TAG, "Error in attachRealtimeFirebaseListenerDirect: ${e.message}")
            }
        }
    }

    init {
        ensureFirebaseAuth {
            attachRealtimeFirebaseListenerDirect()
        }
    }

    val startingBalance: StateFlow<Double> = globalStartingBalance

    private val _balanceState = MutableStateFlow(DashboardBalanceState())
    val balanceState: StateFlow<DashboardBalanceState> = _balanceState.asStateFlow()

    /**
     * Initializes baseline starting balance from local persistence and starts Firebase Realtime Database observer.
     */
    fun initStartingBalance(context: Context) {
        attachRealtimeFirebaseListener(context)
    }

    /**
     * Allows Admin to update the baseline starting balance and recompute financial state,
     * immediately persisting locally and pushing to Firebase Realtime Database.
     */
    fun updateStartingBalance(
        context: Context,
        amount: Double,
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value
    ) {
        saveStartingBalance(context, amount)
        _balanceState.value = computeBalanceState(phones, expenses, amount, _globalDueCollections.value, otherIncomes)
    }

    /**
     * Updates dynamic calculations based on real-time Room / Firebase data updates.
     */
    fun updateData(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        startingCapital: Double = _globalStartingBalance.value,
        otherIncomes: List<OtherIncomeItem> = _globalOtherIncomes.value
    ) {
        _balanceState.value = computeBalanceState(phones, expenses, startingCapital, _globalDueCollections.value, otherIncomes)
    }
}
