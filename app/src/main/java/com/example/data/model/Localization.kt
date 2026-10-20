package com.example.data.model

object AppStrings {
    fun isBn(language: String): Boolean = false

    // Navigation Tabs
    fun navDashboard(isBn: Boolean = false) = "Dashboard"
    fun navStock(isBn: Boolean = false) = "Stock & Inventory"
    fun navSales(isBn: Boolean = false) = "Sales & Invoices"
    fun navReports(isBn: Boolean = false) = "Reports & Profit"
    fun navSettings(isBn: Boolean = false) = "Settings"

    // Roles & Badges
    fun roleAdmin(isBn: Boolean = false) = "Admin Mode 👑"
    fun roleTeamMember(isBn: Boolean = false) = "Team Member"
    fun roleAdminUnlocked(isBn: Boolean = false) = "All Data Unlocked"
    fun roleCostLocked(isBn: Boolean = false) = "Cost Price Locked 🔒"
    fun switchToAdmin(isBn: Boolean = false) = "Admin Mode"
    fun switchToTeamMember(isBn: Boolean = false) = "Team Member"

    // Common Actions
    fun cancel(isBn: Boolean = false) = "Cancel"
    fun save(isBn: Boolean = false) = "Save Changes"
    fun close(isBn: Boolean = false) = "Close"
    fun delete(isBn: Boolean = false) = "Delete"
    fun edit(isBn: Boolean = false) = "Edit"
    fun searchPlaceholder(isBn: Boolean = false) = "Search brand, model, IMEI, or date..."
    fun all(isBn: Boolean = false) = "All"
    fun inStock(isBn: Boolean = false) = "In Stock 🏬"
    fun inTransit(isBn: Boolean = false) = "In Transit ✈️"
    fun sold(isBn: Boolean = false) = "Sold"
    fun today(isBn: Boolean = false) = "Today"

    // Currency
    fun rmbRate(rate: Double) = "¥1 = ৳${String.format("%.2f", rate)}"
    fun bdtSymbol() = "৳"
    fun rmbSymbol() = "¥"

    // Number & Badge Formatting
    fun formatBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return buildString {
            for (ch in input) {
                if (ch in '0'..'9') append(bnDigits[ch - '0']) else append(ch)
            }
        }
    }

    fun formatBadgeCount(count: Int, isBn: Boolean = false): String {
        if (count <= 0) return ""
        if (count > 99) return if (isBn) "৯৯+" else "99+"
        val str = count.toString()
        return if (isBn) formatBengaliDigits(str) else str
    }
}
