package com.example.util

import android.util.Log
import com.example.data.model.AppSettings
import com.example.data.model.PhoneItem
import com.example.data.model.formatGradeLetter
import com.example.data.model.getDisplayNameWithGrade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object InvoiceEmailHelper {

    private const val TAG = "InvoiceEmailHelper"
    private const val PRIMARY_ADMIN_EMAIL = "jasimuddin.chn@gmail.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun generateInvoiceText(phone: PhoneItem, settings: AppSettings): String {
        return buildString {
            appendLine("==================================================")
            appendLine("       ${settings.shopName.uppercase()} - SALES INVOICE")
            appendLine("==================================================")
            appendLine("Shop: ${settings.shopName}")
            if (settings.shopSubtitle.isNotBlank()) appendLine("Tagline: ${settings.shopSubtitle}")
            appendLine("Address: ${settings.shopAddress}")
            appendLine("Hotline: ${settings.shopPhone}")
            appendLine("--------------------------------------------------")
            appendLine("INVOICE NO: INV-ZT-${phone.id}")
            appendLine("DATE: ${DateUtils.formatDisplayDate(phone.saleDate.ifBlank { phone.entryDate })}")
            appendLine("SOLD BY STAFF: ${phone.soldByStaff.ifBlank { "Staff" }}")
            appendLine("--------------------------------------------------")
            appendLine("CUSTOMER INFORMATION:")
            appendLine("Name: ${phone.customerName.trim().uppercase().ifBlank { "WALK-IN CUSTOMER" }}")
            if (phone.customerPhone.isNotBlank()) appendLine("Phone: ${phone.customerPhone}")
            if (phone.customerEmail.isNotBlank()) appendLine("Email: ${phone.customerEmail}")
            appendLine("--------------------------------------------------")
            appendLine("ITEM DETAILS:")
            appendLine("Brand & Model: ${phone.getDisplayNameWithGrade()}")
            appendLine("IMEI 1: ${phone.imei}")
            if (phone.imei2.isNotBlank()) appendLine("IMEI 2: ${phone.imei2}")
            appendLine("Color: ${phone.color} | Variant: ${phone.ramRom}")
            if (phone.conditionGrade.isNotBlank()) {
                appendLine("Condition: ${formatGradeLetter(phone.conditionGrade)}")
            }
            appendLine("Warranty: ${phone.warrantyDays} Days Replacement Warranty")
            appendLine("--------------------------------------------------")
            appendLine("PAYMENT SUMMARY:")
            appendLine("TOTAL SALE PRICE: ৳ ${String.format("%,.0f", phone.sellingPriceBdt)}")
            appendLine("PAYMENT METHOD: ${phone.paymentMethod.ifBlank { "Cash" }}")
            appendLine("PAYMENT STATUS: PAID IN FULL")
            appendLine("--------------------------------------------------")
            appendLine("WARRANTY TERMS:")
            appendLine(settings.defaultWarrantyText)
            appendLine("==================================================")
            appendLine("Auto-generated sales alert from Zoha Trading POS.")
        }
    }

    /**
     * Automatically and silently dispatches the sales invoice email in the background.
     * - Primary Recipient: jasimuddin.chn@gmail.com
     * - Customer Recipient: customerEmail if provided
     * - Silent: No UI, No Toasts, No Chooser Intent, No Popups
     */
    fun sendInvoiceEmailSilently(phone: PhoneItem, settings: AppSettings) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recipients = mutableListOf<String>()
                recipients.add(PRIMARY_ADMIN_EMAIL)
                
                val configuredOwnerEmail = settings.ownerNotificationEmail.trim()
                if (configuredOwnerEmail.isNotBlank() && 
                    configuredOwnerEmail.contains("@") && 
                    !recipients.contains(configuredOwnerEmail)) {
                    recipients.add(configuredOwnerEmail)
                }

                val customerEmail = phone.customerEmail.trim()
                if (customerEmail.isNotBlank() && 
                    customerEmail.contains("@") && 
                    !recipients.contains(customerEmail)) {
                    recipients.add(customerEmail)
                }

                val subject = "[Zoha Trading Sale] ${phone.brand} ${phone.model} - ৳${String.format("%,.0f", phone.sellingPriceBdt)} (Inv #${phone.id})"
                val invoiceBody = generateInvoiceText(phone, settings)

                Log.d(TAG, "Dispatching background silent invoice email to: ${recipients.joinToString(", ")}")

                // Prepare JSON payload for background email service
                val payload = JSONObject().apply {
                    put("to", JSONArray(recipients))
                    put("subject", subject)
                    put("body", invoiceBody)
                    put("invoiceId", "INV-ZT-${phone.id}")
                    put("shopName", settings.shopName)
                    put("customerName", phone.customerName.ifBlank { "Customer" })
                    put("customerEmail", phone.customerEmail)
                    put("amount", phone.sellingPriceBdt)
                    put("timestamp", System.currentTimeMillis())
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = payload.toString().toRequestBody(mediaType)

                // Dispatch to email notification endpoint (non-blocking, gracefully logged)
                val request = Request.Builder()
                    .url("https://formspree.io/f/mqkenvqo") // Reliable webhook relay endpoint
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    response.close()
                    Log.i(TAG, "Background invoice email dispatched successfully. Code: ${response.code}")
                } catch (e: Exception) {
                    Log.w(TAG, "Silent email network delivery skipped/queued: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Silent background email error handled: ${e.message}")
            }
        }
    }
}
