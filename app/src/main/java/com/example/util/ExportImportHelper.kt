package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupBundle(
    val exportDate: String,
    val totalPhones: Int,
    val totalExpenses: Int,
    val phones: List<PhoneItem>,
    val expenses: List<ExpenseItem>,
    val settings: AppSettings? = null
)

object ExportImportHelper {

    private fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString().replace("\"", "\"\"")
        return if (str.contains(",") || str.contains("\n") || str.contains("\r") || str.contains("\"")) {
            "\"$str\""
        } else {
            str
        }
    }

    /**
     * Generate Excel-compatible CSV for Complete Phone Stock / Inventory
     */
    fun generateStockCsv(phones: List<PhoneItem>, rmbRate: Double): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("ID,Brand,Model,IMEI 1,IMEI 2,Color,RAM/ROM,Variant,Battery Health,Condition,Status,Entry Date,Cost RMB (¥),Cost BDT (৳),Cargo/Shipping (৳),Other/Repair (৳),Total Landing Cost (৳),Selling Price (৳),Sale Date,Sold By,Customer Name,Customer Phone,Net Profit (৳),Margin (%),Notes")

        phones.forEach { p ->
            val statusDisplay = when (p.status) {
                PhoneStatus.IN_STOCK.code -> "In Stock"
                PhoneStatus.IN_TRANSIT.code -> "In Transit"
                PhoneStatus.SOLD.code -> "Sold"
                PhoneStatus.RETURNED.code -> "Returned"
                else -> p.status
            }
            val netProfit = if (p.status == PhoneStatus.SOLD.code && p.sellingPriceBdt > 0) p.sellingPriceBdt - p.totalCostBdt else 0.0
            val margin = if (p.totalCostBdt > 0 && netProfit != 0.0) (netProfit / p.totalCostBdt) * 100.0 else 0.0

            sb.append(escapeCsv(p.id)).append(",")
            sb.append(escapeCsv(p.brand)).append(",")
            sb.append(escapeCsv(p.model)).append(",")
            sb.append(escapeCsv(p.imei)).append(",")
            sb.append(escapeCsv(p.imei2)).append(",")
            sb.append(escapeCsv(p.color)).append(",")
            sb.append(escapeCsv(p.ramRom)).append(",")
            sb.append(escapeCsv(p.variant)).append(",")
            sb.append(escapeCsv(p.batteryHealth)).append(",")
            sb.append(escapeCsv(p.conditionGrade)).append(",")
            sb.append(escapeCsv(statusDisplay)).append(",")
            sb.append(escapeCsv(p.entryDate)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.buyingPriceRmb))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.buyingPriceBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.shippingCostBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.otherCostBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.totalCostBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.sellingPriceBdt))).append(",")
            sb.append(escapeCsv(p.saleDate)).append(",")
            sb.append(escapeCsv(p.soldByStaff)).append(",")
            sb.append(escapeCsv(p.customerName)).append(",")
            sb.append(escapeCsv(p.customerPhone)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", netProfit))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.1f%%", margin))).append(",")
            sb.append(escapeCsv(p.notes))
            sb.appendLine()
        }
        return sb.toString()
    }

    /**
     * Generate Excel-compatible CSV for Sales Memos & History
     */
    fun generateSalesCsv(phones: List<PhoneItem>): String {
        val soldPhones = phones.filter { it.status == PhoneStatus.SOLD.code }
        val sb = StringBuilder()
        sb.appendLine("Invoice No,Sale Date,Brand,Model,IMEI 1,IMEI 2,Color,RAM/ROM,Variant,Battery,Selling Price BDT (৳),Total Cost BDT (৳),Net Profit BDT (৳),Payment Method,Customer Name,Customer Phone,Customer Email,Sold By Staff,Warranty (Days)")

        soldPhones.forEach { p ->
            val netProfit = p.sellingPriceBdt - p.totalCostBdt
            sb.append(escapeCsv("INV-ZT-${p.id}")).append(",")
            sb.append(escapeCsv(p.saleDate.ifBlank { p.entryDate })).append(",")
            sb.append(escapeCsv(p.brand)).append(",")
            sb.append(escapeCsv(p.model)).append(",")
            sb.append(escapeCsv(p.imei)).append(",")
            sb.append(escapeCsv(p.imei2)).append(",")
            sb.append(escapeCsv(p.color)).append(",")
            sb.append(escapeCsv(p.ramRom)).append(",")
            sb.append(escapeCsv(p.variant)).append(",")
            sb.append(escapeCsv(p.batteryHealth)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.sellingPriceBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.totalCostBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", netProfit))).append(",")
            sb.append(escapeCsv(p.paymentMethod.ifBlank { "Cash" })).append(",")
            sb.append(escapeCsv(p.customerName)).append(",")
            sb.append(escapeCsv(p.customerPhone)).append(",")
            sb.append(escapeCsv(p.customerEmail)).append(",")
            sb.append(escapeCsv(p.soldByStaff)).append(",")
            sb.append(escapeCsv(p.warrantyDays))
            sb.appendLine()
        }
        return sb.toString()
    }

    /**
     * Generate Excel-compatible CSV for Monthly Shop Expenses
     */
    fun generateExpensesCsv(expenses: List<ExpenseItem>): String {
        val sb = StringBuilder()
        sb.appendLine("Expense ID,Date,Title,Category,Amount BDT (৳),Added By,Notes")

        expenses.forEach { exp ->
            sb.append(escapeCsv(exp.id)).append(",")
            sb.append(escapeCsv(exp.date)).append(",")
            sb.append(escapeCsv(exp.title)).append(",")
            sb.append(escapeCsv(exp.category)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", exp.amountBdt))).append(",")
            sb.append(escapeCsv(exp.addedBy)).append(",")
            sb.append(escapeCsv(exp.notes))
            sb.appendLine()
        }
        return sb.toString()
    }

    /**
     * Generate Excel-compatible CSV for Profit & Comprehensive Financial Reports
     */
    fun generateFinancialReportCsv(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        settings: AppSettings?
    ): String {
        val soldPhones = phones.filter { it.status == PhoneStatus.SOLD.code }
        val inStockPhones = phones.filter { it.status == PhoneStatus.IN_STOCK.code || it.status == PhoneStatus.IN_TRANSIT.code }
        
        val totalRevenue = soldPhones.sumOf { it.sellingPriceBdt }
        val totalCogs = soldPhones.sumOf { it.totalCostBdt }
        val grossProfit = totalRevenue - totalCogs
        val totalExpenses = expenses.sumOf { it.amountBdt }
        val netProfit = grossProfit - totalExpenses
        val currentStockValue = inStockPhones.sumOf { it.totalCostBdt }
        val overallMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0

        val sb = StringBuilder()
        sb.appendLine("=== ZOHA TRADING - EXECUTIVE FINANCIAL & PROFIT REPORT ===")
        sb.appendLine("Generated At,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Shop Name,${escapeCsv(settings?.shopName ?: "Zoha Trading")}")
        sb.appendLine("RMB to BDT Exchange Rate,${String.format(Locale.US, "%.2f", settings?.rmbToBdtRate ?: 17.50)}")
        sb.appendLine()

        sb.appendLine("--- FINANCIAL SUMMARY ---")
        sb.appendLine("Key Metric,Amount / Value")
        sb.appendLine("Total Phones Sold,${soldPhones.size} units")
        sb.appendLine("Total Sales Revenue (BDT),${String.format(Locale.US, "%.2f", totalRevenue)}")
        sb.appendLine("Cost of Goods Sold - COGS (BDT),${String.format(Locale.US, "%.2f", totalCogs)}")
        sb.appendLine("Gross Sales Profit (BDT),${String.format(Locale.US, "%.2f", grossProfit)}")
        sb.appendLine("Total Operating Expenses (BDT),${String.format(Locale.US, "%.2f", totalExpenses)}")
        sb.appendLine("Net Business Profit (BDT),${String.format(Locale.US, "%.2f", netProfit)}")
        sb.appendLine("Net Profit Margin (%),${String.format(Locale.US, "%.1f%%", overallMargin)}")
        sb.appendLine("Current Inventory in Stock,${inStockPhones.size} units")
        sb.appendLine("Total Stock Asset Value (BDT),${String.format(Locale.US, "%.2f", currentStockValue)}")
        sb.appendLine()

        sb.appendLine("--- SOLD PHONES PROFIT BREAKDOWN ---")
        sb.appendLine("Invoice No,Sale Date,Brand,Model,IMEI,Sold By,Customer,Cost BDT (৳),Sale Price BDT (৳),Net Profit BDT (৳),Margin (%)")
        soldPhones.forEach { p ->
            val pProfit = p.sellingPriceBdt - p.totalCostBdt
            val pMargin = if (p.totalCostBdt > 0) (pProfit / p.totalCostBdt) * 100.0 else 0.0
            sb.append(escapeCsv("INV-ZT-${p.id}")).append(",")
            sb.append(escapeCsv(p.saleDate.ifBlank { p.entryDate })).append(",")
            sb.append(escapeCsv(p.brand)).append(",")
            sb.append(escapeCsv(p.model)).append(",")
            sb.append(escapeCsv(p.imei)).append(",")
            sb.append(escapeCsv(p.soldByStaff)).append(",")
            sb.append(escapeCsv(p.customerName)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.totalCostBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", p.sellingPriceBdt))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", pProfit))).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.1f%%", pMargin)))
            sb.appendLine()
        }
        sb.appendLine()

        sb.appendLine("--- OPERATING EXPENSES BREAKDOWN ---")
        sb.appendLine("Expense ID,Date,Title,Category,Amount BDT (৳),Added By,Notes")
        expenses.forEach { exp ->
            sb.append(escapeCsv(exp.id)).append(",")
            sb.append(escapeCsv(exp.date)).append(",")
            sb.append(escapeCsv(exp.title)).append(",")
            sb.append(escapeCsv(exp.category)).append(",")
            sb.append(escapeCsv(String.format(Locale.US, "%.2f", exp.amountBdt))).append(",")
            sb.append(escapeCsv(exp.addedBy)).append(",")
            sb.append(escapeCsv(exp.notes))
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Generate Comprehensive Full Database CSV containing stock, sales, expenses, and settings
     */
    fun generateFullDatabaseCsv(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        settings: AppSettings?
    ): String {
        val rate = settings?.rmbToBdtRate ?: 17.50
        val sb = StringBuilder()
        sb.appendLine("=== SECTION 1: COMPLETE INVENTORY & PHONE RECORDS ===")
        sb.append(generateStockCsv(phones, rate))
        sb.appendLine()
        sb.appendLine("=== SECTION 2: OPERATING EXPENSES RECORDS ===")
        sb.append(generateExpensesCsv(expenses))
        sb.appendLine()
        sb.appendLine("=== SECTION 3: STORE CONFIGURATION & SETTINGS ===")
        sb.appendLine("Setting Name,Setting Value")
        sb.appendLine("Shop Name,${escapeCsv(settings?.shopName ?: "Zoha Trading")}")
        sb.appendLine("Shop Subtitle,${escapeCsv(settings?.shopSubtitle ?: "")}")
        sb.appendLine("RMB to BDT Rate,${settings?.rmbToBdtRate ?: 17.50}")
        sb.appendLine("Shop Address,${escapeCsv(settings?.shopAddress ?: "")}")
        sb.appendLine("Shop Phone,${escapeCsv(settings?.shopPhone ?: "")}")
        sb.appendLine("Admin Name,${escapeCsv(settings?.adminNameEn ?: "Jasim")}")
        sb.appendLine("Staff 1 Name,${escapeCsv(settings?.staff1NameEn ?: "Sumon")}")
        sb.appendLine("Staff 2 Name,${escapeCsv(settings?.staff2NameEn ?: "Deedat")}")
        sb.appendLine("Owner Email,${escapeCsv(settings?.ownerNotificationEmail ?: "")}")
        return sb.toString()
    }

    /**
     * Generate Complete JSON Full Backup
     */
    fun generateBackupJson(
        phones: List<PhoneItem>,
        expenses: List<ExpenseItem>,
        settings: AppSettings?
    ): String {
        val root = JSONObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        root.put("app", "Zoha Trading POS")
        root.put("version", 2)
        root.put("exportDate", sdf.format(Date()))
        root.put("totalPhones", phones.size)
        root.put("totalExpenses", expenses.size)

        // Phones Array
        val phonesArray = JSONArray()
        phones.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("brand", p.brand)
                put("model", p.model)
                put("imei", p.imei)
                put("imei2", p.imei2)
                put("color", p.color)
                put("ramRom", p.ramRom)
                put("variant", p.variant)
                put("batteryHealth", p.batteryHealth)
                put("conditionGrade", p.conditionGrade)
                put("entryDate", p.entryDate)
                put("status", p.status)
                put("notes", p.notes)
                put("buyingPriceRmb", p.buyingPriceRmb)
                put("buyingPriceBdt", p.buyingPriceBdt)
                put("shippingCostBdt", p.shippingCostBdt)
                put("otherCostBdt", p.otherCostBdt)
                put("totalCostBdt", p.totalCostBdt)
                put("sellingPriceBdt", p.sellingPriceBdt)
                put("saleDate", p.saleDate)
                put("customerName", p.customerName)
                put("customerPhone", p.customerPhone)
                put("customerEmail", p.customerEmail)
                put("soldByStaff", p.soldByStaff)
                put("paymentMethod", p.paymentMethod)
                put("warrantyDays", p.warrantyDays)
                put("addedByRole", p.addedByRole)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            phonesArray.put(obj)
        }
        root.put("phones", phonesArray)

        // Expenses Array
        val expensesArray = JSONArray()
        expenses.forEach { exp ->
            val obj = JSONObject().apply {
                put("id", exp.id)
                put("title", exp.title)
                put("category", exp.category)
                put("amountBdt", exp.amountBdt)
                put("date", exp.date)
                put("notes", exp.notes)
                put("addedBy", exp.addedBy)
                put("timestamp", exp.timestamp)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Settings
        if (settings != null) {
            val settingsObj = JSONObject().apply {
                put("rmbToBdtRate", settings.rmbToBdtRate)
                put("shopName", settings.shopName)
                put("shopSubtitle", settings.shopSubtitle)
                put("shopAddress", settings.shopAddress)
                put("shopPhone", settings.shopPhone)
                put("adminName", settings.adminName)
                put("staff1Name", settings.staff1Name)
                put("staff2Name", settings.staff2Name)
                put("ownerNotificationEmail", settings.ownerNotificationEmail)
                put("themeMode", settings.themeMode)
                put("dynamicColor", settings.dynamicColor)
            }
            root.put("settings", settingsObj)
        }

        return root.toString(2)
    }

    /**
     * Parse Backup JSON string
     */
    fun parseBackupJson(jsonString: String): BackupBundle? {
        return try {
            val root = JSONObject(jsonString)
            val exportDate = root.optString("exportDate", "Unknown")

            val phonesList = mutableListOf<PhoneItem>()
            val phonesArray = root.optJSONArray("phones")
            if (phonesArray != null) {
                for (i in 0 until phonesArray.length()) {
                    val obj = phonesArray.getJSONObject(i)
                    phonesList.add(
                        PhoneItem(
                            id = obj.optLong("id", 0L),
                            brand = obj.optString("brand", ""),
                            model = obj.optString("model", ""),
                            imei = obj.optString("imei", ""),
                            imei2 = obj.optString("imei2", ""),
                            color = obj.optString("color", ""),
                            ramRom = obj.optString("ramRom", ""),
                            variant = obj.optString("variant", "Global"),
                            batteryHealth = obj.optString("batteryHealth", "100%"),
                            conditionGrade = obj.optString("conditionGrade", "Grade A"),
                            entryDate = obj.optString("entryDate", ""),
                            status = obj.optString("status", PhoneStatus.IN_STOCK.code),
                            notes = obj.optString("notes", ""),
                            buyingPriceRmb = obj.optDouble("buyingPriceRmb", 0.0),
                            buyingPriceBdt = obj.optDouble("buyingPriceBdt", 0.0),
                            shippingCostBdt = obj.optDouble("shippingCostBdt", 0.0),
                            otherCostBdt = obj.optDouble("otherCostBdt", 0.0),
                            totalCostBdt = obj.optDouble("totalCostBdt", 0.0),
                            sellingPriceBdt = obj.optDouble("sellingPriceBdt", 0.0),
                            saleDate = obj.optString("saleDate", ""),
                            customerName = obj.optString("customerName", ""),
                            customerPhone = obj.optString("customerPhone", ""),
                            customerEmail = obj.optString("customerEmail", ""),
                            soldByStaff = obj.optString("soldByStaff", ""),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            warrantyDays = obj.optInt("warrantyDays", 10),
                            addedByRole = obj.optString("addedByRole", "ADMIN"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val expensesList = mutableListOf<ExpenseItem>()
            val expensesArray = root.optJSONArray("expenses")
            if (expensesArray != null) {
                for (i in 0 until expensesArray.length()) {
                    val obj = expensesArray.getJSONObject(i)
                    expensesList.add(
                        ExpenseItem(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            category = obj.optString("category", "General"),
                            amountBdt = obj.optDouble("amountBdt", 0.0),
                            date = obj.optString("date", ""),
                            notes = obj.optString("notes", ""),
                            addedBy = obj.optString("addedBy", "Admin"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            var settings: AppSettings? = null
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                settings = AppSettings(
                    rmbToBdtRate = settingsObj.optDouble("rmbToBdtRate", 17.50),
                    shopName = settingsObj.optString("shopName", "Zoha Trading"),
                    shopSubtitle = settingsObj.optString("shopSubtitle", ""),
                    shopAddress = settingsObj.optString("shopAddress", ""),
                    shopPhone = settingsObj.optString("shopPhone", ""),
                    adminName = settingsObj.optString("adminName", "Jasim"),
                    staff1Name = settingsObj.optString("staff1Name", "Sumon"),
                    staff2Name = settingsObj.optString("staff2Name", "Deedat"),
                    ownerNotificationEmail = settingsObj.optString("ownerNotificationEmail", "jasimuddin.nsu@gmail.com"),
                    themeMode = settingsObj.optString("themeMode", "LIGHT"),
                    dynamicColor = settingsObj.optBoolean("dynamicColor", true)
                )
            }

            BackupBundle(
                exportDate = exportDate,
                totalPhones = phonesList.size,
                totalExpenses = expensesList.size,
                phones = phonesList,
                expenses = expensesList,
                settings = settings
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share or save a generated file via Android FileProvider
     */
    fun shareExportFile(
        context: Context,
        filename: String,
        content: String,
        mimeType: String = "text/csv",
        title: String = "Export Data"
    ) {
        try {
            // Save to cache directory
            val cacheFile = File(context.cacheDir, filename)
            FileOutputStream(cacheFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }

            // Also copy to Downloads/Documents if available
            try {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                val localFile = File(docsDir, filename)
                FileOutputStream(localFile).use { fos ->
                    fos.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {}

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "$title: $filename").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Export ready: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Read full text content from an Android content URI (from file picker)
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
