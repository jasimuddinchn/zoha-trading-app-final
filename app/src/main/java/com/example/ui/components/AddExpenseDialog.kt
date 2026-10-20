package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.model.ExpenseItem

/**
 * Expense Add/Edit Dialog wrapper delegating to [AddEditExpenseDialog]
 * with full support for photo receipts, existing categories plus Brac Bank Deposit,
 * City Bank Deposit, and UCB Bank Deposit.
 */
@Composable
fun AddExpenseDialog(
    isBengali: Boolean = false,
    initialExpense: ExpenseItem? = null,
    onDismiss: () -> Unit,
    onAddExpense: (title: String, category: String, amountBdt: Double, date: String, notes: String, receiptUrl: String?) -> Unit,
    onUpdateExpense: ((ExpenseItem) -> Unit)? = null
) {
    AddEditExpenseDialog(
        isBengali = isBengali,
        initialExpense = initialExpense,
        onDismiss = onDismiss,
        onAddExpense = onAddExpense,
        onUpdateExpense = onUpdateExpense
    )
}
