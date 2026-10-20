package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.UserRole
import com.example.data.model.formatDeviceDisplayName
import com.example.data.model.getDisplayNameWithGrade
import com.example.ui.theme.CardBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StockGreen
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.DateUtils
import com.example.ui.components.AppListIconButton

data class StatusCardPalette(
    val cardBg: Color,
    val cardBorder: Color,
    val footerBg: Color,
    val footerBorder: Color,
    val iconBg: Color,
    val iconBorder: Color,
    val iconTint: Color
)

@Composable
fun getStatusCardPalette(status: String): StatusCardPalette {
    val isDark = isAppInDarkTheme()
    return when (status) {
        PhoneStatus.SOLD.code, "SOLD" -> {
            if (isDark) {
                StatusCardPalette(
                    cardBg = Color(0xFF141936),
                    cardBorder = Color(0xFF3730A3).copy(alpha = 0.65f),
                    footerBg = Color(0xFF0E1228),
                    footerBorder = Color(0xFF3730A3).copy(alpha = 0.4f),
                    iconBg = Color(0xFF312E81).copy(alpha = 0.4f),
                    iconBorder = Color(0xFF4338CA).copy(alpha = 0.5f),
                    iconTint = Color(0xFF93C5FD)
                )
            } else {
                StatusCardPalette(
                    cardBg = Color(0xFFF0F4FF),
                    cardBorder = Color(0xFFC7D2FE),
                    footerBg = Color(0xFFE5EDFF),
                    footerBorder = Color(0xFFD5E0FF),
                    iconBg = Color(0xFFE0E7FF),
                    iconBorder = Color(0xFFC7D2FE),
                    iconTint = Color(0xFF3730A3)
                )
            }
        }
        PhoneStatus.IN_TRANSIT.code, "TRANSIT" -> {
            if (isDark) {
                StatusCardPalette(
                    cardBg = Color(0xFF24190B),
                    cardBorder = Color(0xFF92400E).copy(alpha = 0.65f),
                    footerBg = Color(0xFF1A1207),
                    footerBorder = Color(0xFF92400E).copy(alpha = 0.4f),
                    iconBg = Color(0xFF78350F).copy(alpha = 0.4f),
                    iconBorder = Color(0xFF92400E).copy(alpha = 0.5f),
                    iconTint = Color(0xFFFDE68A)
                )
            } else {
                StatusCardPalette(
                    cardBg = Color(0xFFFFFBEB),
                    cardBorder = Color(0xFFFDE68A),
                    footerBg = Color(0xFFFEF3C7).copy(alpha = 0.65f),
                    footerBorder = Color(0xFFFDE68A).copy(alpha = 0.6f),
                    iconBg = Color(0xFFFEF3C7),
                    iconBorder = Color(0xFFFDE68A),
                    iconTint = Color(0xFFB45309)
                )
            }
        }
        PhoneStatus.RETURNED.code, "RETURN", "RETURNED" -> {
            if (isDark) {
                StatusCardPalette(
                    cardBg = Color(0xFF1C112E),
                    cardBorder = Color(0xFF6B21A8).copy(alpha = 0.65f),
                    footerBg = Color(0xFF140B22),
                    footerBorder = Color(0xFF6B21A8).copy(alpha = 0.4f),
                    iconBg = Color(0xFF581C87).copy(alpha = 0.4f),
                    iconBorder = Color(0xFF7E22CE).copy(alpha = 0.5f),
                    iconTint = Color(0xFFD8B4FE)
                )
            } else {
                StatusCardPalette(
                    cardBg = Color(0xFFFAF5FF),
                    cardBorder = Color(0xFFE9D5FF),
                    footerBg = Color(0xFFF3E8FF),
                    footerBorder = Color(0xFFE9D5FF),
                    iconBg = Color(0xFFF3E8FF),
                    iconBorder = Color(0xFFE9D5FF),
                    iconTint = Color(0xFF7E22CE)
                )
            }
        }
        else -> { // IN_STOCK ("STOCK")
            if (isDark) {
                StatusCardPalette(
                    cardBg = Color(0xFF0D2419),
                    cardBorder = Color(0xFF166534).copy(alpha = 0.65f),
                    footerBg = Color(0xFF081C13),
                    footerBorder = Color(0xFF166534).copy(alpha = 0.4f),
                    iconBg = Color(0xFF14532D).copy(alpha = 0.4f),
                    iconBorder = Color(0xFF166534).copy(alpha = 0.5f),
                    iconTint = Color(0xFF86EFAC)
                )
            } else {
                StatusCardPalette(
                    cardBg = Color(0xFFF0FDF4),
                    cardBorder = Color(0xFFBBF7D0),
                    footerBg = Color(0xFFE6F8ED),
                    footerBorder = Color(0xFFDCFCE7),
                    iconBg = Color(0xFFDCFCE7),
                    iconBorder = Color(0xFFBBF7D0),
                    iconTint = Color(0xFF15803D)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneCard(
    phone: PhoneItem,
    currentRole: UserRole,
    rmbRate: Double,
    isBengali: Boolean = false,
    onSellClick: () -> Unit,
    onViewMemoClick: () -> Unit,
    onEditCostsClick: () -> Unit,
    onMarkInStockClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onStatusChange: ((String) -> Unit)? = null,
    onQuickFilter: ((filterType: String, query: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val isAdmin = currentRole == UserRole.ADMIN
    val isSold = phone.status == PhoneStatus.SOLD.code
    val isInTransit = phone.status == PhoneStatus.IN_TRANSIT.code
    val isReturned = phone.status == PhoneStatus.RETURNED.code || phone.status == "RETURN"

    val statusPalette = getStatusCardPalette(phone.status)

    var showStatusDropdown by remember { mutableStateOf(false) }

    val profit = if (isSold && phone.totalCostBdt > 0) {
        phone.sellingPriceBdt - phone.totalCostBdt
    } else 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusPalette.cardBg),
        border = BorderStroke(1.dp, statusPalette.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("phone_card_${phone.id}")
    ) {
        Column {
            // MAIN CARD BODY (Header, Details, Badges)
            Column(modifier = Modifier.padding(12.dp)) {
                // Glitter Gradient and Glowing Shadow for Phone Model Title
                val glitterBrush = if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFDE047), // Gold
                            Color(0xFF38BDF8), // Cyan shimmer
                            Color(0xFFF59E0B), // Vivid Amber
                            Color(0xFF67E8F9), // Electric Sky
                            Color(0xFFFDE047)  // Gold highlight
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB45309), // Amber gold
                            Color(0xFF0284C7), // Luminous cyan
                            Color(0xFFD97706), // Warm amber
                            Color(0xFF0369A1), // Sapphire shimmer
                            Color(0xFFB45309)  // Deep gold
                        )
                    )
                }
                val glitterShadow = Shadow(
                    color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFF0284C7).copy(alpha = 0.35f),
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )

                // 1. Header Row: 3-Line Vertical Specification Stack (Flush Left) + Status Badge & Date (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: 3-Line Vertical Specification Stack (Phone Icon removed, flush to left edge)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        val displayName = formatDeviceDisplayName(phone.brand, phone.model)

                        // Line 1: Phone Model Name (15.sp font, Pure White in Dark Mode / Pure Black in Light Mode)
                        Text(
                            text = displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = onQuickFilter != null) {
                                    val q = phone.model.ifBlank { phone.brand }
                                    onQuickFilter?.invoke("MODEL", q)
                                }
                                .testTag("phone_model_click_${phone.id}")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Line 2: IMEI Number (e.g., IMEI: 358291048291023)
                        Text(
                            text = "IMEI: ${phone.imei.ifBlank { "N/A" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFCBD5E1) else Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Line 3: Color, Storage, Grade (e.g., Black | 256GB | Grade A)
                        val specsLine = formatPhoneSpecsLine(phone.color, phone.ramRom, phone.conditionGrade)
                        if (specsLine.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = specsLine,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Column: Status Badge with Dropdown for Admin + Entry Date
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            StatusBadge(
                                status = phone.status,
                                isAdmin = isAdmin,
                                onClick = if (isAdmin) { { showStatusDropdown = true } } else null
                            )

                            if (isAdmin) {
                                DropdownMenu(
                                    expanded = showStatusDropdown,
                                    onDismissRequest = { showStatusDropdown = false }
                                ) {
                                    Text(
                                        text = "Change Status (Admin)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    HorizontalDivider(color = Slate100)

                                    // Stock Option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (phone.status == PhoneStatus.SOLD.code) "🏬 Stock (Mark Return 🔄)" else "🏬 In Stock",
                                                fontWeight = if (phone.status == PhoneStatus.IN_STOCK.code) FontWeight.Bold else FontWeight.Normal,
                                                color = StockGreen
                                            )
                                        },
                                        onClick = {
                                            showStatusDropdown = false
                                            val target = if (phone.status == PhoneStatus.SOLD.code) PhoneStatus.RETURNED.code else PhoneStatus.IN_STOCK.code
                                            onStatusChange?.invoke(target)
                                        }
                                    )

                                    // Transit Option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "✈️ In Transit",
                                                fontWeight = if (phone.status == PhoneStatus.IN_TRANSIT.code) FontWeight.Bold else FontWeight.Normal,
                                                color = Color(0xFFB45309)
                                            )
                                        },
                                        onClick = {
                                            showStatusDropdown = false
                                            onStatusChange?.invoke(PhoneStatus.IN_TRANSIT.code)
                                        }
                                    )

                                    // Sold Option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "✅ Sold",
                                                fontWeight = if (phone.status == PhoneStatus.SOLD.code) FontWeight.Bold else FontWeight.Normal,
                                                color = Color(0xFF2563EB)
                                            )
                                        },
                                        onClick = {
                                            showStatusDropdown = false
                                            onStatusChange?.invoke(PhoneStatus.SOLD.code)
                                        }
                                    )

                                    // Return Option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "🔄 Return (Restock)",
                                                fontWeight = if (isReturned) FontWeight.Bold else FontWeight.Normal,
                                                color = Color(0xFF7E22CE)
                                            )
                                        },
                                        onClick = {
                                            showStatusDropdown = false
                                            onStatusChange?.invoke(PhoneStatus.RETURNED.code)
                                        }
                                    )
                                }
                            }
                        }

                        val entryDateToDisplay = if (phone.entryDate.isNotBlank()) phone.entryDate else DateUtils.getTodayStorageDate()
                        Surface(
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, statusPalette.cardBorder.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = onQuickFilter != null) {
                                    onQuickFilter?.invoke("STOCK_DATE", entryDateToDisplay)
                                }
                                .testTag("stock_date_click_${phone.id}")
                        ) {
                            Text(
                                text = DateUtils.formatStockDate(entryDateToDisplay),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Badges Section (Flex/Wrap Row): Battery Health, Condition, Variant, Accessories
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Battery Health Badge
                    if (phone.batteryHealth.isNotBlank()) {
                        Surface(
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.batteryHealth,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Item Condition Badge (Used, Intact, Brand New, Inactive) - directly beside Battery info
                    if (phone.itemCondition.isNotBlank()) {
                        Surface(
                            color = if (isDark) Color(0xFF14532D).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFBBF7D0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.itemCondition,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Region / Variant Badge
                    if (phone.variant.isNotBlank()) {
                        Surface(
                            color = if (isDark) Color(0xFF581C87).copy(alpha = 0.35f) else Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF9333EA).copy(alpha = 0.5f) else Color(0xFFE9D5FF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.variant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6B21A8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Packaging / Box Details Badge
                    if (phone.packaging.isNotBlank()) {
                        Surface(
                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFA7F3D0))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone.packaging,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Optional Notes line
                if (phone.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📝 ${phone.notes}",
                        fontSize = 11.sp,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. DISTINCT FOOTER SECTION: Price Info & Action Buttons
            Surface(
                color = statusPalette.footerBg,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                border = BorderStroke(1.dp, statusPalette.footerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Sold Customer & Date Card (Visible to All Roles: Admin & Salesmen)
                    if (isSold) {
                        Surface(
                            color = if (isDark) Color(0xFF141936) else Color(0xFFE5EDFF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3730A3).copy(alpha = 0.5f) else Color(0xFFC7D2FE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sold_customer_info_card_${phone.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val custNameVal = phone.customerName.trim().ifBlank { "Walk-in Customer" }
                                val mobileVal = phone.customerPhone.trim().ifBlank { "N/A" }
                                val soldDateToDisplay = phone.saleDate.ifBlank { phone.entryDate }

                                // Line 1: Customer Name (strictly left-aligned)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = onQuickFilter != null && phone.customerName.isNotBlank()) {
                                            onQuickFilter?.invoke("CUSTOMER", phone.customerName)
                                        }
                                        .testTag("customer_name_click_${phone.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Customer",
                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF3730A3),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Customer: $custNameVal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Line 2: Mobile Number on a NEW line directly below Customer Name formatted strictly as Mobile No: 01XXXXXXXXX
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = onQuickFilter != null && phone.customerPhone.isNotBlank()) {
                                            onQuickFilter?.invoke("CUSTOMER", phone.customerPhone)
                                        }
                                        .testTag("customer_phone_click_${phone.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Mobile No",
                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF3730A3),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mobile No: $mobileVal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4338CA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Line 3: Sold Date on a NEW line directly below Mobile Number formatted strictly as Sold Date: DD MMM YYYY (e.g., "Sold Date: 24 Sep 2026")
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = onQuickFilter != null && soldDateToDisplay.isNotBlank()) {
                                            onQuickFilter?.invoke("SALE_DATE", soldDateToDisplay)
                                        }
                                        .testTag("sold_date_click_${phone.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Sold Date",
                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF4338CA),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sold Date: ${DateUtils.formatSoldDateDisplay(soldDateToDisplay)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Price Row
                    if (isAdmin) {
                        // Admin Cost & Price Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Base Cost:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700,
                                    maxLines = 1
                                )
                                if (phone.totalCostBdt > 0) {
                                    Text(
                                        text = "৳ ${String.format("%,.0f", phone.totalCostBdt)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                        maxLines = 1
                                    )
                                    if (phone.buyingPriceRmb > 0) {
                                        Text(
                                            text = "(¥${phone.buyingPriceRmb.toInt()} + Cargo ৳${phone.shippingCostBdt.toInt()})",
                                            fontSize = 9.sp,
                                            color = Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Cost Pending ⚠️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706),
                                        maxLines = 1
                                    )
                                }
                            }

                            if (isSold) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Sold Price & Profit:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen,
                                        maxLines = 1
                                    )
                                    if (phone.askingPriceBdt > 0) {
                                        Text(
                                            text = "Asking: ৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            maxLines = 1
                                        )
                                    }
                                    if (phone.totalCostBdt > 0) {
                                        Text(
                                            text = "Profit: +৳${String.format("%,.0f", profit)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (profit >= 0) ProfitGreen else Color(0xFFDC2626),
                                            maxLines = 1
                                        )
                                    }
                                }
                            } else if (phone.askingPriceBdt > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Asking Price:",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                        maxLines = 1
                                    )
                                    if (phone.desiredProfitBdt > 0) {
                                        Text(
                                            text = "Margin: +৳${String.format("%,.0f", phone.desiredProfitBdt)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Staff View (Salesmen: Sumon, Deedat): Clean Asking / Sold Price (Cost Protected controls hidden)
                        if (isSold) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Sold Price:",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "৳ ${String.format("%,.0f", phone.sellingPriceBdt)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen,
                                        maxLines = 1
                                    )
                                }
                                if (phone.askingPriceBdt > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Asking Price:",
                                            fontSize = 10.sp,
                                            color = Slate700,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Asking Price:",
                                        fontSize = 10.sp,
                                        color = Slate700,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    if (phone.askingPriceBdt > 0) {
                                        Text(
                                            text = "৳ ${String.format("%,.0f", phone.askingPriceBdt)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                            maxLines = 1
                                        )
                                    } else {
                                        Text(
                                            text = "Price Pending (Ask Admin)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val showBottomActions = if (isInTransit) {
                        isAdmin
                    } else {
                        true
                    }

                    if (showBottomActions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = statusPalette.cardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons Row with Bottom-Right Alignment
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left action button (Sell Phone / Receive into stock / Cash memo)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(end = if (isAdmin) 84.dp else 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isInTransit) {
                                    if (isAdmin) {
                                        AppActionButton(
                                            text = "Transfer to Stock",
                                            icon = Icons.Default.Inventory2,
                                            gradientColors = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                                            onClick = onMarkInStockClick,
                                            enabled = true,
                                            testTag = "mark_stock_btn_${phone.id}"
                                        )
                                    }
                                } else if (!isSold) {
                                    AppActionButton(
                                        text = if (isReturned) "Sell Return Unit" else "Sell Phone",
                                        icon = Icons.Default.ShoppingCartCheckout,
                                        gradientColors = listOf(Color(0xFF0D9488), Color(0xFF10B981)),
                                        onClick = onSellClick,
                                        testTag = "sell_phone_btn_${phone.id}"
                                    )
                                } else {
                                    AppOutlinedActionButton(
                                        text = "Cash Memo",
                                        icon = Icons.Default.ReceiptLong,
                                        borderColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                        contentColor = if (isDark) Color(0xFF38BDF8) else PrimaryNavy,
                                        onClick = onViewMemoClick,
                                        testTag = "view_memo_btn_${phone.id}"
                                    )
                                }
                            }

                            // Modern Admin-Only Edit Button strictly positioned at bottom-right corner (Alignment.BottomEnd)
                            if (isAdmin) {
                                Box(
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    AdminCardEditButton(
                                        onClick = onEditCostsClick,
                                        testTag = "edit_costs_btn_${phone.id}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    isAdmin: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val (bgColor, textColor, text, borderColor) = when (status) {
        PhoneStatus.IN_STOCK.code, "STOCK" -> Quad(
            if (isDark) Color(0xFF14532D).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
            "Stock",
            if (isDark) Color(0xFF166534) else Color(0xFF86EFAC)
        )
        PhoneStatus.IN_TRANSIT.code, "TRANSIT" -> Quad(
            if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
            "Transit",
            if (isDark) Color(0xFFB45309) else Color(0xFFFDE68A)
        )
        PhoneStatus.SOLD.code, "SOLD" -> Quad(
            if (isDark) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFE0E7FF),
            if (isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3),
            "Sold",
            if (isDark) Color(0xFF4338CA) else Color(0xFFC7D2FE)
        )
        PhoneStatus.RETURNED.code, "RETURN", "RETURNED" -> Quad(
            if (isDark) Color(0xFF581C87).copy(alpha = 0.5f) else Color(0xFFF3E8FF),
            if (isDark) Color(0xFFD8B4FE) else Color(0xFF6B21A8),
            "Return",
            if (isDark) Color(0xFF7E22CE) else Color(0xFFD8B4FE)
        )
        else -> Quad(
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
            status.replace("IN_", "").lowercase().replaceFirstChar { it.uppercase() },
            if (isDark) Color(0xFF374151) else Color(0xFFCBD5E1)
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = (if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }).testTag("status_badge_${status.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PhoneGradeBadge(
    grade: String,
    modifier: Modifier = Modifier
) {
    val cleanGrade = grade.trim()
    if (cleanGrade.isBlank()) return

    val (bgColor, textColor) = getGradeColors(cleanGrade)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 0.5.dp,
        modifier = modifier.testTag("phone_grade_badge_${cleanGrade.lowercase().replace(" ", "_")}")
    ) {
        Text(
            text = cleanGrade,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun getGradeColors(grade: String): Pair<Color, Color> {
    val g = grade.trim().uppercase()
    return when {
        // Grade A+: Bright Green background
        g.contains("A+") -> Color(0xFF22C55E) to Color.White
        // Grade A-: Light/Sage Green background
        g.contains("A-") -> Color(0xFF86EFAC) to Color(0xFF14532D)
        // Grade A: Green background
        g == "GRADE A" || g == "A GRADE" || g == "A" || (g.contains("A") && !g.contains("B") && !g.contains("C")) -> Color(0xFF15803D) to Color.White
        // Grade B: Amber/Yellow background
        g.contains("B") -> Color(0xFFF59E0B) to Color.White
        // Grade C: Orange/Red background
        g.contains("C") -> Color(0xFFEF4444) to Color.White
        else -> Color(0xFF334155) to Color.White
    }
}

fun formatPhoneSpecsLine(color: String, ramRom: String, conditionGrade: String): String {
    val cleanColor = color.trim()

    val cleanRamRom = ramRom.trim()
    val formattedStorage = if (cleanRamRom.isNotBlank()) {
        val parts = cleanRamRom.split("/", "-", " ", "|").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val ram = parts[0].replace("GB", "", ignoreCase = true).trim()
            val rom = parts[1].replace("GB", "", ignoreCase = true).trim()
            if (ram.isNotEmpty() && rom.isNotEmpty()) {
                "$ram-${rom}GB"
            } else {
                cleanRamRom
            }
        } else {
            val digitsOnly = cleanRamRom.replace("GB", "", ignoreCase = true).trim()
            if (digitsOnly.isNotEmpty() && digitsOnly.all { it.isDigit() }) {
                "${digitsOnly}GB"
            } else {
                cleanRamRom
            }
        }
    } else {
        ""
    }

    val cleanGrade = if (conditionGrade.isNotBlank()) {
        val g = conditionGrade.trim()
        val withoutGradePrefix = g.replaceFirst(Regex("^(?i)Grade\\s*"), "").trim()
        if (withoutGradePrefix.isNotBlank()) withoutGradePrefix else g
    } else {
        ""
    }

    return when {
        cleanColor.isNotBlank() && formattedStorage.isNotBlank() && cleanGrade.isNotBlank() ->
            "$cleanColor । $formattedStorage। $cleanGrade"
        cleanColor.isNotBlank() && formattedStorage.isNotBlank() ->
            "$cleanColor । $formattedStorage"
        cleanColor.isNotBlank() && cleanGrade.isNotBlank() ->
            "$cleanColor । $cleanGrade"
        formattedStorage.isNotBlank() && cleanGrade.isNotBlank() ->
            "$formattedStorage। $cleanGrade"
        formattedStorage.isNotBlank() ->
            formattedStorage
        cleanColor.isNotBlank() ->
            cleanColor
        cleanGrade.isNotBlank() ->
            cleanGrade
        else -> ""
    }
}



