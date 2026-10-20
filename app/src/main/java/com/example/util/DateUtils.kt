package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    const val STORAGE_DATE_PATTERN = "yyyy-MM-dd"
    const val DISPLAY_DATE_PATTERN = "dd-MMM-yyyy"
    const val DISPLAY_DATE_TIME_PATTERN = "dd-MMM-yyyy, hh:mm a"

    private val inputDatePatterns = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )

    fun getTodayStorageDate(): String {
        return SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.US).format(Date())
    }

    fun getYesterdayStorageDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat(STORAGE_DATE_PATTERN, Locale.US).format(cal.time)
    }

    fun getTodayDisplayDate(): String {
        return formatDisplayDate(getTodayStorageDate())
    }

    fun getYesterdayDisplayDate(): String {
        return formatDisplayDate(getYesterdayStorageDate())
    }

    fun formatSoldDateDisplay(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "N/A"
        val clean = dateStr.trim()
        if (clean.equals("N/A", ignoreCase = true)) return clean

        for (pattern in inputDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed)
                }
            } catch (_: Exception) {
                // continue to next pattern
            }
        }
        return clean
    }

    fun formatStockDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Stock: N/A"
        val clean = dateStr.trim()
        if (clean.equals("N/A", ignoreCase = true)) return "Stock: N/A"

        for (pattern in inputDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    val formatted = SimpleDateFormat("dd.MMM.yy", Locale.US).format(parsed)
                    return "Stock: $formatted"
                }
            } catch (_: Exception) {
                // continue to next pattern
            }
        }
        return "Stock: $clean"
    }

    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val clean = dateStr.trim()
        if (clean.equals("N/A", ignoreCase = true) || clean.equals("ALL", ignoreCase = true)) return clean

        for (pattern in inputDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    return SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(parsed)
                }
            } catch (_: Exception) {
                // continue to next pattern
            }
        }
        return clean
    }

    fun formatDisplayDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDisplayDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            SimpleDateFormat(DISPLAY_DATE_TIME_PATTERN, Locale.US).format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Strictly formats date as "dd-MMM-yyyy" (e.g. 12-Sep-2026), omitting any time components.
     * Used across Customer Ledger history and Due Management.
     */
    fun formatLedgerDateOnly(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    fun formatLedgerDateOnly(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val clean = dateStr.trim()
        if (clean.equals("N/A", ignoreCase = true) || clean.equals("ALL", ignoreCase = true)) return clean

        for (pattern in inputDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    return SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(parsed)
                }
            } catch (_: Exception) {}
        }
        return clean
    }

    fun formatDateHeading(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val clean = dateStr.trim()
        val todayStorage = getTodayStorageDate()
        val yesterdayStorage = getYesterdayStorageDate()
        val formatted = formatDisplayDate(clean)

        return when {
            clean.startsWith(todayStorage) || formatted == getTodayDisplayDate() -> "Today ($formatted)"
            clean.startsWith(yesterdayStorage) || formatted == getYesterdayDisplayDate() -> "Yesterday ($formatted)"
            else -> formatted
        }
    }

    fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        val clean = dateStr.trim()
        if (clean.equals("N/A", ignoreCase = true) || clean.equals("ALL", ignoreCase = true)) return 0L
        for (pattern in inputDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return 0L
    }
}
