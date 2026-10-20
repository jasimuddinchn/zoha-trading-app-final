package com.example.data.model

/**
 * Preset options for Other Income source / category.
 */
val OTHER_INCOME_CATEGORIES = listOf(
    "Accessory Sale",
    "Mobile Repair Service",
    "Commission",
    "Other"
)

/**
 * Data model representing non-device income (Extra Income / Other Income)
 * such as accessory sales, mobile repair servicing, commissions, or other miscellaneous cash inflows.
 *
 * Rules:
 * - Does NOT interact with or reduce phone/device stock inventory.
 * - Included directly into both Total Net Balance and Monthly Cash In.
 * - Editable/Deletable by Admins via PIN/Biometric verification.
 */
data class OtherIncomeItem(
    val id: String = "",
    val category: String = "Accessory Sale",
    val amountBdt: Double = 0.0,
    val date: String = "", // "DD/MM/YYYY" format
    val notes: String = "",
    val addedBy: String = "Staff",
    val timestamp: Long = System.currentTimeMillis()
)
