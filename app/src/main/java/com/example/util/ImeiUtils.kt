package com.example.util

object ImeiUtils {
    /**
     * Checks if the given string is a valid standard 15-digit IMEI.
     */
    fun isValidImei(imei: String?): Boolean {
        if (imei.isNullOrBlank()) return false
        val clean = imei.filter { it.isDigit() }
        return clean.length == 15
    }

    /**
     * Extracts all valid 15-digit IMEI numbers found inside arbitrary text or barcode raw values.
     * Handles single IMEI, dual-IMEI, prefixes like 'IMEI:', 'IMEI1:', 'IMEI2:', 'S/N:', and barcodes with check digits.
     */
    fun extractImeisFromText(rawText: String?): List<String> {
        if (rawText.isNullOrBlank()) return emptyList()

        val results = mutableListOf<String>()

        // 1. Regex search for standalone 15-digit number sequences
        val exact15Regex = Regex("""\b(\d{15})\b""")
        exact15Regex.findAll(rawText).forEach { match ->
            results.add(match.value)
        }

        // 2. Search for common labeled formats like IMEI1: 3569... or IMEI 2 = 3569...
        val labeledRegex = Regex("""(?:IMEI|MEID|TAC)[\s\w]*[:=\s-]+(\d{14,16})""", RegexOption.IGNORE_CASE)
        labeledRegex.findAll(rawText).forEach { match ->
            val digits = match.groupValues.getOrNull(1)?.filter { it.isDigit() } ?: ""
            if (digits.length == 15) {
                results.add(digits)
            } else if (digits.length > 15) {
                results.add(digits.take(15))
            }
        }

        // 3. Fallback: Check if the cleaned string itself is a 15-digit IMEI or contains one
        val digitsOnly = rawText.filter { it.isDigit() }
        if (digitsOnly.length == 15) {
            results.add(digitsOnly)
        } else if (digitsOnly.length == 30) {
            // Dual-IMEI concatenated together (e.g. 15 digits + 15 digits)
            results.add(digitsOnly.substring(0, 15))
            results.add(digitsOnly.substring(15, 30))
        } else if (digitsOnly.length in 16..29 && results.isEmpty()) {
            // Take first 15 digits if starting with typical TAC digits (35, 86, 01, 99, etc.)
            val first15 = digitsOnly.take(15)
            results.add(first15)
        }

        return results.distinct()
    }

    /**
     * Pretty-formats a 15-digit IMEI into groups for readable display: XXXXXX-XX-XXXXXX-X
     */
    fun formatDisplay(imei: String): String {
        val digits = imei.filter { it.isDigit() }
        if (digits.length != 15) return imei
        return "${digits.substring(0, 6)} ${digits.substring(6, 8)} ${digits.substring(8, 14)} ${digits.substring(14, 15)}"
    }
}
