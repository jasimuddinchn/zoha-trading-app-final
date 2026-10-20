package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppNotificationItem
import com.example.data.model.AppStrings
import com.example.data.model.NotificationType
import com.example.data.model.PhoneItem
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NotificationHelper

@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotificationItem>,
    onDismiss: () -> Unit,
    onSelectNotification: (AppNotificationItem) -> Unit,
    onMarkAllAsRead: () -> Unit,
    isBengali: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNREAD"

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == "UNREAD") {
            notifications.filter { !it.isRead }
        } else {
            notifications
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
            shadowElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("notification_center_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isBengali) "নোটিফিকেশন" else "Notifications",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Slate900
                                )
                                if (unreadCount > 0) {
                                    val unreadBadgeText = if (isBengali) "${AppStrings.formatBengaliDigits(unreadCount.toString())} নতুন" else "$unreadCount new"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = unreadBadgeText,
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isBengali) "লাইভ অ্যাক্টিভিটি লগ ও আপডেট" else "Real-time activity logs & updates",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filters & "Mark all read" row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val allCountStr = if (isBengali) AppStrings.formatBengaliDigits(notifications.size.toString()) else notifications.size.toString()
                        val unreadCountStr = if (isBengali) AppStrings.formatBengaliDigits(unreadCount.toString()) else unreadCount.toString()
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text(if (isBengali) "সব ($allCountStr)" else "All ($allCountStr)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF),
                                selectedLabelColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "UNREAD",
                            onClick = { selectedFilter = "UNREAD" },
                            label = { Text(if (isBengali) "অপঠিত ($unreadCountStr)" else "Unread ($unreadCountStr)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllAsRead,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark all read",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(6.dp))

                // Notification List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedFilter == "UNREAD") "No unread notifications!" else "No notifications yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New events will appear here automatically",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            NotificationListItemCard(
                                item = item,
                                isDark = isDark,
                                onClick = { onSelectNotification(item) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                        contentColor = if (isDark) Color.White else Slate900
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationListItemCard(
    item: AppNotificationItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val containerBg = when {
        !item.isRead -> if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)
        else -> if (isDark) Color(0xFF0F172A) else Color(0xFFFAFAFA)
    }

    val borderColor = when {
        !item.isRead -> if (isDark) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFF86EFAC)
        else -> if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.isRead) 1.5.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notif_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = item.type.accentColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                border = BorderStroke(0.5.dp, item.type.accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.type.icon,
                        contentDescription = item.type.label,
                        tint = item.type.accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = item.type.accentColor.copy(alpha = if (isDark) 0.2f else 0.12f)
                    ) {
                        Text(
                            text = item.type.label,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.type.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = NotificationHelper.formatRelativeTime(item.timestamp),
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        if (!item.isRead) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isDark) Color.White else Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.summary,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NotificationDetailDialog(
    item: AppNotificationItem,
    onDismiss: () -> Unit,
    onViewCashMemo: ((PhoneItem) -> Unit)? = null,
    onViewInvoice: ((PhoneItem) -> Unit)? = null,
    onOpenUpdateDialog: (() -> Unit)? = null,
    isBengali: Boolean = false
) {
    val isDark = isAppInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.type.accentColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                    border = BorderStroke(1.dp, item.type.accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.type.icon,
                            contentDescription = item.type.label,
                            tint = item.type.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.type.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Slate900,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Event time banner: Time & Date:
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time & Date:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            text = NotificationHelper.formatFullDateTime(item.timestamp),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Slate900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isBengali) "ইভেন্টের বিস্তারিত" else "Event Details",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Slate700
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Key-value Details Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.details.forEachIndexed { index, field ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = field.label,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Slate700,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = field.value,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Slate900
                                )
                            }
                            if (index < item.details.size - 1) {
                                HorizontalDivider(
                                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Context action button
                if (item.targetPhone != null && (item.type == NotificationType.PHONE_SOLD || item.type == NotificationType.CREDIT_SALE_DUE)) {
                    if (onViewCashMemo != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onViewCashMemo(item.targetPhone)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cash Memo", fontSize = 12.sp)
                        }
                    }
                } else if (item.type == NotificationType.APP_UPDATE_AVAILABLE && onOpenUpdateDialog != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenUpdateDialog()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Update", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    )
}
