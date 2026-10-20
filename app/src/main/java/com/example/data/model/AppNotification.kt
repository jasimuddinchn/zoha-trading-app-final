package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationType(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    PHONE_SOLD(
        label = "Phone Sold",
        icon = Icons.Default.PointOfSale,
        accentColor = Color(0xFF10B981) // Emerald Green
    ),
    CREDIT_SALE_DUE(
        label = "Credit / Due Sale",
        icon = Icons.Default.ReceiptLong,
        accentColor = Color(0xFFEA580C) // Amber Orange
    ),
    EXPENSE_ADDED(
        label = "Expense Added",
        icon = Icons.Default.Payments,
        accentColor = Color(0xFFEF4444) // Expense Red
    ),
    PHONE_ADDED(
        label = "New Set In Stock",
        icon = Icons.Default.PhoneAndroid,
        accentColor = Color(0xFF2563EB) // Royal Blue
    ),
    TRANSIT_TO_STOCK(
        label = "Transit Received",
        icon = Icons.Default.LocalShipping,
        accentColor = Color(0xFF059669) // Darker Green
    ),
    DUE_PAYMENT_COLLECTED(
        label = "Due Collected",
        icon = Icons.Default.CheckCircle,
        accentColor = Color(0xFF16A34A) // Green
    ),
    PHONE_RETURN_RESTOCK(
        label = "Set Returned / Restocked",
        icon = Icons.Default.AssignmentReturn,
        accentColor = Color(0xFF8B5CF6) // Purple
    ),
    APP_UPDATE_AVAILABLE(
        label = "App Version Update",
        icon = Icons.Default.SystemUpdate,
        accentColor = Color(0xFF0284C7) // Sky Blue
    )
}

data class NotificationDetailField(
    val label: String,
    val value: String
)

data class AppNotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val summary: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val referenceId: String = "",
    val details: List<NotificationDetailField> = emptyMap<String, String>().map { NotificationDetailField(it.key, it.value) },
    val targetPhone: PhoneItem? = null
)
