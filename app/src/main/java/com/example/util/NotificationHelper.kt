package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppNotificationItem
import com.example.data.model.DuePaymentRecord
import com.example.data.model.ExpenseItem
import com.example.data.model.NotificationDetailField
import com.example.data.model.NotificationType
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    private const val PREFS_NAME = "zoha_app_notifications_prefs"
    private const val KEY_READ_IDS = "read_notification_ids"
    private const val KEY_CUSTOM_TRANSIT_EVENTS = "custom_transit_events"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getReadIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    }

    fun markNotificationAsRead(context: Context, id: String) {
        val current = getReadIds(context).toMutableSet()
        current.add(id)
        getPrefs(context).edit().putStringSet(KEY_READ_IDS, current).apply()
    }

    fun markAllAsRead(context: Context, notificationIds: Collection<String>) {
        val current = getReadIds(context).toMutableSet()
        current.addAll(notificationIds)
        getPrefs(context).edit().putStringSet(KEY_READ_IDS, current).apply()
    }

    fun logTransitReceived(context: Context, phone: PhoneItem) {
        val current = getPrefs(context).getStringSet(KEY_CUSTOM_TRANSIT_EVENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val entry = "${phone.id}###${phone.brand}###${phone.model}###${phone.imei}###${System.currentTimeMillis()}###${phone.color}###${phone.ramRom}###${phone.conditionGrade}###${phone.batteryHealth}###${phone.itemCondition}###${phone.packaging}###${phone.entryDate}###${phone.addedByRole}"
        current.add(entry)
        getPrefs(context).edit().putStringSet(KEY_CUSTOM_TRANSIT_EVENTS, current).apply()
    }

    fun buildNotifications(
        context: Context,
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        duePayments: List<DuePaymentRecord>
    ): List<AppNotificationItem> {
        val readIds = getReadIds(context)
        val notifications = mutableListOf<AppNotificationItem>()

        // 1. PHONE SALES (Sell হলে)
        val soldPhones = phones.filter { it.status == PhoneStatus.SOLD.code }
        soldPhones.forEach { phone ->
            val id = "notif_sale_${phone.id}"
            val saleMillis = DateUtils.parseDateToMillis(phone.saleDate.ifBlank { phone.entryDate })
            val timestamp = if (saleMillis > 0L) saleMillis else (if (phone.updatedAt > 0L) phone.updatedAt else phone.createdAt)

            val details = listOf(
                NotificationDetailField("Brand & Model", "${phone.brand} ${phone.model}".trim()),
                NotificationDetailField("IMEI", phone.imei.ifBlank { "N/A" }),
                NotificationDetailField("Selling Price", "৳ ${String.format(Locale.US, "%,.0f", phone.sellingPriceBdt)}"),
                NotificationDetailField("Cash Received", "৳ ${String.format(Locale.US, "%,.0f", phone.effectiveCashReceived)}"),
                NotificationDetailField("Customer Name", phone.customerName.trim().ifBlank { "Walk-in Customer" }),
                NotificationDetailField("Customer Mobile", phone.customerPhone.trim().ifBlank { "N/A" }),
                NotificationDetailField("Sold Date", DateUtils.formatSoldDateDisplay(phone.saleDate.ifBlank { phone.entryDate })),
                NotificationDetailField("Sold By", phone.soldByStaff.ifBlank { "Staff" }),
                NotificationDetailField("Payment Method", phone.paymentMethod.ifBlank { "Cash" }),
                NotificationDetailField("Invoice / Memo", "#PH-${phone.id}")
            )

            notifications.add(
                AppNotificationItem(
                    id = id,
                    type = NotificationType.PHONE_SOLD,
                    title = "Phone Sold: ${phone.brand} ${phone.model}".trim(),
                    summary = "Sold for ৳ ${String.format(Locale.US, "%,.0f", phone.sellingPriceBdt)} to ${phone.customerName.trim().ifBlank { "Customer" }}",
                    timestamp = timestamp,
                    isRead = readIds.contains(id),
                    referenceId = phone.id.toString(),
                    details = details,
                    targetPhone = phone
                )
            )

            // 7. CREDIT / DUE SALE (কেও বাকি তে সেট নিলে)
            if (phone.effectiveDueAmount > 0.0 || phone.dueAmountBdt > 0.0) {
                val dueId = "notif_due_sale_${phone.id}"
                val dueDetails = listOf(
                    NotificationDetailField("Customer Name", phone.customerName.trim().ifBlank { "Customer" }),
                    NotificationDetailField("Customer Phone", phone.customerPhone.trim().ifBlank { "N/A" }),
                    NotificationDetailField("Purchased Device", "${phone.brand} ${phone.model} (${phone.ramRom})".trim()),
                    NotificationDetailField("IMEI", phone.imei.ifBlank { "N/A" }),
                    NotificationDetailField("Total Price", "৳ ${String.format(Locale.US, "%,.0f", phone.sellingPriceBdt)}"),
                    NotificationDetailField("Cash Paid", "৳ ${String.format(Locale.US, "%,.0f", phone.effectiveCashReceived)}"),
                    NotificationDetailField("Remaining Due", "৳ ${String.format(Locale.US, "%,.0f", phone.effectiveDueAmount)}"),
                    NotificationDetailField("Sale Date", DateUtils.formatSoldDateDisplay(phone.saleDate.ifBlank { phone.entryDate })),
                    NotificationDetailField("Sold By", phone.soldByStaff.ifBlank { "Staff" }),
                    NotificationDetailField("Due Status", "Pending Collection")
                )

                notifications.add(
                    AppNotificationItem(
                        id = dueId,
                        type = NotificationType.CREDIT_SALE_DUE,
                        title = "Credit Sale Due: ${phone.customerName.trim().ifBlank { "Customer" }}",
                        summary = "Remaining due ৳ ${String.format(Locale.US, "%,.0f", phone.effectiveDueAmount)} on ${phone.brand} ${phone.model}",
                        timestamp = timestamp + 1000L,
                        isRead = readIds.contains(dueId),
                        referenceId = phone.id.toString(),
                        details = dueDetails,
                        targetPhone = phone
                    )
                )
            }
        }

        // 2. EXPENSES (কোনো expense অ্যাড হলে)
        expenses.forEach { expense ->
            val id = "notif_expense_${expense.id}"
            val expMillis = if (expense.timestamp > 0L) expense.timestamp else DateUtils.parseDateToMillis(expense.date)
            val timestamp = if (expMillis > 0L) expMillis else (if (expense.entryDate > 0L) expense.entryDate else System.currentTimeMillis())

            val details = listOf(
                NotificationDetailField("Expense Category", expense.category),
                NotificationDetailField("Amount", "৳ ${String.format(Locale.US, "%,.0f", expense.amountBdt)}"),
                NotificationDetailField("Date", DateUtils.formatDisplayDate(expense.date)),
                NotificationDetailField("Notes / Purpose", expense.notes.ifBlank { expense.category }),
                NotificationDetailField("Recorded By", expense.addedBy.ifBlank { "Admin" })
            )

            notifications.add(
                AppNotificationItem(
                    id = id,
                    type = NotificationType.EXPENSE_ADDED,
                    title = "Expense: ${expense.category}",
                    summary = "৳ ${String.format(Locale.US, "%,.0f", expense.amountBdt)} spent on ${expense.notes.ifBlank { expense.category }}",
                    timestamp = timestamp,
                    isRead = readIds.contains(id),
                    referenceId = expense.id.toString(),
                    details = details
                )
            )
        }

        // 3. NEW SET ENTRY (নতুন সেট এন্ট্রি হলে)
        phones.forEach { phone ->
            val id = "notif_entry_${phone.id}"
            val entryMillis = DateUtils.parseDateToMillis(phone.entryDate)
            val timestamp = if (entryMillis > 0L) entryMillis else (if (phone.createdAt > 0L) phone.createdAt else System.currentTimeMillis())

            val details = listOf(
                NotificationDetailField("Device Model", "${phone.brand} ${phone.model}".trim()),
                NotificationDetailField("IMEI", phone.imei.ifBlank { "N/A" }),
                NotificationDetailField("Specs", "${phone.color} | ${phone.ramRom} | ${phone.conditionGrade}"),
                NotificationDetailField("Battery & Health", phone.batteryHealth.ifBlank { "100%" }),
                NotificationDetailField("Condition", phone.itemCondition.ifBlank { "Used" }),
                NotificationDetailField("Packaging", phone.packaging.ifBlank { "Full Box" }),
                NotificationDetailField("Entry Date", DateUtils.formatSoldDateDisplay(phone.entryDate)),
                NotificationDetailField("Added By", phone.addedByRole.ifBlank { "Staff" }),
                NotificationDetailField("Current Status", when (phone.status) {
                    PhoneStatus.IN_STOCK.code -> "In Stock"
                    PhoneStatus.IN_TRANSIT.code -> "In Transit"
                    PhoneStatus.SOLD.code -> "Sold"
                    PhoneStatus.RETURNED.code -> "Returned"
                    else -> phone.status
                })
            )

            notifications.add(
                AppNotificationItem(
                    id = id,
                    type = NotificationType.PHONE_ADDED,
                    title = "New Set In Stock: ${phone.brand} ${phone.model}".trim(),
                    summary = "IMEI: ${phone.imei.ifBlank { "N/A" }} added to stock (${phone.ramRom}, ${phone.color})",
                    timestamp = timestamp,
                    isRead = readIds.contains(id),
                    referenceId = phone.id.toString(),
                    details = details,
                    targetPhone = phone
                )
            )
        }

        // 5. TRANSIT TO STOCK (transit থেকে স্টক এ সেট আসলে)
        // Check custom logged transit receipts
        val transitRaw = getPrefs(context).getStringSet(KEY_CUSTOM_TRANSIT_EVENTS, emptySet()) ?: emptySet()
        transitRaw.forEach { raw ->
            val parts = raw.split("###")
            if (parts.size >= 5) {
                val pId = parts[0]
                val pBrand = parts[1]
                val pModel = parts[2]
                val pImei = parts[3]
                val pTime = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                val id = "notif_transit_${pId}_$pTime"

                val matchedPhone = phones.find { it.id.toString() == pId || (it.imei.isNotBlank() && it.imei == pImei) }

                val phoneToDisplay = matchedPhone ?: PhoneItem(
                    id = pId.toLongOrNull() ?: 0L,
                    brand = pBrand,
                    model = pModel,
                    imei = pImei,
                    color = parts.getOrNull(5) ?: "",
                    ramRom = parts.getOrNull(6) ?: "",
                    conditionGrade = parts.getOrNull(7) ?: "Grade A",
                    batteryHealth = parts.getOrNull(8) ?: "100%",
                    itemCondition = parts.getOrNull(9) ?: "Used",
                    packaging = parts.getOrNull(10) ?: "Full Box",
                    entryDate = parts.getOrNull(11) ?: DateUtils.getTodayStorageDate(),
                    status = PhoneStatus.IN_STOCK.code,
                    addedByRole = parts.getOrNull(12) ?: "Staff"
                )

                val details = listOf(
                    NotificationDetailField("Device Model", "${phoneToDisplay.brand} ${phoneToDisplay.model}".trim()),
                    NotificationDetailField("IMEI", phoneToDisplay.imei.ifBlank { "N/A" }),
                    NotificationDetailField("Specs", "${phoneToDisplay.color} | ${phoneToDisplay.ramRom} | ${phoneToDisplay.conditionGrade}"),
                    NotificationDetailField("Battery & Health", phoneToDisplay.batteryHealth.ifBlank { "100%" }),
                    NotificationDetailField("Condition", phoneToDisplay.itemCondition.ifBlank { "Used" }),
                    NotificationDetailField("Packaging", phoneToDisplay.packaging.ifBlank { "Full Box" }),
                    NotificationDetailField("Entry Date", DateUtils.formatSoldDateDisplay(phoneToDisplay.entryDate)),
                    NotificationDetailField("Added By", phoneToDisplay.addedByRole.ifBlank { "Staff" }),
                    NotificationDetailField("Current Status", "In Stock")
                )

                notifications.add(
                    AppNotificationItem(
                        id = id,
                        type = NotificationType.TRANSIT_TO_STOCK,
                        title = "Transit Received: ${phoneToDisplay.brand} ${phoneToDisplay.model}".trim(),
                        summary = "Device IMEI ${phoneToDisplay.imei.ifBlank { "N/A" }} arrived from Transit and is now In Stock (${phoneToDisplay.ramRom}, ${phoneToDisplay.color})",
                        timestamp = pTime,
                        isRead = readIds.contains(id),
                        referenceId = pId,
                        details = details,
                        targetPhone = matchedPhone ?: phoneToDisplay
                    )
                )
            }
        }

        // 6. DUE PAYMENT COLLECTED (বাকি পেইমেন্ট কালেক্ট হলে)
        duePayments.forEach { payment ->
            val id = "notif_due_pay_${payment.id}"
            val payMillis = if (payment.timestamp > 0L) payment.timestamp else DateUtils.parseDateToMillis(payment.date)
            val timestamp = if (payMillis > 0L) payMillis else System.currentTimeMillis()

            val details = listOf(
                NotificationDetailField("Customer Name", payment.customerName.trim().ifBlank { "Customer" }),
                NotificationDetailField("Customer Phone", payment.customerPhone.trim().ifBlank { "N/A" }),
                NotificationDetailField("Amount Collected", "৳ ${String.format(Locale.US, "%,.0f", payment.amount)}"),
                NotificationDetailField("Date", DateUtils.formatDisplayDate(payment.date)),
                NotificationDetailField("Payment Method", payment.paymentMethod.ifBlank { "Cash" }),
                NotificationDetailField("Collected By", payment.collectedBy.ifBlank { "Staff" }),
                NotificationDetailField("Notes", payment.notes.ifBlank { "Due recovery" })
            )

            notifications.add(
                AppNotificationItem(
                    id = id,
                    type = NotificationType.DUE_PAYMENT_COLLECTED,
                    title = "Due Payment Received: ৳ ${String.format(Locale.US, "%,.0f", payment.amount)}",
                    summary = "Collected from ${payment.customerName.trim().ifBlank { "Customer" }} via ${payment.paymentMethod.ifBlank { "Cash" }}",
                    timestamp = timestamp,
                    isRead = readIds.contains(id),
                    referenceId = payment.id,
                    details = details
                )
            )
        }

        // 8. PHONE RETURNED OR RESTOCKED (কোনো সেট রিটার্ন আসলে বা restock হলে)
        val returnedPhones = phones.filter {
            it.status == PhoneStatus.RETURNED.code ||
            it.status.equals("RETURN", ignoreCase = true) ||
            it.status.equals("RETURNED", ignoreCase = true)
        }
        returnedPhones.forEach { phone ->
            val id = "notif_return_${phone.id}"
            val retMillis = if (phone.updatedAt > 0L) phone.updatedAt else (if (phone.createdAt > 0L) phone.createdAt else System.currentTimeMillis())

            val details = listOf(
                NotificationDetailField("Device Model", "${phone.brand} ${phone.model}".trim()),
                NotificationDetailField("IMEI", phone.imei.ifBlank { "N/A" }),
                NotificationDetailField("Customer", phone.customerName.trim().ifBlank { "Walk-in" }),
                NotificationDetailField("Returned Status", "Returned / Restocked in inventory"),
                NotificationDetailField("Return Date", DateUtils.formatSoldDateDisplay(phone.saleDate.ifBlank { phone.entryDate })),
                NotificationDetailField("Reason / Notes", phone.notes.ifBlank { "Customer return processed" })
            )

            notifications.add(
                AppNotificationItem(
                    id = id,
                    type = NotificationType.PHONE_RETURN_RESTOCK,
                    title = "Set Returned: ${phone.brand} ${phone.model}".trim(),
                    summary = "Device IMEI ${phone.imei.ifBlank { "N/A" }} returned to stock inventory",
                    timestamp = retMillis,
                    isRead = readIds.contains(id),
                    referenceId = phone.id.toString(),
                    details = details,
                    targetPhone = phone
                )
            )
        }

        // 4. APP VERSION UPDATE AVAILABLE (নতুন অ্যাপ এর ভার্সন আপডেট আসলে)
        val updateInfo = InAppUpdateManager.cachedUpdateInfo
        if (updateInfo != null && updateInfo.isDriveVersionHigher) {
            val updateId = "notif_app_update_${updateInfo.driveVersionName}"
            val updateDetails = listOf(
                NotificationDetailField("Application", "Zoha Trading POS"),
                NotificationDetailField("Installed Version", "v${updateInfo.installedVersionName}"),
                NotificationDetailField("New Drive Version", "v${updateInfo.driveVersionName}"),
                NotificationDetailField("Status", "New Update Available"),
                NotificationDetailField("Update Channel", "Official Google Drive Direct Installer")
            )
            notifications.add(
                AppNotificationItem(
                    id = updateId,
                    type = NotificationType.APP_UPDATE_AVAILABLE,
                    title = "New App Version: v${updateInfo.driveVersionName} Available",
                    summary = "A new version (v${updateInfo.driveVersionName}) is available on Google Drive. Tap to download and install.",
                    timestamp = System.currentTimeMillis() - (10 * 60 * 1000L),
                    isRead = readIds.contains(updateId),
                    details = updateDetails
                )
            )
        }

        // Sort newest first
        return notifications.sortedByDescending { it.timestamp }
    }

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Recent"
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val oneMin = 60 * 1000L
        val oneHour = 60 * oneMin
        val oneDay = 24 * oneHour

        return when {
            diff < 0L -> "Just now"
            diff < oneMin -> "Just now"
            diff < oneHour -> "${(diff / oneMin).coerceAtLeast(1)}m ago"
            diff < oneDay -> "${diff / oneHour}h ago"
            diff < 2 * oneDay -> "Yesterday"
            diff < 7 * oneDay -> "${diff / oneDay}d ago"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timestamp))
        }
    }

    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(timestamp))
    }
}
