package com.example.util

import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MonthFilterHelper {

    fun getCurrentYearMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    fun formatMonthDisplayName(yearMonth: String): String {
        if (yearMonth.equals("ALL", ignoreCase = true) || yearMonth.isBlank()) return "All Time"
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = parser.parse(yearMonth) ?: return yearMonth
            SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun extractYearMonth(dateStr: String?, fallbackTimestamp: Long = 0L): String {
        if (!dateStr.isNullOrBlank()) {
            val clean = dateStr.trim()
            if (clean.length >= 7 && clean[4] == '-') {
                return clean.substring(0, 7)
            }
            if (clean.length >= 7 && clean[4] == '/') {
                return clean.substring(0, 7).replace('/', '-')
            }
            val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM")
            for (fmt in formats) {
                try {
                    val d = SimpleDateFormat(fmt, Locale.US).parse(clean)
                    if (d != null) {
                        return SimpleDateFormat("yyyy-MM", Locale.US).format(d)
                    }
                } catch (_: Exception) {}
            }
        }
        if (fallbackTimestamp > 0L) {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(fallbackTimestamp))
        }
        return getCurrentYearMonth()
    }

    fun getAllAvailableMonths(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>
    ): List<String> {
        val set = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        val ymFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        for (i in 0..5) {
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -i)
            set.add(ymFormat.format(c.time))
        }

        phones.forEach { p ->
            if (p.saleDate.isNotBlank()) set.add(extractYearMonth(p.saleDate, p.updatedAt))
            if (p.entryDate.isNotBlank()) set.add(extractYearMonth(p.entryDate, p.createdAt))
        }
        expenses.forEach { e ->
            if (e.date.isNotBlank()) set.add(extractYearMonth(e.date, e.timestamp))
        }

        return set.toList().sortedDescending()
    }

    fun filterSoldPhones(phones: List<PhoneItem>, yearMonth: String): List<PhoneItem> {
        val sold = phones.filter { it.status == PhoneStatus.SOLD.code }
        if (yearMonth.equals("ALL", ignoreCase = true)) return sold
        return sold.filter { p ->
            val ym = extractYearMonth(p.saleDate, p.updatedAt)
            ym == yearMonth
        }
    }

    fun filterStockAddedPhones(phones: List<PhoneItem>, yearMonth: String): List<PhoneItem> {
        if (yearMonth.equals("ALL", ignoreCase = true)) return phones
        return phones.filter { p ->
            val ym = extractYearMonth(p.entryDate, p.createdAt)
            ym == yearMonth
        }
    }

    fun filterExpenses(expenses: List<ExpenseItem>, yearMonth: String): List<ExpenseItem> {
        if (yearMonth.equals("ALL", ignoreCase = true)) return expenses
        return expenses.filter { e ->
            val ym = extractYearMonth(e.date, e.timestamp)
            ym == yearMonth
        }
    }

    fun filterOtherIncomes(
        otherIncomes: List<com.example.data.model.OtherIncomeItem>,
        yearMonth: String
    ): List<com.example.data.model.OtherIncomeItem> {
        if (yearMonth.equals("ALL", ignoreCase = true)) return otherIncomes
        return otherIncomes.filter { item ->
            val ym = extractYearMonth(item.date, item.timestamp)
            ym == yearMonth
        }
    }

    data class MonthFinancialSummary(
        val yearMonth: String,
        val displayName: String,
        val soldPhonesCount: Int,
        val newStockAddedCount: Int,
        val totalRevenue: Double,
        val totalCostOfGoodsSold: Double,
        val grossProfit: Double,
        val totalExpenses: Double,
        val netProfit: Double
    )

    fun calculateSummary(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        yearMonth: String
    ): MonthFinancialSummary {
        val soldList = filterSoldPhones(phones, yearMonth)
        val addedList = filterStockAddedPhones(phones, yearMonth)
        val expList = filterExpenses(expenses, yearMonth)

        val totalRev = soldList.sumOf { it.sellingPriceBdt }
        val cogs = soldList.sumOf { if (it.totalCostBdt > 0.0) it.totalCostBdt else it.buyingPriceBdt }
        val gross = totalRev - cogs
        val totalExp = expList.sumOf { it.amountBdt }
        val net = soldList.sumOf { phone ->
            val purchaseCost = if (phone.totalCostBdt > 0.0) phone.totalCostBdt else phone.buyingPriceBdt
            phone.sellingPriceBdt - purchaseCost
        }

        return MonthFinancialSummary(
            yearMonth = yearMonth,
            displayName = formatMonthDisplayName(yearMonth),
            soldPhonesCount = soldList.size,
            newStockAddedCount = addedList.size,
            totalRevenue = totalRev,
            totalCostOfGoodsSold = cogs,
            grossProfit = gross,
            totalExpenses = totalExp,
            netProfit = net
        )
    }
}
