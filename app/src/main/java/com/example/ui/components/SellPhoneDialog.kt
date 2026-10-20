package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.model.CustomerDueProfile
import com.example.data.model.PhoneItem

/**
 * Sell Phone Dialog wrapper that forwards directly to [AddEditSaleDialog]
 * to maintain 100% full support for Credit Breakdown, Due Calculation,
 * and Customer Due Ledger posting.
 */
@Composable
fun SellPhoneDialog(
    phone: PhoneItem,
    defaultStaffName: String,
    staff1Name: String,
    staff2Name: String,
    isBengali: Boolean = false,
    customerProfiles: List<CustomerDueProfile> = emptyList(),
    phones: List<PhoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSaleWithDue: ((
        totalSalePrice: Double,
        cashReceived: Double,
        dueAmount: Double,
        saleDate: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        soldByStaff: String,
        paymentMethod: String,
        warrantyDays: Int,
        notes: String
    ) -> Unit)? = null,
    onConfirmSale: (
        sellingPriceBdt: Double,
        saleDate: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        soldByStaff: String,
        paymentMethod: String,
        warrantyDays: Int
    ) -> Unit
) {
    AddEditSaleDialog(
        phone = phone,
        defaultStaffName = defaultStaffName,
        staff1Name = staff1Name,
        staff2Name = staff2Name,
        isBengali = isBengali,
        customerProfiles = customerProfiles,
        phones = phones,
        onDismiss = onDismiss,
        onConfirmSale = { totalPrice, cashReceived, dueAmount, saleDate, customerName, customerPhone, customerEmail, soldByStaff, paymentMethod, warrantyDays, notes ->
            if (onConfirmSaleWithDue != null) {
                onConfirmSaleWithDue(
                    totalPrice,
                    cashReceived,
                    dueAmount,
                    saleDate,
                    customerName,
                    customerPhone,
                    customerEmail,
                    soldByStaff,
                    paymentMethod,
                    warrantyDays,
                    notes
                )
            } else {
                onConfirmSale(
                    totalPrice,
                    saleDate,
                    customerName,
                    customerPhone,
                    customerEmail,
                    soldByStaff,
                    paymentMethod,
                    warrantyDays
                )
            }
        }
    )
}
