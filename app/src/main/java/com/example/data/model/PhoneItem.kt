package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.DateUtils

@Entity(tableName = "phones")
data class PhoneItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Core Phone Info (Entered by Staff or Owner)
    val brand: String,
    val model: String,
    val imei: String,
    val imei2: String = "",
    val color: String = "",
    val ramRom: String = "",
    val variant: String = "Global", // e.g. "Global", "USA (LL/A)", "Japan (J/A)", "China (CH/A)"
    val batteryHealth: String = "100%", // e.g. "100%", "92%", "85%"
    val conditionGrade: String = "Grade A",
    val itemCondition: String = "Used", // e.g. "Used", "Intact", "Brand New", "Inactive"
    val packaging: String = "Full Box", // e.g. "Full Box", "With Box", "Only Set", "Set & Charger", "Set & Cover"
    val entryDate: String, // e.g. "2026-08-20"
    val status: String = PhoneStatus.IN_STOCK.code, // IN_STOCK, IN_TRANSIT, SOLD, RETURNED
    val notes: String = "", // Battery health, variant (e.g. LL/A, CH/A), accessories
    
    // Sourcing / Cost Details (ONLY visible/editable by OWNER)
    val buyingPriceRmb: Double = 0.0, // Buying price in Chinese Yuan (¥)
    val buyingPriceBdt: Double = 0.0, // Buying price in Bangladeshi Taka (৳)
    val shippingCostBdt: Double = 0.0, // Air Cargo / Courier cost to BD (৳)
    val otherCostBdt: Double = 0.0, // Repair, testing, customs or tax (৳)
    val totalCostBdt: Double = 0.0, // Calculated total landing cost = buyingPriceBdt + shippingCostBdt + otherCostBdt
    val desiredProfitBdt: Double = 0.0, // Desired Profit Margin in BDT (৳)
    
    // Sales Details (Updated upon sale by Staff or Owner)
    val sellingPriceBdt: Double = 0.0, // Actual sale price in BDT (৳)
    val saleDate: String = "", // e.g. "2026-08-22"
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val soldByStaff: String = "", // e.g. "কর্মী ১ (করিম)", "কর্মী ২ (রহিম)"
    val paymentMethod: String = "",
    val warrantyDays: Int = 10,
    val cashReceivedBdt: Double = 0.0, // Partial or full cash received at the time of sale
    val dueAmountBdt: Double = 0.0, // Remaining due balance: totalSalePrice - cashReceived

    val addedByRole: String = "STAFF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Calculated effective cash received upon sale
    val effectiveCashReceived: Double
        get() = if (status == PhoneStatus.SOLD.code) {
            if (cashReceivedBdt > 0.0 || dueAmountBdt > 0.0) cashReceivedBdt else sellingPriceBdt
        } else 0.0

    // Calculated effective due amount
    val effectiveDueAmount: Double
        get() = if (status == PhoneStatus.SOLD.code) {
            if (dueAmountBdt > 0.0) {
                dueAmountBdt
            } else if (cashReceivedBdt > 0.0 && sellingPriceBdt > cashReceivedBdt) {
                sellingPriceBdt - cashReceivedBdt
            } else {
                0.0
            }
        } else 0.0
    // Calculated Asking Price = Base Cost + Desired Profit
    val askingPriceBdt: Double
        get() = if (totalCostBdt > 0 || desiredProfitBdt > 0) {
            totalCostBdt + desiredProfitBdt
        } else {
            0.0
        }

    // Calculated net profit for this phone (Owner only)
    val netProfitBdt: Double
        get() = if (status == PhoneStatus.SOLD.code && sellingPriceBdt > 0) {
            sellingPriceBdt - totalCostBdt
        } else {
            0.0
        }

    val profitMarginPercent: Double
        get() = if (totalCostBdt > 0 && netProfitBdt != 0.0) {
            (netProfitBdt / totalCostBdt) * 100.0
        } else {
            0.0
        }
}

fun formatGradeLetter(grade: String): String {
    val trimmed = grade.trim()
    if (trimmed.isBlank()) return ""
    return when {
        trimmed.equals("Grade A+", ignoreCase = true) || trimmed.equals("A+ Grade", ignoreCase = true) || trimmed.equals("A+", ignoreCase = true) -> "A+"
        trimmed.equals("Grade A-", ignoreCase = true) || trimmed.equals("A- Grade", ignoreCase = true) || trimmed.equals("A-", ignoreCase = true) -> "A-"
        trimmed.equals("Grade A", ignoreCase = true) || trimmed.equals("A Grade", ignoreCase = true) || trimmed.equals("A", ignoreCase = true) -> "A"
        trimmed.equals("Grade B", ignoreCase = true) || trimmed.equals("B Grade", ignoreCase = true) || trimmed.equals("B", ignoreCase = true) -> "B"
        trimmed.equals("Grade C", ignoreCase = true) || trimmed.equals("C Grade", ignoreCase = true) || trimmed.equals("C", ignoreCase = true) -> "C"
        else -> {
            val withoutGrade = trimmed.replace("Grade", "", ignoreCase = true).trim()
            if (withoutGrade.isNotBlank()) withoutGrade else trimmed
        }
    }
}

fun PhoneItem.getDisplayNameWithGrade(): String {
    val gradeLetter = formatGradeLetter(conditionGrade)
    val base = if (brand.isNotBlank() && model.isNotBlank()) {
        "$brand $model"
    } else if (brand.isNotBlank()) {
        brand
    } else {
        model.ifBlank { "Phone" }
    }
    return if (gradeLetter.isNotBlank()) "$base ($gradeLetter)" else base
}

/**
 * Sorts all phones (In-Stock, In-Transit, or general inventory) strictly by entry date descending.
 * The set entered most recently appears at the very top (first) of the list.
 * Tie-breaker: createdAt descending, then updatedAt descending, then id descending.
 */
fun List<PhoneItem>.sortedByEntryDateDescending(): List<PhoneItem> {
    return this.sortedWith(
        compareByDescending<PhoneItem> {
            DateUtils.parseDateToMillis(it.entryDate.trim())
        }.thenByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }
        .thenByDescending { it.updatedAt }
        .thenByDescending { it.id }
    )
}

/**
 * Sorts sold phones strictly by selling date descending.
 * The set sold most recently appears at the very top (first) of the list.
 * Tie-breaker: updatedAt descending, then createdAt descending, then id descending.
 */
fun List<PhoneItem>.sortedBySaleDateDescending(): List<PhoneItem> {
    return this.sortedWith(
        compareByDescending<PhoneItem> {
            val d = it.saleDate.ifBlank { it.entryDate }.trim()
            DateUtils.parseDateToMillis(d)
        }.thenByDescending { if (it.updatedAt > 0L) it.updatedAt else it.createdAt }
        .thenByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }
        .thenByDescending { it.id }
    )
}
