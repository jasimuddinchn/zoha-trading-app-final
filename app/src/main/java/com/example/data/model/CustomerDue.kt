package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Record representing a credit / partial payment sale that produced an outstanding due.
 */
data class CustomerDueRecord(
    val id: String = "",
    val phoneId: Long = 0L,
    val phoneModel: String = "",
    val imei: String = "",
    val totalSalePrice: Double = 0.0,
    val cashReceived: Double = 0.0,
    val dueAmount: Double = 0.0,
    val saleDate: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val soldByStaff: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Record representing a payment collected against a customer's due balance.
 */
data class DuePaymentRecord(
    val id: String = "",
    val customerKey: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "Cash",
    val collectedBy: String = "",
    val date: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Aggregated profile for a customer in the Due Management Ledger.
 */
data class CustomerDueProfile(
    val customerKey: String,
    val customerName: String,
    val customerPhone: String,
    val totalCreditSales: Double = 0.0,
    val totalCashReceivedAtSale: Double = 0.0,
    val totalDueCollected: Double = 0.0,
    val remainingDue: Double = 0.0,
    val creditSales: List<CustomerDueRecord> = emptyList(),
    val paymentCollections: List<DuePaymentRecord> = emptyList(),
    val lastActivityDate: String = ""
) {
    val isCleared: Boolean
        get() = remainingDue <= 0.0
}

/**
 * Utility functions for customer identification and formatting.
 */
object CustomerDueUtils {
    /**
     * Sanitizes customer phone/name into a reliable key for Firebase nodes and local grouping.
     */
    fun getCustomerKey(phone: String, name: String): String {
        val cleanPhone = phone.trim().replace(Regex("[^0-9+]"), "")
        if (cleanPhone.length >= 6) {
            return cleanPhone
        }
        val cleanName = name.trim().uppercase().replace(Regex("[^A-Z0-9_]"), "_")
        return if (cleanName.isNotBlank()) cleanName else "WALK_IN_CUSTOMER"
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
