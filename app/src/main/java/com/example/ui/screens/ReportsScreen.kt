package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.ExpenseItem
import com.example.data.model.PhoneItem
import com.example.data.model.UserRole

/**
 * ReportsScreen now delegates completely to ExpenseScreen as requested:
 * Renamed to Expense with ONLY Expense Management kept.
 */
@Composable
fun ReportsScreen(
    phones: List<PhoneItem> = emptyList(),
    expenses: List<ExpenseItem>,
    currentRole: UserRole,
    isBengali: Boolean = false,
    adminPin: String = "1111",
    onAddExpenseClick: () -> Unit,
    onDeleteExpenseClick: (ExpenseItem) -> Unit,
    onUpdateExpenseClick: ((ExpenseItem) -> Unit)? = null,
    onRequireAdminAuth: ((title: String, actionTitle: String, actionDesc: String, icon: ImageVector, confirmText: String, isDanger: Boolean, onAuth: () -> Unit) -> Unit)? = null,
    onUnlockAdminClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ExpenseScreen(
        expenses = expenses,
        currentRole = currentRole,
        isBengali = isBengali,
        adminPin = adminPin,
        phones = phones,
        onAddExpenseClick = onAddExpenseClick,
        onDeleteExpenseClick = onDeleteExpenseClick,
        onUpdateExpenseClick = onUpdateExpenseClick,
        onRequireAdminAuth = onRequireAdminAuth,
        onUnlockAdminClick = onUnlockAdminClick,
        modifier = modifier
    )
}
